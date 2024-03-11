package ncbi.taggerOne.model.normalization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ncbi.taggerOne.lexicon.Index;
import ncbi.taggerOne.types.Entity;
import ncbi.taggerOne.types.MentionName;
import ncbi.taggerOne.util.Dictionary;
import ncbi.taggerOne.util.RankedList;
import ncbi.taggerOne.util.matrix.DenseBySparseMatrix;
import ncbi.taggerOne.util.vector.DenseVector;
import ncbi.taggerOne.util.vector.SparseVector;
import ncbi.taggerOne.util.vector.Vector;
import ncbi.taggerOne.util.vector.Vector.VectorIterator;
import ncbi.util.Profiler;

public class LowMemCompiledNormalizationModel implements NormalizationModelPredictor {

	private static final Logger logger = LoggerFactory.getLogger(LowMemCompiledNormalizationModel.class);
	private static final long serialVersionUID = 1L;

	protected Index index;

	protected Dictionary<String> mentionVectorSpace;
	protected Dictionary<String> nameVectorSpace;
	protected Dictionary<Vector<String>> nameVectorDictionary;

	protected double[] cosineSimWeight;
	protected DenseBySparseMatrix<String, String> weights; // rows indexed by mention vector space, columns indexed by name vector space
	protected DenseBySparseMatrix<String, Vector<String>> lexiconMatrix; // rows indexed by name vector space, columns indexed by name vectors
	protected int[] mentionIndexToNameIndex;

	private Vector<String> highestVector;
	private int[] indexOfHighestVector;

	public LowMemCompiledNormalizationModel(NormalizationModel model) {
		this.index = model.index;
		this.mentionVectorSpace = model.mentionVectorSpace;
		this.nameVectorSpace = model.nameVectorSpace;
		this.nameVectorDictionary = index.getNameVectorDictionary();
		this.cosineSimWeight = model.cosineSimWeight;
		this.weights = model.weights;
		this.lexiconMatrix = model.lexiconMatrix;
		this.mentionIndexToNameIndex = model.mentionIndexToNameIndex;
		initDataStructures();
	}

	private void initDataStructures() {
		// Initialize highestVector
		this.highestVector = new DenseVector<String>(mentionVectorSpace);
		this.indexOfHighestVector = new int[mentionVectorSpace.size()];
		for (int mentionVectorSpaceIndex = 0; mentionVectorSpaceIndex < mentionVectorSpace.size(); mentionVectorSpaceIndex++) {
			Vector<String> nameVectorEquivalent = new SparseVector<String>(nameVectorSpace);
			int nameIndex = mentionIndexToNameIndex[mentionVectorSpaceIndex];
			if (nameIndex >= 0) {
				nameVectorEquivalent.increment(nameIndex, cosineSimWeight[0]);
			}
			Vector<String> weightMatrixRow = weights.getRowVector(mentionVectorSpaceIndex);
			if (weightMatrixRow != null) {
				nameVectorEquivalent.increment(weightMatrixRow);
			}
			double highest = 0.0;
			int highestIndex = -1;
			if (!nameVectorEquivalent.isEmpty()) {
				Vector<Vector<String>> nameScores = convertNameVectorToNameScores(nameVectorEquivalent);
				if (nameScores != null) {
					VectorIterator shortcutIterator = nameScores.getIterator();
					while (shortcutIterator.next()) {
						double value = shortcutIterator.getValue();
						if (highest < value) {
							highest = value;
							highestIndex = shortcutIterator.getIndex();
						}
					}
				}
			}
			highestVector.set(mentionVectorSpaceIndex, highest);
			indexOfHighestVector[mentionVectorSpaceIndex] = highestIndex;
		}
	}

	@Override
	public NormalizationModelPredictor compile() {
		return this;
	}

	@Override
	public double getScoreBound(Vector<String> mentionVector) {
		Profiler.start("CompiledNormalizationModel.getScoreBound()");
		double highest = 0.0;
		if (mentionVector != null) {
			highest = mentionVector.dotProduct(highestVector);
		}
		Profiler.stop("CompiledNormalizationModel.getScoreBound()");
		return highest;
	}

	@Override
	public void findBest(Vector<String> mentionVector, RankedList<Entity> bestEntities) {
		Profiler.start("CompiledNormalizationModel.findBest()");
		double unknownScore = scoreEntity(mentionVector, index.getUnknownEntity());
		bestEntities.add(unknownScore, index.getUnknownEntity());
		if (mentionVector.cardinality() == 1 && bestEntities.maxSize() == 1) {
			Profiler.start("CompiledNormalizationModel.findBest()@FAST");
			VectorIterator mentionIterator = mentionVector.getIterator();
			mentionIterator.next();
			int mentionIndex = mentionIterator.getIndex();
			int nameVectorIndex = indexOfHighestVector[mentionIndex];
			if (nameVectorIndex != -1) {
				double mentionValue = mentionIterator.getValue();
				Vector<String> nameVector = nameVectorDictionary.getElement(nameVectorIndex);
				bestEntities.add(mentionValue * highestVector.get(mentionIndex), index.getEntities(nameVector).iterator().next());
			}
			Profiler.stop("CompiledNormalizationModel.findBest()@FAST");
			Profiler.stop("CompiledNormalizationModel.findBest()");
			return;
		}
		Vector<String> nameVectorEquivalent = convertMentionVectorToNameVectorEquivalent(mentionVector);
		Vector<Vector<String>> nameVectorScores = convertNameVectorToNameScores(nameVectorEquivalent);
		VectorIterator nameVectorIterator = nameVectorScores.getIterator();
		while (nameVectorIterator.next()) {
			double score = nameVectorIterator.getValue();
			if (bestEntities.check(score)) {
				int nameVectorIndex = nameVectorIterator.getIndex();
				Vector<String> nameVector = nameVectorDictionary.getElement(nameVectorIndex);
				Set<Entity> entities = index.getEntities(nameVector);
				for (Entity entity : entities) {
					bestEntities.add(score, entity);
				}
			}
		}
		Profiler.stop("CompiledNormalizationModel.findBest()");
	}

	protected Vector<String> convertMentionVectorToNameVectorEquivalent(Vector<String> mentionVector) {
		Profiler.start("NormalizationModel.convertMentionVectorToNameVectorEquivalent()");
		SparseVector<String> nameVectorEquivalent = new SparseVector<String>(nameVectorSpace);
		// Converts a mention vector to a name vector
		VectorIterator mentionIterator = mentionVector.getIterator();
		while (mentionIterator.next()) {
			int mentionIndex = mentionIterator.getIndex();
			double mentionValue = mentionIterator.getValue();
			// Add value from cosine sim
			int nameIndex = mentionIndexToNameIndex[mentionIndex];
			if (nameIndex >= 0) {
				nameVectorEquivalent.increment(nameIndex, mentionValue * cosineSimWeight[0]);
			}
			// Add values from weights matrix
			Vector<String> nameVector = weights.getRowVector(mentionIndex);
			if (nameVector != null) {
				nameVectorEquivalent.increment(mentionValue, nameVector);
			}
		}
		Profiler.stop("NormalizationModel.convertMentionVectorToNameVectorEquivalent()");
		return nameVectorEquivalent;
	}

	private Vector<Vector<String>> convertNameVectorToNameScores(Vector<String> nameVectorEquivalent) {
		Profiler.start("CompiledNormalizationModel.convertNameVectorToNameScores()");
		DenseVector<Vector<String>> nameVectorScores = new DenseVector<Vector<String>>(nameVectorDictionary);
		VectorIterator nameVectorSpaceIndexIterator = nameVectorEquivalent.getIterator();
		while (nameVectorSpaceIndexIterator.next()) {
			int nameVectorSpaceIndex = nameVectorSpaceIndexIterator.getIndex();
			double nameVectorSpaceValue = nameVectorSpaceIndexIterator.getValue();
			Vector<Vector<String>> lexiconVector = lexiconMatrix.getRowVector(nameVectorSpaceIndex);
			if (lexiconVector != null) {
				nameVectorScores.increment(nameVectorSpaceValue, lexiconVector);
			}
		}
		Profiler.stop("CompiledNormalizationModel.convertNameVectorToNameScores()");
		return nameVectorScores;
	}

	@Override
	public MentionName findBestName(Vector<String> mentionVector, Entity entity) {
		Profiler.start("CompiledNormalizationModel.findBestName()");
		if (mentionVector.dimensions() != mentionVectorSpace.size()) {
			throw new IllegalArgumentException("Mention vector dimensions are not equal");
		}
		MentionName bestName = null;
		double bestScore = Double.NEGATIVE_INFINITY; // Always pick a name
		for (MentionName name : entity.getNames()) {
			Vector<String> nameVector = name.getVector();
			double score = 0.0;
			if (nameVector != null) {
				score = score(mentionVector, nameVector);
				if (score > bestScore) {
					bestScore = score;
					bestName = name;
				}
			}
		}
		Profiler.stop("CompiledNormalizationModel.findBestName()");
		return bestName;
	}

	@Override
	public double scoreEntity(Vector<String> mentionVector, Entity entity) {
		Profiler.start("CompiledNormalizationModel.scoreEntity()");
		if (mentionVector.dimensions() != mentionVectorSpace.size()) {
			throw new IllegalArgumentException("Mention vector dimensions are not equal");
		}
		double bestScore = Double.NEGATIVE_INFINITY; // Always pick a name
		for (MentionName name : entity.getNames()) {
			Vector<String> nameVector = name.getVector();
			double score = 0.0;
			if (nameVector != null) {
				score = score(mentionVector, nameVector);
				if (score > bestScore) {
					bestScore = score;
				}
			}
		}
		Profiler.stop("CompiledNormalizationModel.scoreEntity()");
		return bestScore;
	}

	@Override
	public double scoreNameVector(Vector<String> mentionVector, Vector<String> nameVector) {
		Profiler.start("CompiledNormalizationModel.scoreNameVector()");
		if (mentionVector.dimensions() != mentionVectorSpace.size()) {
			throw new IllegalArgumentException("Mention vector dimensions are not equal");
		}
		if (nameVector.dimensions() != nameVectorSpace.size()) {
			throw new IllegalArgumentException("Name vector dimensions are not equal");
		}
		double score = score(mentionVector, nameVector);
		Profiler.stop("CompiledNormalizationModel.scoreNameVector()");
		return score;
	}

	@Override
	public void visualizeScore(Vector<String> mentionVector, Vector<String> nameVector) {
		Profiler.start("CompiledNormalizationModel.visualizeScore()");
		List<String> scoreLines = new ArrayList<String>();
		if (mentionVectorSpace != null && nameVectorSpace != null && mentionVector != null) {
			for (int i = 0; i < mentionVector.dimensions(); i++) {
				if (mentionVector.get(i) != 0.0) {
					String mentionElement = mentionVectorSpace.getElement(i);
					for (int j = 0; j < nameVector.dimensions(); j++) {
						if (nameVector.get(j) != 0.0) {
							String nameElement = nameVectorSpace.getElement(j);
							double weight = weights.get(i, j);
							if (j == mentionIndexToNameIndex[i]) {
								scoreLines.add("\t\t\t" + mentionElement + "\t" + mentionVector.get(i) + "\t" + nameElement + "\t" + nameVector.get(j) + "\t" + cosineSimWeight[0] + "\t" + weight);
							} else if (weight != 0.0) {
								scoreLines.add("\t\t\t" + mentionElement + "\t" + mentionVector.get(i) + "\t" + nameElement + "\t" + nameVector.get(j) + "\t0.0\t" + weight);
							}
						}
					}
				}
			}
		}
		Collections.sort(scoreLines);
		for (String line : scoreLines) {
			logger.info(line);
		}
		Profiler.stop("CompiledNormalizationModel.visualizeScore()");
	}

	private double score(Vector<String> mentionVector, Vector<String> nameVector) {
		Profiler.start("CompiledNormalizationModel.score()");
		double score = 0.0;
		if (mentionVector != null && nameVector != null) {
			VectorIterator mentionIterator = mentionVector.getIterator();
			while (mentionIterator.next()) {
				int mentionIndex = mentionIterator.getIndex();
				double mentionValue = mentionIterator.getValue();
				VectorIterator nameIterator = nameVector.getIterator();
				while (nameIterator.next()) {
					int nameIndex = nameIterator.getIndex();
					double weight = weights.get(mentionIndex, nameIndex);
					if (nameIndex == mentionIndexToNameIndex[mentionIndex]) {
						weight += cosineSimWeight[0];
					}
					if (weight != 0.0) {
						double nameValue = nameIterator.getValue();
						score += mentionValue * weight * nameValue;
					}
				}
			}
		}
		Profiler.stop("CompiledNormalizationModel.score()");
		return score;
	}
}
