package eu.broth.intellilab.model;

/**
 * @author Bastian Roth
 * @version 09.04.2014
 */
public class GitlabUser extends GitlabEntity<GitlabUser> {

	private String username;

	private String name;

	public String getUsername() {
		return username;
	}

	public String getName() {
		return name;
	}

	@Override
	void merge(GitlabUser other) {
		username = other.username;
		name = other.name;
	}

	@Override
	public String toString() {
		return name;
	}
}
