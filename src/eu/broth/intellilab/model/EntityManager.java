package eu.broth.intellilab.model;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Bastian Roth
 * @version 13.04.2014
 */
class EntityManager {

	public final String JS_NULL = "";

	private Gson gs;
	private Map<String, GitlabEntity> entities;

	public EntityManager() {
		reset();
	}

	private void reset() {
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeHierarchyAdapter(GitlabEntity.class, new CachingDeserializer(true));
		gs = builder.create();
		entities = new HashMap<>();
	}

	public String toJson(Object issue) {
		if (issue == null)
			return JS_NULL;
		return gs.toJson(issue);
	}

	public GitlabProject extractProject(String json) {
		reset();
		return gs.fromJson(json, GitlabProject.class);
	}

	public List<GitlabProject> extractProjects(String json) {
		reset();
		return gs.fromJson(json, LIST_TYPES.get(GitlabProject.class));
	}

	public GitlabIssue extractIssue(String json) {
		return  gs.fromJson(json, GitlabIssue.class);
	}

	public List<GitlabIssue> extractIssues(String json) {
		reset();
		return gs.fromJson(json, LIST_TYPES.get(GitlabIssue.class));
	}

	public List<GitlabUser> extractUsers(String json) {
		return gs.fromJson(json, LIST_TYPES.get(GitlabUser.class));
	}

	private String toKey(final GitlabEntity entity) {
		return entity.getClass().getSimpleName() + entity.getId();
	}


	private static final Map<Class<? extends GitlabEntity>, Type> LIST_TYPES = new HashMap<>();

	static {
		LIST_TYPES.put(GitlabProject.class, new TypeToken<List<GitlabProject>>() {
		}.getType());
		LIST_TYPES.put(GitlabUser.class, new TypeToken<List<GitlabUser>>() {
		}.getType());
		LIST_TYPES.put(GitlabIssue.class, new TypeToken<List<GitlabIssue>>() {
		}.getType());
	}

	private class CachingDeserializer implements JsonDeserializer {

		private final Gson tempGs;

		private CachingDeserializer(boolean supportsNestedUsers) {
			// required to cache GitLab users as well
			GsonBuilder builder = new GsonBuilder();
			if (supportsNestedUsers) {
				builder.registerTypeHierarchyAdapter(GitlabUser.class, new CachingDeserializer(false));
			}
			tempGs = builder.create();
		}

		@Override
		public Object deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonContext) throws JsonParseException {
			GitlabEntity tempEntity = tempGs.fromJson(jsonElement, type);
			String key = toKey(tempEntity);
			GitlabEntity entity = entities.get(key);
			if (entity == null) {
				entity = tempEntity;
				entities.put(key, entity);
			} else {
				entity.merge(tempEntity);
			}
			return entity;
		}
	}

}
