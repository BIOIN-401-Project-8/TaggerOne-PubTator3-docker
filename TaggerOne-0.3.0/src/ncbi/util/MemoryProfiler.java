package ncbi.util;

import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import ncbi.taggerOne.T1Constants;

public class MemoryProfiler {

	private static final Logger logger = LoggerFactory.getLogger(MemoryProfiler.class);

	private static Instrumentation instrumentation;

	private static Object2IntMap<String> counters;
	private static Object2LongMap<String> memory;
	private static final ProfilerComparator comp = new ProfilerComparator();

	static {
		memory = new Object2LongOpenHashMap<String>();
		memory.defaultReturnValue(0);
		counters = new Object2IntOpenHashMap<String>();
		counters.defaultReturnValue(0);
	}

	public static void premain(@SuppressWarnings("unused") String options, Instrumentation instrumentation) {
		MemoryProfiler.instrumentation = instrumentation;
	}

	public static void logMemoryUsed() {
		if (!logger.isDebugEnabled()) {
			return;
		}
		System.gc();
		System.gc();
		System.gc();
		logger.debug("Memory used = " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));

		MemoryUsage usage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
		logger.debug("Heap usage:");
		logger.debug("\tinit = " + usage.getInit());
		logger.debug("\tused = " + usage.getUsed());
		logger.debug("\tcommitted = " + usage.getCommitted());
		logger.debug("\tmax = " + usage.getMax());

		usage = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();
		logger.debug("Non-heap usage:");
		logger.debug("\tinit = " + usage.getInit());
		logger.debug("\tused = " + usage.getUsed());
		logger.debug("\tcommitted = " + usage.getCommitted());
		logger.debug("\tmax = " + usage.getMax());
	}

	public static void summarizeMemoryUsed(String context, Object obj) {
		if (!logger.isDebugEnabled()) {
			return;
		}
		memory.clear();
		counters.clear();
		Map<Object, Long> doneObj = new IdentityHashMap<Object, Long>();
		long total = summarizeMemoryUsed(obj, "", doneObj);

		logger.debug("MEMORY " + context + ": total memory = " + total);
		List<String> names = new ArrayList<String>(memory.keySet());
		Collections.sort(names, comp);
		for (String name : names) {
			int count = counters.getInt(name);
			long size = memory.getLong(name);
			long average = size / count;
			double proportion = ((double) size) / total;
			logger.debug("MEMORY " + context + ": " + name + " has " + count + " instances, total memory = " + size + "b, average = " + average + "b, proportion of total = " + String.format(T1Constants.SCORING_FORMAT, proportion));
		}
	}

	private static long summarizeMemoryUsed(Object obj, String parentName, Map<Object, Long> doneObj) {
		if (instrumentation == null) {
			throw new IllegalStateException("Instrumentation is null");
		}
		if (!logger.isDebugEnabled()) {
			return 0;
		}
		if (obj == null) {
			return 0;
		}
		if (doneObj.containsKey(obj)) {
			return 0;
		}
		doneObj.put(obj, null);
		long size = instrumentation.getObjectSize(obj);
		String className = obj.getClass().getName();
		if (className.equals("java.lang.String")) {
			className = parentName + "->" + className;
		} else if (className.startsWith("[")) {
			className = parentName + className;
		}
		int currentCount = counters.getInt(className);
		counters.put(className, currentCount + 1);
		long currentMemory = memory.getLong(className);
		memory.put(className, currentMemory + size);

		if (obj instanceof Object[]) {
			Object[] oArray = (Object[]) obj;
			for (int i = 0; i < oArray.length; i++) {
				size += summarizeMemoryUsed(oArray[i], obj.getClass().getName(), doneObj);
			}
		} else {
			Field[] fields = obj.getClass().getDeclaredFields();
			for (Field field : fields) {
				field.setAccessible(true);
				Object fieldObj;
				try {
					fieldObj = field.get(obj);
				} catch (IllegalArgumentException e) {
					throw new RuntimeException(e);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
				if (!isAPrimitiveType(field.getType())) {
					size += summarizeMemoryUsed(fieldObj, obj.getClass().getName(), doneObj);
				}
			}
		}
		return size;
	}

	private static class ProfilerComparator extends SimpleComparator<String> {

		private static final long serialVersionUID = 1L;

		public ProfilerComparator() {
			// Empty
		}

		@Override
		public int compare(String name1, String name2) {
			long elapsed1 = memory.getLong(name1);
			long elapsed2 = memory.getLong(name2);
			return Long.compare(elapsed2, elapsed1);
		}

	}

	/**
	 * Return true if the specified class is a primitive type
	 */
	private static <T> boolean isAPrimitiveType(Class<T> clazz) {
		if (clazz == java.lang.Boolean.TYPE)
			return true;

		if (clazz == java.lang.Character.TYPE)
			return true;

		if (clazz == java.lang.Byte.TYPE)
			return true;

		if (clazz == java.lang.Short.TYPE)
			return true;

		if (clazz == java.lang.Integer.TYPE)
			return true;

		if (clazz == java.lang.Long.TYPE)
			return true;

		if (clazz == java.lang.Float.TYPE)
			return true;

		if (clazz == java.lang.Double.TYPE)
			return true;

		if (clazz == java.lang.Void.TYPE)
			return true;

		return false;
	}
}
