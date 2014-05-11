package eu.broth.intellilab.ui;

import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import eu.broth.intellilab.model.GitlabIssue;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * @author Bastian Roth
 * @version 13.04.2014
 */
class IssueRenderer extends DefaultTableCellRenderer {



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

		boolean dark = UIUtil.isUnderDarcula();
		Color textColor = JBColor.foreground();
		Color bugColor = JBColor.red;
		if (issue.isClosed()) {
			textColor = dark ? textColor.darker() : ColorUtil.mix(textColor, JBColor.white, 0.4);
			bugColor = dark ? bugColor.darker() : ColorUtil.mix(bugColor, JBColor.white, 0.4);
		}
		if (issue.getState() == GitlabIssue.State.ACTIVE) {
			textColor = ColorUtil.mix(textColor, JBColor.green, 0.6);
		} else if (issue.isBug()) {
			textColor = ColorUtil.mix(textColor, bugColor, 0.7);
		}
		if (isSelected) {
			textColor = dark ? textColor.brighter() : textColor.darker().darker();
		}
		label.setForeground(textColor);

		if (column == 0) {
			if (issue.isBug()) {
				if (issue.isClosed())
					label.setIcon(Icons.BUG_CLOSED);
				else
					label.setIcon(Icons.BUG_OPEN);
			}
			else {
				if (issue.isClosed())
					label.setIcon(Icons.TASK_CLOSED);
				else
					label.setIcon(Icons.TASK_OPEN);
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