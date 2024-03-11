package ncbi.taggerOne.processing.textInstance;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import ncbi.taggerOne.types.TextInstance;
import ncbi.taggerOne.types.Token;
import ncbi.taggerOne.util.tokenization.FineTokenizer;

public class TokenFeatureProcessorTest {

	@Test
	public void test1() {
		String text = "A common human skin tumour is caused by activating mutations in beta-catenin.";
		TextInstance instance = new TextInstance(null, "123456-00", "123456", text, 23);
		TextInstanceTokenizer tokenizer = new TextInstanceTokenizer(new FineTokenizer());
		tokenizer.process(instance);
		List<Token> tokens = instance.getTokens();
		assertEquals("A", tokens.get(0).getText());
		assertEquals("common", tokens.get(1).getText());
		assertEquals("human", tokens.get(2).getText());
		assertEquals("skin", tokens.get(3).getText());
		assertEquals("tumour", tokens.get(4).getText());
		assertEquals("is", tokens.get(5).getText());
		assertEquals("caused", tokens.get(6).getText());
		assertEquals("by", tokens.get(7).getText());
		assertEquals("activating", tokens.get(8).getText());
		assertEquals("mutations", tokens.get(9).getText());
		assertEquals("in", tokens.get(10).getText());
		assertEquals("beta", tokens.get(11).getText());
		assertEquals("-", tokens.get(12).getText());
		assertEquals("catenin", tokens.get(13).getText());
		assertEquals(".", tokens.get(14).getText());
		assertEquals(15, tokens.size());
	}
}
