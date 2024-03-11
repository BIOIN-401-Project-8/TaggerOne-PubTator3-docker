package ncbi.taggerOne.dataset;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ncbi.taggerOne.lexicon.Lexicon;
import ncbi.taggerOne.types.AnnotatedSegment;
import ncbi.taggerOne.types.Entity;
import ncbi.taggerOne.types.TextInstance;
import ncbi.taggerOne.util.Dictionary;
import ncbi.util.StaticUtilMethods;

public class CHEMDNERDataset implements Dataset {

	private static final Logger logger = LoggerFactory.getLogger(CHEMDNERDataset.class);

	private Lexicon lexicon;
	private String abstractsFilename;
	private String annotationsFilename;
	private String pmidsFilename;
	private Map<String, String> entityTypeMap;
	private Map<String, String> entityTypeUsageMap;

	public CHEMDNERDataset() {
		// Empty
	}

	@Override
	public void setArgs(String... args) {
		if (args.length < 6) {
			throw new IllegalArgumentException("CHEMDNERDataset must have five arguments: abstractsFilename, annotationsFilename, pmidsFilename, entityTypeMap, entityTypeUsageMap: " + Arrays.asList(args));
		}
		abstractsFilename = args[1];
		annotationsFilename = args[2].trim();
		pmidsFilename = args[3].trim();
		if (pmidsFilename.length() == 0) {
			pmidsFilename = null;
		}
		entityTypeMap = StaticUtilMethods.getStringMap(args[4]);

		entityTypeUsageMap = StaticUtilMethods.getStringMap(args[5]);
		for (String entityType : entityTypeUsageMap.keySet()) {
			String usageStr = entityTypeUsageMap.get(entityType).toUpperCase(Locale.US);
			if (Dataset.Usage.IDENTIFY.name().equals(usageStr)) {
				throw new IllegalArgumentException("CHEMDNERDataset only supports RECOGNIZE and IGNORE");
			} else if (!Dataset.Usage.RECOGNIZE.name().equals(usageStr) && !Dataset.Usage.IGNORE.name().equals(usageStr)) {
				throw new IllegalArgumentException("Unknown usage type for entity type " + entityType);
			}
		}
	}

	@Override
	public Set<String> getEntityTypes() {
		// TODO Implement
		throw new IllegalArgumentException("Not implemented");
	}

	@Override
	public boolean containsEntityAnnotations() {
		// TODO Implement
		throw new IllegalArgumentException("Not implemented");
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
	public List<TextInstance> getInstances() {
		try {
			Set<String> pmids = null;
			if (pmidsFilename != null) {
				pmids = getPMIDs(pmidsFilename);
			}
			Map<String, Abstract> abstracts = loadAbstracts(abstractsFilename, pmids);
			logger.info("abstracts.size()=" + abstracts.size());
			loadAnnotations(annotationsFilename, abstracts);

			List<TextInstance> instances = new ArrayList<TextInstance>();
			for (String documentId : abstracts.keySet()) {
				Abstract a = abstracts.get(documentId);
				List<Tag> tags = a.getTags();
				String id = a.getId();
				String text = a.getText();
				TextInstance textInstance = new TextInstance(null, id, id, text, 0);
				List<AnnotatedSegment> annotations = new ArrayList<AnnotatedSegment>();
				for (Tag tag : new ArrayList<Tag>(tags)) {
					// logger.info("id=" + tag.id + " type=" + tag.type);
					Set<Entity> entities = null;
					String usageStr = entityTypeUsageMap.get(tag.type).toUpperCase(Locale.US);
					if (!usageStr.equals(Dataset.Usage.IGNORE.name())) {
						entities = Collections.singleton(lexicon.getAnyEntity(tag.type));
						AnnotatedSegment mention = new AnnotatedSegment(textInstance, tag.start, tag.end, null, entities, 0.0);
						annotations.add(mention);
					}
				}
				textInstance.setTargetAnnotation(annotations);
				instances.add(textInstance);
			}
			return instances;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Map<String, Abstract> loadAbstracts(String abstractsFilename, Set<String> pmids) throws IOException {
		Map<String, Abstract> abstracts = new LinkedHashMap<String, Abstract>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(abstractsFilename), "UTF8"));
		try {
			String line = reader.readLine();
			while (line != null) {
				line = line.trim();
				if (line.length() > 0) {
					String[] split = line.split("\\t");
					String documentId = split[0].trim();
					String titleText = split[1].trim();
					String abstractText = split[2];
					if (pmids == null || pmids.contains(documentId)) {
						if (abstracts.containsKey(documentId))
							throw new IllegalArgumentException("Duplicate abstract " + documentId);
						Abstract a = new Abstract();
						a.setId(documentId);
						a.setTitleText(titleText);
						a.setAbstractText(abstractText);
						abstracts.put(documentId, a);
					}
				}
				line = reader.readLine();
			}
		} finally {
			reader.close();
		}
		System.out.println("Loaded " + abstracts.size() + " abstracts");
		if (pmids != null && pmids.size() != abstracts.size()) {
			logger.warn("pmids.size() = " + pmids.size() + ", abstracts.size() = " + abstracts.size());
		}
		return abstracts;
	}

	private void loadAnnotations(String annotationsFilename, Map<String, Abstract> abstracts) throws IOException {
		int count = 0;
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(annotationsFilename), "UTF8"));
		try {
			String line = reader.readLine();
			while (line != null) {
				line = line.trim();
				if (line.length() > 0) {
					String[] split = line.split("\\t");
					String documentId = split[0].trim();
					String textField = split[1].trim();
					if (!textField.equals("T") && !textField.equals("A")) {
						throw new IllegalArgumentException("Unknown text field: " + textField);
					}
					int mentionStart = Integer.parseInt(split[2].trim());
					int mentionEnd = Integer.parseInt(split[3].trim());
					String mentionText = split[4].trim();
					String typeText = mapEntityType(split[5].trim());

					Abstract a = abstracts.get(documentId);
					if (a != null) {
						// Add length of title to start & end if type is "A"
						if (textField.equals("A")) {
							int titleLength = a.getTitleText().length() + 1;
							mentionStart += titleLength;
							mentionEnd += titleLength;
						}
						// Verify text matches
						String mentionTextFromAbstract = a.getSubText(mentionStart, mentionEnd);
						if (!mentionText.equals(mentionTextFromAbstract)) {
							logger.warn("Abstract id = " + a.getId());
							logger.warn("Abstract text = " + a.getText().replaceAll("\\t", "%%%"));
							logger.warn("textField = " + textField);
							logger.warn("mentionStart = " + mentionStart);
							logger.warn("mentionEnd = " + mentionEnd);
							logger.warn("mentionText = " + mentionText);
							logger.warn("typeText = " + typeText);
							logger.warn("titleLength = " + a.getText().indexOf('\t'));
							throw new IllegalArgumentException("Mention text (" + mentionText + ") does not match mention text in abstract (" + mentionTextFromAbstract + ") in document " + documentId);
						}
						if (mentionStart == mentionEnd) {
							logger.warn("Abstract id = " + a.getId());
							logger.warn("Abstract text = " + a.getText().replaceAll("\\t", "%%%"));
							logger.warn("textField = " + textField);
							logger.warn("mentionStart = " + mentionStart);
							logger.warn("mentionEnd = " + mentionEnd);
							logger.warn("mentionText = " + mentionText);
							logger.warn("typeText = " + typeText);
							logger.warn("titleLength = " + a.getText().indexOf('\t'));
							throw new IllegalArgumentException("Mention length must be at least 1 in document " + documentId);
						}
						Tag t = new Tag(typeText, mentionStart, mentionEnd);
						a.addTag(t);
						count++;
					}
				}
				line = reader.readLine();
			}
			System.out.println("Loaded " + count + " annotations");
		} finally {
			reader.close();
		}
	}

	private String mapEntityType(String entityType) {
		if (!entityTypeMap.containsKey(entityType)) {
			throw new RuntimeException("Entity type map does not contain key \"" + entityType + "\"");
		}
		return entityTypeMap.get(entityType);
	}

	private Set<String> getPMIDs(String filename) throws IOException {
		Set<String> pmids = new HashSet<String>();
		BufferedReader dataFile = new BufferedReader(new FileReader(filename));
		String line = dataFile.readLine();
		while (line != null) {
			line = line.trim();
			if (line.length() > 0)
				pmids.add(line);
			line = dataFile.readLine();
		}
		dataFile.close();
		return pmids;
	}

	private static class Abstract {
		private String id;
		private List<Tag> tags;
		private String titleText;
		private String abstractText;

		public Abstract() {
			tags = new ArrayList<Tag>();
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public void setTitleText(String titleText) {
			this.titleText = titleText;
		}

		public void setAbstractText(String abstractText) {
			this.abstractText = abstractText;
		}

		public String getTitleText() {
			return titleText;
		}

		public List<Tag> getTags() {
			return tags;
		}

		public void addTag(Tag tag) {
			tags.add(tag);
		}

		public String getSubText(int start, int end) {
			return getText().substring(start, end);
		}

		public String getText() {
			return titleText + " " + abstractText;
		}
	}

	private static class Tag {
		public String type;
		public int start;
		public int end;

		public Tag(String type, int start, int end) {
			this.type = type;
			this.start = start;
			this.end = end;
		}

	}
}
