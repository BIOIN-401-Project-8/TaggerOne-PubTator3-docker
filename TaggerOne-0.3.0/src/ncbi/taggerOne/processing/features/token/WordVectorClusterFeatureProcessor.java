package ncbi.taggerOne.processing.features.token;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ncbi.taggerOne.T1Constants;
import ncbi.taggerOne.processing.features.FeatureProcessor;
import ncbi.taggerOne.processing.features.FeatureProcessorCallback;
import ncbi.taggerOne.types.TextInstance;
import ncbi.taggerOne.types.Token;
import ncbi.taggerOne.util.Trie;
import ncbi.taggerOne.util.tokenization.Tokenizer;
import ncbi.taggerOne.util.vector.Vector;
import ncbi.util.Profiler;

public class WordVectorClusterFeatureProcessor implements FeatureProcessor {

	private static final Logger logger = LoggerFactory.getLogger(WordVectorClusterFeatureProcessor.class);

	private static final long serialVersionUID = 1L;

	private String prefix;
	private Tokenizer tokenizer;
	private Trie<String, Set<String>> nameTypeTrie;

	public WordVectorClusterFeatureProcessor(String prefix, Tokenizer tokenizer) {
		this.prefix = prefix;
		this.tokenizer = tokenizer;
		this.nameTypeTrie = new Trie<String, Set<String>>();
	}

	public void loadFromFile(String filename) {
		Profiler.start("WordVectorClusterFeatureProcessor.loadFromFile()");
		logger.info("Loading WordVectorClusterFeatureProcessor from file " + filename);
		int entries = 0;
		try {
			BufferedReader reader = null;
			if (filename.endsWith(".gz")) {
				reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(filename)), T1Constants.UTF8_FORMAT));
			} else {
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), T1Constants.UTF8_FORMAT));
			}
			// BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename + ".out"), T1Constants.UTF8_FORMAT));
			String line = reader.readLine();
			while (line != null) {
				line = line.trim();
				if (line.length() > 0) {
					entries++;
					String[] fields = line.split("\t");
					List<String> nameTokens = prepareKey(fields[0]);
					if (nameTokens != null && !nameTokens.isEmpty()) {
						// Profiler.start("WordVectorClusterFeatureProcessor.nameTypeTrie.getValue()");
						Set<String> result = nameTypeTrie.get(nameTokens);
						// Profiler.stop("WordVectorClusterFeatureProcessor.nameTypeTrie.getValue()");
						if (result == null) {
							result = new HashSet<String>();
							nameTypeTrie.add(nameTokens, result);
						}
						for (int fieldIndex = 1; fieldIndex < fields.length; fieldIndex++) {
							result.add(fields[fieldIndex]);
							// writer.write(StaticUtilMethods.makeStringList(nameTokens, " ") + "\t" + fields[fieldIndex] + "\n");
						}
					}
				}
				line = reader.readLine();
			}
			reader.close();
			// writer.close();
		} catch (IOException e) {
			// TODO Improve error handling
			throw new RuntimeException(e);
		}
		logger.info("Loaded " + entries + " entries into WordVectorClusterFeatureProcessor");
		Profiler.stop("WordVectorClusterFeatureProcessor.loadFromFile()");
	}

	@Override
	public void process(TextInstance input, FeatureProcessorCallback featureProcessorCallback) {
		List<Token> tokens = input.getTokens();
		for (int i = 0; i < tokens.size(); i++) {
			Token token = tokens.get(i);
			Vector<String> featureVector = token.getFeatures();
			List<String> tokenKey = prepareKey(token.getText());
			if (tokenKey != null && !tokenKey.isEmpty()) {
				// Profiler.start("WordVectorClusterFeatureProcessor.nameTypeTrie.getValue()");
				Set<String> types = nameTypeTrie.get(tokenKey);
				// Profiler.stop("WordVectorClusterFeatureProcessor.nameTypeTrie.getValue()");
				for (String type : types) {
					String featureName = prefix + "=" + type;
					featureProcessorCallback.callback(featureName, 1.0, featureVector);
				}
			}
		}
	}

	private List<String> prepareKey(String segmentText) {
		Profiler.start("WordVectorClusterFeatureProcessor.prepareKey()");
		List<String> tokens = new ArrayList<String>();
		tokenizer.reset(segmentText);
		while (tokenizer.nextToken()) {
			String token = segmentText.substring(tokenizer.startChar(), tokenizer.endChar());
			tokens.add(token);
		}
		Profiler.stop("WordVectorClusterFeatureProcessor.prepareKey()");
		// Ignore all inputs longer than one token
		if (tokens.size() > 1) {
			return null;
		}
		return tokens;
	}

}
