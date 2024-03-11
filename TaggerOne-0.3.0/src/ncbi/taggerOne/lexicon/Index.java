package ncbi.taggerOne.lexicon;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ncbi.taggerOne.types.Entity;
import ncbi.taggerOne.types.MentionName;
import ncbi.taggerOne.util.ArraySet;
import ncbi.taggerOne.util.Dictionary;
import ncbi.taggerOne.util.vector.Vector;

public class Index implements Serializable {

	private static final Logger logger = LoggerFactory.getLogger(Index.class);
	private static final long serialVersionUID = 1L;

	private Entity unknownEntity;
	private Dictionary<Vector<String>> nameVectors;
	private Dictionary<String> mentionVectorSpace;
	private Dictionary<String> nameVectorSpace;
	private Comparator<Entity> entityComparator;
	private Dictionary<Entity> entityDictionary;
	private List<Set<Entity>> nameVectorEntitySets;

	public Index(Dictionary<String> mentionVectorSpace, Dictionary<String> nameVectorSpace, Set<Entity> entities, Entity unknownEntity, boolean removeAmbiguousIfPrimaryForOther) {
		if (mentionVectorSpace == null) {
			throw new IllegalArgumentException("mentionVectorSpace cannot be null");
		}
		if (nameVectorSpace == null) {
			throw new IllegalArgumentException("nameVectorSpace cannot be null");
		}
		if (entities == null) {
			throw new IllegalArgumentException("entities cannot be null");
		}
		if (unknownEntity == null) {
			throw new IllegalArgumentException("unknownEntity cannot be null");
		}
		this.mentionVectorSpace = mentionVectorSpace;
		this.nameVectorSpace = nameVectorSpace;
		// TODO Verify entities are all one type
		Map<Vector<String>, Set<Entity>> vectorToEntityMap = new HashMap<Vector<String>, Set<Entity>>();
		Map<Vector<String>, Set<Entity>> primaryVectorToEntityMap = new HashMap<Vector<String>, Set<Entity>>();
		entityDictionary = new Dictionary<Entity>();
		for (Entity entity : entities) {
			entityDictionary.addElement(entity);
			Vector<String> primaryVector = entity.getPrimaryName().getVector();
			Set<Entity> primaryEntitySet = primaryVectorToEntityMap.get(primaryVector);
			if (primaryEntitySet == null) {
				primaryEntitySet = new HashSet<Entity>();
				primaryVectorToEntityMap.put(primaryVector, primaryEntitySet);
			}
			primaryEntitySet.add(entity);
			for (MentionName name : entity.getNames()) {
				Vector<String> nameVector = name.getVector();
				if (nameVector != null) {
					Set<Entity> entitySet = vectorToEntityMap.get(nameVector);
					if (entitySet == null) {
						entitySet = new HashSet<Entity>();
						vectorToEntityMap.put(nameVector, entitySet);
					}
					entitySet.add(entity);
				}
			}
		}
		entityDictionary.freeze();
		List<Vector<String>> vectors = new ArrayList<Vector<String>>(vectorToEntityMap.keySet());
		nameVectors = new Dictionary<Vector<String>>();
		Set<Entity> emptySet = new HashSet<Entity>();
		int ambiguous = 0;
		for (Vector<String> vector : vectors) {
			nameVectors.addElement(vector);
			Set<Entity> primaryEntitySet = primaryVectorToEntityMap.get(vector);
			if (primaryEntitySet == null) {
				primaryEntitySet = emptySet;
			}
			Set<Entity> entitySet = vectorToEntityMap.get(vector);
			if (removeAmbiguousIfPrimaryForOther && entitySet.size() > 1 && primaryEntitySet.size() > 0) {
				// This vector is ambiguous and we need to handle it
				logger.debug("Name vector \"" + vector.visualize() + "\" for entity type " + unknownEntity.getType() + " is ambiguous; primary = " + primaryEntitySet + " all= " + Entity.visualizePrimaryIdentifiers(entitySet));
				Set<Entity> removeSet = new HashSet<Entity>(entitySet);
				removeSet.removeAll(primaryEntitySet);
				for (Entity entity : removeSet) {
					Set<MentionName> names = new HashSet<MentionName>(entity.getNames());
					for (MentionName name : names) {
						if (name.getVector().equals(vector)) {
							entity.removeName(name);
						}
					}
					entitySet.remove(entity);
				}
			}
			if (entitySet.size() > 1) {
				// This vector is (still) ambiguous
				logger.warn("Name vector \"" + vector.visualize() + "\" for entity type " + unknownEntity.getType() + " is ambiguous between " + entitySet.size() + " entities: " + Entity.visualizePrimaryIdentifiers(entitySet));
				ambiguous++;
			}
		}
		nameVectors.freeze();
		primaryVectorToEntityMap = null;
		logger.info("Number of unique name vectors for entity type " + unknownEntity.getType() + ": " + nameVectors.size());
		logger.info("Number of ambiguous name vectors for entity type " + unknownEntity.getType() + ": " + ambiguous);
		// TODO MEMORY Consider replacing name vectors with the canonical vector to save space (e.g. about 10% for genes)
		this.unknownEntity = unknownEntity;
		entityComparator = null;

		nameVectorEntitySets = new ArrayList<Set<Entity>>(nameVectors.size());
		for (int nameVectorIndex = 0; nameVectorIndex < nameVectors.size(); nameVectorIndex++) {
			Vector<String> nameVector = nameVectors.getElement(nameVectorIndex);
			Set<Entity> entitySet = vectorToEntityMap.get(nameVector);
			nameVectorEntitySets.add(new ArraySet<Entity>(entitySet));
		}
	}

	public Dictionary<String> getMentionVectorSpace() {
		return mentionVectorSpace;
	}

	public Dictionary<String> getNameVectorSpace() {
		return nameVectorSpace;
	}

	public Dictionary<Vector<String>> getNameVectorDictionary() {
		return nameVectors;
	}

	public Dictionary<Entity> getEntityDictionary() {
		return entityDictionary;
	}

	public void setEntityComparator(Comparator<Entity> entityComparator) {
		this.entityComparator = entityComparator;
	}

	public Set<Entity> getEntities(Vector<String> nameVector) {
		int nameVectorIndex = nameVectors.getIndex(nameVector);
		if (nameVectorIndex < 0) {
			return null;
		}
		Set<Entity> entities = nameVectorEntitySets.get(nameVectorIndex);
		if (entityComparator == null || entities == null) {
			return entities;
		}
		List<Entity> entityList = new ArrayList<Entity>(entities);
		Collections.sort(entityList, entityComparator);
		return new LinkedHashSet<Entity>(entityList);
	}

	public Entity getUnknownEntity() {
		return unknownEntity;
	}
}
