package eu.wajja.filter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.tika.Tika;
import org.apache.tika.langdetect.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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

	private static final String METADATA_TITLE = "TITLE";
	private static final String METADATA_URL = "url";
	private static final String METADATA_CONTEXT = "context";
	private static final String METADATA_CONTENT = "content";
	private static final String METADATA_LANGUAGES = "languages";
	private static final String METADATA_TYPE = "type";
	private static final String METADATA_REFERENCE = "reference";

	private static final String PROPERTY_EXPORT_CONTENT = "exportContent";
	private static final String PROPERTY_EXTRACT_CONTENT = "extractContent";

	private static final String PROPERTY_EXTRACT_TITLE_CSS = "extractTitleCss";
	private static final String PROPERTY_EXTRACT_BODY_CSS = "extractBodyCss";

	private static final String PROPERTY_EXTRACT_TITLE_REGEX = "extractTitleRegex";
	private static final String PROPERTY_EXTRACT_CONTENT_REGEX = "extractContentRegex";
	private static final String PROPERTY_EXTRACT_METADATA_REGEX = "extractMetadataRegex";

	private static final String PROPERTY_DATA_FOLDER = "dataFolder";
	private static final String PROPERTY_DEFAULT_TITLE = "defaultTitle";
	private static final String PROPERTY_REMOVE_CONTENT = "removeContent";
	private static final String PROPERTY_METADATA = "metadata";

	public static final PluginConfigSpec<String> CONFIG_DATA_FOLDER = PluginConfigSpec.stringSetting(PROPERTY_DATA_FOLDER);
	public static final PluginConfigSpec<String> CONFIG_DEFAULT_TITLE = PluginConfigSpec.stringSetting(PROPERTY_DEFAULT_TITLE, "Home");
	public static final PluginConfigSpec<List<Object>> CONFIG_METADATA = PluginConfigSpec.arraySetting(PROPERTY_METADATA);
	public static final PluginConfigSpec<Boolean> CONFIG_EXPORT_CONTENT = PluginConfigSpec.booleanSetting(PROPERTY_EXPORT_CONTENT, false);
	public static final PluginConfigSpec<Boolean> CONFIG_EXTRACT_CONTENT = PluginConfigSpec.booleanSetting(PROPERTY_EXTRACT_CONTENT, false);
	public static final PluginConfigSpec<List<Object>> CONFIG_REMOVE_CONTENT = PluginConfigSpec.arraySetting(PROPERTY_REMOVE_CONTENT, new ArrayList<>(), false, false);

	public static final PluginConfigSpec<List<Object>> CONFIG_EXTRACT_TITLE_CSS = PluginConfigSpec.arraySetting(PROPERTY_EXTRACT_TITLE_CSS, new ArrayList<>(), false, false);
	public static final PluginConfigSpec<List<Object>> CONFIG_EXTRACT_BODY_CSS = PluginConfigSpec.arraySetting(PROPERTY_EXTRACT_BODY_CSS, new ArrayList<>(), false, false);

	public static final PluginConfigSpec<List<Object>> CONFIG_EXTRACT_TITLE_REGEX = PluginConfigSpec.arraySetting(PROPERTY_EXTRACT_TITLE_REGEX, new ArrayList<>(), false, false);
	public static final PluginConfigSpec<List<Object>> CONFIG_EXTRACT_CONTENT_REGEX = PluginConfigSpec.arraySetting(PROPERTY_EXTRACT_CONTENT_REGEX, new ArrayList<>(), false, false);
	public static final PluginConfigSpec<List<Object>> CONFIG_EXTRACT_METADATA_REGEX = PluginConfigSpec.arraySetting(PROPERTY_EXTRACT_METADATA_REGEX, new ArrayList<>(), false, false);

	private final Tika tika = new Tika();
	private LanguageDetector detector;
	private String threadId;

	private String dataFolder;
	private String defaultTitle;
	private Map<String, List<String>> metadataMap = new HashMap<>();
	private Boolean extractContent;
	private Boolean exportContent;
	private List<String> removeContent;

	private List<String> titleCss;
	private List<String> bodyCss;

	private List<String> titleRegex;
	private List<String> bodyRegex;
	private List<String> metadataRegex;

	/**
	 * Mandatory constructor
	 * 
	 * @param id
	 * @param config
	 * @param context
	 * @throws IOException
	 */
	public HtmlPlugin(String id, Configuration config, Context context) {

		if (context != null && LOGGER.isDebugEnabled()) {
			LOGGER.debug(context.toString());
		}

		this.threadId = id;

		if (config.contains(CONFIG_METADATA)) {

			config.get(CONFIG_METADATA).stream().forEach(c -> {

				String metadataString = (String) c;
				metadataMap.put(metadataString.split("=")[0], Arrays.asList(metadataString.substring(metadataString.split("=")[0].length() + 1)));
			});
		}

		this.extractContent = config.get(CONFIG_EXTRACT_CONTENT);
		this.exportContent = config.get(CONFIG_EXPORT_CONTENT);
		this.titleCss = config.get(CONFIG_EXTRACT_TITLE_CSS).stream().map(Object::toString).collect(Collectors.toList());
		this.bodyCss = config.get(CONFIG_EXTRACT_BODY_CSS).stream().map(Object::toString).collect(Collectors.toList());
		this.dataFolder = config.get(CONFIG_DATA_FOLDER);
		this.defaultTitle = config.get(CONFIG_DEFAULT_TITLE);
		this.detector = new OptimaizeLangDetector().loadModels();
		this.removeContent = config.get(CONFIG_REMOVE_CONTENT).stream().map(Object::toString).collect(Collectors.toList());

		this.titleRegex = config.get(CONFIG_EXTRACT_TITLE_REGEX).stream().map(Object::toString).collect(Collectors.toList());
		this.bodyRegex = config.get(CONFIG_EXTRACT_CONTENT_REGEX).stream().map(Object::toString).collect(Collectors.toList());
		this.metadataRegex = config.get(CONFIG_EXTRACT_METADATA_REGEX).stream().map(Object::toString).collect(Collectors.toList());

	}

	/**
	 * Returns a list of all configuration
	 */
	@Override
	public Collection<PluginConfigSpec<?>> configSchema() {
		return Arrays.asList(CONFIG_METADATA,
				CONFIG_EXTRACT_CONTENT, CONFIG_DATA_FOLDER, CONFIG_EXPORT_CONTENT,
				CONFIG_EXTRACT_BODY_CSS, CONFIG_EXTRACT_TITLE_CSS, CONFIG_DEFAULT_TITLE, CONFIG_REMOVE_CONTENT,
				CONFIG_EXTRACT_TITLE_REGEX, CONFIG_EXTRACT_CONTENT_REGEX, CONFIG_EXTRACT_METADATA_REGEX);
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

				/**
				 * Add the configured metadata
				 */

				metadataMap.entrySet().stream().forEach(entry -> eventData.put(entry.getKey(), entry.getValue()));

				/**
				 * Only parse HTML here
				 */

				if (type.contains("html")) {

					try {

						/**
						 * Extracts the content if needed
						 */

						if (extractContent) {

							Document document = Jsoup.parse(new String(bytes));

							/**
							 * Title extraction
							 */
							String newTitle = extractTitle(document, "blue");
							eventData.put(METADATA_TITLE, newTitle);

							/**
							 * Content Extraction
							 */
							String newContent = extractContent(document);

							for (String removeText : removeContent) {
								newContent = newContent.replace(removeText, " ");
							}

							eventData.put(METADATA_CONTENT, Base64.getEncoder().encodeToString(newContent.getBytes()));

							/**
							 * Metadata Extraction
							 */

							Map<String, List<String>> metadata = extractMetadata(document);

							for (Entry<String, List<String>> entry : metadata.entrySet()) {

								Entry<String, Object> entryTmp = eventData.entrySet().stream().filter(x -> x.getKey().equals(entry.getKey())).findFirst().orElse(null);

								if (entryTmp != null && entryTmp.getValue() instanceof List) {

									@SuppressWarnings("unchecked")
									List<String> list = new ArrayList<>((List<String>) entryTmp.getValue());
									list.addAll(entry.getValue());
									eventData.put(entry.getKey(), list);

								} else {
									eventData.put(entry.getKey(), entry.getValue());
								}

							}

							/**
							 * Export and save to disk for debugging
							 */

							if (this.exportContent && this.dataFolder != null) {

								String rawContent = document.toString();

								String context = eventData.get(METADATA_CONTEXT).toString();
								String exportFolder = new StringBuilder(this.dataFolder).append("/data-export/").append(Base64.getEncoder().encodeToString(context.getBytes())).append("/").toString();
								Path path = Paths.get(exportFolder);

								if (!path.toFile().exists()) {
									Files.createDirectories(path);
								}

								String reference = eventData.get(METADATA_REFERENCE).toString();
								String extension = (type.contains("html")) ? ".html" : ".txt";
								Path pathFile = Paths.get(new StringBuilder(exportFolder).append(reference).append(extension).toString());

								if (pathFile.toFile().exists()) {
									Files.delete(pathFile);
								}

								Files.write(pathFile, rawContent.getBytes(), StandardOpenOption.CREATE_NEW);

							}

						} else {

							String url = eventData.get(METADATA_URL).toString();
							eventData.put(METADATA_TITLE, url);
						}

						/**
						 * Detects the language if language is not specified
						 */

						if (!eventData.containsKey(METADATA_LANGUAGES) && eventData.containsKey(METADATA_CONTENT)) {

							String newContent = eventData.get(METADATA_CONTENT).toString();
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

	private Map<String, List<String>> extractMetadata(Document document) {

		Map<String, List<String>> map = new HashMap<>();

		if (this.metadataRegex != null && !this.metadataRegex.isEmpty()) {

			String content = document.toString();

			for (String regex : this.metadataRegex) {

				String propertyName = regex.substring(0, regex.indexOf(':'));
				String realRegex = regex.substring(regex.indexOf(':') + 1);

				Pattern p = Pattern.compile(realRegex);
				Matcher m = p.matcher(content);

				while (m.find()) {

					if (map.containsKey(propertyName)) {

						List<String> list = new ArrayList<>(map.get(propertyName));
						list.add(m.group(m.groupCount()));
						map.put(propertyName, list);

					} else {
						map.put(propertyName, Arrays.asList(m.group(m.groupCount())));
					}

				}

			}

			return map;
		}

		return map;
	}

	private String extractContent(Document document) {

		if (this.bodyRegex != null && !this.bodyRegex.isEmpty()) {

			String content = document.toString();
			String newContent = getContentFromRegex(content, this.bodyRegex);

			if (newContent != null && !newContent.isEmpty()) {
				return newContent;
			}
		}

		if (this.bodyCss != null && !this.bodyCss.isEmpty()) {

			StringBuilder stringBuilder = new StringBuilder();

			for (String content : this.bodyCss) {

				Elements elements = document.select(content);

				if (!elements.isEmpty()) {
					elements.attr("style", "border: 5px solid red;");
					elements.eachText().forEach(t -> stringBuilder.append(t).append(" "));
				}
			}

			return Jsoup.clean(stringBuilder.toString(), Whitelist.basic());
		}

		return Jsoup.clean(document.toString(), Whitelist.basic());
	}

	private String extractTitle(Document document, String url) {

		if (this.titleRegex != null && !this.titleRegex.isEmpty()) {

			String content = document.toString();
			String title = getContentFromRegex(content, this.titleRegex);

			if (title != null && !title.isEmpty()) {
				return title;
			}

		}

		if (this.titleCss != null && !this.titleCss.isEmpty()) {

			StringBuilder stringBuilder = new StringBuilder();

			for (String content : this.titleCss) {

				Elements elements = document.select(content);

				if (!elements.isEmpty()) {
					elements.attr("style", "border: 5px solid blue;");
					elements.eachText().forEach(t -> stringBuilder.append(t).append(" "));
				}
			}

			String title = Jsoup.clean(stringBuilder.toString(), Whitelist.basic());

			if (title != null && !title.isEmpty()) {
				return title;
			}
		}

		return url.substring(url.lastIndexOf('/') + 1);

	}

	private String getContentFromRegex(String content, List<String> regexes) {

		StringBuilder stringBuilder = new StringBuilder();

		for (String regex : regexes) {

			Pattern p = Pattern.compile(regex);
			Matcher m = p.matcher(content);

			while (m.find()) {
				stringBuilder.append(m.group(m.groupCount())).append(" ");
			}

			if (!stringBuilder.toString().isEmpty()) {
				break;
			}

		}

		return stringBuilder.toString().trim();
	}

}
