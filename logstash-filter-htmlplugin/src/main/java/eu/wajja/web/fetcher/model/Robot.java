package eu.wajja.web.fetcher.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Robot {

	private Map<String, Set<String>> disallowedLocations = new HashMap<>();
	private Map<String, Set<String>> allowedLocations = new HashMap<>();
	private Map<String, Set<String>> sitemapLocations = new HashMap<>();

	public Map<String, Set<String>> getDisallowedLocations() {
		return disallowedLocations;
	}

	public void setDisallowedLocations(Map<String, Set<String>> disallowedLocations) {
		this.disallowedLocations = disallowedLocations;
	}

	public Map<String, Set<String>> getAllowedLocations() {
		return allowedLocations;
	}

	public void setAllowedLocations(Map<String, Set<String>> allowedLocations) {
		this.allowedLocations = allowedLocations;
	}

	public Map<String, Set<String>> getSitemapLocations() {
		return sitemapLocations;
	}

	public void setSitemapLocations(Map<String, Set<String>> sitemapLocations) {
		this.sitemapLocations = sitemapLocations;
	}

}
