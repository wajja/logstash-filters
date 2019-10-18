package eu.cec.digit.search.logstash.filter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.langdetect.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import org.jruby.RubyString;
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
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;

/**
 * Simple tool to fetch http content and send it to logstash
 * 
 * @author mahytom
 *
 */
@LogstashPlugin(name = "htmlfilter")
public class HtmlFilter implements Filter {

	private static final Logger LOGGER = LoggerFactory.getLogger(HtmlFilter.class);

	protected static final String METADATA_TITLE = "TITLE";
	protected static final String METADATA_URL = "url";
	protected static final String METADATA_CONTEXT = "context";
	protected static final String METADATA_CONTENT = "content";
	protected static final String METADATA_LANGUAGES = "languages";
	protected static final String METADATA_TYPE = "type";
	protected static final String METADATA_REFERENCE = "reference";
	protected static final String METADATA_ACL_USERS = "aclUsers";
	protected static final String METADATA_ACL_NO_USERS = "aclNoUsers";
	protected static final String METADATA_ACL_GROUPS = "aclGroups";
	protected static final String METADATA_ACL_NO_GROUPS = "aclNoGroups";

	protected static final String PROPERTY_METADATA = "metadata";
	protected static final String PROPERTY_OPEN_NLP_BIN = "nlpBin";
	protected static final String PROPERTY_EXPORT_CONTENT = "exportContent";
	protected static final String PROPERTY_EXTRACT_CONTENT = "extractContent";
	protected static final String PROPERTY_EXTRACT_TITLE_CSS = "extractTitleCss";
	protected static final String PROPERTY_EXTRACT_BODY_CSS = "extractBodyCss";
	protected static final String PROPERTY_DATA_FOLDER = "dataFolder";

	public static final PluginConfigSpec<String> CONFIG_DATA_FOLDER = PluginConfigSpec.stringSetting(PROPERTY_DATA_FOLDER);
	public static final PluginConfigSpec<List<Object>> CONFIG_METADATA = PluginConfigSpec.arraySetting(PROPERTY_METADATA);
	public static final PluginConfigSpec<String> CONFIG_OPEN_NLP = PluginConfigSpec.stringSetting(PROPERTY_OPEN_NLP_BIN);
	public static final PluginConfigSpec<Boolean> CONFIG_EXPORT_CONTENT = PluginConfigSpec.booleanSetting(PROPERTY_EXPORT_CONTENT, false);
	public static final PluginConfigSpec<Boolean> CONFIG_EXTRACT_CONTENT = PluginConfigSpec.booleanSetting(PROPERTY_EXTRACT_CONTENT, false);
	public static final PluginConfigSpec<List<Object>> CONFIG_EXTRACT_TITLE_CSS = PluginConfigSpec.arraySetting(PROPERTY_EXTRACT_TITLE_CSS, new ArrayList<>(), false, false);
	public static final PluginConfigSpec<List<Object>> CONFIG_EXTRACT_BODY_CSS = PluginConfigSpec.arraySetting(PROPERTY_EXTRACT_BODY_CSS, new ArrayList<>(), false, false);

	private final Tika tika = new Tika();
	private final CountDownLatch done = new CountDownLatch(1);
	private volatile boolean stopped;
	private NameFinderME finder;
	private LanguageDetector detector;
	private String threadId;

	private String dataFolder;
	private Map<String, List<String>> metadataMap = new HashMap<>();
	private Boolean extractContent;
	private Boolean exportContent;
	private List<String> titleCss;
	private List<String> bodyCss;

	/**
	 * Mandatory constructor
	 * 
	 * @param id
	 * @param config
	 * @param context
	 * @throws IOException
	 */
	public HtmlFilter(String id, Configuration config, Context context) throws IOException {

		this.threadId = id;

		if (config.contains(CONFIG_METADATA)) {

			config.get(CONFIG_METADATA).stream().forEach(c -> {

				String metadataString = (String) c;
				metadataMap.put(metadataString.split("=")[0], Arrays.asList(metadataString.substring(metadataString.split("=")[0].length() + 1)));
			});
		}

		if (config.get(CONFIG_OPEN_NLP) != null) {
			InputStream is = new FileInputStream(new File(config.get(CONFIG_OPEN_NLP)));
			finder = new NameFinderME(new TokenNameFinderModel(is));
		}

		this.extractContent = config.get(CONFIG_EXTRACT_CONTENT);
		this.exportContent = config.get(CONFIG_EXPORT_CONTENT);
		this.titleCss = config.get(CONFIG_EXTRACT_TITLE_CSS).stream().map(c -> c.toString()).collect(Collectors.toList());
		this.bodyCss = config.get(CONFIG_EXTRACT_BODY_CSS).stream().map(c -> c.toString()).collect(Collectors.toList());
		this.dataFolder = config.get(CONFIG_DATA_FOLDER);

		this.detector = new OptimaizeLangDetector().loadModels();
	}

	/**
	 * Returns a list of all configuration
	 */
	@Override
	public Collection<PluginConfigSpec<?>> configSchema() {
		return Arrays.asList(CONFIG_METADATA, CONFIG_OPEN_NLP, CONFIG_EXTRACT_CONTENT, CONFIG_DATA_FOLDER, CONFIG_EXPORT_CONTENT, CONFIG_EXTRACT_BODY_CSS, CONFIG_EXTRACT_TITLE_CSS);
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
				if (!eventData.containsKey(METADATA_TYPE)) {

					type = tika.detect(bytes);
					eventData.put(METADATA_TYPE, type);

				} else {
					type = ((RubyString) eventData.get(METADATA_TYPE)).toString();
				}

				// Only parse HTML here

				if (type.contains("html")) {

					try {

						/**
						 * Extracts the content if needed
						 */

						String newContent = null;

						if (extractContent) {

							String rawContent = null;
							String newTitle = null;

							Document document = Jsoup.parse(new String(bytes));

							newTitle = extractContent(this.titleCss, document, "blue");
							newContent = extractContent(this.bodyCss, document, "red");

							rawContent = document.toString();

							if (this.exportContent && this.dataFolder != null) {

								String context = ((RubyString) eventData.get(METADATA_CONTEXT)).toString();
								String exportFolder = new StringBuilder(this.dataFolder).append("/data-export/").append(Base64.getEncoder().encodeToString(context.getBytes())).append("/").toString();
								Path path = Paths.get(exportFolder);

								if (!path.toFile().exists()) {
									Files.createDirectories(path);
								}

								String reference = ((RubyString) eventData.get(METADATA_REFERENCE)).toString();
								String extension = (type.contains("html")) ? ".html" : ".txt";
								Path pathFile = Paths.get(new StringBuilder(exportFolder).append(reference).append(extension).toString());

								if (pathFile.toFile().exists()) {
									Files.delete(pathFile);
								}

								Files.write(pathFile, rawContent.getBytes(), StandardOpenOption.CREATE_NEW);

							}

							eventData.put(METADATA_TITLE, newTitle);
							eventData.put(METADATA_CONTENT, newContent);

						} else {

							String url = ((RubyString) eventData.get(METADATA_URL)).toString();
							eventData.put(METADATA_TITLE, url);
						}

						/**
						 * Detects the language if language is not specified
						 */
						if (!eventData.containsKey(METADATA_LANGUAGES)) {

							if (newContent == null) {
								newContent = tika.parseToString(new ByteArrayInputStream(bytes));
							}

							LanguageResult languageResult = detector.detect(newContent);
							if (languageResult.isReasonablyCertain()) {
								eventData.put(METADATA_LANGUAGES, Arrays.asList(languageResult.getLanguage()));
							}
						}

//					List<DetectedEntity> entities = getEntities(newContent);
//					entities.stream().forEach(e -> eventData.put(e.getEntity(), e.getValue()));

					} catch (IOException | TikaException e) {
						LOGGER.error("Failed to extract content from file", e);
					}

					if (!metadataMap.isEmpty()) {
						eventData.put(PROPERTY_METADATA, metadataMap);
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

				}

			}

		});

		return events;

	}

	private String extractContent(List<String> cssItems, Document document, String color) {

		if (cssItems != null && !cssItems.isEmpty()) {

			StringBuilder stringBuilder = new StringBuilder();

			for (String content : cssItems) {

				Elements elements = document.select(content);

				if (!elements.isEmpty()) {
					elements.attr("style", "border: 5px solid " + color + ";");
					elements.eachText().forEach(t -> stringBuilder.append(t).append(" "));
				}
			}

			return Jsoup.clean(stringBuilder.toString(), Whitelist.basic());
		}

		return Jsoup.clean(document.toString(), Whitelist.basic());
	}

//	public List<DetectedEntity> getEntities(String source) throws IOException {
//
//		List<DetectedEntity> detectedEntities = new ArrayList<>();
//
//		if (finder != null) {
//
//			String[] whitespaceTokenizerLine = WhitespaceTokenizer.INSTANCE.tokenize(source);
//			Span[] spans = finder.find(whitespaceTokenizerLine);
//			String[] strings = Span.spansToStrings(spans, whitespaceTokenizerLine);
//
//			for (int i = 0; i < spans.length; i++) {
//				detectedEntities.add(new DetectedEntity(spans[i].getType(), strings[i]));
//			}
//
//		}
//
//		return detectedEntities;
//
//	}

}
