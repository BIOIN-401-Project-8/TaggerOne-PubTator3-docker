package ncbi.taggerOne.processing.evaluation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import ncbi.taggerOne.T1Constants;
import ncbi.taggerOne.types.AnnotatedSegment;
import ncbi.taggerOne.types.TextInstance;
import ncbi.util.StaticUtilMethods;

public class AnnotationLevelEvaluationProcessor extends EvaluationProcessor {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(AnnotationLevelEvaluationProcessor.class);

	private Condition[] conditions;
	private ScoreKeeper overallScore;
	private Map<String, ScoreKeeper> scoresByType;

	public AnnotationLevelEvaluationProcessor(String scoreDetailPrefix, Condition... conditions) {
		super(scoreDetailPrefix);
		this.conditions = conditions;
		reset();
	}

	@Override
	public AnnotationLevelEvaluationProcessor copy() {
		return new AnnotationLevelEvaluationProcessor(scoreDetailPrefix, conditions);
	}

	@Override
	public void reset() {
		overallScore = new ScoreKeeper();
		scoresByType = new HashMap<String, ScoreKeeper>();
	}

	@Override
	public double score() {
		return overallScore.getF();
	}

	@Override
	public String scoreDetail() {
		StringBuilder detail = new StringBuilder();
		detail.append(String.format("%s\t%s\t%s\t%s%n", scoreDetailPrefix, Arrays.toString(conditions), T1Constants.OVERALL_EVALUATION, overallScore.scoreDetail()));
		for (String entityType : scoresByType.keySet()) {
			detail.append(String.format("%s\t%s\t%s\t%s%n", scoreDetailPrefix, Arrays.toString(conditions), entityType, scoresByType.get(entityType).scoreDetail()));
		}
		return detail.toString().trim();
	}

	@Override
	public Object2DoubleMap<String> getScores() {
		Object2DoubleMap<String> scores = new Object2DoubleOpenHashMap<String>();
		Object2DoubleMap<String> overallScores = overallScore.getScores();
		for (String s : overallScores.keySet()) {
			String s2 = String.format("%s\t%s\t%s", Arrays.toString(conditions), T1Constants.OVERALL_EVALUATION, s);
			scores.put(s2, overallScores.get(s));
		}
		for (String entityType : scoresByType.keySet()) {
			Object2DoubleMap<String> scoresForType = scoresByType.get(entityType).getScores();
			for (String s : scoresForType.keySet()) {
				String s2 = String.format("%s\t%s\t%s", Arrays.toString(conditions), entityType, s);
				scores.put(s2, scoresForType.get(s));
			}
		}
		return scores;
	}

	@Override
	public void process(TextInstance input) {

		List<AnnotatedSegment> targetAnnotation = input.getTargetAnnotation();
		List<AnnotatedSegment> predictedAnnotation = input.getPredictedAnnotations().getObject(0);

		for (AnnotatedSegment segment : targetAnnotation) {
			if (find(segment, predictedAnnotation)) {
				overallScore.incrementTp();
				getScoreKeeper(segment.getEntityClass()).incrementTp();
				logger.trace(input.getInstanceId() + "\tTP\t" + segment.getEntityClass() + "\t" + segment.getStartChar() + "\t" + segment.getEndChar() + "\t" + segment.getText());
			} else {
				overallScore.incrementFn();
				getScoreKeeper(segment.getEntityClass()).incrementFn();
				logger.trace(input.getInstanceId() + "\tFN\t" + segment.getEntityClass() + "\t" + segment.getStartChar() + "\t" + segment.getEndChar() + "\t" + segment.getText());
			}
		}

		for (AnnotatedSegment segment : predictedAnnotation) {
			if (!find(segment, targetAnnotation)) {
				overallScore.incrementFp();
				getScoreKeeper(segment.getEntityClass()).incrementFp();
				logger.trace(input.getInstanceId() + "\tFP\t" + segment.getEntityClass() + "\t" + segment.getStartChar() + "\t" + segment.getEndChar() + "\t" + segment.getText());
			}
		}
	}

	private ScoreKeeper getScoreKeeper(String entityType) {
		ScoreKeeper scoreKeeperForType = scoresByType.get(entityType);
		if (scoreKeeperForType == null) {
			scoreKeeperForType = new ScoreKeeper();
			scoresByType.put(entityType, scoreKeeperForType);
		}
		return scoreKeeperForType;
	}

	private boolean find(AnnotatedSegment segment1, List<AnnotatedSegment> annotations) {
		// TODO PERFORMANCE and DESIGN
		for (AnnotatedSegment segment2 : annotations) {
			boolean match = true;
			for (Condition condition : conditions) {
				if (!condition.check(segment1, segment2)) {
					match = false;
				}
			}
			if (match) {
				return true;
			}
		}
		return false;
	}

	public enum Condition {
		EXACT_BOUNDARY {
			@Override
			public boolean check(AnnotatedSegment segment1, AnnotatedSegment segment2) {
				return segment1.getStartChar() == segment2.getStartChar() && segment1.getEndChar() == segment2.getEndChar();
			}
		},
		OVERLAP_BOUNDARY {
			@Override
			public boolean check(AnnotatedSegment segment1, AnnotatedSegment segment2) {
				return segment1.getStartChar() < segment2.getEndChar() && segment2.getStartChar() < segment1.getEndChar();
			}
		},
		ENTITY_CLASS {
			@Override
			public boolean check(AnnotatedSegment segment1, AnnotatedSegment segment2) {
				return segment1.getEntityClass().equals(segment2.getEntityClass());
			}
		},
		ENTITY_ID {
			@Override
			public boolean check(AnnotatedSegment segment1, AnnotatedSegment segment2) {
				return StaticUtilMethods.equalElements(segment1.getEntities(), segment2.getEntities());
			}
		};

		public abstract boolean check(AnnotatedSegment segment1, AnnotatedSegment segment2);
	}
}
