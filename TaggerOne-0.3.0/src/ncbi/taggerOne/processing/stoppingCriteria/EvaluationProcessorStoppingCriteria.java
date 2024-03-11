package ncbi.taggerOne.processing.stoppingCriteria;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ncbi.taggerOne.processing.TrainingProgressTracker;
import ncbi.taggerOne.processing.evaluation.EvaluationProcessor;
import ncbi.taggerOne.processing.textInstance.AnnotationModelTrainer;
import ncbi.taggerOne.processing.textInstance.TextInstanceProcessingPipeline;
import ncbi.taggerOne.types.TextInstance;

public class EvaluationProcessorStoppingCriteria extends StoppingCriteria {

	private static final Logger logger = LoggerFactory.getLogger(AnnotationModelTrainer.class);
	private static final long serialVersionUID = 1L;

	private int iterationsSinceImprovement;
	private double highestScore;
	private int previousIteration;
	private List<TextInstance> holdoutInstances;
	private TextInstanceProcessingPipeline preEvaluationPipeline;
	private TextInstanceProcessingPipeline postEvaluationPipeline;
	private List<EvaluationProcessor> evaluationProcessors;
	private List<EvaluationProcessor> allProcessors;
	private TextInstanceProcessingPipeline annotationPipeline;
	private ModelNameFormatter modelNameFormatter;
	private String previousModelOutputFilename;

	public EvaluationProcessorStoppingCriteria(int maxIterations, int iterationsSinceImprovement, TrainingProgressTracker callback, List<TextInstance> holdoutInstances, TextInstanceProcessingPipeline preEvaluationPipeline,
			TextInstanceProcessingPipeline postEvaluationPipeline, TextInstanceProcessingPipeline annotationPipeline, ModelNameFormatter modelNameFormatter, List<EvaluationProcessor> evaluationProcessors,
			List<EvaluationProcessor> additionalProcessors) {
		super(maxIterations, callback);
		this.iterationsSinceImprovement = iterationsSinceImprovement;
		this.highestScore = 0.0;
		this.previousIteration = 0;
		this.holdoutInstances = holdoutInstances;
		this.preEvaluationPipeline = preEvaluationPipeline;
		this.postEvaluationPipeline = postEvaluationPipeline;
		this.annotationPipeline = annotationPipeline;
		this.modelNameFormatter = modelNameFormatter;
		this.evaluationProcessors = evaluationProcessors;
		this.allProcessors = new ArrayList<EvaluationProcessor>(evaluationProcessors);
		this.allProcessors.addAll(additionalProcessors);
		this.previousModelOutputFilename = null;
	}

	@Override
	public boolean stop() {
		if (super.stop()) {
			return true;
		}

		int currentIteration = callback.getIteration();

		// Initial model assigns a score of 0.0 to everything, need to do some training before checking holdout set
		if (currentIteration == 0) {
			return false;
		}

		preEvaluationPipeline.reset();
		postEvaluationPipeline.reset();
		annotationPipeline.reset();

		for (EvaluationProcessor evaluationProcessor : allProcessors) {
			evaluationProcessor.reset();
		}
		for (TextInstance instance : holdoutInstances) {
			preEvaluationPipeline.process(instance);
			logger.debug("Instance " + instance.getInstanceId() + " made " + instance.getPredictedAnnotations().getObject(0).size() + " predictions");
			for (EvaluationProcessor evaluationProcessor : allProcessors) {
				evaluationProcessor.process(instance);
			}
			postEvaluationPipeline.process(instance);
		}
		for (EvaluationProcessor evaluationProcessor : allProcessors) {
			logger.debug(evaluationProcessor.scoreDetail());
		}
		double invsum = 0.0;
		for (EvaluationProcessor evaluationProcessor : evaluationProcessors) {
			invsum += 1.0 / evaluationProcessor.score();
		}
		double score = evaluationProcessors.size() / invsum;
		logger.info("Evaluation score is " + score + " for iteration " + currentIteration);
		if (score > highestScore) {
			highestScore = score;
			previousIteration = currentIteration;
			outputModel();
			return false;
		} else if (currentIteration - previousIteration > iterationsSinceImprovement) {
			return true;
		} else {
			return false;
		}
	}

	private void outputModel() {
		if (modelNameFormatter == null) {
			return;
		}
		// Write out the model
		try {
			String modelOutputFilename = modelNameFormatter.getModelOutputFilename();
			logger.info("Writing updated model to file " + modelOutputFilename);
			ObjectOutputStream oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(modelOutputFilename)));
			oos.writeObject(annotationPipeline);
			oos.close();
			// Write was successful, delete previous model if there is one
			if (previousModelOutputFilename != null && !modelOutputFilename.equals(previousModelOutputFilename)) {
				logger.info("Deleting previous model " + previousModelOutputFilename);
				Path previousPath = Paths.get(previousModelOutputFilename);
				Files.deleteIfExists(previousPath);
			}
			previousModelOutputFilename = modelOutputFilename;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public double getHighestScore() {
		return highestScore;
	}
}
