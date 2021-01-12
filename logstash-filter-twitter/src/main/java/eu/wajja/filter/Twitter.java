package eu.wajja.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;

import org.jruby.RubyString;
import org.logstash.ext.JrubyTimestampExtLibrary.RubyTimestamp;
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
@LogstashPlugin(name = "twitter")
public class Twitter implements Filter {

	private static final Logger LOGGER = LoggerFactory.getLogger(Twitter.class);

	private static final String METADATA_DATE = "DATE";
	private static final String METADATA_CONTENT_TYPE = "CONTENT-TYPE";
	private static final String METADATA_URL = "url";
	private static final String METADATA_CONTENT = "content";
	private static final String METADATA_TYPE = "type";
	private static final String METADATA_REFERENCE = "reference";

	private String threadId;

	/**
	 * Mandatory constructor
	 * 
	 * @param id
	 * @param config
	 * @param context
	 * @throws IOException
	 */
	public Twitter(String id, Configuration config, Context context) {

		if (context != null && LOGGER.isDebugEnabled()) {
			LOGGER.debug(context.toString());
		}

		this.threadId = id;
	}

	/**
	 * Returns a list of all configuration
	 */
	@Override
	public Collection<PluginConfigSpec<?>> configSchema() {
		return new ArrayList<>();
	}

	@Override
	public String getId() {

		return this.threadId;
	}

	@Override
	public Collection<Event> filter(Collection<Event> events, FilterMatchListener filterMatchListener) {

		events.stream().forEach(event -> {

			Map<String, Object> eventData = event.getData();

			String content = ((RubyString) eventData.get("message")).toString();
			String url = ((RubyString) eventData.get("source")).toString();
			String date = ((RubyTimestamp) eventData.get("@timestamp")).toString();

			eventData.put(METADATA_CONTENT, Base64.getEncoder().encodeToString(content.getBytes()));
			eventData.put(METADATA_CONTENT_TYPE, "application/twitter");
			eventData.put(METADATA_TYPE, "application/twitter");
			eventData.put(METADATA_URL, url);
			eventData.put(METADATA_REFERENCE, Base64.getEncoder().encodeToString(url.getBytes()));
			eventData.put(METADATA_DATE, date);
		});

		return events;

	}
}
