package ncbi.taggerOne.lexicon;

import gnu.trove.impl.Constants;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.procedure.TIntProcedure;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ncbi.taggerOne.types.Entity;
import ncbi.taggerOne.types.MentionName;
import ncbi.taggerOne.util.Dictionary;
import ncbi.util.Profiler;

public class LexiconMappings {

	protected static final Logger logger = LoggerFactory.getLogger(LexiconMappings.class);

	private Set<String> namespaces;
	private Set<String> entityTypes;
	private Map<String, String> identifierToType;
	private Set<String> preferredIdentifiers;
	private int nextEquivalencyKey;
	private TIntObjectHashMap<Set<String>> equivalencyKeyToSet;
	private TObjectIntHashMap<String> identifierToEquivalencyKey;
	private Map<String, Set<String>> identifierToNames;
	private Map<String, String> identifierToPreferredName;

	public LexiconMappings(Set<String> namespaces, Set<String> entityTypes) {
		this.namespaces = namespaces;
		this.entityTypes = entityTypes;
		identifierToType = new HashMap<String, String>();
		preferredIdentifiers = new HashSet<String>();
		nextEquivalencyKey = 0;
		equivalencyKeyToSet = new TIntObjectHashMap<Set<String>>();
		identifierToEquivalencyKey = new TObjectIntHashMap<String>(Constants.DEFAULT_CAPACITY, Constants.DEFAULT_LOAD_FACTOR, Integer.MIN_VALUE);
		identifierToNames = new HashMap<String, Set<String>>();
		identifierToPreferredName = new HashMap<String, String>();
	}

	public void addIdentifier(String identifier, String entityType, boolean isPreferred) {
		Profiler.start("LexiconMappings.addIdentifier()");
		logger.trace("Adding identifier " + identifier + " of entity type " + entityType + ", isPreferred = " + isPreferred);
		if (!entityTypes.contains(entityType)) {
			throw new IllegalArgumentException("Cannot add identifier " + identifier + ": entity type " + entityType + " is unknown");
		}
		// Check identifier format
		String[] split = identifier.split(":");
		if (split.length != 2 || split[1].length() == 0) {
			throw new IllegalArgumentException("Identifier " + identifier + " is in the incorrect format");
		}
		if (!namespaces.contains(split[0])) {
			throw new IllegalArgumentException("Identifier " + identifier + " must begin with a known namespace");
		}
		if (identifierToType.containsKey(identifier)) {
			if (!entityType.equals(identifierToType.get(identifier))) {
				logger.error("Cannot add identifier " + identifier + " with entity type " + entityType + ": already present with entity type " + identifierToType.get(identifier));
			}
		} else {
			identifierToType.put(identifier, entityType);
			// initialize for equivalency calculation
			identifierToEquivalencyKey.put(identifier, nextEquivalencyKey);
			Set<String> equivalencySet = new HashSet<String>();
			equivalencySet.add(identifier);
			equivalencyKeyToSet.put(nextEquivalencyKey, equivalencySet);
			nextEquivalencyKey++;
		}
		if (isPreferred) {
			preferredIdentifiers.add(identifier);
		}
		Profiler.stop("LexiconMappings.addIdentifier()");
	}

	public boolean containsIdentifier(String identifier) {
		Profiler.start("LexiconMappings.containsIdentifier()");
		boolean contains = identifierToType.containsKey(identifier);
		Profiler.stop("LexiconMappings.containsIdentifier()");
		return contains;
	}

	public String getIdentifierType(String identifier) {
		return identifierToType.get(identifier);
	}

	public void addIdentifierEquivalence(String identifier1, String identifier2) {
		Profiler.start("LexiconMappings.addIdentifierEquivalence()");
		logger.trace("Adding identifier equivalence: " + identifier1 + " = " + identifier2);
		String entityType1 = identifierToType.get(identifier1);
		if (entityType1 == null) {
			throw new IllegalArgumentException("Identifier " + identifier1 + " must first be added");
		}
		String entityType2 = identifierToType.get(identifier2);
		if (entityType2 == null) {
			throw new IllegalArgumentException("Identifier " + identifier2 + " must first be added");
		}
		if (!entityType1.equals(entityType2)) {
			throw new IllegalArgumentException("Cannot make identifier " + identifier1 + " equivalent to " + identifier2 + "; they are different entity types");
		}
		int equivalencyKey1 = identifierToEquivalencyKey.get(identifier1);
		int equivalencyKey2 = identifierToEquivalencyKey.get(identifier2);
		if (equivalencyKey1 != equivalencyKey2) {
			// Merge set 2 into set 1
			Set<String> equivalencySet1 = equivalencyKeyToSet.get(equivalencyKey1);
			Set<String> equivalencySet2 = equivalencyKeyToSet.get(equivalencyKey2);
			equivalencySet1.addAll(equivalencySet2);
			equivalencyKeyToSet.remove(equivalencyKey2);
			// Change all identifiers in set 2 to point to key 1
			for (String identifier : equivalencySet2) {
				identifierToEquivalencyKey.put(identifier, equivalencyKey1);
			}
		}
		Profiler.stop("LexiconMappings.addIdentifierEquivalence()");
	}

	public void addTerm(String identifier, String name, boolean isPreferred) {
		logger.trace("Adding term to identifier " + identifier + " name: " + name + ", isPreferred = " + isPreferred);
		if (!identifierToType.containsKey(identifier)) {
			throw new IllegalArgumentException("Identifier " + identifier + " must first be added");
		}
		Set<String> names = identifierToNames.get(identifier);
		if (names == null) {
			names = new HashSet<String>();
			identifierToNames.put(identifier, names);
		}
		names.add(name);
		if (isPreferred) {
			identifierToPreferredName.put(identifier, name);
		}
	}

	public Lexicon createLexicon() {
		// Create the entity type dictionary
		Dictionary<String> entityTypeDictionary = new Dictionary<String>();
		for (String entityType : entityTypes) {
			entityTypeDictionary.addElement(entityType);
		}
		entityTypeDictionary.freeze();
		Lexicon lexicon = new Lexicon(entityTypeDictionary);
		EntityCreator creator = new EntityCreator(lexicon, identifierToType, preferredIdentifiers, equivalencyKeyToSet, identifierToNames, identifierToPreferredName);
		equivalencyKeyToSet.forEach(creator);
		logger.info("Loaded " + creator.getNumEntities() + " entities, with " + creator.getNumIdentifiers() + " identifiers, and " + creator.getNumNames() + " names.");
		creator = null;
		namespaces = null;
		entityTypes = null;
		identifierToType = null;
		preferredIdentifiers = null;
		equivalencyKeyToSet = null;
		identifierToEquivalencyKey = null;
		identifierToPreferredName = null;
		return lexicon;
	}

	private static class EntityCreator implements TIntProcedure {

		private Map<String, String> identifierToType;
		private Set<String> preferredIdentifiers;
		private TIntObjectHashMap<Set<String>> equivalencyKeyToSet;
		private Map<String, Set<String>> identifierToNames;
		private Map<String, String> identifierToPreferredName;

		private Lexicon lexicon;
		private int numIdentifiers;
		private int numNames;
		private int numEntities;

		public EntityCreator(Lexicon lexicon, Map<String, String> identifierToType, Set<String> preferredIdentifiers, TIntObjectHashMap<Set<String>> equivalencyKeyToSet, Map<String, Set<String>> identifierToNames,
				Map<String, String> identifierToPreferredName) {
			this.lexicon = lexicon;
			this.identifierToType = identifierToType;
			this.preferredIdentifiers = preferredIdentifiers;
			this.equivalencyKeyToSet = equivalencyKeyToSet;
			this.identifierToNames = identifierToNames;
			this.identifierToPreferredName = identifierToPreferredName;

			numIdentifiers = 0;
			numNames = 0;
			numEntities = 0;
		}

		public int getNumIdentifiers() {
			return numIdentifiers;
		}

		public int getNumNames() {
			return numNames;
		}

		public int getNumEntities() {
			return numEntities;
		}

		@Override
		public boolean execute(int equivalencyKey) {
			Set<String> identifierSet = equivalencyKeyToSet.get(equivalencyKey);
			// Determine the primary identifier: lexicographically lowest identifier that is also a preferred identifier
			Set<String> sortedIdentifierSet = new TreeSet<String>(identifierSet);
			sortedIdentifierSet.retainAll(preferredIdentifiers);
			if (sortedIdentifierSet.size() == 0) {
				throw new IllegalArgumentException("No identifiers in " + identifierSet + " are preferred");
			}
			String primaryIdentifier = sortedIdentifierSet.iterator().next();

			// Get the entity type (identifiers are guaranteed to be the same type)
			String entityType = identifierToType.get(primaryIdentifier);

			// Determine the the primary name: the preferred name for the preferred identifier
			String primaryName = identifierToPreferredName.get(primaryIdentifier);
			if (primaryName == null) {
				throw new IllegalArgumentException("No preferred name set for primary identifier " + primaryIdentifier);
			}

			// Prepare names
			Set<MentionName> mentionNameSet = new HashSet<MentionName>();
			for (String identifier : identifierSet) {
				Set<String> nameSet = identifierToNames.get(identifier);
				if (nameSet != null) {
					for (String name : nameSet) {
						mentionNameSet.add(new MentionName(name));
					}
				}
			}

			// Create entity
			Entity entity = new Entity(entityType, primaryIdentifier, new MentionName(primaryName));
			entity.addIdentifiers(identifierSet);
			entity.addNames(mentionNameSet);

			lexicon.addEntity(entity);

			numEntities++;
			numIdentifiers += identifierSet.size();
			numNames += mentionNameSet.size();
			return true;
		}
	}
}
