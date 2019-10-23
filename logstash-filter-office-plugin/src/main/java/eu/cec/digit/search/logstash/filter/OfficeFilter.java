package eu.cec.digit.search.logstash.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;

import org.apache.tika.Tika;
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
@LogstashPlugin(name = "officefilter")
public class OfficeFilter implements Filter {

	private static final Logger LOGGER = LoggerFactory.getLogger(OfficeFilter.class);

	private final Tika tika = new Tika();

	private static final String PROPERTY_DATA_FOLDER = "dataFolder";
	private static final PluginConfigSpec<String> CONFIG_DATA_FOLDER = PluginConfigSpec.stringSetting(PROPERTY_DATA_FOLDER);

	private static final String METADATA_URL = "url";
	private static final String METADATA_CONTENT = "content";
	private static final String METADATA_TYPE = "type";
	private static final String METADATA_CONTENT_TYPE = "CONTENT-TYPE";
	private static final String METADATA_REFERENCE = "reference";

	private String threadId;
	private String dataFolder;

	/**
	 * Mandatory constructor
	 * 
	 * @param id
	 * @param config
	 * @param context
	 * @throws IOException
	 */
	public OfficeFilter(String id, Configuration config, Context context) {

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
		return Arrays.asList(CONFIG_DATA_FOLDER);
	}

	@Override
	public String getId() {
		return this.threadId;
	}

	@Override
	public Collection<Event> filter(Collection<Event> events, FilterMatchListener filterMatchListener) {

		events.stream().forEach(event -> {

			Map<String, Object> eventData = event.getData();

			if (eventData.containsKey(METADATA_URL) && eventData.containsKey(METADATA_CONTENT)) {

				String contentString = ((RubyString) eventData.get(METADATA_CONTENT)).toString();
				byte[] bytes = Base64.getDecoder().decode(contentString);

				/**
				 * Detects type if does not exist
				 */

				String type;

				if (eventData.containsKey(METADATA_TYPE)) {
					type = ((RubyString) eventData.get(METADATA_TYPE)).toString();

				} else {
					type = tika.detect(bytes);
					eventData.put(METADATA_TYPE, type);
				}

				// Only parse HTML here

				if (type.contains("office")) {

					String reference = ((RubyString) eventData.get(METADATA_REFERENCE)).toString();
					LOGGER.info("Found document with type {}, {}", type, reference);

				}

				eventData.put(METADATA_CONTENT_TYPE, type);
			}

		});

		return events;

	}

}
