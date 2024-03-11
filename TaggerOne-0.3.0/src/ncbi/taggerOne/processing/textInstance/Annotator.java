package ncbi.taggerOne.processing.textInstance;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ncbi.taggerOne.T1Constants;
import ncbi.taggerOne.lexicon.Index;
import ncbi.taggerOne.lexicon.Lexicon;
import ncbi.taggerOne.model.normalization.CachedNormalizationModel;
import ncbi.taggerOne.model.normalization.NormalizationModelPredictor;
import ncbi.taggerOne.model.recognition.RecognitionModelPredictor;
import ncbi.taggerOne.types.AnnotatedSegment;
import ncbi.taggerOne.types.Entity;
import ncbi.taggerOne.types.Segment;
import ncbi.taggerOne.types.TextInstance;
import ncbi.taggerOne.types.Token;
import ncbi.taggerOne.util.RankedList;
import ncbi.taggerOne.util.vector.Vector;
import ncbi.util.Profiler;

public class Annotator extends TextInstanceProcessor {

	private static final Logger logger = LoggerFactory.getLogger(Annotator.class);
	private static final long serialVersionUID = 1L;

	protected Lexicon lexicon;
	protected RecognitionModelPredictor recognitionModel;
	protected Map<String, NormalizationModelPredictor> normalizationModels;
	protected Map<String, Vector<String>> unknownEntityVectors;

	public Annotator(Lexicon lexicon, RecognitionModelPredictor recognitionModel, Map<String, NormalizationModelPredictor> normalizationModels) {
		this.lexicon = lexicon;
		this.recognitionModel = recognitionModel;
		this.normalizationModels = normalizationModels;
		unknownEntityVectors = new HashMap<String, Vector<String>>();
		for (String entityType : normalizationModels.keySet()) {
			unknownEntityVectors.put(entityType, lexicon.getIndex(entityType).getUnknownEntity().getPrimaryName().getVector());
		}
	}

	@Override
	public void reset() {
		for (String entityType : normalizationModels.keySet()) {
			NormalizationModelPredictor normalizationModelPredictor = normalizationModels.get(entityType);
			if (normalizationModelPredictor instanceof CachedNormalizationModel) {
				((CachedNormalizationModel) normalizationModelPredictor).clearCache();
			}
		}
	}

	public Lexicon getLexicon() {
		return lexicon;
	}

	public RecognitionModelPredictor getRecognitionModel() {
		return recognitionModel;
	}

	public Map<String, NormalizationModelPredictor> getNormalizationModels() {
		return normalizationModels;
	}

	@Override
	public void process(TextInstance input) {
		Profiler.start("Annotator.process()");
		RankedList<List<AnnotatedSegment>> paths = getPredictedStateSequences(input);
		input.setPredictedStates(paths);
		Profiler.stop("Annotator.process()");
	}

	public double scoreStateSequence(List<AnnotatedSegment> stateSequence) {
		Profiler.start("Annotator.scoreStateSequence()");
		if (logger.isTraceEnabled()) {
			logger.trace("SCORE\tAnnotator.scoreStateSequence()");
		}
		double score = 0.0;
		for (AnnotatedSegment nextSegment : stateSequence) {
			double recognitionScore = recognitionModel.predict(nextSegment.getEntityClass(), nextSegment);
			score += recognitionScore;
			String toState = nextSegment.getEntityClass();
			NormalizationModelPredictor normalizationModel = normalizationModels.get(toState);
			Vector<String> mentionVector = nextSegment.getMentionName().getVector();
			double normalizationScore = 0.0;
			if (normalizationModel != null && mentionVector != null) {
				normalizationScore = Double.NEGATIVE_INFINITY;
				for (Entity entity : nextSegment.getEntities()) {
					double normalizationScore2 = normalizationModel.scoreEntity(mentionVector, entity);
					if (normalizationScore2 > normalizationScore) {
						normalizationScore = normalizationScore2;
					}
				}
				score += normalizationScore;
			}
			if (logger.isTraceEnabled()) {
				logger.trace("SCORE\tBOTH\t" + nextSegment.getStartChar() + "\t" + nextSegment.getEndChar() + "\t" + nextSegment.getEntityClass() + "\t" + nextSegment.getText() + "\t" + recognitionScore + "\t"
						+ nextSegment.getMentionName().getName() + "\t" + normalizationScore + "\t" + nextSegment.visualizeEntitiesPrimaryIdentifiers());
			}
		}
		Profiler.stop("Annotator.scoreStateSequence()");
		return score;
	}

	public RankedList<List<AnnotatedSegment>> getPredictedStateSequences(TextInstance input) {
		Profiler.start("Annotator.getPredictedStateSequences()");
		Profiler.start("Annotator.getPredictedStateSequences():init");
		// Initialize data structures to hold partial paths and scores
		List<Token> tokens = input.getTokens();
		int length = tokens.size();
		PathNode[] paths = new PathNode[length];
		Profiler.stop("Annotator.getPredictedStateSequences():init");

		for (int tokenIndex = 0; tokenIndex < length; tokenIndex++) {
			// Given paths up to tokenIndex - 1 are calculated
			// Calculate highest path to tokenIndex
			List<Segment> segments = input.getSegmentsEndingAt(tokenIndex);
			List<PathNode> boundedPaths = getBoundedPaths(paths, segments);
			paths[tokenIndex] = getBestPath(boundedPaths);
		}

		Profiler.start("Annotator.getPredictedStateSequences():finalize");
		RankedList<List<AnnotatedSegment>> rankedPaths = new RankedList<List<AnnotatedSegment>>(1);
		if (length == 0) {
			rankedPaths.add(0.0, new ArrayList<AnnotatedSegment>());
		} else {
			PathNode finalPath = paths[length - 1];
			List<AnnotatedSegment> bestPath = finalPath.getFinalPath();
			rankedPaths.add(finalPath.getPathScore(), bestPath);
		}
		Profiler.stop("Annotator.getPredictedStateSequences():finalize");
		Profiler.stop("Annotator.getPredictedStateSequences()");
		return rankedPaths;
	}

	private List<PathNode> getBoundedPaths(PathNode[] paths, List<Segment> segments) {
		Profiler.start("Annotator.getBoundedPaths()");
		List<PathNode> boundedPaths = new ArrayList<PathNode>(segments.size());

		// Do segments as nonentity type
		for (Segment segment : segments) {
			// Nonentity segments can only be length 1
			if (segment.getTokens().size() == 1) {
				PathNode parent = null;
				int parentIndex = segment.getStartIndex() - 1;
				if (parentIndex >= 0) {
					parent = paths[parentIndex];
				}
				double recognitionScore = recognitionModel.predict(T1Constants.NONENTITY_STATE, segment);
				// NOTE: It is NOT faster to reuse PathNodes
				PathNode path = new PathNode(parent, segment, T1Constants.NONENTITY_STATE, recognitionScore, 0.0);
				if (logger.isTraceEnabled()) {
					logger.trace("SCORE\tNON\t" + segment.getStartChar() + "\t" + segment.getEndChar() + "\t" + T1Constants.NONENTITY_STATE + "\t" + segment.getText() + "\t" + recognitionScore + "\t" + segment.getMentionName().getName()
							+ "\t0.0");
				}
				boundedPaths.add(path);
			}
		}

		// Do segments as entity types
		for (String entityType : normalizationModels.keySet()) {
			NormalizationModelPredictor normalizationPredictor = normalizationModels.get(entityType);
			for (Segment segment : segments) {
				PathNode parent = null;
				int parentIndex = segment.getStartIndex() - 1;
				if (parentIndex >= 0) {
					parent = paths[parentIndex];
				}
				double recognitionScore = recognitionModel.predict(entityType, segment);
				double normalizationScoreBound = 0.0;
				Vector<String> mentionVector = segment.getMentionName().getVector();
				if (mentionVector != null) {
					normalizationScoreBound += normalizationPredictor.getScoreBound(mentionVector);
				}
				// NOTE: It is not faster to reuse PathNodes
				PathNode path = new PathNode(parent, segment, entityType, recognitionScore, normalizationScoreBound);
				if (logger.isTraceEnabled()) {
					logger.trace("SCORE\tNER\t" + segment.getStartChar() + "\t" + segment.getEndChar() + "\t" + entityType + "\t" + segment.getText() + "\t" + recognitionScore + "\t" + segment.getMentionName().getName() + "\t"
							+ normalizationScoreBound);
				}
				boundedPaths.add(path);
			}
		}

		Profiler.stop("Annotator.getBoundedPaths()");
		return boundedPaths;
	}

	private PathNode getBestPath(List<PathNode> boundedPaths) {
		Profiler.start("Annotator.getBestPath()");
		Collections.sort(boundedPaths);
		PathNode bestPath = null;
		for (int pathIndex = 0; pathIndex < boundedPaths.size(); pathIndex++) {
			PathNode path = boundedPaths.get(pathIndex);
			if (bestPath != null && bestPath.getPathScore() > path.getBoundedPathScore() + T1Constants.EPSILON) {
				// This node and all remaining have lower bounded path scores than the current best path score
				if (logger.isTraceEnabled()) {
					logger.trace("Annotator.getBestPath(): " + bestPath.getPathScore() + " > " + path.getBoundedPathScore() + " @ " + pathIndex + "/" + boundedPaths.size() + " " + bestPath.toString());
				}
				logger.trace("Annotator.getBestPath(): " + bestPath.getPathScore() + " > " + path.getBoundedPathScore() + " @ " + pathIndex + "/" + boundedPaths.size() + " {} ", bestPath);
				Profiler.stop("Annotator.getBestPath()");
				return bestPath;
			}
			Profiler.start("Annotator.getBestPath()@" + Integer.toString(pathIndex));
			String entityType = path.getEntityType();
			if (entityType.equals(T1Constants.NONENTITY_STATE)) {
				path.setNormalization(0.0, lexicon.getNonEntity());
			} else {
				Segment segment = path.getSegment();
				Index index = lexicon.getIndex(entityType);
				Vector<String> mentionVector = segment.getMentionName().getVector();
				double normalizationScore = 0.0;
				Entity entity = index.getUnknownEntity();
				if (mentionVector != null) {
					NormalizationModelPredictor normalizationPredictor = normalizationModels.get(entityType);
					if (normalizationPredictor != null) {
						// TODO PERFORMANCE Only score the unknown entity if it couldn't run findBest
						normalizationScore = normalizationPredictor.scoreEntity(mentionVector, index.getUnknownEntity());
						RankedList<Entity> bestEntities = new RankedList<Entity>(1);
						normalizationPredictor.findBest(mentionVector, bestEntities);
						if (bestEntities.size() > 0) {
							normalizationScore = bestEntities.getValue(0);
							entity = bestEntities.getObject(0);
							// double normalizationScore2 = normalizationPredictor.scoreEntity(mentionVector, entity);
							// MentionName bestName = normalizationPredictor.findBestName(mentionVector, entity);
							// if (Math.abs(normalizationScore - normalizationScore2) > T1Constants.EPSILON) {
							// logger.error("normalizationScore=" + normalizationScore);
							// logger.error("normalizationScore2=" + normalizationScore2);
							// normalizationPredictor.visualizeScore(mentionVector, bestName.getVector());
							// }
						}
						if (logger.isTraceEnabled()) {
							logger.trace("SCORE\tNORM\t" + segment.getStartChar() + "\t" + segment.getEndChar() + "\t" + entityType + "\t" + segment.getText() + "\t\t" + segment.getMentionName().getName() + "\t" + normalizationScore + "\t"
									+ entity.getPrimaryIdentifier() + "\t" + segment.getMentionName().getVector().visualize());
						}
					}
				}
				path.setNormalization(normalizationScore, entity);
			}
			if (bestPath == null) {
				bestPath = path;
			} else if (path.getPathScore() > bestPath.getPathScore()) {
				bestPath = path;
			}
			Profiler.stop("Annotator.getBestPath()@" + Integer.toString(pathIndex));
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Annotator.getBestPath(): " + bestPath.getPathScore() + " " + bestPath.toString());
		}
		Profiler.stop("Annotator.getBestPath()");
		return bestPath;
	}

	private static final class PathNode implements Comparable<PathNode> {

		private PathNode parent;
		private Segment segment;
		private String entityType;
		private double recognitionScore;
		private double normalizationScore;
		private double boundedPathScore;

		private Entity entity;
		private double pathScore;

		public PathNode(PathNode parent, Segment segment, String entityType, double recognitionScore, double normalizationScoreBound) {
			this.parent = parent;
			this.segment = segment;
			this.entityType = entityType;
			this.recognitionScore = recognitionScore;
			this.pathScore = recognitionScore;
			boundedPathScore = recognitionScore + normalizationScoreBound;
			if (parent != null) {
				boundedPathScore += parent.pathScore;
			}
			this.entity = null;
			this.normalizationScore = Double.NaN;
		}

		public Segment getSegment() {
			return segment;
		}

		public String getEntityType() {
			return entityType;
		}

		public double getBoundedPathScore() {
			return boundedPathScore;
		}

		public void setNormalization(double normalizationScore, Entity entity) {
			this.entity = entity;
			this.normalizationScore = normalizationScore;
			pathScore += normalizationScore;
			if (parent != null) {
				pathScore += parent.getPathScore();
			}
		}
		
		public double getPathScore() {
			if (Double.isNaN(normalizationScore)) {
				return Double.NaN;
			}
			return pathScore;
		}

		@Override
		public int compareTo(PathNode node2) {
			return -Double.compare(boundedPathScore, node2.boundedPathScore);
		}

		private void toString(StringBuilder str) {
			if (parent != null) {
				parent.toString(str);
				str.append(" ");
			}
			str.append(segment.getText());
			str.append("(");
			str.append(segment.getStartIndex());
			str.append(", ");
			str.append(segment.getEndIndex());
			str.append(")=");
			str.append(entityType);
			if (entity != null) {
				str.append(":");
				str.append(entity.getPrimaryIdentifier());
			}
		}

		public String toString() {
			// In this implementation each PathNode writes the data into the same StringBuilder
			// This avoids StackOverflowError on long paths and is much faster
			StringBuilder str = new StringBuilder();
			toString(str);
			return str.toString();
		}

		private AnnotatedSegment getAnnotatedSegment() {
			AnnotatedSegment annotatedSegment = segment.getAnnotatedCopy(entityType);
			if (entity == null) {
				logger.error("getFinalPath() path.getEntity() is null for segment " + segment.getText());
			}
			annotatedSegment.setEntities(Collections.singleton(entity), recognitionScore + normalizationScore);
			return annotatedSegment;
		}

		public List<AnnotatedSegment> getFinalPath() {
			Deque<PathNode> path = new ArrayDeque<PathNode>();
			PathNode current = this;
			while (current != null) {
				path.push(current);
				current = current.parent;
			}
			List<AnnotatedSegment> finalPath = new ArrayList<AnnotatedSegment>();
			while (!path.isEmpty()) {
				current = path.pop();
				finalPath.add(current.getAnnotatedSegment());
			}
			return finalPath;
		}
	}
}
