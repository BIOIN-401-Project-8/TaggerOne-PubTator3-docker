package ncbi.taggerOne.processing.textInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ncbi.taggerOne.T1Constants;
import ncbi.taggerOne.lexicon.Lexicon;
import ncbi.taggerOne.model.normalization.NormalizationModelPredictor;
import ncbi.taggerOne.types.AnnotatedSegment;
import ncbi.taggerOne.types.Entity;
import ncbi.taggerOne.types.TextInstance;
import ncbi.taggerOne.util.RankedList;
import ncbi.taggerOne.util.vector.Vector;
import ncbi.util.Profiler;

public class UnknownNormalizationIterationWrapper extends TextInstanceProcessor {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(UnknownNormalizationIterationWrapper.class);

	private AnnotationModelTrainingIteration trainingIteration;
	private Lexicon lexicon;
	private Map<String, ? extends NormalizationModelPredictor> normalizationPredictionModels;

	public UnknownNormalizationIterationWrapper(AnnotationModelTrainingIteration trainingIteration, Lexicon lexicon, Map<String, ? extends NormalizationModelPredictor> normalizationPredictionModels) {
		this.trainingIteration = trainingIteration;
		this.lexicon = lexicon;
		this.normalizationPredictionModels = normalizationPredictionModels;
	}

	@Override
	public void process(TextInstance input) {
		Profiler.start("UnknownNormalizationIterationWrapper.process()");

		// Get a copy of the instance with the any entities replaced with best guesses
		TextInstance inputCopy = input.copy();
		List<AnnotatedSegment> targetStateSequence = guessAnyEntities(inputCopy.getTargetStateSequence());
		inputCopy.setTargetStateSequence(targetStateSequence);

		// Send the clone to the trainingIteration
		trainingIteration.process(inputCopy);

		// TODO Is there any cleanup needed?
		Profiler.stop("UnknownNormalizationIterationWrapper.process()");
	}

	private List<AnnotatedSegment> guessAnyEntities(List<AnnotatedSegment> segments) {
		Profiler.start("UnknownNormalizationIterationWrapper.guessAnyEntities()");
		boolean updated = false;
		List<AnnotatedSegment> segments2 = new ArrayList<AnnotatedSegment>();
		for (AnnotatedSegment segment : segments) {
			String state = segment.getEntityClass();
			Entity anyEntityForState = lexicon.getAnyEntity(state);
			Set<Entity> entities = segment.getEntities();
			boolean notNon = !state.equals(T1Constants.NONENTITY_STATE);
			boolean containsUnknown = entities.contains(anyEntityForState);
			boolean resolve = notNon && containsUnknown;
			AnnotatedSegment segment2 = segment.copy();
			if (resolve) {
				if (entities.size() > 1) {
					throw new RuntimeException("Any entity must be singleton");
				}
				RankedList<Entity> bestEntities = resolveAnyEntity(segment2);
				Entity entity2 = bestEntities.getObject(0);
				double entityScore2 = bestEntities.getValue(0);
				logger.debug("Resolved mention text \"" + segment.getText() + "\" to entity " + entity2.getPrimaryIdentifier() + ": " + entity2.getPrimaryName().getName() + " score = " + entityScore2);
				updated = true;
				segment2.setEntities(Collections.singleton(entity2), entityScore2);
			}
			segments2.add(segment2);
		}
		if (updated) {
			logger.info("Raw target = " + AnnotatedSegment.visualizeStates(segments));
			logger.info("Resolved target = " + AnnotatedSegment.visualizeStates(segments2));
		}
		Profiler.stop("UnknownNormalizationIterationWrapper.guessAnyEntities()");
		return segments2;
	}

	private RankedList<Entity> resolveAnyEntity(AnnotatedSegment segment) {
		Profiler.start("UnknownNormalizationIterationWrapper.resolveAnyEntity()");
		String entityClass = segment.getEntityClass();
		NormalizationModelPredictor normalizationModelPredictor = normalizationPredictionModels.get(entityClass);
		Vector<String> mentionVector = segment.getMentionName().getVector();
		RankedList<Entity> bestEntities = new RankedList<Entity>(1);
		if (mentionVector == null) {
			logger.error("Mention vector is null");
			bestEntities.add(0.0, lexicon.getUnknownEntity(entityClass));
			return bestEntities;
		}
		normalizationModelPredictor.findBest(segment.getMentionName().getVector(), bestEntities);
		if (bestEntities.size() == 0) {
			logger.error("Lookup returned no matches");
			bestEntities.add(0.0, lexicon.getUnknownEntity(entityClass));
		}
		Profiler.stop("UnknownNormalizationIterationWrapper.resolveAnyEntity()");
		return bestEntities;
	}
}
