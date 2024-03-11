package ncbi.taggerOne.processing.features.token;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ncbi.taggerOne.T1Constants;
import ncbi.taggerOne.processing.features.FeatureProcessor;
import ncbi.taggerOne.processing.features.FeatureProcessorCallback;
import ncbi.taggerOne.processing.string.StringProcessor;
import ncbi.taggerOne.types.TextInstance;
import ncbi.taggerOne.types.Token;
import ncbi.taggerOne.util.vector.Vector;
import ncbi.util.Profiler;

public class BrownClustersFeatureProcessor implements FeatureProcessor {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(BrownClustersFeatureProcessor.class);

	private String featureNamePrefix;
	private int[] prefixLengths;
	private Map<String, String> tokenToPath;
	private StringProcessor stringProcessor;

	public BrownClustersFeatureProcessor(String featureNamePrefix, String filename, int threshold, StringProcessor stringProcessor, int[] prefixLengths) {
		this.featureNamePrefix = featureNamePrefix;
		this.stringProcessor = stringProcessor;
		this.prefixLengths = prefixLengths;
		tokenToPath = new HashMap<String, String>();
		Profiler.start("BrownClustersFeatureProcessor.loadFromFile()");
		logger.info("Loading BrownClustersFeatureProcessor from file " + filename);
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
					String[] fields = line.split("\t");
					String path = fields[0];
					String word = fields[1];
					int occ = Integer.parseInt(fields[2]);
					if (occ > threshold) {
						tokenToPath.put(word, path);
					}
				}
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			// TODO Improve error handling
			throw new RuntimeException(e);
		}
		logger.info("Loaded " + tokenToPath.size() + " entries into BrownClustersFeatureProcessor");
		Profiler.stop("BrownClustersFeatureProcessor.loadFromFile()");
	}

	@Override
	public void process(TextInstance input, FeatureProcessorCallback featureProcessorCallback) {
		List<Token> tokens = input.getTokens();
		for (int tokenIndex = 0; tokenIndex < tokens.size(); tokenIndex++) {
			Token token = tokens.get(tokenIndex);
			Vector<String> featureVector = token.getFeatures();
			String tokenText = token.getText();
			String processedText = tokenText;
			if (stringProcessor != null) {
				processedText = stringProcessor.process(tokenText);
			}
			String path = tokenToPath.get(processedText);
			if (path != null) {
				String pathPrefix = path.substring(0, Math.min(path.length(), prefixLengths[0]));
				String featureName = featureNamePrefix + prefixLengths[0] + "=" + pathPrefix;
				logger.trace("Setting feature " + featureName + " = 1.0 for token text \"" + tokenText + "\"");
				featureProcessorCallback.callback(featureName, 1.0, featureVector);
				for (int j = 1; j < prefixLengths.length; j++) {
					if (prefixLengths[j - 1] < path.length()) {
						pathPrefix = path.substring(0, Math.min(path.length(), prefixLengths[j]));
						featureName = featureNamePrefix + prefixLengths[j] + "=" + pathPrefix;
						logger.trace("Setting feature " + featureName + " = 1.0 for token text \"" + tokenText + "\"");
						featureProcessorCallback.callback(featureName, 1.0, featureVector);
					}
				}
			}
		}
	}
}
