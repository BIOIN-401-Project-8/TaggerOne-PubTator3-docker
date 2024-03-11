package ncbi.taggerOne.dataset;

import java.util.List;
import java.util.Set;

import ncbi.taggerOne.lexicon.Lexicon;
import ncbi.taggerOne.types.TextInstance;

public interface Dataset {

	// TODO How to handle a dataset that will not all fit in memory?

	public void setArgs(String... args);

	public Set<String> getEntityTypes();

	public void setLexicon(Lexicon lexicon);

	// TODO Consider refactoring to provide a map of entity types and what type of annotations the dataset contains
	public boolean containsEntityAnnotations();

	public List<TextInstance> getInstances();

	public enum Usage {
		IDENTIFY, RECOGNIZE, IGNORE;
	}

}
