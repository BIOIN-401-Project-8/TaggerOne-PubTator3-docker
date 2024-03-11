package ncbi.taggerOne.model.recognition;

import ncbi.taggerOne.processing.TrainingProgressTracker;
import ncbi.taggerOne.types.Segment;
import ncbi.taggerOne.types.Token;
import ncbi.taggerOne.util.Dictionary;
import ncbi.taggerOne.util.matrix.DenseByDenseMatrix;
import ncbi.taggerOne.util.matrix.Matrix;
import ncbi.taggerOne.util.vector.Vector;
import ncbi.util.Profiler;

public class AveragedRecognitionModel extends RecognitionModel {

	private static final long serialVersionUID = 1L;

	protected DenseByDenseMatrix<String, String> featureWeights2;

	public AveragedRecognitionModel(Dictionary<String> featureSet, Dictionary<String> entityClassStates, TrainingProgressTracker trainingProgress) {
		super(featureSet, entityClassStates, trainingProgress);
		featureWeights2 = new DenseByDenseMatrix<String, String>(entityClassStates, featureSet);
	}

	public RecognitionModelPredictor getTrainingPredictor() {
		return new RecognitionModel(featureSet, entityClassStates, featureWeights, trainingProgress);
	}

	@Override
	public RecognitionModelPredictor compile() {
		double factor = -1.0 / trainingProgress.getInstances();
		DenseByDenseMatrix<String, String> compiledFeatureWeights = new DenseByDenseMatrix<String, String>(entityClassStates, featureSet);
		for (int i = 0; i < entityClassStates.size(); i++) {
			compiledFeatureWeights.incrementRow(i, featureWeights.getRowVector(i));
			compiledFeatureWeights.incrementRow(i, factor, featureWeights2.getRowVector(i));
		}
		return new RecognitionModel(featureSet, entityClassStates, compiledFeatureWeights, trainingProgress);
	}

	@Override
	public double predict(String toState, Segment segment) {
		Profiler.start("AveragedRecognitionModel.predict()");
		int toStateIndex = entityClassStates.getIndex(toState);
		Vector<String> featureVector = segment.getFeatures();
		double score = featureVector.dotProduct(featureWeights.getRowVector(toStateIndex)) - featureVector.dotProduct(featureWeights2.getRowVector(toStateIndex)) / trainingProgress.getInstances();
		for (Token token : segment.getTokens()) {
			featureVector = token.getFeatures();
			score += featureVector.dotProduct(featureWeights.getRowVector(toStateIndex)) - featureVector.dotProduct(featureWeights2.getRowVector(toStateIndex)) / trainingProgress.getInstances();
		}
		Profiler.stop("AveragedRecognitionModel.predict()");
		return score;
	}

	@Override
	public void update(Matrix<String, String> updates) {
		Profiler.start("AveragedRecognitionModel.update()");
		featureWeights.increment(updates);
		featureWeights2.increment(trainingProgress.getInstances(), updates);
		Profiler.stop("AveragedRecognitionModel.update()");
	}
}
