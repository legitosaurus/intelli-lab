package eu.broth.intellilab.ui;

import com.intellij.ui.table.JBTable;
import eu.broth.intellilab.model.GitlabClient;
import eu.broth.intellilab.model.GitlabIssue;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.util.Collections;
import java.util.List;

/**
 * @author Bastian Roth
 * @version 08.04.2014
 */
public class IssuesTable extends JBTable {

	private GitlabClient client;

	private IssuesTableModel model;

	public IssuesTable() {
		setDefaultRenderer(GitlabIssue.class, new IssueRenderer());
	}

	public void setClient(GitlabClient client) {
		this.client = client;
	}

	public void setIssues(List<GitlabIssue> issues) {
		if (model == null) {
			model = new IssuesTableModel();
			setModel(model);

			TableColumnModel columnModel = getColumnModel();
			columnModel.getColumn(0).setMaxWidth(80);
			columnModel.getColumn(1).setPreferredWidth(350);
			columnModel.getColumn(2).setPreferredWidth(150);
			columnModel.getColumn(3).setPreferredWidth(80);
			columnModel.getColumn(4).setPreferredWidth(80);

			TableColumn stateColumn = columnModel.getColumn(5);
			stateColumn.setCellEditor(new IssueStateEditor());
			stateColumn.setMinWidth(80);
			stateColumn.setMaxWidth(80);
		} else {
			model.setIssues(issues);
		}
	}

	private class IssuesTableModel extends AbstractTableModel {

		final String[] COLS = new String[]{"ID", "Summary", "Labels", "Assigned to", "Created by", ""};

		private List<GitlabIssue> issues;

		public IssuesTableModel() {
			this.issues = Collections.emptyList();
		}

		public void setIssues(List<GitlabIssue> issues) {
			if (issues == null)
				this.issues = Collections.emptyList();
			else
				this.issues = issues;
			fireTableDataChanged();
		}

		@Override
		public int getRowCount() {
			return issues.size();
		}

		@Override
		public int getColumnCount() {
			return COLS.length;
		}

		@Override
		public String getColumnName(int column) {
			return COLS[column];
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			return issues.get(rowIndex);
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return GitlabIssue.class;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return columnIndex == 5;
		}

		@Override
		public void setValueAt(Object value, int rowIndex, int columnIndex) {
			if (value instanceof GitlabIssue.Transition) {
				GitlabIssue.Transition transition = (GitlabIssue.Transition) value;
				GitlabIssue issue = issues.get(rowIndex);
				client.performIssueTransition(issue, transition);
			}
		}
	}

}
