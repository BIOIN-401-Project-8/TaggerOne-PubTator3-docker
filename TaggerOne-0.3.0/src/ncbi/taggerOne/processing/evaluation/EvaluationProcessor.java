package ncbi.taggerOne.processing.evaluation;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import ncbi.taggerOne.processing.textInstance.TextInstanceProcessor;

public abstract class EvaluationProcessor extends TextInstanceProcessor {

	private static final long serialVersionUID = 1L;

	public static final String TP = "tp";
	public static final String FP = "fp";
	public static final String FN = "fn";
	public static final String P = "p";
	public static final String R = "r";
	public static final String F = "f";

	protected String scoreDetailPrefix;

	public EvaluationProcessor(String scoreDetailPrefix) {
		this.scoreDetailPrefix = scoreDetailPrefix;
	}

	// TODO Refactor to a getScore() method that returns a ScoreKeeper
	public abstract double score();

	public abstract String scoreDetail();

	public abstract Object2DoubleMap<String> getScores();

	// TODO Fix classes to not calculate overall when there's only one type
		
	public abstract EvaluationProcessor copy();

}
