package eu.broth.intellilab.ui.settings;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.DocumentAdapter;
import eu.broth.intellilab.model.GitlabClient;
import eu.broth.intellilab.model.GitlabIssue;
import eu.broth.intellilab.model.GitlabProject;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author Bastian Roth
 * @version 04.04.2014
 */
public class GitlabConfigurationUI implements Disposable {
	@Nls
	private static final String NO_PROJECTS_MSG = "Before selecting any project is possible, load available projects first";
	@Nls
	private static final String NO_PROJECT_ITEM = "<no project>";

	private JPanel panel;
	private JTextField urlField;
	private JPasswordField tokenField;
	private JButton loadButton;
	private JComboBox projectsCombo;

	private GitlabProject selectedProject;

	private final GitlabClient client;

	public GitlabConfigurationUI(Project project) {
		client = GitlabClient.getInstance(project);
		reset();
		emptyComboBoxModel();
		loadButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (isModified()) {
					int result = Messages.showYesNoCancelDialog("Your settings have been changed. Do you want to apply them before loading project?\n" +
									"Otherwise, former settings are used.",
							"Settings not applied", Messages.getQuestionIcon());
					switch (result) {
						case Messages.YES: apply(); break;
						case Messages.CANCEL: return;
					}
				}
				MutableComboBoxModel cbModel = new DefaultComboBoxModel();
				cbModel.addElement(NO_PROJECT_ITEM);
				if (client.fetchProjects(cbModel::addElement)) {
					projectsCombo.setModel(cbModel);
					if (selectedProject != null) {
						projectsCombo.setSelectedItem(selectedProject);
					}
					projectsCombo.setEnabled(true);
				} else {
					// something went wrong during fetching available projects
					selectedProject = null;
					emptyComboBoxModel();
				}
			}
		});

		DocumentListener docListener = new DocumentAdapter() {
			@Override
			protected void textChanged(DocumentEvent e) {
				if (isModified()) {
					selectedProject = null;
				} else {
					selectedProject = client.getProject();
				}
				emptyComboBoxModel();
			}
		};
		urlField.getDocument().addDocumentListener(docListener);
		tokenField.getDocument().addDocumentListener(docListener);

		projectsCombo.addActionListener(event -> {
			Object item = projectsCombo.getSelectedItem();
			if (item instanceof GitlabProject) {
				this.selectedProject = (GitlabProject) item;
			} else {
				this.selectedProject = null;
			}
		});
	}

	private void emptyComboBoxModel() {
		DefaultComboBoxModel<Object> cbModel = new DefaultComboBoxModel<>();
		if (selectedProject != null) {
			cbModel.addElement(selectedProject);
		} else {
			cbModel.addElement(NO_PROJECT_ITEM);
		}
		projectsCombo.setModel(cbModel);
		projectsCombo.setEnabled(false);
		projectsCombo.setToolTipText(NO_PROJECTS_MSG);
	}

	public JPanel getPanel() {
		return panel;
	}

	public boolean isModified() {
		boolean projectChanged = !client.projectLoaded(selectedProject);
		return !urlField.getText().equals(client.getServerUrl()) ||
				!String.valueOf(tokenField.getPassword()).equals(client.getPrivateToken()) ||
				projectChanged;
	}

	public void apply() {
		boolean urlChanged = !client.getServerUrl().equals(urlField.getText());
		boolean projectChanged = !client.projectLoaded(selectedProject);
		if (urlChanged || projectChanged) {
			// show warning if at least one issue is associated with an task
			boolean associated = false;
			for (GitlabIssue issue : client.getIssues()) {
				if (issue.getTask() != null) {
					associated = true;
					break;
				}
			}
			if (associated) {
				int result = Messages.showYesNoDialog("There is at least one issue associated with a local task. " +
						"All such associations will get lost irreversibly if you confirm!\n\n" +
						"Do you really want to continue?", "Task associations will get lost", Messages.getWarningIcon());
				if (result != Messages.YES)
					return;
			}
		}
		client.setServerUrl(urlField.getText());
		client.setPrivateToken(String.valueOf(tokenField.getPassword()));
		client.loadProject(selectedProject);
	}

	public void reset() {
		urlField.setText(client.getServerUrl());
		tokenField.setText(client.getPrivateToken());
		selectedProject = client.getProject();
		emptyComboBoxModel();
	}

	@Override
	public void dispose() {
	}
}
