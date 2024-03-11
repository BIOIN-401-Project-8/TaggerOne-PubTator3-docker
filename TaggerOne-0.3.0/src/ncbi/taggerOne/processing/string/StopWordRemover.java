package ncbi.taggerOne.processing.string;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class StopWordRemover implements StringProcessor {

	public static final Set<String> DEFAULT_STOP_WORDS = new HashSet<String>(Arrays.asList("a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "if", "in", "into", "is", "it", "no", "not", "of", "on", "or", "such", "that", "the",
			"their", "then", "there", "these", "they", "this", "to", "was", "will", "with"));

	private static final long serialVersionUID = 1L;

	private StringProcessor processor;
	private Set<String> stopWords;

	public StopWordRemover(StringProcessor processor, Set<String> stopWords) {
		this.processor = processor;
		if (processor == null) {
			this.stopWords = new HashSet<String>(stopWords);
		} else {
			this.stopWords = new HashSet<String>();
			for (String stopWord : stopWords) {
				this.stopWords.add(processor.process(stopWord));
			}
		}
	}

	@Override
	public String process(String str) {
		String str2 = str;
		if (processor != null) {
			str2 = processor.process(str);
		}
		if (stopWords.contains(str2)) {
			return "";
		}
		return str;
	}
}
