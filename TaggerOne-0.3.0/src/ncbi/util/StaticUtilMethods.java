package ncbi.util;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

public class StaticUtilMethods {

	private StaticUtilMethods() {
		// Make uninstantiable
	}

	public static boolean containsAny(Set<String> set1, Set<String> set2) {
		for (String element : set1) {
			if (set2.contains(element)) {
				return true;
			}
		}
		return false;
	}

	public static Set<String> getStringSet(String str) {
		String[] split = str.split(",");
		Set<String> strSet = new HashSet<String>();
		for (int i = 0; i < split.length; i++) {
			strSet.add(split[i]);
		}
		return strSet;
	}

	public static List<String> getStringList(String str) {
		String[] split = str.split(",");
		List<String> strList = new ArrayList<String>();
		for (int i = 0; i < split.length; i++) {
			strList.add(split[i]);
		}
		return strList;
	}

	public static Map<String, String> getStringMap(String str) {
		String[] split = str.split(",");
		Map<String, String> strMap = new HashMap<String, String>();
		for (int i = 0; i < split.length; i++) {
			String[] fields = split[i].split("->");
			strMap.put(fields[0], fields[1]);
		}
		return strMap;
	}

	public static int[] getIntArray(String str) {
		String[] split = str.split(",");
		int[] intArray = new int[split.length];
		for (int i = 0; i < split.length; i++) {
			intArray[i] = Integer.parseInt(split[i]);
		}
		return intArray;
	}

	/*
	 * Returns TRUE iff the two sets contain exactly the same set of elements. This is useful for determining if the content of two Sets is the same, even if the type of the Set is different.
	 */
	public static <E> boolean equalElements(Set<E> s1, Set<E> s2) {
		if (s1 == null) {
			return s2 == null;
		}
		if (s1.size() != s2.size()) {
			return false;
		}
		for (E e : s1) {
			if (!s2.contains(e)) {
				return false;
			}
		}
		return true;
	}

	public static Int2IntMap getByteToCharOffsets(String str, Charset charset) {
		Int2IntMap byteToChar = new Int2IntOpenHashMap();
		byteToChar.defaultReturnValue(Integer.MIN_VALUE);
		int bytePosition = 0;
		for (int charIndex = 0; charIndex < str.length(); charIndex++) {
			byteToChar.put(bytePosition, charIndex);
			String c = str.substring(charIndex, charIndex + 1);
			// System.out.println("B->C Character " + charIndex + " is \"" + c + "\" at bytePosition " + bytePosition);
			byte[] bytes = charset.encode(c).array();
			bytePosition += getCharLength(bytes);
		}
		byteToChar.put(bytePosition, str.length());
		return byteToChar;
	}

	public static Int2IntMap getCharToByteOffsets(String str, Charset charset) {
		Int2IntMap charToByte = new Int2IntOpenHashMap();
		charToByte.defaultReturnValue(Integer.MIN_VALUE);
		int bytePosition = 0;
		for (int charIndex = 0; charIndex < str.length(); charIndex++) {
			charToByte.put(charIndex, bytePosition);
			String c = str.substring(charIndex, charIndex + 1);
			// System.out.println("C->B Character " + charIndex + " is \"" + c + "\" at bytePosition " + bytePosition);
			byte[] bytes = charset.encode(c).array();
			bytePosition += getCharLength(bytes);
		}
		charToByte.put(str.length(), bytePosition);
		return charToByte;
	}

	public static int getCharLength(byte[] bytes) {
		for (int i = 4; i > 1; i--) {
			if (bytes.length >= i && bytes[i - 1] != 0) {
				return i;
			}
		}
		return 1;
	}
}
