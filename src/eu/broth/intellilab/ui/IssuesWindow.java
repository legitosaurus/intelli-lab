package eu.broth.intellilab.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.util.messages.MessageBus;
import eu.broth.intellilab.model.GitlabClient;
import eu.broth.intellilab.model.GitlabIssue;
import eu.broth.intellilab.ui.editing.EditIssueDialog;
import eu.broth.intellilab.ui.settings.GitlabConfigurable;

import javax.swing.*;

/**
 * @author Bastian Roth
 * @version 02.04.2014
 */
public class IssuesWindow extends SimpleToolWindowPanel {
	private JPanel rootPanel;
	private IssuesTable table;

	private Project project;
	private GitlabClient client;

	public IssuesWindow() {
		super(false, true);
	}

	void init(Project project) {
		this.project = project;
		initObservation(project.getMessageBus());
		client = GitlabClient.getInstance(project);

		setContent(rootPanel);
		setToolbar(createToolbar().getComponent());

		table.setClient(client);
		table.setIssues(client.getIssues());
	}

	private void initObservation(MessageBus bus) {
		bus.connect().subscribe(GitlabClient.ISSUES_LOADED_TOPIC, issues -> {
			SwingUtilities.invokeLater(() -> table.setIssues(issues));
		});
	}


	private ActionToolbar createToolbar() {
		DefaultActionGroup group = new DefaultActionGroup();
		group.add(new CreateIssue());
		group.add(new EditIssue());
		group.addSeparator();
		group.add(new RefreshIssues());
		group.addSeparator();
		group.add(new EditGitlabSettings());

		return ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, group, false);
	}

	private class EditGitlabSettings extends DumbAwareAction {

		private EditGitlabSettings() {
			super("GitLab settings", "Edit GitLab settings", AllIcons.General.Settings);
		}

		@Override
		public void actionPerformed(AnActionEvent e) {
			ShowSettingsUtil.getInstance().editConfigurable(project, new GitlabConfigurable(project));
		}
	}

	private class RefreshIssues extends DumbAwareAction {

		private RefreshIssues() {
			super("Refresh issues", "Refresh issues for loaded GitLab project", AllIcons.Actions.Refresh);
		}

		@Override
		public void actionPerformed(AnActionEvent e) {
			client.refreshIssues();
		}

		@Override
		public void update(AnActionEvent e) {
			e.getPresentation().setEnabled(client.getProject() != null);
		}
	}

	private class EditIssue extends DumbAwareAction {

		private EditIssue() {
			super("Edit issue", "Edit currently selected issue", AllIcons.Actions.Edit);
		}

		@Override
		public void actionPerformed(AnActionEvent e) {
			int row = table.getSelectedRow();
			if (row < 0)
				return;

			GitlabIssue issue = client.getIssues().get(row);
			EditIssueDialog dialog = new EditIssueDialog(project, issue);
			dialog.show();
		}

		@Override
		public void update(AnActionEvent e) {
			e.getPresentation().setEnabled(table.getSelectedRow() >= 0);
		}
	}

	private class CreateIssue extends DumbAwareAction {

		private CreateIssue() {
			super("Create issue", "Create a new issue", AllIcons.General.Add);
		}

		@Override
		public void actionPerformed(AnActionEvent e) {
			EditIssueDialog dialog = new EditIssueDialog(project, null);
			dialog.show();
		}
	}
}
