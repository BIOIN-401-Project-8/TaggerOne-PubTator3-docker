package ncbi.taggerOne.processing.string;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class StopWordRemoverTest {

	@Test
	public void test1() {
		Set<String> stopWords = new HashSet<String>();
		stopWords.add("OPeZ");
		stopWords.add("UrSq");
		stopWords.add("inrm");
		stopWords.add("QqOv");
		stopWords.add("DRRu");

		StopWordRemover stopWordRemover = new StopWordRemover(null, stopWords);

		// Verify matching words correctly removed
		assertEquals("", stopWordRemover.process("OPeZ"));
		assertEquals("", stopWordRemover.process("UrSq"));
		assertEquals("", stopWordRemover.process("inrm"));
		assertEquals("", stopWordRemover.process("QqOv"));
		assertEquals("", stopWordRemover.process("DRRu"));

		// Verify nonmatching words correctly retained
		assertEquals("doqw", stopWordRemover.process("doqw"));
		assertEquals("bMeJ", stopWordRemover.process("bMeJ"));
		assertEquals("wTLj", stopWordRemover.process("wTLj"));
		assertEquals("Wzor", stopWordRemover.process("Wzor"));
		assertEquals("UQFb", stopWordRemover.process("UQFb"));

		// Verify words requiring processing correctly marked nonmatching
		assertEquals("OPEZ", stopWordRemover.process("OPEZ"));
		assertEquals("URSQ", stopWordRemover.process("URSQ"));
		assertEquals("INRM", stopWordRemover.process("INRM"));
		assertEquals("QQOV", stopWordRemover.process("QQOV"));
		assertEquals("DRRU", stopWordRemover.process("DRRU"));

		// Verify internal cache of stop words is independent
		stopWords.clear();
		assertEquals("", stopWordRemover.process("OPeZ"));
		assertEquals("", stopWordRemover.process("UrSq"));
		assertEquals("", stopWordRemover.process("inrm"));
		assertEquals("", stopWordRemover.process("QqOv"));
		assertEquals("", stopWordRemover.process("DRRu"));
	}

	@Test
	public void test2() {
		Set<String> stopWords = new HashSet<String>();
		stopWords.add("OPeZ");
		stopWords.add("UrSq");
		stopWords.add("inrm");
		stopWords.add("QqOv");
		stopWords.add("DRRu");

		StopWordRemover stopWordRemover = new StopWordRemover(new LowerCaseStringProcessor(), stopWords);

		// Verify matching words correctly removed
		assertEquals("", stopWordRemover.process("OPeZ"));
		assertEquals("", stopWordRemover.process("UrSq"));
		assertEquals("", stopWordRemover.process("inrm"));
		assertEquals("", stopWordRemover.process("QqOv"));
		assertEquals("", stopWordRemover.process("DRRu"));

		// Verify nonmatching words correctly retained
		assertEquals("doqw", stopWordRemover.process("doqw"));
		assertEquals("bMeJ", stopWordRemover.process("bMeJ"));
		assertEquals("wTLj", stopWordRemover.process("wTLj"));
		assertEquals("Wzor", stopWordRemover.process("Wzor"));
		assertEquals("UQFb", stopWordRemover.process("UQFb"));

		// Verify words requiring processing correctly marked matching
		assertEquals("", stopWordRemover.process("OPEZ"));
		assertEquals("", stopWordRemover.process("URSQ"));
		assertEquals("", stopWordRemover.process("INRM"));
		assertEquals("", stopWordRemover.process("QQOV"));
		assertEquals("", stopWordRemover.process("DRRU"));

		// Verify internal cache of stop words is independent
		stopWords.clear();
		assertEquals("", stopWordRemover.process("OPeZ"));
		assertEquals("", stopWordRemover.process("UrSq"));
		assertEquals("", stopWordRemover.process("inrm"));
		assertEquals("", stopWordRemover.process("QqOv"));
		assertEquals("", stopWordRemover.process("DRRu"));
	}

}
