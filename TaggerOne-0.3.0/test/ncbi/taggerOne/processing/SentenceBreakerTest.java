package ncbi.taggerOne.processing;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import ncbi.taggerOne.types.TextInstance;

public class SentenceBreakerTest {

	@Test
	public void test1() {
		String text0 = "The adenomatous polyposis coli (APC) protein controls Wnt by forming a kinase 3beta (GSK-3beta), axin/conductin and betacatenin. ";
		String text1 = "Complex formation induces the rapid degradation of betacatenin. ";
		String text2 = "In carcinomas, loss of APC leads to accumulation, where it activates the Tcf-4 transcription factor (reviewed in [1] [2]). ";
		String text3 = "Here, we report the identification and genomic structure of APC homologues.";
		String text = text0 + text1 + text2 + text3;
		TextInstance instance = new TextInstance(null, "123456-00", "123456", text, 23);

		SentenceBreaker sb = new SentenceBreaker();
		List<TextInstance> instances = sb.breakSentences(instance);
		assertEquals(text0, instances.get(0).getText());
		assertEquals(text1, instances.get(1).getText());
		assertEquals(text2, instances.get(2).getText());
		assertEquals(text3, instances.get(3).getText());
		assertEquals(4, instances.size());
	}

	@Test
	public void test2() {
		String text0 = "This is short. ";
		String text1 = "Testing (A. B. C. E.) also. ";
		String text2 = "And another.";
		String text = text0 + text1 + text2;
		TextInstance instance = new TextInstance(null, "123456-00", "123456", text, 23);

		SentenceBreaker sb = new SentenceBreaker();
		List<TextInstance> instances = sb.breakSentences(instance);
		assertEquals(text0, instances.get(0).getText());
		assertEquals(text1, instances.get(1).getText());
		assertEquals(text2, instances.get(2).getText());
		assertEquals(3, instances.size());
	}

	@Test
	public void test3() {
		String text0 = "The invention concerns novel aryl glycinamide derivatives of general formula (I) and pharmaceutically acceptable salts thereof, the symbols having the following meanings: R?1 and R2¿ together with the nitrogen atom to which they are bound form a ring of formula (II) shown in which p is 2 or 3 and X stands for oxygen, N(CH¿2?)nR?6 or CR7R8; and R3, R4, R5, R6, R7, R8¿, Ar and n have the meanings indicated in the description. ";
		String text = text0;
		TextInstance instance = new TextInstance(null, "123456-00", "123456", text, 23);

		SentenceBreaker sb = new SentenceBreaker();
		List<TextInstance> instances = sb.breakSentences(instance);
		assertEquals(text0, instances.get(0).getText());
		assertEquals(1, instances.size());
	}

	@Test
	public void testError1() {
		String text0 = "Interesting. ";
		String text1 = "Can we trigger an error?";
		String text = text0 + text1;
		TextInstance instance = new TextInstance(null, "123456-00", "123456", text, 23);

		SentenceBreaker sb = new SentenceBreaker();
		List<TextInstance> instances = sb.breakSentences(instance);
		assertEquals(text0, instances.get(0).getText());
		assertEquals(text1, instances.get(1).getText());
		assertEquals(2, instances.size());
	}

	@Test
	public void testError2() {
		String text0 = "Another (error check";
		String text = text0;
		TextInstance instance = new TextInstance(null, "123456-00", "123456", text, 23);

		SentenceBreaker sb = new SentenceBreaker();
		List<TextInstance> instances = sb.breakSentences(instance);
		assertEquals(text0, instances.get(0).getText());
		assertEquals(1, instances.size());
	}

}
