package ncbi.taggerOne.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ncbi.taggerOne.types.MentionName;
import ncbi.util.Profiler;

public class AbbreviationResolver implements Serializable {

	private static final Logger logger = LoggerFactory.getLogger(AbbreviationResolver.class);

	private static final long serialVersionUID = 1L;

	private Map<String, Map<String, String>> abbreviations;

	public AbbreviationResolver() {
		abbreviations = new HashMap<String, Map<String, String>>();
	}

	public void addAbbreviations(String id, Map<String, String> abbreviation) {
		Map<String, String> abbreviationMap = abbreviations.get(id);
		if (abbreviationMap == null) {
			abbreviationMap = new HashMap<String, String>();
			abbreviations.put(id, abbreviationMap);
		}
		// Iterate through mappings and add
		Iterator<String> shortFormIterator = abbreviation.keySet().iterator();
		while (shortFormIterator.hasNext()) {
			String shortForm = shortFormIterator.next();
			Pattern shortFormPattern = Pattern.compile("\\b" + Pattern.quote(shortForm) + "\\b");
			String longForm = abbreviation.get(shortForm);
			Matcher longFormMatcher = shortFormPattern.matcher(longForm);
			if (longFormMatcher.find()) {
				logger.warn("Ignoring abbreviation \"" + shortForm + "\" -> \"" + longForm + "\" because long form contains short form");
			} else {
				abbreviationMap.put(shortForm, longForm);
			}
		}
	}

	public void clear() {
		abbreviations.clear();
	}

	public int size() {
		return abbreviations.size();
	}

	public String expandAbbreviations(String documentId, String lookupText) {
		Profiler.start("AbbreviationResolver.expandAbbreviations()");
		Map<String, String> abbreviationMap = abbreviations.get(documentId);
		if (abbreviationMap == null) {
			Profiler.stop("AbbreviationResolver.expandAbbreviations()");
			return lookupText;
		}
		// Make a copy so we can mark short forms as used
		logger.trace("Abbreviation resolver found " + abbreviationMap.size() + " abbreviations for " + documentId);
		abbreviationMap = new HashMap<String, String>(abbreviationMap);
		Set<String> history = new HashSet<String>();
		String result = lookupText;
		while (!history.contains(result)) {
			history.add(result);
			Iterator<String> shortFormIterator = abbreviationMap.keySet().iterator();
			while (shortFormIterator.hasNext()) {
				String shortForm = shortFormIterator.next();
				if (result.contains(shortForm)) {
					String longForm = abbreviationMap.get(shortForm);
					String updated = null;
					if (result.contains(longForm)) {
						// TODO PERFORMANCE Convert these to use Pattern
						updated = result.replaceAll("\\s*\\(\\s*" + Pattern.quote(shortForm) + "\\s*\\)\\s*$", "");
						updated = updated.replaceAll("\\s*\\(\\s*" + Pattern.quote(shortForm) + "\\s*\\)\\s*", " ");
					} else {
						// TODO PERFORMANCE Convert these to use Pattern
						updated = result.replaceAll("\\b" + Pattern.quote(shortForm) + "\\b", Matcher.quoteReplacement(longForm));
					}
					if (!updated.equals(result)) {
						logger.trace("Resolving abbreviation \"" + shortForm + "\"->\"" + longForm + "\" to convert \"" + result + "\" to \"" + updated + "\" in document ID " + documentId);
						result = updated;
						// Remove this short form so it cannot be used again
						shortFormIterator.remove();
					}
				}
			}
		}
		Profiler.stop("AbbreviationResolver.expandAbbreviations()");
		return result;
	}

	// TODO Move this functionality to someplace related to a MentionNameProcessor
	public void expand(String documentId, MentionName mentionName) {
		if (mentionName.isLabel()) {
			return;
		}
		Profiler.start("AbbreviationResolver.expand()");
		String originalText = mentionName.getName();
		String modifiedText = expandAbbreviations(documentId, originalText);
		if (!modifiedText.equals(originalText)) {
			mentionName.setName(modifiedText);
		}
		Profiler.stop("AbbreviationResolver.expand()");
	}
}
