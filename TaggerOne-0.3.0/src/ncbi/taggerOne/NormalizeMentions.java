package ncbi.taggerOne;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import javax.xml.stream.XMLStreamException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bioc.BioCAnnotation;
import bioc.BioCCollection;
import bioc.BioCDocument;
import bioc.BioCPassage;
import bioc.io.BioCDocumentWriter;
import bioc.io.BioCFactory;
import bioc.io.woodstox.ConnectorWoodstox;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import ncbi.taggerOne.abbreviation.AbbreviationSource;
import ncbi.taggerOne.abbreviation.AbbreviationSourceProcessor;
import ncbi.taggerOne.abbreviation.FileAbbreviationSource;
import ncbi.taggerOne.lexicon.Lexicon;
import ncbi.taggerOne.model.normalization.NormalizationModelPredictor;
import ncbi.taggerOne.model.recognition.RecognitionModelPredictor;
import ncbi.taggerOne.processing.SentenceBreaker;
import ncbi.taggerOne.processing.analysis.OutputAnalysisProcessor;
import ncbi.taggerOne.processing.postProcessing.AbsoluteConsistencyPostProcessing;
import ncbi.taggerOne.processing.postProcessing.CoordinationPostProcessor;
import ncbi.taggerOne.processing.postProcessing.FalseModifierRemover;
import ncbi.taggerOne.processing.postProcessing.FilterByPattern;
import ncbi.taggerOne.processing.textInstance.AbbreviationResolverProcessor;
import ncbi.taggerOne.processing.textInstance.Annotator;
import ncbi.taggerOne.processing.textInstance.InstanceElementClearer;
import ncbi.taggerOne.processing.textInstance.InstanceElementClearer.InstanceElement;
import ncbi.taggerOne.processing.textInstance.SegmentMentionProcessor;
import ncbi.taggerOne.processing.textInstance.Segmenter;
import ncbi.taggerOne.processing.textInstance.TextInstanceProcessingPipeline;
import ncbi.taggerOne.processing.textInstance.TextInstanceProcessor;
import ncbi.taggerOne.types.AnnotatedSegment;
import ncbi.taggerOne.types.Entity;
import ncbi.taggerOne.types.MentionName;
import ncbi.taggerOne.types.Segment;
import ncbi.taggerOne.types.TextInstance;
import ncbi.taggerOne.types.Token;
import ncbi.taggerOne.util.AbbreviationResolver;
import ncbi.util.Profiler;
import ncbi.taggerOne.processing.mentionName.MentionNameProcessor;
import ncbi.taggerOne.util.RankedList;

public class NormalizeMentions {

	private static final Logger logger = LoggerFactory.getLogger(ProcessText.class);
	private static final String TMP_FILE_PREFIX = "tmp";

	public static void main(String[] args) throws IOException, ClassNotFoundException, XMLStreamException {
		OptionParser parser = new OptionParser();
		// Input data
		OptionSpec<String> input = parser.accepts("input").withRequiredArg().ofType(String.class).required();
		OptionSpec<String> output = parser.accepts("output").withRequiredArg().ofType(String.class).required();
		OptionSpec<String> modelInputFilename = parser.accepts("modelInputFilename").withRequiredArg().ofType(String.class).required();
		OptionSpec<Boolean> compileModel = parser.accepts("compileModel").withRequiredArg().ofType(Boolean.class).defaultsTo(true);
		OptionSpec<String> abbreviationDir = parser.accepts("abbreviationDir").withRequiredArg().ofType(String.class);
		// TODO Add options for post-processing
		OptionSet options = parser.parse(args);
		// TODO Validate
		logger.info("Command line options:");
		for (OptionSpec<?> spec : options.specs()) {
			StringBuilder str = new StringBuilder();
			List<String> optionNames = spec.options();
			if (optionNames.size() == 1) {
				str.append(optionNames.get(0));
			} else {
				str.append(optionNames.toString());
			}
			str.append(" = ");
			List<?> values = spec.values(options);
			if (values.size() == 1) {
				str.append(values.get(0).toString());
			} else {
				str.append(values.toString());
			}
			logger.info("\t" + str.toString());
		}

		// Load the annotation pipeline
		logger.info("Loading model");
		long start = System.currentTimeMillis();
		ObjectInputStream ois = new ObjectInputStream(new GZIPInputStream(new FileInputStream(options.valueOf(modelInputFilename))));
		TextInstanceProcessingPipeline originalAnnotationPipeline = (TextInstanceProcessingPipeline) ois.readObject();
		ois.close();
		List<TextInstanceProcessor> originalProcessors = originalAnnotationPipeline.getProcessors();
		AbbreviationResolverProcessor abbreviationResolverProcessor = (AbbreviationResolverProcessor) originalProcessors.get(3);
		AbbreviationResolver abbreviationResolver = abbreviationResolverProcessor.getAbbreviationResolver();
		SegmentMentionProcessor segmentMentionProcessor = (SegmentMentionProcessor) originalProcessors.get(4);
		MentionNameProcessor mentionNameProcessor = segmentMentionProcessor.getProcessor();
		Annotator originalAnnotator = (Annotator) originalProcessors.get(5);
		Map<String, NormalizationModelPredictor> originalNormalizationPredictorModels = originalAnnotator.getNormalizationModels();
		Map<String, NormalizationModelPredictor> normalizationPredictorModels = originalNormalizationPredictorModels;
		logger.info("Elapsed = " + (System.currentTimeMillis() - start));

		// Prepare abbreviations source
		logger.info("Loading abbreviations");
		start = System.currentTimeMillis();
		// For each file in dir, load abbreviations into abbreviationResolver
		String abbreviationDirStr = options.valueOf(abbreviationDir);
		if (!abbreviationDirStr.endsWith("/")) {
			abbreviationDirStr = abbreviationDirStr + "/";
		}
		File[] abbreviationFiles = (new File(abbreviationDirStr)).listFiles();
		for (int i = 0; i < abbreviationFiles.length; i++) {
			if (abbreviationFiles[i].isFile()) {
				FileAbbreviationSource abbreviationLoader = new FileAbbreviationSource();
				String abbreviationFilename = abbreviationDirStr + abbreviationFiles[i].getName();
				logger.debug("Loading abbreviations from file " + abbreviationFilename);
				abbreviationLoader.loadAbbreviations(abbreviationFilename);
				Map<String, Map<String, String>> abbreviations = abbreviationLoader.getAllAbbreviations();
				for (String documentId : abbreviations.keySet()) {
					abbreviationResolver.addAbbreviations(documentId, abbreviations.get(documentId));
				}
			}
		}
		logger.info("Elapsed = " + (System.currentTimeMillis() - start));

		// Compile model
		if (options.valueOf(compileModel)) {
			logger.info("Compiling model");
			RecognitionModelPredictor recognitionModel = originalAnnotator.getRecognitionModel().compile();
			normalizationPredictorModels = new HashMap<String, NormalizationModelPredictor>();
			for (String entityType : originalNormalizationPredictorModels.keySet()) {
				NormalizationModelPredictor originalPredictor = originalNormalizationPredictorModels.get(entityType);
				NormalizationModelPredictor newPredictor = originalPredictor.compile();
				normalizationPredictorModels.put(entityType, newPredictor);
			}
			logger.info("Elapsed = " + (System.currentTimeMillis() - start));
		}

		// Process file(s)
		String inputFilename = options.valueOf(input);
		String outputFilename = options.valueOf(output);
		logger.debug("inputFilename = " + inputFilename);
		logger.debug("outputFilename = " + outputFilename);
		File inFile = new File(inputFilename);
		File outFile = new File(outputFilename);
		if (inFile.isDirectory()) {
			throw new IllegalArgumentException("Input must be a TSV file");
		}
		if (outFile.isDirectory()) {
			throw new IllegalArgumentException("Output must be a file");
		}
		processTSV(inputFilename, outputFilename, normalizationPredictorModels, mentionNameProcessor, abbreviationResolver);
		Profiler.print("\t");
		logger.info("Done.");
	}

	private static void processTSV(String inputFilename, String outputFilename, Map<String, NormalizationModelPredictor> normalizationPredictorModels, MentionNameProcessor mentionNameProcessor, AbbreviationResolver abbreviationResolver)
			throws IOException {

	
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFilename), T1Constants.UTF8_FORMAT));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFilename), T1Constants.UTF8_FORMAT));
		String line = reader.readLine();
		Map<String, String> titles = new HashMap<String, String>();
		while (line != null) {
			line = line.trim();
			String[] fields = line.split("\t");
			if (fields.length != 3) {
				throw new RuntimeException("Number of fields must be 3: " + fields.length);
			}
			String documentId = fields[0];
			String mentionText = fields[1];
			String type = fields[2];
			// String identifier = fields[3];
			NormalizationModelPredictor normalizationModel = normalizationPredictorModels.get(type);
			logger.debug("normalizationModel is " + normalizationModel);
			
			if (normalizationModel != null) {
				logger.debug("documentId = " + documentId);
				logger.debug("Mention = " + mentionText);
				String expandedText = abbreviationResolver.expandAbbreviations(documentId, mentionText);
				logger.debug("Expanded = " + expandedText);
				MentionName mentionName = new MentionName(expandedText);
				mentionNameProcessor.process(mentionName);
				logger.debug("tokens = " + mentionName.getTokens());
				logger.debug("vector = " + mentionName.getVector());
				
				if (logger.isDebugEnabled()) {
					RankedList<Entity> bestEntities = new RankedList<Entity>(5);
					normalizationModel.findBest(mentionName.getVector(), bestEntities);
					for (int i = 0; i < bestEntities.size(); i++) {
						Entity entity = bestEntities.getObject(i);
						double score = bestEntities.getValue(i);
						logger.debug(i + "\t" + score + "\t" + entity.getPrimaryIdentifier() + "\t" + entity.getPrimaryName().getName() + "\t" + entity.getPrimaryName().getVector());
					
					}
				}
				
				RankedList<Entity> bestEntities = new RankedList<Entity>(1);
				normalizationModel.findBest(mentionName.getVector(), bestEntities);
				writer.write(documentId + "\t" + mentionText + "\t" + type);
				if (bestEntities.size() == 0) {
					writer.write("\t\t\n");
				} else {
					Entity entity = bestEntities.getObject(0);
					writer.write("\t" + entity.getPrimaryIdentifier() + "\t" + entity.getPrimaryName().getName() + "\n");
				}
			}
			line = reader.readLine();
		}
		reader.close();
		writer.close();
	}

	private static String visualizeIdentifiers(Set<Entity> entities) {
		List<String> entityIDs = new ArrayList<String>();
		for (Entity entity : entities) {
			if (entity != null) {
				String primaryIdentifier = entity.getPrimaryIdentifier();
				if (!primaryIdentifier.startsWith(T1Constants.UNKNOWN_ENTITY_ID_PREFIX) && !primaryIdentifier.equals(T1Constants.NONENTITY_STATE)) {
					entityIDs.add(primaryIdentifier);
				}
			}
		}
		if (entityIDs.size() == 0) {
			return null;
		}
		Collections.sort(entityIDs);
		StringBuilder identifiers = new StringBuilder();
		identifiers.append(entityIDs.get(0));
		for (int i = 1; i < entityIDs.size(); i++) {
			identifiers.append("|");
			identifiers.append(entityIDs.get(i));
		}
		return identifiers.toString();
	}
}
