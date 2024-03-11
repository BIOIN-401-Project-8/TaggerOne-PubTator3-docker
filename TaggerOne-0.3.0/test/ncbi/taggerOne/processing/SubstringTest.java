package ncbi.taggerOne.processing;

import static org.junit.Assert.*;

import org.junit.Test;

public class SubstringTest {

	@Test
	public void test() {
		assertEquals("5", "5 µm".substring(0, 1));
		assertEquals("5 µm", "5 µm".substring(0, 4));
		assertEquals("µ", "5 µm".substring(2, 3));
	}

	@Test
	public void test2() {
		String text = "EP1381614B1|t|ANTIDEPRESSANT AZAHETEROCYCLYLMETHYL DERIVATIVES OF 2,3-DIHYDRO-1,4-DIOXINO¬2,3-f|QUINOXALINE";
		int index = text.indexOf("|t|");
		assertEquals("EP1381614B1", text.substring(0, index));
		assertEquals("t", text.substring(index + 1, index + 2));
		assertEquals("ANTIDEPRESSANT AZAHETEROCYCLYLMETHYL DERIVATIVES OF 2,3-DIHYDRO-1,4-DIOXINO¬2,3-f|QUINOXALINE", text.substring(index + 3));
	}

}
