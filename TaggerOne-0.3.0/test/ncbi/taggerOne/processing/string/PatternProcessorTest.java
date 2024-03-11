package ncbi.taggerOne.processing.string;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Test;

public class PatternProcessorTest {

	@Test
	public void testError() {
		try {
			new PatternProcessor(PatternProcessor.PUNCTUATION_PATTERNS, PatternProcessor.CHARACTER_CLASS_REPLACEMENTS);
			Assert.fail("Expected exception to be thrown");
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}
	}

	@Test
	public void testRemovePunctuation() {
		PatternProcessor p = new PatternProcessor(PatternProcessor.PUNCTUATION_PATTERNS, PatternProcessor.EMPTY_REPLACEMENT);
		assertEquals("", p.process(""));
		assertEquals("Alpha 1 test 321 7", p.process("Alpha 1% ′test′ 3-2=1 @7"));
	}

	@Test
	public void testPunctuationToSpace() {
		PatternProcessor p = new PatternProcessor(PatternProcessor.PUNCTUATION_PATTERNS, PatternProcessor.SPACE_REPLACEMENT);
		assertEquals("", p.process(""));
		assertEquals("Alpha 1   test  3 2 1  7", p.process("Alpha 1% ′test′ 3-2=1 @7"));
		assertEquals("Hi mom ", p.process("Hi mom!"));
		assertEquals("2 2 4", p.process("2+2=4"));
		assertEquals("3  4  bis difluoromethoxycinnamoylanthranilate ", p.process("3',4'-bis(difluoromethoxycinnamoylanthranilate)"));
		assertEquals("3  4  bis difluoromethoxycinnamoylanthranilate", p.process("3′,4′-bis-difluoromethoxycinnamoylanthranilate"));
		assertEquals("A B", p.process("A¬B"));
	}

	@Test
	public void testRemoveWhitespace() {
		PatternProcessor p = new PatternProcessor(PatternProcessor.WHITESPACE_PATTERNS, PatternProcessor.EMPTY_REPLACEMENT);
		assertEquals("", p.process(""));
		assertEquals("Alpha1%′test′3-2=1@7", p.process("Alpha 1% ′test′ 3-2=1 @7"));
	}

	@Test
	public void testNumberClass() {
		PatternProcessor p = new PatternProcessor(PatternProcessor.NUMBER_CLASS_PATTERNS, PatternProcessor.NUMBER_CLASS_REPLACEMENTS);
		assertEquals("", p.process(""));
		assertEquals("Alpha 00% ′test′ 00-00=00 @000", p.process("Alpha 19% ′test′ 31-21=10 @745"));
	}

	@Test
	public void testBriefNumberClass() {
		PatternProcessor p = new PatternProcessor(PatternProcessor.BRIEF_NUMBER_CLASS_PATTERNS, PatternProcessor.NUMBER_CLASS_REPLACEMENTS);
		assertEquals("", p.process(""));
		assertEquals("Alpha 0% ′test′ 0-0=0 @0", p.process("Alpha 19% ′test′ 31-21=10 @745"));
	}

	@Test
	public void testTokenClass() {
		PatternProcessor p = new PatternProcessor(PatternProcessor.CHARACTER_CLASS_PATTERNS, PatternProcessor.CHARACTER_CLASS_REPLACEMENTS);
		assertEquals("", p.process(""));
		assertEquals("Aaaaa_00x_xaaaax_00x00x00_x000", p.process("Alpha 19% ′test′ 31-21=10 @745"));
	}

	@Test
	public void testBriefTokenClass() {
		PatternProcessor p = new PatternProcessor(PatternProcessor.BRIEF_CHARACTER_CLASS_PATTERNS, PatternProcessor.CHARACTER_CLASS_REPLACEMENTS);
		assertEquals("", p.process(""));
		assertEquals("Aa_0x_xax_0x0x0_x0", p.process("Alpha 19% ′test′ 31-21=10 @745"));
	}

}
