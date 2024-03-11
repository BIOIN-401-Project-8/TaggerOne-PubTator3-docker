package ncbi.taggerOne.util.tokenization;

import static org.junit.Assert.*;

import org.junit.Test;

public class FineTokenizerTest {

	@Test
	public void test1() {
		FineTokenizer t = new FineTokenizer();
		t.reset("test");
		assertTrue(t.nextToken());
		assertEquals(0, t.startChar());
		assertEquals(4, t.endChar());
		assertFalse(t.nextToken());
	}

	@Test
	public void test2() {
		FineTokenizer t = new FineTokenizer();
		t.reset("");
		assertFalse(t.nextToken());
	}

	@Test
	public void test3() {
		FineTokenizer t = new FineTokenizer();
		String str = "Germline BRCA1 alterations -- S65C hERB IKKgamma Receptor";
		t.reset(str);
		assertTrue(t.nextToken());
		assertEquals("Germline", str.substring(t.startChar(), t.endChar()));
		assertEquals(0, t.startChar());
		assertEquals(8, t.endChar());
		assertTrue(t.nextToken());
		assertEquals("BRCA", str.substring(t.startChar(), t.endChar()));
		assertEquals(9, t.startChar());
		assertEquals(13, t.endChar());
		assertTrue(t.nextToken());
		assertEquals("1", str.substring(t.startChar(), t.endChar()));
		assertEquals(13, t.startChar());
		assertEquals(14, t.endChar());
		assertTrue(t.nextToken());
		assertEquals("alterations", str.substring(t.startChar(), t.endChar()));
		assertEquals(15, t.startChar());
		assertEquals(26, t.endChar());
		assertTrue(t.nextToken());
		assertEquals("-", str.substring(t.startChar(), t.endChar()));
		assertEquals(27, t.startChar());
		assertEquals(28, t.endChar());
		assertTrue(t.nextToken());
		assertEquals("-", str.substring(t.startChar(), t.endChar()));
		assertEquals(28, t.startChar());
		assertEquals(29, t.endChar());
		assertTrue(t.nextToken());
		assertEquals("S", str.substring(t.startChar(), t.endChar()));
		assertEquals(30, t.startChar());
		assertEquals(31, t.endChar());
		assertTrue(t.nextToken());
		assertEquals("65", str.substring(t.startChar(), t.endChar()));
		assertEquals(31, t.startChar());
		assertEquals(33, t.endChar());
		assertTrue(t.nextToken());
		assertEquals("C", str.substring(t.startChar(), t.endChar()));
		assertEquals(33, t.startChar());
		assertEquals(34, t.endChar());
		assertTrue(t.nextToken());
		assertEquals("hERB", str.substring(t.startChar(), t.endChar()));
		assertEquals(35, t.startChar());
		assertEquals(39, t.endChar());
		assertTrue(t.nextToken());
		assertEquals("IKKgamma", str.substring(t.startChar(), t.endChar()));
		assertEquals(40, t.startChar());
		assertEquals(48, t.endChar());
		assertTrue(t.nextToken());
		assertEquals("Receptor", str.substring(t.startChar(), t.endChar()));
		assertEquals(49, t.startChar());
		assertEquals(57, t.endChar());
		assertFalse(t.nextToken());
	}

	@Test
	public void test4() {
		FineTokenizer t = new FineTokenizer();
		t.reset("test ");
		assertTrue(t.nextToken());
		assertEquals(0, t.startChar());
		assertEquals(4, t.endChar());
		assertFalse(t.nextToken());
	}

	@Test
	public void test5() {
		FineTokenizer t = new FineTokenizer();
		String text = "3′,4′-bis-difluoromethoxycinnamoylanthranilate";
		t.reset(text);
		assertTrue(t.nextToken());
		assertEquals("3", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("′", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals(",", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("4", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("′", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("-", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("bis", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("-", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("difluoromethoxycinnamoylanthranilate", text.substring(t.startChar(), t.endChar()));
		assertFalse(t.nextToken());
	}

	@Test
	public void test6() {
		FineTokenizer t = new FineTokenizer();
		String text = "N¬-(4-phenylthiazol)acetamide";
		t.reset(text);
		assertTrue(t.nextToken());
		assertEquals("N", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("¬", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("-", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("(", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("4", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("-", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("phenylthiazol", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals(")", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("acetamide", text.substring(t.startChar(), t.endChar()));
	}

	@Test
	public void test7() {
		FineTokenizer t = new FineTokenizer();
		String text = "(22E,24R)-6β-methoxyergosta-7,22-diene-3b,5α-diol [A—A≡A═A] “aA” 1•2 ≧1θ±1 A→A®";
		t.reset(text);
		assertTrue(t.nextToken());
		assertEquals("(", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("22", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("E", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals(",", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("24", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("R", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals(")", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("-", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("6", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("β", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("-", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("methoxyergosta", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("-", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("7", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals(",", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("22", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("-", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("diene", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("-", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("3", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("b", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals(",", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("5", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("α", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("-", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("diol", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("[", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("A", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("—", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("A", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("≡", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("A", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("═", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("A", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("]", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("“", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("aA", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("”", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("1", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("•", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("2", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("≧", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("1", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("θ", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("±", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("1", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("A", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("→", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("A", text.substring(t.startChar(), t.endChar()));
		assertTrue(t.nextToken());
		assertEquals("®", text.substring(t.startChar(), t.endChar()));
		assertFalse(t.nextToken());
	}
}
