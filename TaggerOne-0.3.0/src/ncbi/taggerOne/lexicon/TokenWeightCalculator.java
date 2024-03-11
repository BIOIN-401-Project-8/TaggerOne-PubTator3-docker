package ncbi.taggerOne.lexicon;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ncbi.taggerOne.types.Entity;
import ncbi.taggerOne.types.MentionName;
import ncbi.taggerOne.util.Dictionary;
import ncbi.taggerOne.util.vector.DenseVector;
import ncbi.taggerOne.util.vector.Vector;

public class TokenWeightCalculator implements Serializable {

	private static final Logger logger = LoggerFactory.getLogger(TokenWeightCalculator.class);
	private static final long serialVersionUID = 1L;

	private Vector<String> nameWeights;

	public TokenWeightCalculator(Dictionary<String> nameVectorSpace, Set<Entity> entities) {
		logger.info("Number of entities: " + entities.size());
		// TODO Verify entities are all one type
		Map<ArrayList<String>, Set<Entity>> tokensToEntityMap = new HashMap<ArrayList<String>, Set<Entity>>();
		for (Entity entity : entities) {
			for (MentionName name : entity.getNames()) {
				ArrayList<String> tokens = new ArrayList<String>(name.getTokens());
				Collections.sort(tokens);
				Set<Entity> entitySet = tokensToEntityMap.get(tokens);
				if (entitySet == null) {
					entitySet = new HashSet<Entity>();
					tokensToEntityMap.put(tokens, entitySet);
				}
				entitySet.add(entity);
			}
		}
		Dictionary<ArrayList<String>> tokenList = new Dictionary<ArrayList<String>>();
		for (ArrayList<String> tokens : tokensToEntityMap.keySet()) {
			tokenList.addElement(tokens);
		}
		// Create nameElementToVectorIndices
		List<TIntSet> nameElementToVectorIndices = new ArrayList<TIntSet>(nameVectorSpace.size());
		for (int nameElementIndex = 0; nameElementIndex < nameVectorSpace.size(); nameElementIndex++) {
			nameElementToVectorIndices.add(new TIntHashSet());
		}
		for (int nameVectorIndex = 0; nameVectorIndex < tokenList.size(); nameVectorIndex++) {
			List<String> tokens = tokenList.getElement(nameVectorIndex);
			for (String token : tokens) {
				int nameElementIndex = nameVectorSpace.getIndex(token);
				TIntSet nameVectorIndexSet = nameElementToVectorIndices.get(nameElementIndex);
				nameVectorIndexSet.add(nameVectorIndex);
			}
		}
		double size = tokenList.size();
		nameWeights = new DenseVector<String>(nameVectorSpace);
		for (int nameIndex = 0; nameIndex < nameVectorSpace.size(); nameIndex++) {
			int frequency = nameElementToVectorIndices.get(nameIndex).size();
			double weight = Math.log(size / (frequency + 1));
			nameWeights.set(nameIndex, weight);
		}
	}

	public Vector<String> getNameWeights() {
		return nameWeights;
	}
}
