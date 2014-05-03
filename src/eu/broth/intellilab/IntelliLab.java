package eu.broth.intellilab;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.tasks.LocalTask;
import com.intellij.tasks.TaskListener;
import com.intellij.tasks.TaskListenerAdapter;
import com.intellij.tasks.TaskManager;
import eu.broth.intellilab.model.GitlabIssue;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;

/**
 * @author Bastian Roth
 * @version 15.04.2014
 */
@State(
		name = "IntelliLab",
		storages = {
				@Storage(file = StoragePathMacros.WORKSPACE_FILE)
		}
)
public class IntelliLab extends AbstractProjectComponent implements PersistentStateComponent<IntelliLab.LabStore> {

	private LabStore store;

	private TaskManager manager;

	private TaskListener taskListener;
	private Collection<TaskStateListener> listeners = new HashSet<>();

	IntelliLab(Project project) {
		super(project);
		store = new LabStore();
		manager = TaskManager.getManager(project);

		taskListener = new TaskListenerAdapter() {
			@Override
			public void taskActivated(LocalTask task) {
				String taskId = task.getId();
				store.issues.entrySet().stream().filter(entry -> taskId.equals(entry.getValue())).forEach(entry -> {
					listeners.forEach(l -> l.taskChanged(entry.getKey(), GitlabIssue.State.ACTIVE));
				});
			}

			@Override
			public void taskDeactivated(LocalTask task) {
				String taskId = task.getId();
				store.issues.entrySet().stream().filter(entry -> taskId.equals(entry.getValue())).forEach(entry -> {
					listeners.forEach(l -> l.taskChanged(entry.getKey(), GitlabIssue.State.OPEN));
				});
			}

			@Override
			public void taskRemoved(LocalTask task) {
				String taskId = task.getId();
				// create a copy of the entry set to avoid concurrent modification exception
				Set<Map.Entry<Integer, String>> entries = new HashSet<>(store.issues.entrySet());
				entries.stream().filter(entry -> taskId.equals(entry.getValue())).forEach(entry -> {
					Integer id = entry.getKey();
					listeners.forEach(l -> l.taskChanged(id, GitlabIssue.State.CLOSED));

					// clear issue task association
					store.issues.remove(id);
				});
			}
		};
		manager.addTaskListener(taskListener);
	}

	@Nullable
	@Override
	public LabStore getState() {
		return store;
	}

	@Override
	public void loadState(LabStore state) {
		store = state;
	}

	public void setServerUrl(String url) {
		store.serverUrl = url;
	}

	public String getServerUrl() {
		return store.serverUrl;
	}

	public void setToken(String token) {
		store.token = token;
	}

	public String getToken() {
		return store.token;
	}

	public void setProject(String json) {
		store.project = json;
	}

	public String getProject() {
		return store.project;
	}

	public void issueActivated(GitlabIssue issue) {
		LocalTask task = issue.getTask();
		if (task == null) {
			// create and associate task
			task = manager.createLocalTask("#" + issue.getLocalId() + ": " + issue.getSummary());
			setValue(issue, "task", task);

			// persist task issue association
			store.issues.put(issue.getId(), task.getId());
		}
		manager.activateTask(task, true);
	}

	public void issueStopped(GitlabIssue issue) {
		LocalTask task = issue.getTask();
		if (task != null) {
			// activate default task
			LocalTask defaultTask = manager.getLocalTasks().get(0);
			manager.activateTask(defaultTask, true);
		}
	}

	public void issueClosed(GitlabIssue issue) {
		LocalTask task = issue.getTask();
		if (task != null) {
			// remove association between issue and task and drop the latter one
			manager.removeTask(issue.getTask());
			setValue(issue, "task", null);

			// remove augmentation data for given issue
			store.issues.remove(issue.getId());
		}
	}

	public void clearAugmentations() {
		store.issues.clear();
	}

	public void augmentIssue(GitlabIssue issue) {
		String taskId = store.issues.get(issue.getId());
		if (taskId != null) {
			LocalTask task = manager.findTask(taskId);
			setValue(issue, "task", task);
			if (task == null) {
				// task has been removed externally, so drop task issue association as well
				store.issues.remove(issue.getId());
				return;
			}
			GitlabIssue.State state = task.isActive() ? GitlabIssue.State.ACTIVE : GitlabIssue.State.OPEN;
			setValue(issue, "state", state.toString());
		}
	}

	public void onTaskStateChanged(TaskStateListener listener) {
		listeners.add(listener);
	}

	private void setValue(GitlabIssue issue, String fieldName, Object value) {
		try {
			Field field = issue.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(issue, value);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	public static class LabStore {
		public String serverUrl = "http://localhost";
		public String token = "";
		public String project = "";
		public Map<Integer, String> issues = new HashMap<>();

		public LabStore() {
		}
	}

	public static interface TaskStateListener {
		void taskChanged(int issueId, GitlabIssue.State newState);
	}
}
