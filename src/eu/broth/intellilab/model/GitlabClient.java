package eu.broth.intellilab.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.messages.Topic;
import eu.broth.intellilab.IntelliLab;
import eu.broth.intellilab.util.Params;
import eu.broth.intellilab.util.RestUtil;
import org.jetbrains.annotations.Nullable;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.function.Consumer;

/**
 * @author Bastian Roth
 * @version 05.04.2014
 */
public class GitlabClient {
	public static final Topic<IssuesLoadedNotifier> ISSUES_LOADED_TOPIC = Topic.create("GitLab issues loaded", IssuesLoadedNotifier.class);

	private static final String API_SUFFIX = "/api/v3";

	private static final Map<Project, GitlabClient> CLIENTS = new HashMap<>();

	public static GitlabClient getInstance(Project intellijProject) {
		GitlabClient client = CLIENTS.get(intellijProject);
		if (client == null) {
			client = new GitlabClient(intellijProject);
			CLIENTS.put(intellijProject, client);
		}
		return client;
	}

	private final Project ijProject;
	private final IntelliLab lab;

	private String serverUrl = "http://localhost";
	private String token = "";
	private GitlabProject glProject;

	private List<GitlabIssue> issues = Collections.emptyList();
	private Map<Integer, GitlabIssue> issuesById;
	private GitlabIssue activeIssue;

	private final EntityManager em;

	private GitlabClient(Project intellijProject) {
		ijProject = intellijProject;
		lab = intellijProject.getComponent(IntelliLab.class);

		em = new EntityManager();
		loadConfiguration();

		lab.onTaskStateChanged((issueId, newState) -> {
			GitlabIssue issue = issuesById.get(issueId);
			switch (newState) {
				case OPEN:
					if (issue.isActive()) {
						// issue has been stopped externally
						performIssueTransition(issue, GitlabIssue.Transition.ACTIVE_TO_OPEN);
					}
					break;
				case ACTIVE:
					if (!issue.isActive()) {
						// issue has been started externally
						performIssueTransition(issue, GitlabIssue.Transition.OPEN_TO_ACTIVE);
					}
					break;
				case CLOSED:
					if (!issue.isClosed()) {
						// issue has been closed externally
						int result = Messages.showYesNoDialog("Local task associated with GitLab issue #" + issue.getLocalId() + " has currently been removed.\n\n" +
								"Do you want to close this issue as well?", "Close Issue?", Messages.getQuestionIcon());
						if (result == Messages.YES) {
							GitlabIssue.Transition transition = issue.isActive() ? GitlabIssue.Transition.ACTIVE_TO_CLOSE : GitlabIssue.Transition.OPEN_TO_CLOSE;
							performIssueTransition(issue, transition);
						} else {
							// at least, set issue's task to null
							issue.task = null;
						}
					}
					break;
			}
		});
	}

	private void loadConfiguration() {
		serverUrl = lab.getServerUrl();
		String encodedToken = lab.getToken();
		try {
			token = new String(Base64.getDecoder().decode(encodedToken), "utf-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		setProject(em.extractProject(lab.getProject()));
	}

	public String getServerUrl() {
		return serverUrl;
	}

	public void setServerUrl(String url) {
		this.serverUrl = url;
		lab.setServerUrl(url);
	}

	public String getPrivateToken() {
		return token;
	}

	public void setPrivateToken(String token) {
		this.token = token;
		try {
			String encodedToken = Base64.getEncoder().encodeToString(token.getBytes("utf-8"));
			lab.setToken(encodedToken);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean fetchProjects(Consumer<GitlabProject> processor) {
		return RestUtil.GET(serverUrl + API_SUFFIX + "/projects", token, "Loading projects ...", response -> {
			List<GitlabProject> projects = em.extractProjects(response);
			projects.forEach(processor::accept);
		});
	}

	private void setProject(GitlabProject project) {
		this.glProject = project;
		refreshIssues();
	}

	public GitlabProject getProject() {
		return glProject;
	}

	public boolean projectLoaded(GitlabProject project) {
		if (glProject == project)
			return true;
		if (glProject != null && glProject.equals(project))
			return true;
		return false;
	}

	public void loadProject(GitlabProject project) {
		if (!projectLoaded(project)) {
			// clear issue related stuff
			activeIssue = null;
			lab.clearAugmentations();

			lab.setProject(em.toJson(project));
			setProject(project);
		}
	}


	public void refreshIssues() {
		final IssuesLoadedNotifier notifier = ijProject.getMessageBus().syncPublisher(ISSUES_LOADED_TOPIC);
		if (glProject == null) {
			notifier.accept(null);
			return;
		}

		boolean issuesLoadSuccess = RestUtil.GET(buildUri(null), token, "Loading issues ...", response -> {
			issues = em.extractIssues(response);
			issuesById = new HashMap<>();
			issues.forEach((issue) -> {
				lab.augmentIssue(issue);
				if (issue.getState() == GitlabIssue.State.ACTIVE) {
					activeIssue = issue;
				}
				issuesById.put(issue.getId(), issue);
			});
			notifier.accept(issues);
		});

		// reload potential issue assignees (project members) as well
		if (issuesLoadSuccess) {
			glProject.clearMembers();
			boolean membersLoadSuccess = refreshMembers(false);
			if (membersLoadSuccess && glProject.getNamespace().isGroup()) {
				refreshMembers(true);
			}
		}
	}

	private boolean refreshMembers(boolean inGroup) {
		String typeName = inGroup ? "group" : "project";
		StringBuilder sb = new StringBuilder(serverUrl)
				.append(API_SUFFIX).append("/")
				.append(typeName).append("s/")
				.append(inGroup ? glProject.getNamespace().getId() : glProject.getId())
				.append("/members");
		boolean successful = RestUtil.GET(sb.toString(), token, "Loading " + typeName + " members ...", response -> {
			List<GitlabUser> users = em.extractUsers(response);
			users.forEach(glProject::addMember);
		});
		return successful;
	}

	public List<GitlabIssue> getIssues() {
		return issues;
	}

	public void performIssueTransition(GitlabIssue issue, GitlabIssue.Transition transition) {
		boolean openBefore = issue.isOpenOrActive();
		issue.performTransition(transition);
		boolean openAfter = issue.isOpenOrActive();
		if (openBefore != openAfter) {
			// only notify server if a relevant state change has occurred
			String uri = buildUri(issue);
			if (openAfter) {
				// reopen issue on server
				RestUtil.PUT(uri, token, new Params("state_event", "reopen"), "Reopening issue ...", null);
			} else {
				// close issue on server
				RestUtil.PUT(uri, token, new Params("state_event", "close"), "Closing issue ...", null);
			}
		}

		// remember activated issue
		if (issue.getState() == GitlabIssue.State.ACTIVE) {
			if (!issue.equals(activeIssue)) {
				if (activeIssue != null) {
					// stop current active issue
					performIssueTransition(activeIssue, GitlabIssue.Transition.ACTIVE_TO_OPEN);
				}
				activeIssue = issue;
			}
		} else {
			if (issue.equals(activeIssue)) {
				activeIssue = null;
			}
		}

		// process state changes
		switch (issue.getState()) {
			case OPEN:
				lab.issueStopped(issue);
				break;
			case ACTIVE:
				lab.issueActivated(issue);
				break;
			case CLOSED:
				lab.issueClosed(issue);
				break;
		}
	}

	public void modifyIssue(GitlabIssue issue, boolean bug, String summary, String description, String labels, GitlabUser assignee) {
		Params msg = new Params();

		if (!issue.getSummary().equals(summary))
			msg.add("title", summary);
		if (!description.equals(issue.getDescription()))
			msg.add("description", description);

		String oldLabels = GitlabIssue.getCompleteLabelsText(issue.getLabels(), issue.isBug());
		String newLabels = GitlabIssue.getCompleteLabelsText(labels, bug);
		if (!oldLabels.equals(newLabels))
			if ("".equals(newLabels))
				newLabels = "\"\"";
			msg.add("labels", newLabels);

		if (issue.getAssignedTo() != assignee) {
			int assigneeId = assignee == null ? -1 : assignee.getId();
			msg.add("assignee_id", String.valueOf(assigneeId));
		}

		RestUtil.PUT(buildUri(issue), token, msg, "Updating issue ...", response -> {
			em.extractIssue(response);
		});
	}

	public void createIssue(boolean bug, String summary, String description, String labels, GitlabUser assignee) {
		Params msg = new Params();
		msg.add("title", summary);
		msg.add("description", description);

		String completeLabels = GitlabIssue.getCompleteLabelsText(labels, bug);
		if (!completeLabels.isEmpty()) {
			msg.add("labels", completeLabels);
		}

		if (assignee != null) {
			msg.add("assignee_id", String.valueOf(assignee.getId()));
		}

		RestUtil.POST(buildUri(null), token, msg, "Creating issue ...", response -> {
			GitlabIssue issue = em.extractIssue(response);
			issues.add(0, issue);
			issuesById.put(issue.getId(), issue);

			IssuesLoadedNotifier notifier = ijProject.getMessageBus().syncPublisher(ISSUES_LOADED_TOPIC);
			notifier.accept(issues);
		});
	}


	private String buildUri(@Nullable GitlabIssue issue) {
		StringBuilder sb = new StringBuilder(serverUrl)
				.append(API_SUFFIX)
				.append("/projects/")
				.append(glProject.getId())
				.append("/issues");
		if (issue == null) {
			sb.append("?per_page=100");
		} else {
			sb.append("/").append(issue.getId());
		}
		return sb.toString();
	}


	@FunctionalInterface
	public static interface IssuesLoadedNotifier extends Consumer<List<GitlabIssue>> {
	}
}
