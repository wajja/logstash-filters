package eu.cec.digit.search.logstash.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.jruby.RubyString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.elastic.logstash.api.Configuration;
import co.elastic.logstash.api.Context;
import co.elastic.logstash.api.Event;
import co.elastic.logstash.api.Filter;
import co.elastic.logstash.api.FilterMatchListener;
import co.elastic.logstash.api.LogstashPlugin;
import co.elastic.logstash.api.PluginConfigSpec;

/**
 * Simple tool to fetch http content and send it to logstash
 * 
 * @author mahytom
 *
 */
@LogstashPlugin(name = "europafilter")
public class EuropaFilter implements Filter {

	private static final Logger LOGGER = LoggerFactory.getLogger(EuropaFilter.class);

	protected static final String METADATA_TITLE = "TITLE";
	protected static final String METADATA_URL = "url";
	protected static final String METADATA_CONTENT = "content";
	protected static final String METADATA_LANGUAGES = "languages";
	protected static final String METADATA_REFERENCE = "reference";
	protected static final String METADATA = "metadata";
	protected static final String METADATA_ACL_USERS = "aclUsers";
	protected static final String METADATA_ACL_NO_USERS = "aclNoUsers";
	protected static final String METADATA_ACL_GROUPS = "aclGroups";
	protected static final String METADATA_ACL_NO_GROUPS = "aclNoGroups";

	public static final PluginConfigSpec<List<Object>> CONFIG_METADATA = PluginConfigSpec.arraySetting(METADATA);

	private final CountDownLatch done = new CountDownLatch(1);
	private volatile boolean stopped;

	private String threadId;
	private Map<String, List<String>> metadataMap = new HashMap<>();

	/**
	 * Mandatory constructor
	 * 
	 * @param id
	 * @param config
	 * @param context
	 */
	public EuropaFilter(String id, Configuration config, Context context) {

		this.threadId = id;

		if (config.contains(CONFIG_METADATA)) {

			config.get(CONFIG_METADATA).stream().forEach(c -> {

				String metadataString = (String) c;
				metadataMap.put(metadataString.split("=")[0], Arrays.asList(metadataString.substring(metadataString.split("=")[0].length() + 1)));
			});

		}

	}

	/**
	 * Returns a list of all configuration
	 */
	@Override
	public Collection<PluginConfigSpec<?>> configSchema() {
		return Arrays.asList(CONFIG_METADATA);
	}

	@Override
	public String getId() {
		return this.threadId;
	}

	@Override
	public Collection<Event> filter(Collection<Event> events, FilterMatchListener filterMatchListener) {

		events.stream().forEach(event -> {

			Map<String, Object> eventData = event.getData();

			if (eventData.containsKey(METADATA_URL)) {

				String urlString = ((RubyString) eventData.get(METADATA_URL)).toString();
				eventData.put(METADATA_TITLE, urlString.substring(urlString.lastIndexOf('/') + 1));

				if (!eventData.containsKey(METADATA_LANGUAGES)) {
					eventData.put(METADATA_LANGUAGES, Arrays.asList("en"));
				}

				if (!eventData.containsKey(METADATA_ACL_USERS)) {
					eventData.put(METADATA_ACL_USERS, new ArrayList<>());
				}

				if (!eventData.containsKey(METADATA_ACL_NO_USERS)) {
					eventData.put(METADATA_ACL_NO_USERS, new ArrayList<>());
				}

				if (!eventData.containsKey(METADATA_ACL_GROUPS)) {
					eventData.put(METADATA_ACL_GROUPS, new ArrayList<>());
				}

				if (!eventData.containsKey(METADATA_ACL_NO_GROUPS)) {
					eventData.put(METADATA_ACL_NO_GROUPS, new ArrayList<>());
				}

				if (!eventData.containsKey(METADATA_LANGUAGES)) {
					eventData.put(METADATA_LANGUAGES, new ArrayList<>());
				}

				if (!metadataMap.isEmpty()) {
					eventData.put(METADATA, metadataMap);
				}

			}

		});

		return events;
	}

}
