package ncbi.taggerOne.dataset;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bioc.BioCAnnotation;
import bioc.BioCDocument;
import bioc.BioCLocation;
import bioc.BioCPassage;
import bioc.io.woodstox.ConnectorWoodstox;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import ncbi.taggerOne.T1Constants;
import ncbi.taggerOne.lexicon.Lexicon;
import ncbi.taggerOne.types.AnnotatedSegment;
import ncbi.taggerOne.types.Entity;
import ncbi.taggerOne.types.TextInstance;
import ncbi.taggerOne.util.Dictionary;
import ncbi.util.StaticUtilMethods;

public class BioIDBioCDataset implements Dataset {

	private static final Logger logger = LoggerFactory.getLogger(BioIDBioCDataset.class);

	private Lexicon lexicon;
	private String input;
	private Map<String, String> entityTypeMap;
	private Map<String, String> entityTypeUsageMap;

	public BioIDBioCDataset() {
		// Empty
	}

	// Rules for this dataset:
	// All identifiers must already contain their namespace
	// Entity types are used as-is, there is no functionality for mapping to another type

	@Override
	public void setArgs(String... args) {
		if (args.length < 4) {
			throw new IllegalArgumentException("BioCDataset must have three arguments: input, entityTypeMap, entityTypeUsageMap: " + Arrays.asList(args));
		}
		this.input = args[1];
		entityTypeMap = StaticUtilMethods.getStringMap(args[2]);
		entityTypeUsageMap = StaticUtilMethods.getStringMap(args[3]);
		for (String entityType : entityTypeUsageMap.keySet()) {
			String usageStr = entityTypeUsageMap.get(entityType).toUpperCase(Locale.US);
			if (!usageStr.equals(Dataset.Usage.RECOGNIZE.name()) && !usageStr.equals(Dataset.Usage.IGNORE.name()) && !usageStr.equals(Dataset.Usage.IDENTIFY.name())) {
				throw new IllegalArgumentException("Unknown usage type for entity type " + entityType);
			}
		}
	}

	@Override
	public Set<String> getEntityTypes() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public void setLexicon(Lexicon lexicon) {
		this.lexicon = lexicon;
		// Verify that the values in entityTypeMap are in lexicon and present as a key in entityTypeUsageMap
		Dictionary<String> entityTypes = lexicon.getEntityTypes();
		for (String entityType : entityTypeMap.values()) {
			if (!entityTypeUsageMap.containsKey(entityType)) {
				throw new IllegalArgumentException("All values in entityTypeMap must be present as keys in entityTypeUsageMap: " + entityType);
			}
			String usageStr = entityTypeUsageMap.get(entityType).toUpperCase(Locale.US);
			if (entityTypes.getIndex(entityType) < 0 && !usageStr.equals(Dataset.Usage.IGNORE.name())) {
				throw new IllegalArgumentException("All values in entityTypeMap must be present in lexicon or listed as Ignore: " + entityType);
			}
		}
	}

	@Override
	public boolean containsEntityAnnotations() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public List<TextInstance> getInstances() {
		List<TextInstance> instances = new ArrayList<TextInstance>();
		File inFile = new File(input);
		if (inFile.isDirectory()) {
			if (!input.endsWith("/")) {
				input = input + "/";
			}
			File[] listOfFiles = (new File(input)).listFiles();
			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isFile()) {
					String currentInputFilename = input + listOfFiles[i].getName();
					logger.info("Processing file " + currentInputFilename);
					loadInstances(currentInputFilename, instances);
				}
			}
		} else {
			// Process a single file
			loadInstances(input, instances);
		}

		return instances;
	}

	private void loadInstances(String filename, List<TextInstance> instances) {
		ConnectorWoodstox connector = new ConnectorWoodstox();
		try {
			connector.startRead(new InputStreamReader(new FileInputStream(filename), T1Constants.UTF8_FORMAT));
			while (connector.hasNext()) {
				BioCDocument document = connector.next();
				String documentId = document.getID();
				int counter = 0;
				for (BioCPassage passage : document.getPassages()) {
					String passageText = passage.getText();
					Int2IntMap byteToChar = StaticUtilMethods.getByteToCharOffsets(passageText, Charset.forName("UTF-8"));
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
						// Null entities means we ignore this annotation
						if (entities != null) {
							List<BioCLocation> locations = annotation.getLocations();
							if (locations.size() > 1) {
								throw new RuntimeException("BioCDataset cannnot handle mentions with more than one location in document " + documentId);
							}
							BioCLocation location = locations.get(0);
							int startByte = location.getOffset() - offset;
							int endByte = startByte + location.getLength();
							int startChar = byteToChar.get(startByte);
							if (startChar < 0) {
								logger.error("Passage text: " + passageText);
								logger.error("startByte: " + startByte);
								logger.error("startChar: " + startChar);
								throw new RuntimeException("Start byte is not at a character start: " + startByte);
							}
							int endChar = byteToChar.get(endByte);
							if (endChar < 0) {
								logger.error("Passage text: " + passageText);
								logger.error("startByte: " + startByte);
								logger.error("startChar: " + startChar);
								logger.error("endByte: " + endByte);
								logger.error("endChar: " + endChar);
								throw new RuntimeException("End byte is not at a character start: " + endByte);
							}
							String text = passageText.substring(startChar, endChar);
							if (!text.equals(annotation.getText())) {
								throw new IllegalArgumentException("Text from mention definition (\"" + annotation.getText() + "\") does not match text specified by mention boundaries (\"" + text + "\") in document " + documentId);
							}
							AnnotatedSegment segment = new AnnotatedSegment(instance, startChar, endChar, null, entities, 0.0);
							annotations.add(segment);
						}
					}
					instance.setTargetAnnotation(annotations);
					instances.add(instance);
				}
			}
		} catch (IOException e) {
			// TODO Improve error handling
			throw new RuntimeException(e);
		}
	}

	private Set<Entity> getEntities(BioCAnnotation annotation) {
		// Get full identifier
		String identifierStr = annotation.getInfon("type");
		if (identifierStr == null) {
			throw new RuntimeException("Annotation missing type infon " + annotation.getID());
		}
		// Replace underscore with colon & remove spaces
		String identifierStrMod = identifierStr.replaceAll("_", ":").replaceAll("\\s", "");

		// Pull source/namespace and convert to entityType
		String sourceStr = identifierStrMod.split(":")[0];
		if (!entityTypeMap.containsKey(sourceStr)) {
			throw new RuntimeException("Entity type map does not contain key \"" + sourceStr + "\"");
		}
		String entityType = entityTypeMap.get(sourceStr);

		// Get usage and return if ignore
		String usageStr = entityTypeUsageMap.get(entityType).toUpperCase(Locale.US);
		if (usageStr.equals(Dataset.Usage.IGNORE.name())) {
			return null;
		}

		// Get entity and add to set
		Set<Entity> entities = null;
		if (!usageStr.equals(Dataset.Usage.IDENTIFY.name())) {
			entities = Collections.singleton(lexicon.getUnknownEntity(entityType));
		} else {
			entities = getEntities(identifierStrMod, entityType);
		}
		return entities;
	}

	private Set<Entity> getEntities(String idSetStr, String type) {
		if (idSetStr == null) {
			return Collections.singleton(lexicon.getUnknownEntity(type));
		}
		Set<Entity> entities = new HashSet<Entity>();
		String[] idFields = idSetStr.split("\\||,");
		for (String id : idFields) {
			String entityId = id;
			if (entityId != null) {
				if (entityId.equals("-1")) {
					entityId = null;
				}
			}
			if (entityId != null) {
				if (entityId.contains("+") || entityId.equals("")) {
					logger.warn("Ignoring identifier \"" + entityId + "\"");
					// FIXME Handle composite mentions
					entityId = null;
				}
			}
			Entity entity = lexicon.getUnknownEntity(type);
			if (entityId != null) {
				entity = lexicon.getEntity(entityId);
			}
			// TODO Check entity against type
			if (entity == null) {
				logger.error("Lexicon does not contain entity " + entityId + " of type " + type);
				entity = lexicon.getUnknownEntity(type);
			}
			entities.add(entity);
		}
		return entities;
	}
}
