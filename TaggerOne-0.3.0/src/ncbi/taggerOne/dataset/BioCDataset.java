package ncbi.taggerOne.dataset;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bioc.BioCAnnotation;
import bioc.BioCDocument;
import bioc.BioCLocation;
import bioc.BioCPassage;
import bioc.io.woodstox.ConnectorWoodstox;
import ncbi.taggerOne.T1Constants;
import ncbi.taggerOne.lexicon.Lexicon;
import ncbi.taggerOne.types.AnnotatedSegment;
import ncbi.taggerOne.types.Entity;
import ncbi.taggerOne.types.TextInstance;
import ncbi.taggerOne.util.Dictionary;

public class BioCDataset implements Dataset {

	private static final Logger logger = LoggerFactory.getLogger(BioCDataset.class);

	private Lexicon lexicon;
	private String filename;
	private boolean containsEntityAnnotations;

	public BioCDataset() {
		// Empty
	}

	// Rules for this dataset:
	// All identifiers must already contain their namespace
	// Entity types are used as-is, there is no functionality for mapping to another type

	@Override
	public void setArgs(String... args) {
		if (args.length < 2) {
			throw new IllegalArgumentException("BioCDataset must have one argument: filename: " + Arrays.asList(args));
		}
		this.filename = args[1];
	}

	@Override
	public Set<String> getEntityTypes() {
		Set<String> entityTypes = new HashSet<String>();
		ConnectorWoodstox connector = new ConnectorWoodstox();
		try {
			connector.startRead(new InputStreamReader(new FileInputStream(filename), T1Constants.UTF8_FORMAT));
			while (connector.hasNext()) {
				BioCDocument document = connector.next();
				for (BioCPassage passage : document.getPassages()) {
					for (BioCAnnotation annotation : passage.getAnnotations()) {
						String entityType = annotation.getInfon("type");
						entityTypes.add(entityType);
					}
				}
			}
		} catch (IOException e) {
			// TODO Improve error handling
			throw new RuntimeException(e);
		}
		return entityTypes;
	}

	@Override
	public void setLexicon(Lexicon lexicon) {
		this.lexicon = lexicon;
		prepareEntityAnnotations();
	}

	private void prepareEntityAnnotations() {
		Dictionary<String> entityTypes = lexicon.getEntityTypes();
		Set<String> namespaces = new HashSet<String>();
		ConnectorWoodstox connector = new ConnectorWoodstox();
		try {
			connector.startRead(new InputStreamReader(new FileInputStream(filename), T1Constants.UTF8_FORMAT));
			while (connector.hasNext()) {
				BioCDocument document = connector.next();
				for (BioCPassage passage : document.getPassages()) {
					for (BioCAnnotation annotation : passage.getAnnotations()) {
						String entityType = annotation.getInfon("type");
						if (entityTypes.getIndex(entityType) < 0) {
							throw new IllegalArgumentException("Lexicon does not contain entities of type " + entityType);
						}
						Map<String, String> infons = new HashMap<String, String>(annotation.getInfons());
						infons.remove("type");
						namespaces.addAll(infons.keySet());
						containsEntityAnnotations = containsEntityAnnotations || infons.size() > 0;
					}
				}
			}
		} catch (IOException e) {
			// TODO Improve error handling
			throw new RuntimeException(e);
		}
		logger.info("Namespaces present in file " + filename + ": " + namespaces);
	}

	@Override
	public boolean containsEntityAnnotations() {
		return containsEntityAnnotations;
	}

	@Override
	public List<TextInstance> getInstances() {
		List<TextInstance> instances = new ArrayList<TextInstance>();
		ConnectorWoodstox connector = new ConnectorWoodstox();
		try {
			connector.startRead(new InputStreamReader(new FileInputStream(filename), T1Constants.UTF8_FORMAT));
			while (connector.hasNext()) {
				BioCDocument document = connector.next();
				String documentId = document.getID();
				int counter = 0;
				for (BioCPassage passage : document.getPassages()) {
					int offset = passage.getOffset();
					String counterText = Integer.toString(counter);
					while (counterText.length() < 2) {
						counterText = "0" + counterText;
					}
					TextInstance instance = new TextInstance(null, documentId, documentId + "-" + counter, passage.getText(), offset);
					List<AnnotatedSegment> annotations = new ArrayList<AnnotatedSegment>();
					for (BioCAnnotation annotation : passage.getAnnotations()) {
						// Get entities
						Set<Entity> entities = getEntities(annotation);
						List<BioCLocation> locations = annotation.getLocations();
						if (locations.size() > 1) {
							throw new RuntimeException("BioCDataset cannnot handle mentions with more than one location in document " + documentId);
						}
						BioCLocation location = locations.get(0);
						int startChar = location.getOffset() - offset;
						int endChar = startChar + location.getLength();
						String text = passage.getText().substring(startChar, endChar);
						if (!text.equals(annotation.getText())) {
							throw new IllegalArgumentException("Text from mention definition (\"" + annotation.getText() + "\") does not match text specified by mention boundaries (\"" + text + "\") in document " + documentId);
						}
						AnnotatedSegment segment = new AnnotatedSegment(instance, startChar, endChar, null, entities, 0.0);
						annotations.add(segment);
					}
					instance.setTargetAnnotation(annotations);
					instances.add(instance);
				}
			}
		} catch (IOException e) {
			// TODO Improve error handling
			throw new RuntimeException(e);
		}
		return instances;
	}

	private Set<Entity> getEntities(BioCAnnotation annotation) {
		String entityType = annotation.getInfon("type");
		String identifierStr = annotation.getInfon("identifier");
		Set<Entity> entities = new HashSet<Entity>();
		if (identifierStr == null) {
			entities.add(lexicon.getUnknownEntity(entityType));
		} else {
			String[] identifierArray = identifierStr.split("\\||,");
			for (int identifierIndex = 0; identifierIndex < identifierArray.length; identifierIndex++) {
				String entityId = identifierArray[identifierIndex];
				if (entityId != null) {
					if (entityId.length() == 0 || entityId.equals("-1")) {
						entityId = null;
					} else if (entityId.contains("+")) {
						logger.warn("Ignoring identifier \"" + entityId + "\"");
						entityId = null;
					}
				}
				if (entityId != null) {
					Entity entity = lexicon.getEntity(entityId);
					if (entity == null) {
						logger.warn("Lexicon does not contain entity " + entityId + " of type " + entityType + ", converting to Unknown");
						entities.add(lexicon.getUnknownEntity(entityType));
					} else {
						entities.add(entity);
					}
				} else {
					entities.add(lexicon.getUnknownEntity(entityType));
				}
			}
		}
		return entities;
	}

	private Set<Entity> getEntitiesOLD(BioCAnnotation annotation) {
		String entityType = annotation.getInfon("type");
		Set<Entity> entities = new HashSet<Entity>();
		Map<String, String> infons = new HashMap<String, String>(annotation.getInfons());
		infons.remove("type");
		if (infons.size() == 0) {
			entities.add(lexicon.getUnknownEntity(entityType));
		} else {
			for (String namespace : infons.keySet()) {
				String[] identifierArray = infons.get(namespace).split("\\||,");
				for (int identifierIndex = 0; identifierIndex < identifierArray.length; identifierIndex++) {
					String identifier = identifierArray[identifierIndex];
					if (identifier != null) {
						if (identifier.length() == 0 || identifier.equals("-1")) {
							identifier = null;
						} else if (identifier.contains("+")) {
							logger.warn("Ignoring identifier \"" + identifier + "\"");
							identifier = null;
						}
					}
					if (identifier != null) {
						String entityId = namespace + ":" + identifier;
						Entity entity = lexicon.getEntity(entityId);
						if (entity == null) {
							logger.warn("Lexicon does not contain entity " + entityId + " of type " + entityType + ", converting to Unknown");
							entities.add(lexicon.getUnknownEntity(entityType));
						} else {
							entities.add(entity);
						}
					} else {
						entities.add(lexicon.getUnknownEntity(entityType));
					}
				}
			}
		}
		return entities;
	}
}
