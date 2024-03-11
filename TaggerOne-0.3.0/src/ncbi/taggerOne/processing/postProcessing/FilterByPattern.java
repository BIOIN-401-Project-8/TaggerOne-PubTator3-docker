package ncbi.taggerOne.processing.postProcessing;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ncbi.taggerOne.processing.textInstance.TextInstanceProcessor;
import ncbi.taggerOne.types.AnnotatedSegment;
import ncbi.taggerOne.types.TextInstance;
import ncbi.taggerOne.util.RankedList;

public class FilterByPattern extends TextInstanceProcessor {

	private static final Logger logger = LoggerFactory.getLogger(FilterByPattern.class);
	private static final long serialVersionUID = 1L;

	private Pattern[] patternsToFilter;

	public FilterByPattern(String... patternsToFilter) {
		this.patternsToFilter = new Pattern[patternsToFilter.length];
		for (int i = 0; i < patternsToFilter.length; i++) {
			this.patternsToFilter[i] = Pattern.compile(patternsToFilter[i]);
		}
	}

	@Override
	public void process(TextInstance input) {
		RankedList<List<AnnotatedSegment>> predictedAnnotationRankedList = input.getPredictedAnnotations();
		int size = predictedAnnotationRankedList.size();
		RankedList<List<AnnotatedSegment>> filteredAnnotationRankedList = new RankedList<List<AnnotatedSegment>>(size);
		for (int i = 0; i < size; i++) {
			List<AnnotatedSegment> predictedAnnotation = predictedAnnotationRankedList.getObject(i);
			List<AnnotatedSegment> filteredAnnotation = new ArrayList<AnnotatedSegment>();
			for (AnnotatedSegment segment : predictedAnnotation) {
				boolean filter = matches(segment.getText());
				if (filter) {
					logger.info("FILTER PATTERN MATCH: " + segment.getText());
				} else {
					filteredAnnotation.add(segment);
				}
			}
			filteredAnnotationRankedList.add(predictedAnnotationRankedList.getValue(i), filteredAnnotation);
		}
		input.setPredictedAnnotations(filteredAnnotationRankedList);
	}

	private boolean matches(String segmentText) {
		for (int i = 0; i < patternsToFilter.length; i++) {
			Matcher m = patternsToFilter[i].matcher(segmentText);
			if (m.matches()) {
				return true;
			}
		}
		return false;
	}
}