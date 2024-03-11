package ncbi.taggerOne.processing.mentionName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ncbi.taggerOne.processing.string.StringProcessor;
import ncbi.taggerOne.types.MentionName;
import ncbi.util.Profiler;

public class StringProcessTokenApplicator extends MentionNameProcessor {

	private static final long serialVersionUID = 1L;

	private List<StringProcessor> processors;

	public StringProcessTokenApplicator(List<StringProcessor> processors) {
		this.processors = processors;
	}

	public StringProcessTokenApplicator(StringProcessor... processors) {
		this(Arrays.asList(processors));
	}

	@Override
	public void process(MentionName entityName) {
		Profiler.start("StringProcessTokenApplicator.process()");
		if (entityName.isLabel()) {
			Profiler.stop("StringProcessTokenApplicator.process()");
			return;
		}
		List<String> tokens = entityName.getTokens();
		ArrayList<String> updatedTokens = new ArrayList<String>(tokens);
		for (StringProcessor p : processors) {
			ArrayList<String> newTokens = new ArrayList<String>(updatedTokens.size());
			String mentionProcessorName = p.getClass().getName();
			Profiler.start("StringProcessTokenApplicator.process()@" + mentionProcessorName);
			for (int i = 0; i < updatedTokens.size(); i++) {
				String token = updatedTokens.get(i);
				token = p.process(token);
				if (token.length() > 0) {
					newTokens.add(token);
				}
			}
			Profiler.stop("StringProcessTokenApplicator.process()@" + mentionProcessorName);
			updatedTokens = newTokens;
		}
		updatedTokens.trimToSize();
		entityName.setTokens(updatedTokens);
		Profiler.stop("StringProcessTokenApplicator.process()");
	}
}
