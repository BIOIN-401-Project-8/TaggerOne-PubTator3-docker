package ncbi.taggerOne.abbreviation;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FolderAbbreviationSource implements AbbreviationSource {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = LoggerFactory.getLogger(FolderAbbreviationSource.class);

	private FileAbbreviationSource fileAbbreviationSource;
	private String dirName;

	public FolderAbbreviationSource() {
		fileAbbreviationSource = new FileAbbreviationSource();
	}

	@Override
	public void setArgs(String... args) {
		if (args.length < 2) {
			throw new IllegalArgumentException("FolderAbbreviationSource must have at least one argument: dirname" + Arrays.asList(args));
		}
		dirName = args[1];
		if (!dirName.endsWith("/")) {
			dirName = dirName + "/";
		}
	}

	public void loadFile(String filename) {
		fileAbbreviationSource.clear();
		File f = new File(dirName + filename);
		if (!f.exists()) {
			logger.warn("Abbreviation file \"" + f.getAbsolutePath() + "\" does not exist, skipping");
		} else {
			fileAbbreviationSource.loadAbbreviations(dirName + filename);
		}
	}

	@Override
	public Map<String, String> getAbbreviations(String id, String text) {
		return fileAbbreviationSource.getAbbreviations(id, text);
	}

}
