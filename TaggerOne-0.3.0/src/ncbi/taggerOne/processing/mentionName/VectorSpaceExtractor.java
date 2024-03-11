package ncbi.taggerOne.processing.mentionName;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ncbi.taggerOne.types.MentionName;
import ncbi.taggerOne.util.Dictionary;

public class VectorSpaceExtractor extends MentionNameProcessor {

	private static final Logger logger = LoggerFactory.getLogger(VectorSpaceExtractor.class);
	private static final long serialVersionUID = 1L;
	private static final Pattern whiteSpacePattern = Pattern.compile("\\s");

	private Dictionary<String> vectorSpace;

	public VectorSpaceExtractor(Dictionary<String> featureSet) {
		this.vectorSpace = featureSet;
	}

	@Override
	public void process(MentionName entityName) {
		List<String> tokens = entityName.getTokens();
		int vectorSpaceSize = vectorSpace.size();
		for (int tokenIndex = 0; tokenIndex < tokens.size(); tokenIndex++) {
			String token = tokens.get(tokenIndex);
			int index = vectorSpace.addElement(token);
			if (index < vectorSpaceSize) {
				// This token was already present: canonize the copy in this token list
				tokens.set(tokenIndex, vectorSpace.getElement(index));
			} else {
				// This token is new: no need to canonize
				// Update the size of the vector space
				vectorSpaceSize = vectorSpace.size();
				// Check if this token contains whitespace
				Matcher matcher = whiteSpacePattern.matcher(token);
				if (matcher.find()) {
					logger.error("Processing name \"" + entityName.getName() + "\" resulted in a token containing whitespace: \"" + token + "\"");
				}
			}
		}
	}
}
