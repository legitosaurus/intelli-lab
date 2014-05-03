package eu.broth.intellilab.ui.editing;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import eu.broth.intellilab.model.GitlabClient;
import eu.broth.intellilab.model.GitlabIssue;
import eu.broth.intellilab.model.GitlabProject;
import eu.broth.intellilab.model.GitlabUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class EditIssueDialog extends DialogWrapper {
	private JPanel contentPane;
	private JLabel idLabel;
	private JTextField summaryField;
	private JCheckBox bugCB;
	private JTextField labelsField;
	private JLabel createdByLabel;
	private JComboBox assignedToCombo;
	private JTextArea descriptionText;

	private GitlabClient client;
	private GitlabIssue issue;

	public EditIssueDialog(@NotNull Project project, @Nullable GitlabIssue issue) {
		super(project);
		this.client = GitlabClient.getInstance(project);
		this.issue = issue;

		init();

		bugCB.addItemListener(e -> {
			if (bugCB.isSelected()) {
				bugCB.setForeground(JBColor.red);
			} else {
				bugCB.setForeground(JBColor.foreground());
			}
		});

		prepareAssigneeCombo();

		if (issue == null) {
			setTitle("New issue");
		} else {
			setTitle("Edit issue #" + issue.getLocalId());

			String hexColor = ColorUtil.toHex(UIUtil.getLabelDisabledForeground());
			idLabel.setText("<html>#" + issue.getLocalId() + " &nbsp;&nbsp;<font color=\"" + hexColor + "\">(global: " + issue.getId() + ")</font></html>");
			bugCB.setSelected(issue.isBug());
			summaryField.setText(issue.getSummary());
			labelsField.setText(issue.getLabelsText());
			createdByLabel.setText(issue.getCreatedBy().getName());
			descriptionText.setText(issue.getDescription());

			if (issue.getAssignedTo() == null) {
				assignedToCombo.setSelectedIndex(0);
			} else {
				assignedToCombo.setSelectedItem(issue.getAssignedTo());
			}
		}
	}

	private void prepareAssigneeCombo() {
		MutableComboBoxModel cbModel = new DefaultComboBoxModel();
		cbModel.addElement("<none>");
		GitlabProject glProject = client.getProject();
		glProject.getMembers().forEach(cbModel::addElement);
		assignedToCombo.setModel(cbModel);
	}

	@Nullable
	@Override
	protected JComponent createCenterPanel() {
		return contentPane;
	}

	@Override
	protected void doOKAction() {
		Object assignee = assignedToCombo.getSelectedItem();
		if (!(assignee instanceof GitlabUser)) {
			assignee = null;
		}

		if (issue != null) {
			// update issue
			client.modifyIssue(issue, bugCB.isSelected(), summaryField.getText(), descriptionText.getText(),
					labelsField.getText(), (GitlabUser) assignee);
		} else {
			// create new issue
			client.createIssue(bugCB.isSelected(), summaryField.getText(), descriptionText.getText(),
					labelsField.getText(), (GitlabUser) assignee);
		}
		super.doOKAction();
	}

	@Nullable
	@Override
	protected ValidationInfo doValidate() {
		// summary field must contain at least three characters
		String summary = summaryField.getText().trim();
		if (summary.length() < 3) {
			return new ValidationInfo("Summary must consist of at least three (non-whitespace) characters.", summaryField);
		}
		return super.doValidate();
	}
}
