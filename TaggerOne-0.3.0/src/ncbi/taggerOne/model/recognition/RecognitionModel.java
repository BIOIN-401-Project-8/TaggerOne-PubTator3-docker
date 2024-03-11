package ncbi.taggerOne.model.recognition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ncbi.taggerOne.processing.TrainingProgressTracker;
import ncbi.taggerOne.types.Segment;
import ncbi.taggerOne.types.Token;
import ncbi.taggerOne.util.Dictionary;
import ncbi.taggerOne.util.matrix.DenseByDenseMatrix;
import ncbi.taggerOne.util.matrix.Matrix;
import ncbi.taggerOne.util.vector.Vector;
import ncbi.util.Profiler;
import ncbi.util.SimpleComparator;

public class RecognitionModel implements RecognitionModelPredictor, RecognitionModelUpdater {

	private static final Logger logger = LoggerFactory.getLogger(RecognitionModel.class);
	private static final long serialVersionUID = 1L;

	protected TrainingProgressTracker trainingProgress;
	protected Dictionary<String> entityClassStates;
	protected Dictionary<String> featureSet;
	protected DenseByDenseMatrix<String, String> featureWeights;

	public RecognitionModel(Dictionary<String> featureSet, Dictionary<String> entityClassStates, TrainingProgressTracker trainingProgress) {
		if (!featureSet.isFrozen()) {
			throw new IllegalStateException("featureSet must be frozen");
		}
		if (!entityClassStates.isFrozen()) {
			throw new IllegalStateException("entityClassStates must be frozen");
		}
		this.featureSet = featureSet;
		this.entityClassStates = entityClassStates;
		featureWeights = new DenseByDenseMatrix<String, String>(entityClassStates, featureSet);
		this.trainingProgress = trainingProgress;
	}

	protected RecognitionModel(Dictionary<String> featureSet, Dictionary<String> entityClassStates, DenseByDenseMatrix<String, String> featureWeights, TrainingProgressTracker trainingProgress) {
		this.featureSet = featureSet;
		this.featureWeights = featureWeights;
		this.entityClassStates = entityClassStates;
		this.trainingProgress = trainingProgress;
	}

	public Dictionary<String> getEntityClassStates() {
		return entityClassStates;
	}

	public Dictionary<String> getFeatureSet() {
		return featureSet;
	}

	public TrainingProgressTracker getTrainingProgress() {
		return trainingProgress;
	}

	public Matrix<String, String> getWeights() {
		return featureWeights;
	}

	@Override
	public void visualize() {
		for (int stateIndex = 0; stateIndex < entityClassStates.size(); stateIndex++) {
			logger.info("Features for state \"" + entityClassStates.getElement(stateIndex) + "\":");
			Vector<String> featureWeightsForState = featureWeights.getRowVector(stateIndex);
			List<String> featureNames = new ArrayList<String>(featureSet.getElements());
			Collections.sort(featureNames, new RecognitionFeatureComparator(featureWeightsForState));
			for (String featureName : featureNames) {
				int featureIndex = featureSet.getIndex(featureName);
				double featureWeight = featureWeightsForState.get(featureIndex);
				if (featureWeight != 0.0) {
					logger.info("\t" + featureName + "=" + featureWeight);
				}
			}
		}
	}

	@Override
	public RecognitionModelPredictor compile() {
		return this;
	}

	private class RecognitionFeatureComparator extends SimpleComparator<String> {

		private static final long serialVersionUID = 1L;

		private Vector<String> featureWeightsForState;

		public RecognitionFeatureComparator(Vector<String> featureWeightsForState) {
			this.featureWeightsForState = featureWeightsForState;
		}

		@Override
		public int compare(String featureName1, String featureName2) {
			int featureIndex1 = featureSet.getIndex(featureName1);
			int featureIndex2 = featureSet.getIndex(featureName2);
			double featureWeight1 = featureWeightsForState.get(featureIndex1);
			double featureWeight2 = featureWeightsForState.get(featureIndex2);
			return Double.compare(featureWeight2, featureWeight1);
		}
	}

	@Override
	public double predict(String toState, Segment segment) {
		Profiler.start("RecognitionModel.predict()");
		int toStateIndex = entityClassStates.getIndex(toState);
		if (toStateIndex < 0) {
			logger.error("toState = " + toState + " index = " + toStateIndex + " segment = " + segment.getText());
		}
		double score = 0.0;
		Vector<String> featureWeightsForState = featureWeights.getRowVector(toStateIndex);
		score = segment.getFeatures().dotProduct(featureWeightsForState);
		for (Token token : segment.getTokens()) {
			score += token.getFeatures().dotProduct(featureWeightsForState);
		}
		Profiler.stop("RecognitionModel.predict()");
		return score;
	}

	@Override
	public void update(Matrix<String, String> updates) {
		Profiler.start("RecognitionModel.update()");
		featureWeights.increment(updates);
		Profiler.stop("RecognitionModel.update()");
	}
}
