package ncbi.taggerOne.lexicon.loader;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import ncbi.taggerOne.T1Constants;
import ncbi.taggerOne.lexicon.LexiconMappings;

public class DelimitedDefaultEntityTypeLoader implements LexiconMappingsLoader {

	private String defaultEntityType;
	private String defaultNamespace;
	private String lexiconFilename;

	/*
	 * This lexicon file format is intended for user-defined data where all concepts are the same entity type. The file format is two columns, separated by a tab character. For each line the first column contains one or more identifiers,
	 * separated by a pipe character ("|"). The second column is not required for every line, but if present it contains one or more names, again separated by a pipe character. Identifiers may appear on more than one line; identifiers
	 * appearing on the same line will be considered to represent the same concept. Identifiers that do specify a namespace (in other words, do not contain a colon) will have the default namespace added to the beginning, followed by a
	 * colon. The first name listed for the identifier will be marked preferred.
	 */

	public DelimitedDefaultEntityTypeLoader() {
		// Empty
	}

	public void setArgs(String... args) {
		if (args.length != 4) {
			throw new IllegalArgumentException("DelimitedDefaultEntityTypeLoader must have three arguments: defaultEntityType, defaultNamespace, lexiconFilename");
		}
		this.defaultEntityType = args[1];
		this.defaultNamespace = args[2];
		this.lexiconFilename = args[3];
	}

	private void checkArgs() {
		if (lexiconFilename == null) {
			throw new IllegalStateException("Cannot use DelimitedDefaultEntityTypeLoader until args are set");
		}
	}

	@Override
	public Set<String> getNamespaceSet() {
		// FIXME
		throw new RuntimeException("Not implemented");
	}

	@Override
	public void loadIdentifiers(LexiconMappings lexiconMappings) {
		checkArgs();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(lexiconFilename), T1Constants.UTF8_FORMAT));
			String line = reader.readLine();

			while (line != null) {
				line = line.trim();
				if (!line.startsWith("#")) {
					String[] fields = line.split("\t");
					String[] ids = fields[0].split("\\|");

					for (String id : ids) {
						// Add default namespace if no namespace defined
						if (!id.contains(":")) {
							id = defaultNamespace + ":" + id;
						}
						// Adds the identifier as the specified type
						// Readding as same type is ok
						// Throws error if adding as a different type
						lexiconMappings.addIdentifier(id, defaultEntityType, true);
					}
				}
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			// TODO Improve error handling
			throw new RuntimeException(e);
		}
	}

	@Override
	public void loadIdentifierEquivalencies(LexiconMappings lexiconMappings) {
		checkArgs();
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(lexiconFilename), T1Constants.UTF8_FORMAT));
			String line = reader.readLine();

			while (line != null) {
				line = line.trim();
				if (!line.startsWith("#")) {
					String[] fields = line.split("\t");
					String[] ids = fields[0].split("\\|");

					String id0 = ids[0];
					// Add default namespace if no namespace defined
					if (!id0.contains(":")) {
						id0 = defaultNamespace + ":" + id0;
					}

					for (int idIndex = 1; idIndex < ids.length; idIndex++) {
						String id = ids[idIndex];
						// Add default namespace if no namespace defined
						if (!id.contains(":")) {
							id = defaultNamespace + ":" + id;
						}
						lexiconMappings.addIdentifierEquivalence(id0, id);
					}
				}
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			// TODO Improve error handling
			throw new RuntimeException(e);
		}
	}

	@Override
	public void loadNames(LexiconMappings lexiconMappings) {
		checkArgs();
		try {
			Set<String> hasPrimaryName = new HashSet<String>();
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(lexiconFilename), T1Constants.UTF8_FORMAT));
			String line = reader.readLine();
			while (line != null) {
				line = line.trim();
				if (!line.startsWith("#")) {
					// logger.info("LINE2: " + line);
					String[] fields = line.split("\t");
					if (fields.length > 1) {
						String[] ids = fields[0].split("\\|");
						String[] names = fields[1].split("\\|");

						String id = ids[0]; // ID
						// Add default namespace if no namespace defined
						if (!id.contains(":")) {
							id = defaultNamespace + ":" + id;
						}

						// Get names
						for (int nameIndex = 0; nameIndex < names.length; nameIndex++) {
							boolean added = hasPrimaryName.add(id);
							lexiconMappings.addTerm(id, names[nameIndex], added);
						}
					}
				}
				line = reader.readLine();
			}
			reader.close();
		} catch (IOException e) {
			// TODO Improve error handling
			throw new RuntimeException(e);
		}
	}
}
