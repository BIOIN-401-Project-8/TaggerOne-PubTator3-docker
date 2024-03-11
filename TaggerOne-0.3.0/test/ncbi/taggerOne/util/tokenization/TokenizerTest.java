package ncbi.taggerOne.util.tokenization;

import static org.junit.Assert.*;

import org.junit.Test;

public class TokenizerTest {

	@Test
	public void test1() {
		assertTrue(Tokenizer.isLowerCaseLetter('a'));
		assertFalse(Tokenizer.isLowerCaseLetter('A'));
		assertFalse(Tokenizer.isLowerCaseLetter('1'));
		assertFalse(Tokenizer.isLowerCaseLetter('%'));
		assertFalse(Tokenizer.isLowerCaseLetter('′'));

		assertFalse(Tokenizer.isUpperCaseLetter('a'));
		assertTrue(Tokenizer.isUpperCaseLetter('A'));
		assertFalse(Tokenizer.isUpperCaseLetter('1'));
		assertFalse(Tokenizer.isUpperCaseLetter('%'));
		assertFalse(Tokenizer.isLowerCaseLetter('′'));
	}

}
