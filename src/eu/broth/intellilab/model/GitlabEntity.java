package eu.broth.intellilab.model;

/**
 * @author Bastian Roth
 * @version 13.04.2014
 */
public abstract class GitlabEntity<GE> {

	protected int id;

	public int getId() {
		return id;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		GitlabEntity that = (GitlabEntity) o;

		if (id != that.id) return false;

		return true;
	}

	abstract void merge(GE other);

	@Override
	public int hashCode() {
		return id;
	}
}
