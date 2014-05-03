package eu.broth.intellilab.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Bastian Roth
 * @version 02.04.2014
 */
public class GitlabProject extends GitlabEntity<GitlabProject> {

	@SerializedName("name_with_namespace")
	private String fullName;

	private GitlabNamespace namespace;

	@Expose
	private List<GitlabUser> members = new LinkedList<>();

	public GitlabProject() {
	}

	public GitlabProject(int id, String fullName) {
		this.id = id;
		this.fullName = fullName;
	}

	public String getFullName() {
		return fullName;
	}

	public GitlabNamespace getNamespace() {
		return namespace;
	}

	public List<GitlabUser> getMembers() {
		return Collections.unmodifiableList(members);
	}

	void clearMembers() {
		members.clear();
	}

	void addMember(GitlabUser user) {
		members.add(user);
	}

	@Override
	void merge(GitlabProject other) {
		fullName = other.fullName;
	}

	@Override
	public String toString() {
		return getFullName();
	}
}
