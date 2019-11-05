package eu.wajja.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tika.Tika;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.langdetect.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
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
@LogstashPlugin(name = "europaplugin")
public class EuropaPlugin implements Filter {

	private static final Logger LOGGER = LoggerFactory.getLogger(EuropaPlugin.class);

	private final Tika tika = new Tika();

	private static final String PROPERTY_DATA_FOLDER = "dataFolder";
	private static final String PROPERTY_METADATA = "metadata";
	private static final PluginConfigSpec<String> CONFIG_DATA_FOLDER = PluginConfigSpec.stringSetting(PROPERTY_DATA_FOLDER);
	public static final PluginConfigSpec<List<Object>> CONFIG_METADATA = PluginConfigSpec.arraySetting(PROPERTY_METADATA);

	private static final String METADATA_SIMPLIFIED_CONTENT_TYPE = "SIMPLIFIED_CONTENT_TYPE	";

	private static final String METADATA_CONTENT = "content";

	private String threadId;
	private String dataFolder;
	private LanguageDetector detector;
	private Map<String, List<String>> metadataMap = new HashMap<>();

	/**
	 * Mandatory constructor
	 * 
	 * @param id
	 * @param config
	 * @param context
	 * @throws IOException
	 */
	public EuropaPlugin(String id, Configuration config, Context context) {

		if (context != null && LOGGER.isDebugEnabled()) {
			LOGGER.debug(context.toString());
		}

		this.threadId = id;
		this.detector = new OptimaizeLangDetector().loadModels();

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
		return Arrays.asList(CONFIG_DATA_FOLDER, CONFIG_METADATA);
	}

	@Override
	public String getId() {
		return this.threadId;
	}

	@Override
	public Collection<Event> filter(Collection<Event> events, FilterMatchListener filterMatchListener) {

		events.stream().forEach(event -> {

			Map<String, Object> eventData = event.getData();

			if (eventData.containsKey(METADATA_CONTENT)) {

				try {
					String contentString = eventData.get(METADATA_CONTENT).toString();
					byte[] bytes = Base64.getDecoder().decode(contentString);

					/**
					 * Detects simplified content type
					 */

					MediaType mediaType = tika.getDetector().detect(TikaInputStream.get(bytes), new Metadata());
					String baseType = mediaType.getBaseType().toString();
					eventData.put(METADATA_SIMPLIFIED_CONTENT_TYPE, baseType);

				} catch (IOException e) {
					LOGGER.error("Failed to detect content type", e);
				}

			}

		});

		return events;

	}
}
