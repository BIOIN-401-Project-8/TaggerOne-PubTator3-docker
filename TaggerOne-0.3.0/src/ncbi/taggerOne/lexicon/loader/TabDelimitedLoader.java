package ncbi.taggerOne.lexicon.loader;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ncbi.taggerOne.T1Constants;
import ncbi.taggerOne.lexicon.LexiconMappings;

public class TabDelimitedLoader implements LexiconMappingsLoader {

	private static final Logger logger = LoggerFactory.getLogger(TabDelimitedLoader.class);

	private String lexiconFilename;

	/*
	 * This lexicon format is intended for user-defined data and therefore loads the data exactly as provided. The file format is: Type \t Identifier \t Name | Name ... Identifiers may appear on more than one line. The first name listed for
	 * the identifier will be marked preferred.
	 */

	public TabDelimitedLoader() {
		// Empty
	}

	public void setArgs(String... args) {
		if (args.length != 2) {
			throw new IllegalArgumentException("TabDelimitedLoader must have one argument: lexiconFilename");
		}
		this.lexiconFilename = args[1];
	}

	private void checkArgs() {
		if (lexiconFilename == null) {
			throw new IllegalStateException("Cannot use TabDelimitedLoader until args are set");
		}
	}

	@Override
	public Set<String> getNamespaceSet() {
		checkArgs();
		try {
			Set<String> namespaces = new HashSet<String>();
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(lexiconFilename), T1Constants.UTF8_FORMAT));
			String line = reader.readLine();
			while (line != null) {
				line = line.trim();
				if (!line.startsWith("#")) {
					String[] fields = line.split("\t");
					String identifier = fields[1]; // ID
					String[] split = identifier.split(":");
					namespaces.add(split[0]);
				}
				line = reader.readLine();
			}
			reader.close();
			return namespaces;
		} catch (IOException e) {
			// TODO Improve error handling
			throw new RuntimeException(e);
		}
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
					// logger.info("LINE1: " + line);
					String[] fields = line.split("\t");

					String entityType = fields[0]; // Entity type
					String id = fields[1]; // ID

					// Adds the identifier as the specified type
					// Readding as same type is ok
					// Throws error if adding as a different type
					lexiconMappings.addIdentifier(id, entityType, true);
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
		// Empty - this format does not define any identifier equivalencies
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

					String id = fields[1]; // ID

					// Get names
					String[] names = fields[2].split("\\|");
					for (int nameIndex = 0; nameIndex < names.length; nameIndex++) {
						boolean added = hasPrimaryName.add(id);
						logger.trace("Adding name " + names[nameIndex] + " to ID " + id);
						lexiconMappings.addTerm(id, names[nameIndex], added);
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
