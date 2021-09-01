package eu.wajja.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.tika.Tika;
import org.apache.tika.langdetect.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;
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
@LogstashPlugin(name = "htmlplugin")
public class HtmlPlugin implements Filter {

	private static final Logger LOGGER = LoggerFactory.getLogger(HtmlPlugin.class);

	/** Metadata fields **/
	protected static final String METADATA_TITLE = "TITLE";
	protected static final String METADATA_URL = "url";
	protected static final String METADATA_CONTEXT = "context";
	protected static final String METADATA_CONTENT = "content";
	protected static final String METADATA_LANGUAGES = "languages";
	protected static final String METADATA_TYPE = "type";
	protected static final String METADATA_REFERENCE = "reference";

	/** CSS Mapping where to get the content and title **/
	protected static final String PROPERTY_EXTRACT_TITLE_CSS = "extractTitleCss";
	protected static final String PROPERTY_EXTRACT_BODY_CSS = "extractBodyCss";
	protected static final String PROPERTY_EXCLUDE_BODY_CSS = "excludeBodyCss";

	/** Metadata manipulation **/
	protected static final String PROPERTY_METADATA_MAPPING = "metadataMapping";
	protected static final String PROPERTY_METADATA_CUSTOM = "metadataCustom";

	/** Remove part of the content **/
	protected static final String PROPERTY_REMOVE_CONTENT = "removeContent";

	private static final PluginConfigSpec<List<Object>> CONFIG_EXTRACT_TITLE_CSS = PluginConfigSpec
			.arraySetting(PROPERTY_EXTRACT_TITLE_CSS, new ArrayList<>(), false, false);
	private static final PluginConfigSpec<List<Object>> CONFIG_EXTRACT_BODY_CSS = PluginConfigSpec
			.arraySetting(PROPERTY_EXTRACT_BODY_CSS, new ArrayList<>(), false, false);
	private static final PluginConfigSpec<List<Object>> CONFIG_EXCLUDE_BODY_CSS = PluginConfigSpec
			.arraySetting(PROPERTY_EXCLUDE_BODY_CSS, new ArrayList<>(), false, false);
	private static final PluginConfigSpec<Map<String, Object>> CONFIG_METADATA_MAPPING = PluginConfigSpec
			.hashSetting(PROPERTY_METADATA_MAPPING, new HashMap<>(), false, false);
	private static final PluginConfigSpec<Map<String, Object>> CONFIG_METADATA_CUSTOM = PluginConfigSpec
			.hashSetting(PROPERTY_METADATA_CUSTOM, new HashMap<>(), false, false);
	private static final PluginConfigSpec<List<Object>> CONFIG_REMOVE_CONTENT = PluginConfigSpec
			.arraySetting(PROPERTY_REMOVE_CONTENT, new ArrayList<>(), false, false);

	private final Tika tika = new Tika();
	private LanguageDetector detector;
	private String threadId;

	private List<String> removeContent;
	private List<String> titleCss;
	private List<String> bodyCss;
	private List<String> excludeBodyCss;
	private Map<String, Object> metadataCustom;
	private Map<String, Object> metadataMapping;

	/**
	 * Mandatory constructor
	 * 
	 * @param id
	 * @param config
	 * @param context
	 */
	public HtmlPlugin(String id, Configuration config, Context context) {

		this.threadId = id;
		this.detector = new OptimaizeLangDetector().loadModels();

		this.titleCss = config.get(CONFIG_EXTRACT_TITLE_CSS).stream().map(Object::toString)
				.collect(Collectors.toList());
		this.bodyCss = config.get(CONFIG_EXTRACT_BODY_CSS).stream().map(Object::toString).collect(Collectors.toList());
		this.excludeBodyCss = config.get(CONFIG_EXCLUDE_BODY_CSS).stream().map(Object::toString)
				.collect(Collectors.toList());
		this.removeContent = config.get(CONFIG_REMOVE_CONTENT).stream().map(Object::toString)
				.collect(Collectors.toList());
		this.metadataCustom = config.get(CONFIG_METADATA_CUSTOM);
		this.metadataMapping = config.get(CONFIG_METADATA_MAPPING);

	}

	/**
	 * Returns a list of all configuration
	 */
	@Override
	public Collection<PluginConfigSpec<?>> configSchema() {
		return Arrays.asList(CONFIG_EXTRACT_TITLE_CSS, CONFIG_EXTRACT_BODY_CSS, CONFIG_EXCLUDE_BODY_CSS,
				CONFIG_REMOVE_CONTENT, CONFIG_METADATA_MAPPING, CONFIG_METADATA_CUSTOM);
	}

	@Override
	public String getId() {
		return this.threadId;
	}

	@SuppressWarnings("unchecked")
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

				/**
				 * Add the configured metadata
				 */

				metadataCustom.entrySet().stream().forEach(entry -> eventData.put(entry.getKey(), entry.getValue()));

				/**
				 * Only parse HTML here
				 */

				if (type.contains("html")) {

					try {

						/**
						 * Metadata Extraction
						 */

						Document document = Jsoup.parse(new String(bytes));
						Elements metadataElements = document.getElementsByTag("meta");

						/** Extract all metadata fields **/
						Map<String, Object> metadata = new HashMap<>();
						Map<String, Object> extractedMetadata = new HashMap<>();

						metadataElements.stream().filter(m -> m.hasAttr("name") && m.hasAttr(METADATA_CONTENT))
								.forEach(m -> extractedMetadata.put(m.attr("name"), getMetaValues(m)));

						metadataElements.stream().filter(m -> m.hasAttr("property") && m.hasAttr(METADATA_CONTENT))
								.forEach(m -> extractedMetadata.put(m.attr("property"), getMetaValues(m)));

						/** Add the custom metadata **/
						this.metadataCustom.entrySet().stream()
								.forEach(m -> metadata.put(m.getKey(), Arrays.asList((String) m.getValue())));

						/** Map the custom metadata **/
						this.metadataMapping.entrySet().stream().forEach(m -> {

							if (extractedMetadata.containsKey(m.getValue())) {
								metadata.put(m.getKey(), extractedMetadata.get(m.getValue()));
							}

						});

						/** Send metadata to event **/
						for (Entry<String, Object> entry : metadata.entrySet()) {

							Entry<String, Object> entryTmp = eventData.entrySet().stream()
									.filter(x -> x.getKey().equals(entry.getKey())).findFirst().orElse(null);

							if (entryTmp != null && entryTmp.getValue() instanceof List) {

								List<String> list = new ArrayList<>((List<String>) entryTmp.getValue());
								list.addAll((List) entry.getValue());
								eventData.put(entry.getKey(), list);

							} else {
								eventData.put(entry.getKey(), entry.getValue());
							}

						}

						/**
						 * Add HTML meta to the event
						 */

						extractedMetadata.entrySet().stream().filter(
								m -> eventData.entrySet().stream().noneMatch(e -> e.getKey().equals(m.getKey())))
								.forEach(m -> eventData.put(m.getKey(), Arrays.asList(m.getValue())));

						/**
						 * Title extraction
						 */

						if (!metadata.containsKey(METADATA_TITLE)) {
							String newTitle = extractTitle(document, eventData.get(METADATA_URL).toString());
							eventData.put(METADATA_TITLE, Arrays.asList(newTitle));
						}

						/**
						 * Content Extraction
						 */

						String newContent = extractContent(document);

						for (String removeText : removeContent) {
							newContent = newContent.replace(removeText, " ");
						}

						eventData.put(METADATA_CONTENT, Base64.getEncoder().encodeToString(newContent.getBytes()));

						/**
						 * Detects the language if language is not specified
						 */

						if (!eventData.containsKey(METADATA_LANGUAGES) && eventData.containsKey(METADATA_CONTENT)) {

							newContent = eventData.get(METADATA_CONTENT).toString();
							newContent = new String(Base64.getDecoder().decode(newContent));

							LanguageResult languageResult = detector.detect(newContent);
							if (languageResult.isReasonablyCertain()) {
								eventData.put(METADATA_LANGUAGES, Arrays.asList(languageResult.getLanguage()));
							}
						}

					} catch (Exception e) {
						LOGGER.error("Failed to extract HTML", e);
					}

				}

			}

		});

		return events;

	}

	private List<String> getMetaValues(Element m) {

		String data = m.attr(METADATA_CONTENT);

		if (data.contains(",")) {
			return Arrays.asList(data.split(",")).stream().map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
		}

		return Arrays.asList(data);
	}

	private String extractContent(Document document) {

		if (this.bodyCss != null && !this.bodyCss.isEmpty()) {

			StringBuilder stringBuilder = new StringBuilder();

			if (this.excludeBodyCss != null && !this.excludeBodyCss.isEmpty()) {
				for (String content : this.excludeBodyCss) {
					document.select(content).remove();
				}
			}

			for (String content : this.bodyCss) {

				Elements elements = document.select(content);

				if (!elements.isEmpty()) {
					elements.eachText().forEach(t -> stringBuilder.append(t).append(" "));
				}
			}

			if (stringBuilder.toString().isEmpty()) {
				return Jsoup.clean(document.text(), Whitelist.simpleText());
			}

			return Jsoup.clean(stringBuilder.toString(), Whitelist.simpleText());
		}

		return Jsoup.clean(document.toString(), Whitelist.simpleText());
	}

	private String extractTitle(Document document, String url) {

		if (this.titleCss != null && !this.titleCss.isEmpty()) {

			for (String content : this.titleCss) {

				Elements elements = document.select(content);

				if (!elements.isEmpty()) {
					
					String title = elements.eachText().stream()
						.map(t -> Jsoup.clean(t, Whitelist.simpleText()))
						.filter(Objects::nonNull)
						.filter(t -> (!t.isEmpty()))
						.findFirst().orElse(null);
					
					if (title != null && !title.isEmpty()) {
						return title;
					}
				}
			}
			
		}

		Elements titleElements = document.getElementsByTag("title");
		return titleElements.stream().findFirst().map(Element::text).orElse(url.substring(url.lastIndexOf('/') + 1));

	}

}
