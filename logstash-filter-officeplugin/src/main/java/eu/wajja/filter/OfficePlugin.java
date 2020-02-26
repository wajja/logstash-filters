package eu.wajja.filter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.tika.Tika;
import org.apache.tika.langdetect.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
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
@LogstashPlugin(name = "officeplugin")
public class OfficePlugin implements Filter {

	private static final Logger LOGGER = LoggerFactory.getLogger(OfficePlugin.class);

	private final Tika tika = new Tika();

	protected static final String PROPERTY_METADATA = "metadata";
	protected static final String PROPERTY_METADATA_CUSTOM = "metadataCustom";

	private static final PluginConfigSpec<Map<String, Object>> CONFIG_METADATA_CUSTOM = PluginConfigSpec.hashSetting(PROPERTY_METADATA_CUSTOM, new HashMap<String, Object>(), false, false);

	private static final String METADATA_TITLE = "TITLE";
	private static final String METADATA_DATE = "DATE";
	private static final String METADATA_CONTENT_TYPE = "CONTENT-TYPE";
	private static final String METADATA_URL = "url";
	private static final String METADATA_CONTENT = "content";
	private static final String METADATA_TYPE = "type";
	private static final String METADATA_REFERENCE = "reference";
	private static final String METADATA_LANGUAGES = "languages";
	private static final String METADATA_CONTENT_DISPOSITION = "Content-Disposition";

	private String threadId;
	private LanguageDetector detector;
	private Map<String, Object> metadataCustom;

	/**
	 * Mandatory constructor
	 * 
	 * @param id
	 * @param config
	 * @param context
	 * @throws IOException
	 */
	public OfficePlugin(String id, Configuration config, Context context) {

		if (context != null && LOGGER.isDebugEnabled()) {
			LOGGER.debug(context.toString());
		}

		this.threadId = id;
		this.detector = new OptimaizeLangDetector().loadModels();
		this.metadataCustom = config.get(CONFIG_METADATA_CUSTOM);
	}

	/**
	 * Returns a list of all configuration
	 */
	@Override
	public Collection<PluginConfigSpec<?>> configSchema() {
		return Arrays.asList(CONFIG_METADATA_CUSTOM);
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

				String contentString = eventData.get(METADATA_CONTENT).toString();
				byte[] bytes = Base64.getDecoder().decode(contentString);

				/**
				 * Detects type if does not exist
				 */

				String type;

				if (eventData.containsKey(METADATA_TYPE)) {
					type = eventData.get(METADATA_TYPE).toString();

				} else {
					type = tika.detect(bytes);
					eventData.put(METADATA_TYPE, type);
				}

				// Only parse Office Docs here

				if (type.contains("office") || type.contains("msword")) {

					String reference = ((RubyString) eventData.get(METADATA_REFERENCE)).toString();
					LOGGER.info("Found document with type {}, {}", type, reference);

					try {

						BodyContentHandler handler = new BodyContentHandler(-1);
						Metadata metadata = new Metadata();
						ParseContext pcontext = new ParseContext();

						Parser parser = new AutoDetectParser();
						parser.parse(new ByteArrayInputStream(bytes), handler, metadata, pcontext);
						String content = handler.toString();

						/**
						 * Detects Title of Document
						 */

						String title = Arrays.asList(metadata.names()).stream().filter(name -> name.contains("title")).map(name -> metadata.get(name).trim()).filter(value -> !value.isEmpty()).findFirst().orElse(null);

						if (title != null && !title.isEmpty()) {
							eventData.put(METADATA_TITLE, title);
						} else {

							if (eventData.containsKey(METADATA_CONTENT_DISPOSITION) && eventData.get(METADATA_CONTENT_DISPOSITION).toString().contains("filename")) {

								String filename = eventData.get(METADATA_CONTENT_DISPOSITION).toString();
								filename = filename.substring(filename.indexOf("filename")).replace("filename", "").trim();

								if (filename.startsWith("=")) {
									filename = filename.substring(1);
								}

								if (filename.contains(".")) {
									filename = filename.substring(0, filename.lastIndexOf('.'));
								}

								filename = filename.replaceAll("[^A-Za-z0-9]", " ").trim();

								eventData.put(METADATA_TITLE, filename);

							} else {

								String url = eventData.get(METADATA_URL).toString();
								url = url.substring(url.lastIndexOf('/') + 1);

								if (url.contains(".")) {
									url = url.substring(0, url.lastIndexOf('.'));
								}

								eventData.put(METADATA_TITLE, url);

							}

						}

						/**
						 * Detects Modified Date
						 */

						String date = Arrays.asList(metadata.names()).stream().filter(name -> name.contains("modified")).map(name -> metadata.get(name).trim()).filter(value -> !value.isEmpty()).findFirst().orElse(null);

						if (date != null) {
							eventData.put(METADATA_DATE, date);
						}

						/**
						 * Detects the language if language is not specified
						 */
						if (!eventData.containsKey(METADATA_LANGUAGES)) {

							LanguageResult languageResult = detector.detect(content);
							if (languageResult.isReasonablyCertain()) {
								eventData.put(METADATA_LANGUAGES, Arrays.asList(languageResult.getLanguage()));
							}
						}

						/**
						 * Add Metadata Field
						 */

						if (!metadataCustom.isEmpty()) {
							eventData.put(PROPERTY_METADATA, metadataCustom);
						}

					} catch (Exception e) {
						LOGGER.error("Failed to extract Office Document", e);
					}

				}

				eventData.put(METADATA_CONTENT_TYPE, type);
			}

		});

		return events;

	}

}
