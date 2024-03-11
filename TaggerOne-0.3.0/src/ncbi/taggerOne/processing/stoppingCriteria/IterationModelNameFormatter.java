package ncbi.taggerOne.processing.stoppingCriteria;

import ncbi.taggerOne.T1Constants;
import ncbi.taggerOne.processing.TrainingProgressTracker;

public class IterationModelNameFormatter implements ModelNameFormatter {

	private static final int ITERATION_COUNTER_LENGTH = 3;

	private String baseFilename;
	private TrainingProgressTracker tracker;

	public IterationModelNameFormatter(String baseFilename, TrainingProgressTracker tracker) {
		this.baseFilename = baseFilename;
		this.tracker = tracker;
	}

	@Override
	public String getModelOutputFilename() {
		int iteration = tracker.getIteration();
		String iterationStr = Integer.toString(iteration);
		while (iterationStr.length() < ITERATION_COUNTER_LENGTH) {
			iterationStr = "0" + iterationStr;
		}
		return baseFilename + "_" + iterationStr + T1Constants.MODEL_FILENAME_EXTENSION;
	}

}
