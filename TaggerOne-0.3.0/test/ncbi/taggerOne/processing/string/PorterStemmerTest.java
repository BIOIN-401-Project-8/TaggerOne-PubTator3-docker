package ncbi.taggerOne.processing.string;

import static org.junit.Assert.*;

import org.junit.Test;

public class PorterStemmerTest {

	@Test
	public void test() {
		PorterStemmer stemmer = new PorterStemmer();
		assertEquals("develop", stemmer.process("developing"));
		assertEquals("antibodi", stemmer.process("antibodies"));
		assertEquals("chromosom", stemmer.process("chromosomes"));
		assertEquals("toe", stemmer.process("toes"));
		assertEquals("locu", stemmer.process("locus"));
		assertEquals("overexpress", stemmer.process("overexpress"));
		assertEquals("irrevers", stemmer.process("irreversible"));
		assertEquals("evid", stemmer.process("evidence"));
		assertEquals("hope", stemmer.process("hope"));
		assertEquals("assess", stemmer.process("assesses"));
		assertEquals("feed", stemmer.process("feed"));
		// 64.3%
	}

}
