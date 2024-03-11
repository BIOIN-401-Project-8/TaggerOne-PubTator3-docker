package ncbi.taggerOne.processing.postProcessing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import ncbi.taggerOne.processing.textInstance.TextInstanceProcessor;
import ncbi.taggerOne.types.AnnotatedSegment;
import ncbi.taggerOne.types.Entity;
import ncbi.taggerOne.types.Segment;
import ncbi.taggerOne.types.TextInstance;
import ncbi.taggerOne.util.RankedList;
import ncbi.util.StaticUtilMethods;

public class AbsoluteConsistencyPostProcessing extends TextInstanceProcessor {

	private static final Logger logger = LoggerFactory.getLogger(AbsoluteConsistencyPostProcessing.class);
	private static final long serialVersionUID = 1L;

	private Set<Entity> nonEntitySet;

	public AbsoluteConsistencyPostProcessing(Entity nonEntity) {
		this.nonEntitySet = Collections.singleton(nonEntity);
	}

	@Override
	public void processAll(List<TextInstance> input) {

		Map<String, Object2IntOpenHashMap<Set<Entity>>> mentionTextToEntityCounts = new HashMap<String, Object2IntOpenHashMap<Set<Entity>>>();
		Object2DoubleOpenHashMap<Set<Entity>> entities2EntityScore = new Object2DoubleOpenHashMap<Set<Entity>>();
		entities2EntityScore.defaultReturnValue(0.0);
		
		// Get mentions and their count with annotations
		for (TextInstance instance : input) {
			List<AnnotatedSegment> predictedAnnotation = instance.getPredictedAnnotations().getObject(0);
			for (AnnotatedSegment segment : predictedAnnotation) {
				String mentionText = segment.getText();
				Object2IntOpenHashMap<Set<Entity>> countsForMention = mentionTextToEntityCounts.get(mentionText);
				if (countsForMention == null) {
					countsForMention = new Object2IntOpenHashMap<Set<Entity>>();
					countsForMention.defaultReturnValue(0);
					mentionTextToEntityCounts.put(mentionText, countsForMention);
				}
				Set<Entity> entitities = segment.getEntities();
				countsForMention.addTo(entitities, 1);
				entities2EntityScore.put(entitities, segment.getEntityScore());
			}
		}

		// For mentions without annotations, add non
		for (TextInstance instance : input) {
			List<AnnotatedSegment> predictedAnnotations = instance.getPredictedAnnotations().getObject(0);
			for (Segment segment : instance.getSegments()) {
				// Determine if this text has at least one annotation
				String mentionText = segment.getText();
				Object2IntOpenHashMap<Set<Entity>> countsForMention = mentionTextToEntityCounts.get(mentionText);
				if (countsForMention != null) {
					// Determine if this segment has an annotation
					AnnotatedSegment predictedSegment = find(segment, predictedAnnotations);
					if (predictedSegment == null) {
						// This segment has no annotation, check if it has an overlapping prediction
						AnnotatedSegment overlappingPrediction = overlap(segment, predictedAnnotations);
						// If no overlapping prediction, count as a nonentity
						if (overlappingPrediction == null) {
							countsForMention.addTo(nonEntitySet, 1);
						}
					}
				}
			}
		}

		// Change types of existing annotations as needed
		for (TextInstance instance : input) {
			List<AnnotatedSegment> predictedAnnotationsCopy = new ArrayList<AnnotatedSegment>(instance.getPredictedAnnotations().getObject(0));
			List<AnnotatedSegment> newAnnotations = new ArrayList<AnnotatedSegment>();
			for (Segment segment : instance.getSegments()) {
				String mentionText = segment.getText();
				// logger.info("Consistency checking counts for " + hashKey);
				Object2IntOpenHashMap<Set<Entity>> countsForMention = mentionTextToEntityCounts.get(mentionText);
				if (countsForMention != null) {
					Set<Entity> maxEntities = getMax(countsForMention);
					AnnotatedSegment predictedSegment = find(segment, predictedAnnotationsCopy);
					Set<Entity> predictedEntities = nonEntitySet;
					if (predictedSegment != null) {
						predictedEntities = predictedSegment.getEntities();
					}
					if (!StaticUtilMethods.equalElements(predictedEntities, maxEntities)) {
						// predicted entities for this segment and the entities most commonly predicted for this mention text do not match

						// Output descriptive log message
						AnnotatedSegment overlappingPrediction = overlap(segment, predictedAnnotationsCopy);
						String logText = instance.getSourceId() + "\t" + instance.getOffset() + "\t" + segment.getStartChar() + "->" + segment.getEndChar() + "\t" + segment.getText();
						logText += "\tCounts = " + getCountString(countsForMention);
						logText += "\tPrediction = " + Entity.visualizePrimaryIdentifiers(predictedEntities);
						logText += "\tMax = " + Entity.visualizePrimaryIdentifiers(maxEntities);
						if (overlappingPrediction == null) {
							logText += "\tOverlapping segment = none";
							logText += "\tContained = n/a";
						} else {
							logText += "\tOverlapping segment = " + overlappingPrediction.getText();
							logText += "\tContained = " + segment.contains(overlappingPrediction);
						}

						double maxEntityScore = entities2EntityScore.getDouble(maxEntities);
						if (overlappingPrediction == null) {
							if (predictedSegment == null) {
								if (StaticUtilMethods.equalElements(maxEntities, nonEntitySet)) {
									// logger.info("Consistency post-processing unchanged #1:\t" + logText);
								} else {
									AnnotatedSegment newAnnotation = new AnnotatedSegment(segment.getSourceText(), segment.getStartChar(), segment.getEndChar(), segment.getTokens(), maxEntities, maxEntityScore);
									newAnnotations.add(newAnnotation);
									logger.info("Consistency post-processing adding prediction #2:\t" + logText);
								}
							} else {
								if (StaticUtilMethods.equalElements(maxEntities, nonEntitySet)) {
									predictedAnnotationsCopy.remove(predictedSegment);
									logger.info("Consistency post-processing removing prediction #3:\t" + logText);
								} else {
									predictedAnnotationsCopy.remove(predictedSegment);
									AnnotatedSegment newAnnotation = new AnnotatedSegment(segment.getSourceText(), segment.getStartChar(), segment.getEndChar(), segment.getTokens(), maxEntities, maxEntityScore);
									newAnnotations.add(newAnnotation);
									logger.info("Consistency post-processing changing prediction #4:\t" + logText);
								}
							}
						} else {
							logger.info("Consistency post-processing unchanged #5:\t" + logText);
						}
					}
				}
			}

			// Add remaining annotations
			newAnnotations.addAll(predictedAnnotationsCopy);
			Collections.sort(newAnnotations);

			// Finalize
			double score = instance.getPredictedAnnotations().getValue(0);
			RankedList<List<AnnotatedSegment>> newAnnotationsList = new RankedList<List<AnnotatedSegment>>(1);
			newAnnotationsList.add(score, newAnnotations);
			instance.setPredictedAnnotations(newAnnotationsList);
		}
	}

	private static String getCountString(Object2IntOpenHashMap<Set<Entity>> countsForMention) {
		List<Set<Entity>> entitiesList = new ArrayList<Set<Entity>>();
		for (Set<Entity> entities : countsForMention.keySet()) {
			entitiesList.add(entities);
		}
		Collections.sort(entitiesList, new DecreasingCountComparator(countsForMention));
		StringBuilder str = new StringBuilder();
		str.append("[");
		for (int entitiesIndex = 0; entitiesIndex < entitiesList.size(); entitiesIndex++) {
			if (entitiesIndex > 0) {
				str.append(", ");
			}
			Set<Entity> entities = entitiesList.get(entitiesIndex);
			int count = countsForMention.getInt(entities);
			str.append(count);
			str.append(":");
			str.append(Entity.visualizePrimaryIdentifiers(entities));
		}
		str.append("]");
		return str.toString();
	}

	private static class DecreasingCountComparator implements Comparator<Set<Entity>> {

		private Object2IntOpenHashMap<Set<Entity>> countsForMention;

		public DecreasingCountComparator(Object2IntOpenHashMap<Set<Entity>> countsForMention) {
			this.countsForMention = countsForMention;
		}

		@Override
		public int compare(Set<Entity> entities1, Set<Entity> entities2) {
			int count1 = countsForMention.getInt(entities1);
			int count2 = countsForMention.getInt(entities2);
			return count1 - count2;
		}

	}

	private Set<Entity> getMax(Object2IntOpenHashMap<Set<Entity>> countsForMention) {
		int maxCount = 0;
		Set<Entity> maxEntities = null;
		for (Set<Entity> entities : countsForMention.keySet()) {
			int count = countsForMention.getInt(entities);
			if (count > maxCount) {
				maxCount = count;
				maxEntities = entities;
			} else if (count == maxCount && StaticUtilMethods.equalElements(maxEntities, nonEntitySet)) {
				maxCount = count;
				maxEntities = entities;
			}
		}
		return maxEntities;
	}

	private static AnnotatedSegment find(Segment segment, List<AnnotatedSegment> states) {
		// TODO PERFORMANCE and DESIGN
		for (AnnotatedSegment s : states) {
			if (s.getStartChar() == segment.getStartChar() && s.getEndChar() == segment.getEndChar()) {
				return s;
			}
		}
		return null;
	}

	// Find a non-exact overlapping segment
	private static AnnotatedSegment overlap(Segment segment, List<AnnotatedSegment> states) {
		// TODO PERFORMANCE and DESIGN
		for (AnnotatedSegment s : states) {
			if (s.overlaps(segment) && (segment.getStartChar() != s.getStartChar() || segment.getEndChar() != s.getEndChar())) {
				return s;
			}
		}
		return null;
	}

	@Override
	public void process(TextInstance input) {
		throw new RuntimeException("Not allowed");
	}
}
