package eu.broth.intellilab.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.intellij.tasks.LocalTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Bastian Roth
 * @version 09.04.2014
 */
public class GitlabIssue extends GitlabEntity<GitlabIssue> {

	private static final String BUG = "bug";

	@SerializedName("iid")
	private int localId;

	private String state;

	@SerializedName("title")
	private String summary;

	private String[] labels;

	@SerializedName("assignee")
	private GitlabUser assignedTo;

	@SerializedName("author")
	private GitlabUser createdBy;

	private String description;

	@Expose
	private boolean labelsCleaned;

	@Expose
	private boolean bug;

	@Expose
	LocalTask task;

	public int getLocalId() {
		return localId;
	}

	public String getSummary() {
		return summary;
	}


	public State getState() {
		return State.get(state);
	}

	private void setState(State state) {
		this.state = state.toString();
	}

	public boolean isOpenOrActive() {
		return !isClosed();
	}

	public boolean isActive() {
		return getState() == State.ACTIVE;
	}

	public boolean isClosed() {
		return getState() == State.CLOSED;
	}

	void performTransition(Transition transition) {
		switch (transition) {
			case OPEN_TO_ACTIVE:
				setState(State.ACTIVE);
				break;
			case OPEN_TO_CLOSE:
				setState(State.CLOSED);
				break;
			case ACTIVE_TO_OPEN:
				setState(State.OPEN);
				break;
			case ACTIVE_TO_CLOSE:
				setState(State.CLOSED);
				break;
			case CLOSE_TO_OPEN:
				setState(State.OPEN);
				break;
			case CLOSE_TO_ACTIVE:
				setState(State.ACTIVE);
				break;
		}
	}

	public List<Transition> getPossibleTransitions() {
		List<Transition> transitions = new ArrayList<>(2);
		switch (State.get(state)) {
			case OPEN: {
				transitions.add(Transition.OPEN_TO_ACTIVE);
				transitions.add(Transition.OPEN_TO_CLOSE);
				break;
			}
			case ACTIVE: {
				transitions.add(Transition.ACTIVE_TO_CLOSE);
				transitions.add(Transition.ACTIVE_TO_OPEN);
				break;
			}
			case CLOSED: {
				transitions.add(Transition.CLOSE_TO_OPEN);
				transitions.add(Transition.CLOSE_TO_ACTIVE);
				break;
			}
		}
		return transitions;
	}


	public boolean isBug() {
		cleanUpLabels();
		return bug;
	}

	public String[] getLabels() {
		cleanUpLabels();
		return labels;
	}

	public String getLabelsText() {
		return getLabelsText(Arrays.asList(getLabels()));
	}

	private void cleanUpLabels() {
		if (!labelsCleaned) {
			labelsCleaned = true;
			List result = new LinkedList();
			for (String label : labels) {
				if (!BUG.equals(label)) {
					result.add(label);
				} else {
					bug = true;
				}
			}
			labels = (String[]) result.toArray(new String[0]);
		}
	}

	public GitlabUser getAssignedTo() {
		return assignedTo;
	}

	public GitlabUser getCreatedBy() {
		return createdBy;
	}

	public String getDescription() {
		return description;
	}

	public LocalTask getTask() {
		return task;
	}

	@Override
	void merge(GitlabIssue other) {
		if (State.get(state) != State.ACTIVE) {
			state = other.state;
		}
		summary = other.summary;
		description = other.description;
		labels = other.labels;
		labelsCleaned = false;
		bug = other.isBug();
		assignedTo = other.assignedTo;
		createdBy = other.createdBy;
	}

	@Override
	public String toString() {
		return "Issue #" + id + " (" + summary + ")";
	}


	public static enum State {
		OPEN("open"), ACTIVE("active"), CLOSED("closed");

		private String text;

		State(String text) {
			this.text = text;
		}

		@Override
		public String toString() {
			return text;
		}

		public static State get(String text) {
			for (State state : values()) {
				if (state.toString().equals(text))
					return state;
			}
			return OPEN;
		}
	}


	public static enum Transition {
		OPEN_TO_ACTIVE("Start"), OPEN_TO_CLOSE("Close"),
		ACTIVE_TO_OPEN("Stop"), ACTIVE_TO_CLOSE("Stop & Close"),
		CLOSE_TO_OPEN("Reopen"), CLOSE_TO_ACTIVE("Reopen & Start");

		private String text;

		Transition(String text) {
			this.text = text;
		}

		@Override
		public String toString() {
			return text;
		}
	}


	public static String getCompleteLabelsText(String labels, boolean bug) {
		labels = labels.trim();
		if (bug) {
			return labels.isEmpty() ? "bug" : "bug, " + labels;
		}
		return labels;
	}

	public static String getCompleteLabelsText(String[] labels, boolean bug) {
		List<String> list = new ArrayList<>(Arrays.asList(labels));
		if (bug) {
			list.add(0, BUG);
		}
		return getLabelsText(list);
	}

	public static String getLabelsText(List<String> labels) {
		return labels.stream().reduce("", (lab1, lab2) -> {
			if (!lab1.isEmpty()) {
				return lab1 + ", " + lab2;
			}
			return lab2;
		});
	}
}
