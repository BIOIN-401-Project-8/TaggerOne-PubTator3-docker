package ncbi.taggerOne.lexicon;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ncbi.taggerOne.T1Constants;
import ncbi.taggerOne.types.Entity;
import ncbi.taggerOne.types.MentionName;
import ncbi.taggerOne.util.Dictionary;

public class Lexicon implements Serializable {

	private static final long serialVersionUID = 1L;

	private Entity nonEntity;
	private Dictionary<String> entityTypes;
	private Map<String, Set<Entity>> typeToEntities;
	private Map<String, Entity> identifierToEntity;
	private Map<String, Entity> anyEntities;
	private Map<String, Entity> unknownEntities;
	private Map<String, Index> typeToIndex;

	public Lexicon(Dictionary<String> entityTypes) {
		if (!entityTypes.isFrozen()) {
			throw new IllegalStateException("Type dictionary must first be frozen");
		}
		if (entityTypes.getIndex(T1Constants.NONENTITY_STATE) >= 0) {
			throw new IllegalArgumentException("Type dictionary may not contain nonentity type");
		}
		this.entityTypes = entityTypes;
		typeToEntities = new HashMap<String, Set<Entity>>();
		identifierToEntity = new HashMap<String, Entity>();

		// Add non-entity
		nonEntity = new Entity(T1Constants.NONENTITY_STATE, T1Constants.NONENTITY_STATE, new MentionName(true, T1Constants.NON_ENTITY_NAME_TOKEN));
		typeToEntities.put(T1Constants.NONENTITY_STATE, new HashSet<Entity>());
		addEntity(nonEntity);

		// Add unknown entities (type but no identity)
		unknownEntities = new HashMap<String, Entity>();
		anyEntities = new HashMap<String, Entity>();
		for (int typeIndex = 0; typeIndex < entityTypes.size(); typeIndex++) {
			String type = entityTypes.getElement(typeIndex);
			typeToEntities.put(type, new HashSet<Entity>());

			// Add the unknown entity
			Entity unknownEntity = new Entity(type, T1Constants.UNKNOWN_ENTITY_ID_PREFIX + type, new MentionName(true, T1Constants.UNKNOWN_NAME_TOKEN));
			unknownEntities.put(type, unknownEntity);
			addEntity(unknownEntity);

			// Add the any entity
			MentionName anyPrimaryName = new MentionName(true, T1Constants.UNKNOWN_NAME_TOKEN);
			Entity anyEntity = new Entity(type, T1Constants.ANY_ENTITY_ID_PREFIX + type, anyPrimaryName);
			anyEntities.put(type, anyEntity);
		}
	}

	public Entity getNonEntity() {
		return nonEntity;
	}

	public Entity getAnyEntity(String type) {
		// TODO Workaround for using Any entities with models trained before their introduction
		if (anyEntities == null) {
			anyEntities = new HashMap<String, Entity>();
			for (String type2 : typeToEntities.keySet()) {
				// Add the any entity
				MentionName anyPrimaryName = new MentionName(true, T1Constants.UNKNOWN_NAME_TOKEN);
				Entity anyEntity = new Entity(type2, T1Constants.ANY_ENTITY_ID_PREFIX + type2, anyPrimaryName);
				anyEntities.put(type2, anyEntity);
			}
		}
		return anyEntities.get(type);
	}

	public Entity getUnknownEntity(String type) {
		return unknownEntities.get(type);
	}

	public void addEntity(Entity entity) {
		if (typeToIndex != null) {
			throw new IllegalStateException("Cannot add entities after index creation");
		}
		String type = entity.getType();
		typeToEntities.get(type).add(entity);
		for (String identifier : entity.getIdentifiers()) {
			if (identifierToEntity.containsKey(identifier)) {
				throw new IllegalArgumentException("Lexicon already contains identifier " + identifier);
			}
			identifierToEntity.put(identifier, entity);
		}
	}

	public Dictionary<String> getEntityTypes() {
		return entityTypes;
	}

	public Set<Entity> getEntities(String type) {
		return typeToEntities.get(type);
	}

	public Entity getEntity(String identifier) {
		return identifierToEntity.get(identifier);
	}

	public void createIndexes(Dictionary<String> mentionVectorSpace, Map<String, Dictionary<String>> nameVectorSpaces, boolean removeAmbiguousIfPrimaryForOther) {
		if (typeToIndex != null) {
			return;
		}
		typeToIndex = new HashMap<String, Index>();
		for (String type : typeToEntities.keySet()) {
			if (!type.equals(T1Constants.NONENTITY_STATE)) {
				Dictionary<String> nameVectorSpace = nameVectorSpaces.get(type);
				Index index = new Index(mentionVectorSpace, nameVectorSpace, typeToEntities.get(type), unknownEntities.get(type), removeAmbiguousIfPrimaryForOther);
				typeToIndex.put(type, index);
			}
		}
	}

	public Index getIndex(String type) {
		if (typeToIndex == null) {
			throw new IllegalStateException("Must first create indexes");
		}
		return typeToIndex.get(type);
	}
}
