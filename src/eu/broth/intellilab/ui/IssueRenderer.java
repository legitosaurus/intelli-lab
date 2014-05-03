package eu.broth.intellilab.ui;

import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import eu.broth.intellilab.model.GitlabIssue;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * @author Bastian Roth
 * @version 13.04.2014
 */
class IssueRenderer extends DefaultTableCellRenderer {

	private static final ClassLoader LOADER = IssueRenderer.class.getClassLoader();
	private static final Icon TASK_OPEN = new ImageIcon(LOADER.getResource("task-open.png"), "Task");
	private static final Icon TASK_CLOSED = new ImageIcon(LOADER.getResource("task-closed.png"), "Task closed");
	private static final Icon BUG_OPEN = new ImageIcon(LOADER.getResource("bug-open.png"), "Bug");
	private static final Icon BUG_CLOSED = new ImageIcon(LOADER.getResource("bug-closed.png"), "Bug closed");

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
		JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		if (value instanceof GitlabIssue) {
			GitlabIssue issue = (GitlabIssue) value;
			adaptLabel(label, issue, column, isSelected);
		}
		return label;
	}

	private void adaptLabel(JLabel label, GitlabIssue issue, int column, boolean isSelected) {
		label.setText(getText(issue, column));

		Color textColor = JBColor.foreground();
		Color errorColor = JBColor.red;
		if (issue.isClosed()) {
			textColor = textColor.darker();
			errorColor = errorColor.darker();
		}
		if (issue.getState() == GitlabIssue.State.ACTIVE) {
			textColor = ColorUtil.mix(textColor, JBColor.green, 0.6);
		} else if (issue.isBug()) {
			textColor = ColorUtil.mix(textColor, errorColor, 0.7);
		}
		if (isSelected) {
			textColor = textColor.brighter();
		}
		label.setForeground(textColor);

		if (column == 0) {
			if (issue.isBug()) {
				if (issue.isClosed())
					label.setIcon(BUG_CLOSED);
				else
					label.setIcon(BUG_OPEN);
			}
			else {
				if (issue.isClosed())
					label.setIcon(TASK_CLOSED);
				else
					label.setIcon(TASK_OPEN);
			}
		} else {
			label.setIcon(null);
		}
		label.setBorder(BorderFactory.createEmptyBorder(5, 2, 5, 2));
	}

	private String getText(GitlabIssue issue, int columnIndex) {
		switch (columnIndex) {
			case 0: {
				return "#" + issue.getLocalId();
			}
			case 1: {
				return issue.getSummary();
			}
			case 2: {
				return issue.getLabelsText();
			}
			case 3: {
				return issue.getAssignedTo() != null ? issue.getAssignedTo().getName() : null;
			}
			case 4: {
				return issue.getCreatedBy().getName();
			}
			case 5: {
				return issue.getState().toString();
			}
		}
		return "";
	}
}