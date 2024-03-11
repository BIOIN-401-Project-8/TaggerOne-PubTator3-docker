package ncbi.taggerOne;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import ncbi.taggerOne.lexicon.Lexicon;
import ncbi.taggerOne.model.normalization.AveragedNormalizationModel;
import ncbi.taggerOne.model.normalization.CachedNormalizationModel;
import ncbi.taggerOne.model.normalization.LowMemCompiledNormalizationModel;
import ncbi.taggerOne.model.normalization.NormalizationModel;
import ncbi.taggerOne.model.normalization.NormalizationModelPredictor;
import ncbi.taggerOne.model.recognition.RecognitionModelPredictor;
import ncbi.taggerOne.processing.textInstance.Annotator;
import ncbi.taggerOne.processing.textInstance.TextInstanceProcessingPipeline;
import ncbi.taggerOne.processing.textInstance.TextInstanceProcessor;

public class CompileModel {

	private static final Logger logger = LoggerFactory.getLogger(CompileModel.class);

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		OptionParser parser = new OptionParser();
		// Input data
		OptionSpec<String> modelInputFilename = parser.accepts("modelInputFilename").withRequiredArg().ofType(String.class).required();
		OptionSpec<String> modelOutputFilename = parser.accepts("modelOutputFilename").withRequiredArg().ofType(String.class).required();
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
		List<TextInstanceProcessor> processors = originalAnnotationPipeline.getProcessors();
		Annotator originalAnnotator = (Annotator) processors.get(5);
		Lexicon lexicon = originalAnnotator.getLexicon();
		Map<String, NormalizationModelPredictor> originalNormalizationPredictorModels = originalAnnotator.getNormalizationModels();
		Map<String, NormalizationModelPredictor> newNormalizationPredictorModels = new HashMap<String, NormalizationModelPredictor>();
		logger.info("Elapsed = " + (System.currentTimeMillis() - start));

		// Compile recognition model
		logger.info("Compiling recognition model");
		start = System.currentTimeMillis();
		RecognitionModelPredictor recognitionModel = originalAnnotator.getRecognitionModel().compile();
		logger.info("Elapsed = " + (System.currentTimeMillis() - start));

		// Compile normalization models
		logger.info("Compiling normalization models");
		start = System.currentTimeMillis();
		for (String entityType : originalNormalizationPredictorModels.keySet()) {
			NormalizationModelPredictor originalPredictor = originalNormalizationPredictorModels.get(entityType);
			int maxCacheSize = ((CachedNormalizationModel) originalPredictor).getMaxCacheSize();
			NormalizationModelPredictor wrappedPredictor = ((CachedNormalizationModel) originalPredictor).getWrappedPredictor();
			CachedNormalizationModel newPredictor = null;
			if (wrappedPredictor instanceof AveragedNormalizationModel) {
				// Only performing a light compile
				logger.info("Compiling AveragedNormalizationModel to NormalizationModel");
				NormalizationModel compiledModel = ((AveragedNormalizationModel) wrappedPredictor).compileShallow();
				newPredictor = new CachedNormalizationModel(compiledModel, maxCacheSize);
			} else if (wrappedPredictor instanceof NormalizationModel) {
				// Only performing a light compile
				logger.info("Compiling NormalizationModel to LowMemCompiledNormalizationModel");
				LowMemCompiledNormalizationModel compiledModel = new LowMemCompiledNormalizationModel((NormalizationModel) wrappedPredictor);
				newPredictor = new CachedNormalizationModel(compiledModel, maxCacheSize);
			} else {
				logger.info("Performing a deep compile of " + wrappedPredictor.getClass().getCanonicalName());
				NormalizationModelPredictor compiledModel = ((AveragedNormalizationModel) wrappedPredictor).compile();
				newPredictor = new CachedNormalizationModel(compiledModel, maxCacheSize);
			}
			newNormalizationPredictorModels.put(entityType, newPredictor);
		}
		logger.info("Elapsed = " + (System.currentTimeMillis() - start));

		logger.info("Writing updated model to file " + options.valueOf(modelOutputFilename));
		ObjectOutputStream oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(options.valueOf(modelOutputFilename))));
		Annotator newAnnotator = new Annotator(lexicon, recognitionModel, newNormalizationPredictorModels);
		processors.set(5, newAnnotator);
		TextInstanceProcessingPipeline annotationPipeline = new TextInstanceProcessingPipeline(processors);
		oos.writeObject(annotationPipeline);
		oos.close();

		logger.info("Done.");
	}

}
