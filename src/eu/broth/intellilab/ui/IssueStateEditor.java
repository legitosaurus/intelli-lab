package eu.broth.intellilab.ui;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.JBColor;
import eu.broth.intellilab.model.GitlabIssue;

import javax.swing.*;
import java.awt.*;

/**
 * @author Bastian Roth
 * @version 13.04.2014
 */
class IssueStateEditor extends DefaultCellEditor {

	private final ComboBox combo;

	public IssueStateEditor() {
		super(new ComboBox());
		combo = (ComboBox) getComponent();
		combo.setRenderer(new StateChangeRenderer());
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		GitlabIssue issue = (GitlabIssue) value;
		combo.removeAllItems();
		combo.addItem(null);
		issue.getPossibleTransitions().forEach(combo::addItem);
		return super.getTableCellEditorComponent(table, value, isSelected, row, column);
	}

	private static class StateChangeRenderer extends DefaultListCellRenderer {
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (value == null) {
				label.setText("<Leave>");
				label.setForeground(JBColor.foreground().darker());
			} else {
				label.setText(value.toString());
				label.setForeground(JBColor.foreground());
			}
			return label;
		}
	}
}
