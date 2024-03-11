package ncbi.taggerOne.processing.mentionName;

import static org.junit.Assert.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import ncbi.taggerOne.processing.string.AcronymPreservingLowerCaseStringProcessor;
import ncbi.taggerOne.processing.string.LowerCaseStringProcessor;
import ncbi.taggerOne.processing.string.PatternProcessor;
import ncbi.taggerOne.processing.string.PluralStemmer;
import ncbi.taggerOne.processing.string.StopWordRemover;
import ncbi.taggerOne.processing.string.StringProcessor;
import ncbi.taggerOne.processing.string.Trimmer;
import ncbi.taggerOne.types.MentionName;
import ncbi.taggerOne.util.tokenization.FinerTokenizer;
import ncbi.taggerOne.util.tokenization.Tokenizer;

public class MentionNameProcessingPipelineTest {

	private static final Pattern whiteSpacePattern = Pattern.compile("\\s");

	@Test
	public void test() {
		StringProcessNameApplicator nameApplicator = new StringProcessNameApplicator(new PatternProcessor(PatternProcessor.PUNCTUATION_PATTERNS, PatternProcessor.SPACE_REPLACEMENT), new Trimmer());
		Tokenizer entityTokenizer = new FinerTokenizer();
		EntityNameTokenizer entityNameTokenizer = new EntityNameTokenizer(entityTokenizer);
		StringProcessor stemmer = new PluralStemmer();
		StringProcessTokenApplicator tokenApplicator = new StringProcessTokenApplicator(new AcronymPreservingLowerCaseStringProcessor(4), new StopWordRemover(new LowerCaseStringProcessor(), StopWordRemover.DEFAULT_STOP_WORDS), stemmer);
		MentionNameProcessor extractorPipeline = new MentionNameProcessingPipeline(nameApplicator, entityNameTokenizer, tokenApplicator);
		verifyNoWhitespace("1­-4", extractorPipeline);
		verifyNoWhitespace("2-(R²-THIO)-10-[3-(4-R1-PIPERAZIN-1-YL)PROPYL]", extractorPipeline);
		verifyNoWhitespace("twentieth (½ to 1/20)", extractorPipeline);
		verifyNoWhitespace("chloride or ½ dibenzoyltartrate", extractorPipeline);
	}

	private static void verifyNoWhitespace(String str, MentionNameProcessor extractorPipeline) {
		MentionName name = new MentionName(str);
		extractorPipeline.process(name);
		for (String token : name.getTokens()) {
			Matcher matcher = whiteSpacePattern.matcher(token);
			assertFalse("Processing name \"" + name.getName() + "\" resulted in a token containing whitespace: \"" + token + "\"", matcher.find());
		}
	}

}
