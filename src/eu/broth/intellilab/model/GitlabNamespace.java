package eu.broth.intellilab.model;

import com.google.gson.annotations.SerializedName;

/**
 * @author Bastian Roth
 * @version 01.05.2014
 */
public class GitlabNamespace extends GitlabEntity<GitlabNamespace> {

	@SerializedName("owner_id")
	private int ownerId;

	public boolean isGroup() {
		return id != ownerId;
	}

	@Override
	void merge(GitlabNamespace other) {
		this.ownerId = other.ownerId;
	}
}
