package ncbi.taggerOne.processing.textInstance;

import java.util.EnumSet;
import java.util.Set;

import ncbi.taggerOne.types.TextInstance;
import ncbi.util.Profiler;

public class InstanceElementClearer extends TextInstanceProcessor {

	private static final long serialVersionUID = 1L;

	private Set<InstanceElement> elementsToClear;

	public InstanceElementClearer() {
		this(EnumSet.allOf(InstanceElement.class));
	}

	public InstanceElementClearer(Set<InstanceElement> elementsToClear) {
		this.elementsToClear = elementsToClear;
	}

	@Override
	public void process(TextInstance input) {
		Profiler.start("InstanceElementClearer.process()");
		for (InstanceElement element : elementsToClear) {
			element.clear(input);
		}
		Profiler.stop("InstanceElementClearer.process()");
	}

	public enum InstanceElement {
		Tokens {
			@Override
			public void clear(TextInstance input) {
				input.setTokens(null);
			}
		},
		Segments {
			@Override
			public void clear(TextInstance input) {
				input.setSegments(null);
			}
		},
		TargetAnnotation {
			@Override
			public void clear(TextInstance input) {
				input.setTargetAnnotation(null);
			}
		},
		TargetStateSequence {
			@Override
			public void clear(TextInstance input) {
				input.setTargetStateSequence(null);
			}
		},
		PredictedStates {
			@Override
			public void clear(TextInstance input) {
				input.setPredictedStates(null);
			}
		},
		PredictedAnnotations {
			@Override
			public void clear(TextInstance input) {
				input.setPredictedAnnotations(null);
			}
		};

		public abstract void clear(TextInstance input);
	}

}
