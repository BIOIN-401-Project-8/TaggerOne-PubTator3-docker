package ncbi.taggerOne.processing.stoppingCriteria;

public class FixedModelNameFormatter implements ModelNameFormatter {

	private String modelFilename;

	public FixedModelNameFormatter(String modelFilename) {
		this.modelFilename = modelFilename;
	}

	@Override
	public String getModelOutputFilename() {
		return modelFilename;
	}

}
