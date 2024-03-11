package ncbi.util;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Profiler {

	private static final Logger logger = LoggerFactory.getLogger(Profiler.class);

	// TODO CONCURRENCY PERFORMANCE Use a read/write lock
	protected static Object2LongMap<String> elapsedTimes; // protected for performance
	private static Object2LongMap<String> counters;
	private static ProfilerComparator comp;

	static {
		elapsedTimes = new Object2LongOpenHashMap<String>();
		elapsedTimes.defaultReturnValue(0);
		counters = new Object2LongOpenHashMap<String>();
		counters.defaultReturnValue(0);
		comp = new ProfilerComparator();
	}

	private Profiler() {
		// Not instantiable
	}

	// TODO Add functionality for tracking the number of calls with low elapsed time (especially 0)

	// TODO Add functionality for only tracking the number of calls

	public synchronized static void start(String name) {
		if (!logger.isDebugEnabled()) {
			return;
		}
		long currentCount = counters.getLong(name);
		counters.put(name, currentCount + 1);
		long currentElapsed = elapsedTimes.getLong(name);
		elapsedTimes.put(name, currentElapsed - System.currentTimeMillis());
	}

	public synchronized static void stop(String name) {
		if (!logger.isDebugEnabled()) {
			return;
		}
		long currentElapsed = elapsedTimes.getLong(name);
		elapsedTimes.put(name, currentElapsed + System.currentTimeMillis());
	}

	public synchronized static void print(String prefix) {
		if (!logger.isDebugEnabled()) {
			return;
		}
		List<String> names = new ArrayList<String>(elapsedTimes.keySet());
		Collections.sort(names, comp);
		for (String name : names) {
			long count = counters.getLong(name);
			long elapsed = elapsedTimes.getLong(name);
			double average = ((double) elapsed) / count;
			logger.debug("PERFORMANCE " + prefix + name + " called " + count + " times, elapsed time = " + elapsed + "ms, average time = " + average + "ms");
		}
	}

	private static class ProfilerComparator extends SimpleComparator<String> {

		private static final long serialVersionUID = 1L;

		public ProfilerComparator() {
			// Empty
		}

		@Override
		public int compare(String name1, String name2) {
			long elapsed1 = elapsedTimes.getLong(name1);
			long elapsed2 = elapsedTimes.getLong(name2);
			return Long.compare(elapsed2, elapsed1);
		}

	}
}
