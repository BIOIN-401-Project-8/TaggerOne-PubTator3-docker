package ncbi.taggerOne.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class ArraySetTest {

	@Test
	public void test1() {
		ArraySet<String> set = new ArraySet<String>();

		assertEquals(0, set.size());
		assertTrue(set.isEmpty());
		assertFalse(set.contains("e0"));
		assertFalse(set.contains("e1"));
		assertFalse(set.contains("e2"));

		set.add("e0");

		assertEquals(1, set.size());
		assertFalse(set.isEmpty());
		assertTrue(set.contains("e0"));
		assertFalse(set.contains("e1"));
		assertFalse(set.contains("e2"));

		set.add("e0");

		assertEquals(1, set.size());
		assertFalse(set.isEmpty());
		assertTrue(set.contains("e0"));
		assertFalse(set.contains("e1"));
		assertFalse(set.contains("e2"));

		set.add("e1");

		assertEquals(2, set.size());
		assertFalse(set.isEmpty());
		assertTrue(set.contains("e0"));
		assertTrue(set.contains("e1"));
		assertFalse(set.contains("e2"));

		set.add("e2");

		assertEquals(3, set.size());
		assertFalse(set.isEmpty());
		assertTrue(set.contains("e0"));
		assertTrue(set.contains("e1"));
		assertTrue(set.contains("e2"));

		set.remove("e1");

		assertEquals(2, set.size());
		assertFalse(set.isEmpty());
		assertTrue(set.contains("e0"));
		assertFalse(set.contains("e1"));
		assertTrue(set.contains("e2"));

		set.clear();

		assertEquals(0, set.size());
		assertTrue(set.isEmpty());
		assertFalse(set.contains("e0"));
		assertFalse(set.contains("e1"));
		assertFalse(set.contains("e2"));
	}

	@Test
	public void test3() {
		ArraySet<String> set1 = new ArraySet<String>();
		set1.add("e0");
		set1.add("e1");
		set1.add("e2");

		assertEquals(3, set1.size());
		assertFalse(set1.isEmpty());
		assertTrue(set1.contains("e0"));
		assertTrue(set1.contains("e1"));
		assertTrue(set1.contains("e2"));

		ArraySet<String> set2 = new ArraySet<String>();
		assertTrue(set2.addAll(set1));
		assertFalse(set2.addAll(set1));

		assertEquals(3, set2.size());
		assertFalse(set2.isEmpty());
		assertTrue(set2.contains("e0"));
		assertTrue(set2.contains("e1"));
		assertTrue(set2.contains("e2"));
		assertTrue(set1.containsAll(set2));
		assertTrue(set2.containsAll(set1));

		set1.clear();

		assertEquals(0, set1.size());
		assertTrue(set1.isEmpty());
		assertFalse(set1.contains("e0"));
		assertFalse(set1.contains("e1"));
		assertFalse(set1.contains("e2"));

		assertEquals(3, set2.size());
		assertFalse(set2.isEmpty());
		assertTrue(set2.contains("e0"));
		assertTrue(set2.contains("e1"));
		assertTrue(set2.contains("e2"));

		assertFalse(set1.containsAll(set2));
		assertTrue(set2.containsAll(set1));
	}

	@Test
	public void test2() {
		ArraySet<String> set1 = new ArraySet<String>();
		set1.add("e0");
		set1.add("e1");
		set1.add("e2");

		assertEquals(3, set1.size());
		assertFalse(set1.isEmpty());
		assertTrue(set1.contains("e0"));
		assertTrue(set1.contains("e1"));
		assertTrue(set1.contains("e2"));

		ArraySet<String> set2 = new ArraySet<String>(set1);

		assertEquals(3, set2.size());
		assertFalse(set2.isEmpty());
		assertTrue(set2.contains("e0"));
		assertTrue(set2.contains("e1"));
		assertTrue(set2.contains("e2"));
		assertTrue(set1.containsAll(set2));
		assertTrue(set2.containsAll(set1));

		set1.clear();

		assertEquals(0, set1.size());
		assertTrue(set1.isEmpty());
		assertFalse(set1.contains("e0"));
		assertFalse(set1.contains("e1"));
		assertFalse(set1.contains("e2"));

		assertEquals(3, set2.size());
		assertFalse(set2.isEmpty());
		assertTrue(set2.contains("e0"));
		assertTrue(set2.contains("e1"));
		assertTrue(set2.contains("e2"));

		assertFalse(set1.containsAll(set2));
		assertTrue(set2.containsAll(set1));
	}
}
