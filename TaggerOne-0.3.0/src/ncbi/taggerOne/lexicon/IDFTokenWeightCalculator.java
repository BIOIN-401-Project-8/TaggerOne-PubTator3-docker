package ncbi.taggerOne.lexicon;

import java.io.Serializable;

import ncbi.taggerOne.types.Entity;
import ncbi.taggerOne.types.MentionName;
import ncbi.taggerOne.util.Dictionary;
import ncbi.taggerOne.util.vector.DenseVector;
import ncbi.taggerOne.util.vector.Vector;
import ncbi.taggerOne.util.vector.Vector.VectorIterator;

public class IDFTokenWeightCalculator implements Serializable {

	private static final long serialVersionUID = 1L;

	private Vector<String> weights;

	public IDFTokenWeightCalculator(Dictionary<String> mentionVectorSpace, Lexicon lexicon) {
		int[] counts = new int[mentionVectorSpace.size()];
		int size = 0;
		Dictionary<String> entityTypes = lexicon.getEntityTypes();
		for (int entityTypeIndex = 0; entityTypeIndex < entityTypes.size(); entityTypeIndex++) {
			String entityType = entityTypes.getElement(entityTypeIndex);
			Index index = lexicon.getIndex(entityType);
			Dictionary<String> nameVectorSpace = index.getNameVectorSpace();
			Dictionary<Entity> entities = index.getEntityDictionary();
			for (int entityIndex = 0; entityIndex < entities.size(); entityIndex++) {
				Entity entity = entities.getElement(entityIndex);
				for (MentionName name : entity.getNames()) {
					Vector<String> nameVector = name.getVector();
					if (nameVector != null) {
						VectorIterator iterator = nameVector.getIterator();
						while (iterator.next()) {
							int nameIndex = iterator.getIndex();
							String token = nameVectorSpace.getElement(nameIndex);
							int mentionIndex = mentionVectorSpace.getIndex(token);
							if (mentionIndex >= 0) {
								counts[mentionIndex]++;
							}
						}
						size++;
					}
				}
			}
		}
		double sizeDouble = size;
		Vector<String> mentionWeights = new DenseVector<String>(mentionVectorSpace);
		for (int mentionIndex = 0; mentionIndex < mentionVectorSpace.size(); mentionIndex++) {
			int count = counts[mentionIndex];
			double weight = Math.log(sizeDouble / (count + 1.0));
			mentionWeights.set(mentionIndex, weight);
		}
		this.weights = mentionWeights;
	}

	public Vector<String> getWeights() {
		return weights;
	}
}
