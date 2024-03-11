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
import ncbi.taggerOne.lexicon.Index;
import ncbi.taggerOne.processing.mentionName.MentionNameProcessor;
import ncbi.taggerOne.util.Dictionary;
import ncbi.taggerOne.util.RankedList;
import ncbi.taggerOne.util.vector.Vector;

public class DumpLexicon {

	private static final Logger logger = LoggerFactory.getLogger(ProcessText.class);
	private static final String TMP_FILE_PREFIX = "tmp";

	public static void main(String[] args) throws IOException, ClassNotFoundException, XMLStreamException {
		OptionParser parser = new OptionParser();
		// Input data
		OptionSpec<String> output = parser.accepts("output").withRequiredArg().ofType(String.class).required();
		OptionSpec<String> modelInputFilename = parser.accepts("modelInputFilename").withRequiredArg().ofType(String.class).required();
		OptionSet options = parser.parse(args);
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
		
		String outputFilename = options.valueOf(output);
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFilename), T1Constants.UTF8_FORMAT));
		Lexicon lexicon = originalAnnotator.getLexicon();
		Dictionary<String> entityTypes = lexicon.getEntityTypes();
		for (String entityType : entityTypes.getElements()) {
			Index index = lexicon.getIndex(entityType);
			Dictionary<Vector<String>> nameVectors = index.getNameVectorDictionary();
			for (Vector<String> nameVector : nameVectors.getElements()) {
				Set<Entity> entitySet = index.getEntities(nameVector);
				String identifiers = Entity.visualizePrimaryIdentifiers(entitySet);
				writer.write(entityType + "\t" + nameVector + "\t" + identifiers + "\n");
			}
		}
		writer.close();
		logger.info("Done.");
	}
}
