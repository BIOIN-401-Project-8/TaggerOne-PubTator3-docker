package ncbi.taggerOne.processing.evaluation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import ncbi.taggerOne.types.TextInstance;

public class BootstrapEvaluationAdapter extends EvaluationProcessor {

	private static final long serialVersionUID = 1L;

	private Random rnd;
	private EvaluationProcessor[] processors;
	private Map<String, Set<EvaluationProcessor>> documentToProcessors;

	public BootstrapEvaluationAdapter(EvaluationProcessor baseProcessor, int instances) {
		super(baseProcessor.scoreDetailPrefix);
		rnd = new Random();
		processors = new EvaluationProcessor[instances];
		for (int i = 0; i < instances; i++) {
			processors[i] = baseProcessor.copy();
		}
		documentToProcessors = new HashMap<String, Set<EvaluationProcessor>>();
	}

	@Override
	public void reset() {
		documentToProcessors.clear();
	}

	@Override
	public double score() {
		// Returns the mean score for all
		double sum = 0.0;
		for (int i = 0; i < processors.length; i++) {
			sum += processors[i].score();
		}
		return sum / processors.length;
	}

	@Override
	public String scoreDetail() {
		Object2DoubleMap<String> scores = getScores();
		StringBuilder detail = new StringBuilder();
		for (String s : scores.keySet()) {
			detail.append(String.format("BOOTSTRAP\t%s\t%s\t%.6f%n", scoreDetailPrefix, s, scores.get(s)));
		}
		return detail.toString();
	}

	public Object2DoubleMap<String> getScores() {

		Object2DoubleOpenHashMap<String> m = new Object2DoubleOpenHashMap<String>();
		m.defaultReturnValue(0.0);
		Object2DoubleOpenHashMap<String> v = new Object2DoubleOpenHashMap<String>();
		v.defaultReturnValue(0.0);

		Object2IntOpenHashMap<String> k = new Object2IntOpenHashMap<String>();
		k.defaultReturnValue(0);
		for (int i = 0; i < processors.length; i++) {
			Object2DoubleMap<String> scoresForProcessor = processors[i].getScores();
			for (String s : scoresForProcessor.keySet()) {
				double value = scoresForProcessor.getDouble(s);
				if (Double.isFinite(value)) {
					int count = k.getInt(s);
					double mean = m.getDouble(s);
					double M2 = v.getDouble(s);
					count = count + 1;
					double delta = value - mean;
					mean = mean + delta / count;
					double delta2 = value - mean;
					M2 = M2 + delta * delta2;
					k.put(s, count);
					m.put(s, mean);
					v.put(s, M2);
				}
			}
		}

		Object2DoubleMap<String> scores = new Object2DoubleOpenHashMap<String>();
		for (String s : k.keySet()) {
			int count = k.getInt(s);
			scores.put(s + "\tmean", m.get(s));
			scores.put(s + "\tcount", count);
			scores.put(s + "\tstdev", Math.sqrt(v.getDouble(s) / (count - 1)));
		}
		return scores;
	}

	@Override
	public EvaluationProcessor copy() {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public void process(TextInstance input) {
		String id = input.getSourceId();
		Set<EvaluationProcessor> evaluationProcessors = documentToProcessors.get(id);
		if (evaluationProcessors == null) {
			// Assign the document to approximately 1/2 of the processors
			evaluationProcessors = new HashSet<EvaluationProcessor>();
			int half = (int) Math.round(processors.length / 2.0);
			for (int i = 0; i < half; i++) {
				EvaluationProcessor temp = processors[i];
				int swap = rnd.nextInt(processors.length);
				processors[i] = processors[swap];
				processors[swap] = temp;
			}
			for (int i = 0; i < half; i++) {
				evaluationProcessors.add(processors[i]);
			}
		}
		for (EvaluationProcessor evaluationProcessor : evaluationProcessors) {
			evaluationProcessor.process(input);
		}
	}
}
