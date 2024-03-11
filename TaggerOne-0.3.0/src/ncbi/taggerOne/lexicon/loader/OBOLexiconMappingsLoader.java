package ncbi.taggerOne.lexicon.loader;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ncbi.taggerOne.T1Constants;
import ncbi.taggerOne.lexicon.LexiconMappings;

public class OBOLexiconMappingsLoader implements LexiconMappingsLoader {

	private static final Logger logger = LoggerFactory.getLogger(OBOLexiconMappingsLoader.class);
	private static final String ID_LINE = "id: ";
	private static final String NAME_LINE = "name: ";
	private static final String SYNONYM_LINE = "synonym: ";
	private static final int ID_START = ID_LINE.length();
	private static final int NAME_START = NAME_LINE.length();
	private static final int SYNONYM_START = SYNONYM_LINE.length();

	private String entityType;
	private String filename;

	@Override
	public void setArgs(String... args) {
		if (args.length != 3) {
			throw new IllegalArgumentException("OBOLexiconMappingsLoader must have two arguments: entityType and lexiconFilename");
		}
		this.entityType = args[1];
		this.filename = args[2];
	}

	@Override
	public Set<String> getNamespaceSet() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void loadIdentifiers(LexiconMappings lexiconMappings) {
		try {
			BufferedReader reader = null;
			if (filename.endsWith(".gz")) {
				reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(filename)), T1Constants.UTF8_FORMAT));
			} else {
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), T1Constants.UTF8_FORMAT));
			}
			String line = reader.readLine();
			while (line != null) {
				// Advance to [Term]
				while (line != null && !line.trim().equals("[Term]")) {
					line = reader.readLine();
				}
				String id = null;
				if (line != null) {
					// Get past [Term]
					line = reader.readLine();
					while (line != null && line.trim().length() > 0) {
						// Handle lines
						if (line.startsWith(ID_LINE)) {
							id = line.substring(ID_START, line.length()).trim();
							lexiconMappings.addIdentifier(id, entityType, true);
							logger.trace("Adding identifier \"" + id + "\"");
						}
						line = reader.readLine();
					}
				}
			}
			reader.close();
			reader = null;
		} catch (IOException e) {
			// TODO Improve error handling
			throw new RuntimeException(e);
		}
	}

	@Override
	public void loadIdentifierEquivalencies(LexiconMappings lexiconMappings) {
		// Not used
	}

	@Override
	public void loadNames(LexiconMappings lexiconMappings) {
		try {
			BufferedReader reader = null;
			if (filename.endsWith(".gz")) {
				reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(filename)), T1Constants.UTF8_FORMAT));
			} else {
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), T1Constants.UTF8_FORMAT));
			}
			String line = reader.readLine();
			while (line != null) {
				// Advance to [Term]
				while (line != null && !line.trim().equals("[Term]")) {
					line = reader.readLine();
				}
				String id = null;
				if (line != null) {
					// Get past [Term]
					line = reader.readLine();
					while (line != null && line.trim().length() > 0) {
						// Handle lines
						if (line.startsWith(ID_LINE)) {
							id = line.substring(ID_START, line.length()).trim();
						} else if (line.startsWith(NAME_LINE)) {
							String termStr = line.substring(NAME_START, line.length()).trim();
							logger.trace("Adding name term \"" + termStr + "\" to " + id);
							lexiconMappings.addTerm(id, termStr, true);
						} else if (line.startsWith(SYNONYM_LINE)) {
							String termStr = line.substring(SYNONYM_START, line.length()).trim();
							logger.trace("Adding synonym term \"" + termStr + "\" to " + id);
							lexiconMappings.addTerm(id, termStr, false);
						}
						line = reader.readLine();
					}
				}
			}
			reader.close();
			reader = null;
		} catch (IOException e) {
			// TODO Improve error handling
			throw new RuntimeException(e);
		}
	}

}
