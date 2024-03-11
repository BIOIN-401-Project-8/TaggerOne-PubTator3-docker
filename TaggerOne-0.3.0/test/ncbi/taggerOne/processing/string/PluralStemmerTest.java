package ncbi.taggerOne.processing.string;

import static org.junit.Assert.*;

import org.junit.Test;

public class PluralStemmerTest {

	@Test
	public void test() {
		PluralStemmer stemmer = new PluralStemmer();
		assertEquals("developing", stemmer.process("developing"));
		assertEquals("antibody", stemmer.process("antibodies"));
		assertEquals("Anopheie", stemmer.process("Anopheies"));
		assertEquals("maie", stemmer.process("maies"));
		assertEquals("chromosome", stemmer.process("chromosomes"));
		assertEquals("Magalhae", stemmer.process("Magalhaes"));
		assertEquals("pedigree", stemmer.process("pedigrees"));
		assertEquals("toe", stemmer.process("toes"));
		assertEquals("locus", stemmer.process("locus"));
		assertEquals("overexpress", stemmer.process("overexpress"));
	}

}
