package ncbi.taggerOne.processing.features.segment;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ncbi.taggerOne.T1Constants;
import ncbi.taggerOne.lexicon.Lexicon;
import ncbi.taggerOne.processing.features.FeatureProcessor;
import ncbi.taggerOne.processing.features.FeatureProcessorCallback;
import ncbi.taggerOne.processing.string.StringProcessor;
import ncbi.taggerOne.types.Entity;
import ncbi.taggerOne.types.MentionName;
import ncbi.taggerOne.types.Segment;
import ncbi.taggerOne.types.TextInstance;
import ncbi.taggerOne.util.Dictionary;
import ncbi.taggerOne.util.Trie;
import ncbi.taggerOne.util.tokenization.Tokenizer;
import ncbi.taggerOne.util.vector.Vector;
import ncbi.util.Profiler;

public class LexicalFeatureProcessor implements FeatureProcessor {

	private static final Logger logger = LoggerFactory.getLogger(LexicalFeatureProcessor.class);

	private static final long serialVersionUID = 1L;

	private String prefix;
	private StringProcessor segmentProcessor;
	private StringProcessor tokenProcessor;
	private Tokenizer tokenizer;
	private Trie<String, Set<String>> nameTypeTrie;

	public LexicalFeatureProcessor(String prefix, Tokenizer tokenizer, StringProcessor segmentProcessor, StringProcessor tokenProcessor) {
		this.prefix = prefix;
		this.tokenizer = tokenizer;
		this.segmentProcessor = segmentProcessor;
		this.tokenProcessor = tokenProcessor;
		this.nameTypeTrie = new Trie<String, Set<String>>();
	}

	public void loadFromLexicon(Lexicon lexicon) {
		Dictionary<String> entityTypes = lexicon.getEntityTypes();
		for (int entityTypeIndex = 0; entityTypeIndex < entityTypes.size(); entityTypeIndex++) {
			String entityType = entityTypes.getElement(entityTypeIndex);
			List<String> entityTypeList = Collections.singletonList(entityType);
			Set<Entity> entities = lexicon.getEntities(entityType);
			for (Entity entity : entities) {
				for (MentionName name : entity.getNames()) {
					addEntry(name.getName(), entityTypeList);
				}
			}
		}
	}

	public void loadFromFile(String filename) {
		Profiler.start("ExactMatchLexicalFeatureProcessor.loadFromFile()");
		logger.info("Loading ExactMatchLexicalFeatureProcessor from file " + filename);
		int entries = 0;
		try {
			BufferedReader reader = null;
			if (filename.endsWith(".gz")) {
				reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(filename)), T1Constants.UTF8_FORMAT));
			} else {
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), T1Constants.UTF8_FORMAT));
			}
			String line = reader.readLine();
			while (line != null) {
				line = line.trim();
				if (line.length() > 0) {
					entries++;
					String[] fields = line.split("\t");
					String name = fields[0];
					List<String> types = Arrays.asList(fields).subList(1, fields.length);
					addEntry(name, types);
				}
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			// TODO Improve error handling
			throw new RuntimeException(e);
		}
		logger.info("Loaded " + entries + " entries into ExactMatchLexicalFeatureProcessor");
		Profiler.stop("ExactMatchLexicalFeatureProcessor.loadFromFile()");
	}

	private void addEntry(String name, List<String> types) {
		List<String> nameTokens = prepareKey(name);
		Set<String> result = nameTypeTrie.get(nameTokens);
		if (result == null) {
			result = new HashSet<String>();
			nameTypeTrie.add(nameTokens, result);
		}
		result.addAll(types);
	}

	@Override
	public void process(TextInstance input, FeatureProcessorCallback featureProcessorCallback) {
		List<Segment> segments = input.getSegments();
		for (int i = 0; i < segments.size(); i++) {
			Segment segment = segments.get(i);
			List<String> tokens = prepareKey(segment.getText());
			Profiler.start("ExactMatchLexicalFeatureProcessor.nameTypeTrie.getValue()");
			Set<String> types = nameTypeTrie.get(tokens);
			Profiler.stop("ExactMatchLexicalFeatureProcessor.nameTypeTrie.getValue()");
			if (types != null) {
				Vector<String> featureVector = segment.getFeatures();
				logger.trace("Marking segment \"" + segment.getText() + "\" as lexical types: " + types);
				for (String type : types) {
					String featureName = prefix + "=" + type;
					featureProcessorCallback.callback(featureName, 1.0, featureVector);
				}
			}
		}
	}

	private List<String> prepareKey(String segmentText) {
		Profiler.start("ExactMatchLexicalFeatureProcessor.prepareKey()");
		String processedText = null;
		if (segmentProcessor == null) {
			processedText = segmentText;
		} else {
			processedText = segmentProcessor.process(segmentText);
		}
		List<String> tokens = new ArrayList<String>();
		tokenizer.reset(processedText);
		while (tokenizer.nextToken()) {
			tokens.add(processedText.substring(tokenizer.startChar(), tokenizer.endChar()));
		}
		if (tokenProcessor != null) {
			List<String> processedTokens = new ArrayList<String>(tokens.size());
			for (String token : tokens) {
				processedTokens.add(tokenProcessor.process(token));
			}
			tokens = processedTokens;
		}
		Profiler.stop("ExactMatchLexicalFeatureProcessor.prepareKey()");
		return tokens;
	}

}
