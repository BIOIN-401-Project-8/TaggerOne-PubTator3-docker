package ncbi.taggerOne.processing.mentionName;

import java.util.List;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import ncbi.taggerOne.types.MentionName;
import ncbi.taggerOne.util.Dictionary;
import ncbi.taggerOne.util.vector.DenseVector;
import ncbi.taggerOne.util.vector.Vector;

public class WeightedVectorSpaceExtractor extends MentionNameProcessor {

	private static final long serialVersionUID = 1L;

	private Dictionary<String> nameVectorSpace;
	private int nameCount;
	private Int2IntOpenHashMap frequencies;

	public WeightedVectorSpaceExtractor(Dictionary<String> nameVectorSpace) {
		this.nameVectorSpace = nameVectorSpace;
		this.nameCount = 0;
		this.frequencies = new Int2IntOpenHashMap();
		this.frequencies.defaultReturnValue(0);
	}

	@Override
	public void process(MentionName entityName) {
		List<String> tokens = entityName.getTokens();
		int vectorSpaceSize = nameVectorSpace.size();
		IntSet addedTokens = new IntOpenHashSet();
		for (int tokenIndex = 0; tokenIndex < tokens.size(); tokenIndex++) {
			String token = tokens.get(tokenIndex);
			int index = nameVectorSpace.addElement(token);
			if (index < vectorSpaceSize) {
				// This token was already present: canonize the copy in this token list
				tokens.set(tokenIndex, nameVectorSpace.getElement(index));
			} else {
				// This token is new: no need to canonize
				// Update the size of the vector space
				vectorSpaceSize = nameVectorSpace.size();
			}
			if (!addedTokens.contains(index)) {
				frequencies.addTo(index, 1);
				addedTokens.add(index);
			}
		}
		nameCount++;
	}

	public Int2IntOpenHashMap getFrequencies() {
		return frequencies;
	}

	public Vector<String> getWeights() {
		if (!nameVectorSpace.isFrozen()) {
			throw new IllegalStateException("NameVectorSpace must be frozen first");
		}
		double sizeDouble = nameCount + 1.0;
		Vector<String> weights = new DenseVector<String>(nameVectorSpace);
		for (int index = 0; index < nameVectorSpace.size(); index++) {
			int frequency = frequencies.get(index);
			double weight = Math.log(sizeDouble / (frequency + 1));
			weights.set(index, weight);
		}
		return weights;
	}
}
