package eu.wajja.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
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
@LogstashPlugin(name = "confluence")
public class Confluence implements Filter {

	private static final Logger LOGGER = LoggerFactory.getLogger(Confluence.class);

	private final Tika tika = new Tika();

	private static final String PROPERTY_METADATA_CUSTOM = "metadataCustom";
	private static final String PROPERTY_SIMPLIFIED_CONTENT_TYPE = "simplifiedContentType";

	private String threadId;
	private LanguageDetector detector;
	private Map<String, Object> metadataCustom;
	private Map<String, Object> simplifiedContentType;

	private static final PluginConfigSpec<Map<String, Object>> CONFIG_METADATA_CUSTOM = PluginConfigSpec.hashSetting(PROPERTY_METADATA_CUSTOM, new HashMap<String, Object>(), false, false);
	private static final PluginConfigSpec<Map<String, Object>> CONFIG_SIMPLIFIED_CONTENT_TYPE = PluginConfigSpec.hashSetting(PROPERTY_SIMPLIFIED_CONTENT_TYPE, new HashMap<String, Object>(), false, false);

	/** metadata fields arriving from the confluence fetcher **/

	private static final String METADATA_TITLE = "title";
	private static final String METADATA_PAGE_NAME = "pageName";
	private static final String METADATA_FILE_NAME = "fileName";
	private static final String METADATA_REFERENCE = "reference";
	private static final String METADATA_CONTENT = "content";
	private static final String METADATA_URL = "url";
	private static final String METADATA_STATUS = "status";
	private static final String METADATA_COMMAND = "command";
	private static final String METADATA_CONTENT_TYPE = "contentType";
	private static final String METADATA_FETCHED_DATE = "fetchedDate";
	private static final String METADATA_MODIFIED_DATE = "modifiedDate";
	private static final String METADATA_MODIFIED_USER = "modifiedBy";
	private static final String METADATA_ACL_USERS = "aclUsers";
	private static final String METADATA_ACL_GROUPS = "aclGroups";
	private static final String METADATA_SPACE_ID = "spaceId";
	private static final String METADATA_SPACE_NAME = "spaceName";
	private static final String METADATA_SPACE_URL = "spaceUrl";
	private static final String METADATA_PARENT_ID = "parentId";
	private static final String METADATA_PARENT_NAME = "parentName";
	private static final String METADATA_PARENT_URL = "parentUrl";

	/** metadata fields that need to be generated **/

	private static final String METADATA_MAP_AUTHOR = "AUTHOR";
	private static final String METADATA_MAP_CONFLUENCESPACE = "CONFLUENCESPACE";
	private static final String METADATA_MAP_DATAORIGIN = "DATAORIGIN";
	private static final String METADATA_MAP_DATE = "DATE";
	private static final String METADATA_MAP_DOCUMENT_TYPE = "DOCUMENT_TYPE";
	private static final String METADATA_MAP_PAGENAME = "PAGENAME";
	private static final String METADATA_MAP_PAGEURI = "PAGEURI";
	private static final String METADATA_MAP_SIMPLIFIED_CONTENT_TYPE = "SIMPLIFIED_CONTENT_TYPE";
	private static final String METADATA_MAP_SPACEURI = "SPACEURI";
	private static final String METADATA_MAP_SPACEHOME = "SPACEHOME";
	private static final String METADATA_MAP_TITLE = "TITLE";
	private static final String METADATA_MAP_CONTENT_TYPE = "CONTENT-TYPE";

	/**
	 * Mandatory constructor
	 * 
	 * @param id
	 * @param config
	 * @param context
	 * @throws IOException
	 */
	public Confluence(String id, Configuration config, Context context) {

		if (context != null && LOGGER.isDebugEnabled()) {
			LOGGER.debug(context.toString());
		}

		this.threadId = id;
		this.detector = new OptimaizeLangDetector().loadModels();
		this.metadataCustom = config.get(CONFIG_METADATA_CUSTOM);
		this.simplifiedContentType = config.get(CONFIG_SIMPLIFIED_CONTENT_TYPE);
	}

	/**
	 * Returns a list of all configuration
	 */
	@Override
	public Collection<PluginConfigSpec<?>> configSchema() {
		return Arrays.asList(CONFIG_METADATA_CUSTOM, CONFIG_SIMPLIFIED_CONTENT_TYPE);
	}

	@Override
	public String getId() {
		return this.threadId;
	}

	@Override
	public Collection<Event> filter(Collection<Event> events, FilterMatchListener filterMatchListener) {

		events.stream().forEach(event -> {

			Map<String, Object> eventData = event.getData();

			if (eventData.containsKey(METADATA_URL) && eventData.containsKey(METADATA_CONTENT) && eventData.get(METADATA_CONTENT_TYPE).toString().startsWith("confluence")) {

				String url = eventData.get(METADATA_URL).toString();
				LOGGER.info("Parsing Confluence filter METADATA_URL -> {}", url);

				eventData.put(METADATA_MAP_AUTHOR, eventData.get(METADATA_MODIFIED_USER).toString());
				eventData.put(METADATA_MAP_CONFLUENCESPACE, eventData.get(METADATA_SPACE_NAME).toString());
				eventData.put(METADATA_MAP_DATAORIGIN, eventData.get(METADATA_SPACE_ID).toString());
				eventData.put(METADATA_MAP_DATE, eventData.get(METADATA_MODIFIED_DATE).toString());

				if (eventData.containsKey(METADATA_PARENT_NAME)) {
					eventData.put(METADATA_MAP_PAGENAME, eventData.get(METADATA_PARENT_NAME).toString());
				}

				if (eventData.containsKey(METADATA_PARENT_URL)) {
					eventData.put(METADATA_MAP_PAGEURI, eventData.get(METADATA_PARENT_URL).toString());
				}

				eventData.put(METADATA_MAP_SPACEURI, eventData.get(METADATA_SPACE_URL).toString());
				eventData.put(METADATA_MAP_SPACEHOME, eventData.get(METADATA_SPACE_NAME).toString());
				eventData.put(METADATA_MAP_TITLE, eventData.get(METADATA_TITLE).toString());
				eventData.put(METADATA_MAP_DOCUMENT_TYPE, eventData.get(METADATA_CONTENT_TYPE).toString());

				/**
				 * Detects content type
				 */

				String contentString = eventData.get(METADATA_CONTENT).toString();
				byte[] bytes = Base64.getDecoder().decode(contentString);
				String type = tika.detect(bytes);
				eventData.put(METADATA_MAP_CONTENT_TYPE, type);

				/**
				 * Add the configured metadata
				 */

				metadataCustom.entrySet().stream().forEach(entry -> eventData.put(entry.getKey(), entry.getValue()));

				/**
				 * Simplified Content Type
				 */

				try {

					MediaType mediaType = tika.getDetector().detect(TikaInputStream.get(bytes), new Metadata());
					String contentType = mediaType.getSubtype();

					if (contentType != null && this.simplifiedContentType.containsKey(contentType)) {
						eventData.put(METADATA_MAP_SIMPLIFIED_CONTENT_TYPE, simplifiedContentType.get(contentType));
					} else {
						LOGGER.info("Could not map simplified content type from url : {}, detected simple : {}", url, contentType);
					}

				} catch (IOException e) {
					LOGGER.error("Failed to detect content type", e);
				}

			}

		});

		return events;

	}
}
