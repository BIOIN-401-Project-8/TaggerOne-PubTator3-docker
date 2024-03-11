package ncbi.taggerOne.model.recognition;

import java.io.Serializable;

import ncbi.taggerOne.util.Dictionary;
import ncbi.taggerOne.util.matrix.Matrix;

public interface RecognitionModelUpdater extends Serializable {
	
	public void update(Matrix<String, String> featureWeights);

	public Dictionary<String> getFeatureSet();

}
