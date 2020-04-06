package eu.wajja.filter;

import java.io.IOException;
import java.net.Proxy;
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
import java.util.stream.Collectors;

import org.apache.tika.Tika;
import org.apache.tika.langdetect.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
import org.jruby.RubyObject;
import org.jruby.javasupport.JavaUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;
import org.logstash.common.DeadLetterQueueFactory;
import org.logstash.common.io.DeadLetterQueueWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import co.elastic.logstash.api.Configuration;
import co.elastic.logstash.api.Context;
import co.elastic.logstash.api.Event;
import co.elastic.logstash.api.Filter;
import co.elastic.logstash.api.FilterMatchListener;
import co.elastic.logstash.api.LogstashPlugin;
import co.elastic.logstash.api.PluginConfigSpec;
import eu.wajja.web.fetcher.controller.ProxyController;
import eu.wajja.web.fetcher.controller.URLController;
import eu.wajja.web.fetcher.controller.WebDriverController;
import eu.wajja.web.fetcher.model.Result;
import eu.wajja.web.fetcher.model.Robot;

/**
 * Simple tool to fetch http content and send it to logstash
 * 
 * @author mahytom
 *
 */
@LogstashPlugin(name = "htmlplugin")
public class HtmlPlugin implements Filter {

	private static final Logger LOGGER = LoggerFactory.getLogger(HtmlPlugin.class);

	/** LOGGER **/
	private static final String LOGGER_REFERENCE = "reference";
	private static final String LOGGER_STATUS = "status";
	private static final String LOGGER_PAGES = "pages";
	private static final String LOGGER_DEPTH = "depth";
	private static final String LOGGER_URL = "url";
	private static final String LOGGER_MESSAGE = "message";
	private static final String LOGGER_ROOT_URL = "rootUrl";
	private static final String LOGGER_SIZE = "size";
	private static final String LOGGER_ACTION = "action";

	/** PROPERTIES **/
	private static final String PROPERTY_URLS = "urls";
	private static final String PROPERTY_EXCLUDE_DATA = "excludeData";
	private static final String PROPERTY_EXCLUDE_LINK = "excludeLink";
	private static final String PROPERTY_TIMEOUT = "timeout";
	private static final String PROPERTY_MAX_DEPTH = "maxdepth";
	private static final String PROPERTY_MAX_PAGES = "maxpages";
	private static final String PROPERTY_SSL_CHECK = "sslcheck";
	private static final String PROPERTY_PROXY_HOST = "proxyHost";
	private static final String PROPERTY_PROXY_PORT = "proxyPort";
	private static final String PROPERTY_PROXY_USER = "proxyUser";
	private static final String PROPERTY_PROXY_PASS = "proxyPass";
	private static final String PROPERTY_CHROME_DRIVER = "chromeDriver";
	private static final String PROPERTY_CRAWLER_USER_AGENT = "crawlerUserAgent";
	private static final String PROPERTY_CRAWLER_REFERER = "crawlerReferer";
	private static final String PROPERTY_READ_ROBOT = "readRobot";
	private static final String PROPERTY_ROOT_URL = "rootUrl";
	private static final String PROPERTY_DEPTH = "depth";
	private static final String PROPERTY_PAGES_COUNT = "pagesCount";
	private static final String PROPERTY_START_TIME = "startTime";

	/** Metadata fields **/
	private static final String METADATA_TITLE = "TITLE";
	private static final String METADATA_URL = "url";
	private static final String METADATA_CONTEXT = "context";
	private static final String METADATA_CONTENT = "content";
	private static final String METADATA_LANGUAGES = "languages";
	private static final String METADATA_TYPE = "type";
	private static final String METADATA_REFERENCE = "reference";
	private static final String METADATA_COMMAND = "command";

	/** CSS Mapping where to get the content and title **/
	private static final String PROPERTY_EXTRACT_TITLE_CSS = "extractTitleCss";
	private static final String PROPERTY_EXTRACT_BODY_CSS = "extractBodyCss";

	/** Metadata manipulation **/
	private static final String PROPERTY_METADATA_MAPPING = "metadataMapping";
	private static final String PROPERTY_METADATA_CUSTOM = "metadataCustom";

	/** Remove part of the content **/
	private static final String PROPERTY_REMOVE_CONTENT = "removeContent";

	/** Pipeline configuration **/

	private static final String PROPERTY_PIPELINE_ID = "pipeline_id";
	private static final String PROPERTY_DEAD_LETTER_QUEUE_PATH = "path";

	List<PluginConfigSpec<?>> propertiesList = Arrays.asList(
			PluginConfigSpec.arraySetting(PROPERTY_EXTRACT_TITLE_CSS, new ArrayList<>(), false, false),
			PluginConfigSpec.arraySetting(PROPERTY_EXTRACT_BODY_CSS, new ArrayList<>(), false, false),
			PluginConfigSpec.hashSetting(PROPERTY_METADATA_MAPPING, new HashMap<String, Object>(), false, false),
			PluginConfigSpec.hashSetting(PROPERTY_METADATA_CUSTOM, new HashMap<String, Object>(), false, false),
			PluginConfigSpec.arraySetting(PROPERTY_REMOVE_CONTENT, new ArrayList<>(), false, false),
			PluginConfigSpec.hashSetting(PROPERTY_METADATA_CUSTOM, new HashMap<String, Object>(), false, false),
			PluginConfigSpec.stringSetting(PROPERTY_PIPELINE_ID),
			PluginConfigSpec.stringSetting(PROPERTY_DEAD_LETTER_QUEUE_PATH),
			PluginConfigSpec.arraySetting(PROPERTY_REMOVE_CONTENT, new ArrayList<>(), false, false));

	private final Tika tika = new Tika();
	private LanguageDetector detector;
	private String threadId;

	private List<String> removeContent;
	private List<String> titleCss;
	private List<String> bodyCss;
	private Map<String, Object> metadataCustom;
	private Map<String, Object> metadataMapping;

	private ObjectMapper objectMapper = new ObjectMapper();
	private HtmlFetcherJob htmlFetcherJob = new HtmlFetcherJob();

	private DeadLetterQueueWriter deadLetterQueueWriter;
	private String pipelineId;
	private String deadLetterQueuePath;

	private URLController urlController;
	private ProxyController proxyController;
	private Robot robot;
	private Path pathListUrls;

	/**
	 * Mandatory constructor
	 * 
	 * @param id
	 * @param config
	 * @param context
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public HtmlPlugin(String id, Configuration config, Context context)  {
		
		this.threadId = id;
		this.detector = new OptimaizeLangDetector().loadModels();

		this.titleCss = (List<String>) config.get(propertiesList.stream().filter(x -> x.name().equals(PROPERTY_EXTRACT_TITLE_CSS)).findFirst().orElse(null));
		this.bodyCss = (List<String>) config.get(propertiesList.stream().filter(x -> x.name().equals(PROPERTY_EXTRACT_BODY_CSS)).findFirst().orElse(null));
		this.removeContent = (List<String>) config.get(propertiesList.stream().filter(x -> x.name().equals(PROPERTY_REMOVE_CONTENT)).findFirst().orElse(null));
		this.metadataCustom = (Map<String, Object>) config.get(propertiesList.stream().filter(x -> x.name().equals(PROPERTY_METADATA_CUSTOM)).findFirst().orElse(null));
		this.metadataMapping = (Map<String, Object>) config.get(propertiesList.stream().filter(x -> x.name().equals(PROPERTY_METADATA_MAPPING)).findFirst().orElse(null));
		this.pipelineId = (String) config.get(propertiesList.stream().filter(x -> x.name().equals(PROPERTY_PIPELINE_ID)).findFirst().orElse(null));
		this.deadLetterQueuePath = (String) config.get(propertiesList.stream().filter(x -> x.name().equals(PROPERTY_DEAD_LETTER_QUEUE_PATH)).findFirst().orElse(null));

		this.deadLetterQueueWriter = DeadLetterQueueFactory.getWriter(pipelineId, deadLetterQueuePath, 100000);
	}

	/**
	 * Returns a list of all configuration
	 */
	@Override
	public Collection<PluginConfigSpec<?>> configSchema() {
		return propertiesList;
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

			if (LOGGER.isDebugEnabled()) {

				LOGGER.debug("Reading event ----------------------------");

				eventData.entrySet().stream().forEach(e -> {
					LOGGER.debug("Reading event : {}:{}", e.getKey(), e.getValue());
				});

				LOGGER.debug("Finished event ----------------------------");
			}

			if (eventData.containsKey(PROPERTY_URLS)) {

				/** Tracking File Creation **/

				try {

					String date = eventData.get(PROPERTY_START_TIME).toString();

					this.pathListUrls = Paths.get(deadLetterQueuePath + "/../" + pipelineId + "_" + date + ".txt");

					if (!this.pathListUrls.toFile().exists()) {
						Files.createFile(this.pathListUrls);
					}

				} catch (IOException e) {
					LOGGER.error("Failed to create file", e);
				}

				/**
				 * Initialize all variables and dependencies
				 */

				List<String> allUrls = (List<String>) getRubyObject(eventData.get(HtmlPlugin.PROPERTY_URLS));

				if (proxyController == null) {

					proxyController = new ProxyController((String) getRubyObject(eventData.get(HtmlPlugin.PROPERTY_PROXY_USER)),
							(String) getRubyObject(eventData.get(HtmlPlugin.PROPERTY_PROXY_PASS)),
							(String) getRubyObject(eventData.get(HtmlPlugin.PROPERTY_PROXY_HOST)),
							(Long) getRubyObject(eventData.get(HtmlPlugin.PROPERTY_PROXY_PORT)),
							(Boolean) getRubyObject(eventData.get(HtmlPlugin.PROPERTY_SSL_CHECK)));
				}

				if (urlController == null) {

					urlController = new URLController(
							(Proxy) getRubyObject(proxyController.getProxy()),
							(Long) getRubyObject(eventData.get(HtmlPlugin.PROPERTY_TIMEOUT)),
							(String) getRubyObject(eventData.get(HtmlPlugin.PROPERTY_CRAWLER_USER_AGENT)),
							(String) getRubyObject(eventData.get(HtmlPlugin.PROPERTY_CRAWLER_REFERER)),
							(String) getRubyObject(eventData.get(HtmlPlugin.PROPERTY_CHROME_DRIVER)));

				}

				/**
				 * Read Page Robot // NOTE : Done once per source
				 */

				Boolean readRobot = (Boolean) getRubyObject(eventData.get(HtmlPlugin.PROPERTY_READ_ROBOT));
				allUrls.stream().filter(x -> robot == null).forEach(u -> robot = HtmlRobotJob.readRobot(urlController, readRobot, u));

				/**
				 * Read the pages Content
				 */

				List<String> excludedDataRegex = (List<String>) getRubyObject(eventData.get(HtmlPlugin.PROPERTY_EXCLUDE_DATA));
				List<String> excludedLinkRegex = (List<String>) getRubyObject(eventData.get(HtmlPlugin.PROPERTY_EXCLUDE_LINK));
				String crawlerUserAgent = (String) getRubyObject(eventData.get(HtmlPlugin.PROPERTY_CRAWLER_USER_AGENT));

				Long depth = (getRubyObject(eventData.get(HtmlPlugin.PROPERTY_DEPTH)) != null) ? (Long) getRubyObject(eventData.get(HtmlPlugin.PROPERTY_DEPTH)) : 0l;
				Long maxPagesCount = (getRubyObject(eventData.get(HtmlPlugin.PROPERTY_PAGES_COUNT)) != null) ? (Long) getRubyObject(eventData.get(HtmlPlugin.PROPERTY_PAGES_COUNT)) : 0l;

				List<String> urlList = getAllUrls();

				allUrls.stream().filter(url -> !urlList.contains(url)).forEach(currentUrl -> {

					writeToFile(currentUrl);
					urlList.add(currentUrl);

					if ((getRubyObject(eventData.get(HtmlPlugin.PROPERTY_ROOT_URL)) == null)) {
						eventData.put(HtmlPlugin.PROPERTY_ROOT_URL, currentUrl);
					}

					LOGGER.info("Fetching url : {}", currentUrl);

					String rootUrl = (String) getRubyObject(eventData.get(HtmlPlugin.PROPERTY_ROOT_URL));

					if (rootUrl.endsWith("/")) {
						rootUrl = rootUrl.substring(0, rootUrl.length() - 1);
					}

					Result result = htmlFetcherJob.fetchContent(urlController, robot, currentUrl, rootUrl, depth, crawlerUserAgent);

					Map<String, Object> loggerMap = new HashMap<>();

					loggerMap.put(LOGGER_REFERENCE, result.getReference());
					loggerMap.put(LOGGER_STATUS, result.getCode());
					loggerMap.put(LOGGER_PAGES, maxPagesCount);
					loggerMap.put(LOGGER_DEPTH, result.getDepth());
					loggerMap.put(LOGGER_MESSAGE, result.getMessage());
					loggerMap.put(LOGGER_ROOT_URL, result.getRootUrl());

					if (result.getChildPages() != null && !result.getChildPages().isEmpty()) {

						List<String> urlsToFetch = result.getChildPages().stream().filter(url -> !urlList.contains(url)).filter(url -> excludedLinkRegex.stream().noneMatch(ex -> url.matches(ex))).collect(Collectors.toList());
						List<String> urlsToSkip = result.getChildPages().stream().filter(url -> urlList.contains(url)).filter(url -> excludedLinkRegex.stream().anyMatch(ex -> url.matches(ex))).collect(Collectors.toList());

						if (result.getContent() != null) {
							loggerMap.put(LOGGER_SIZE, result.getContent().length());
						}

						/** Logging Links that are skipped **/

						urlsToSkip.stream().forEach(url -> {

							try {
								loggerMap.put(LOGGER_URL, url);
								loggerMap.put(LOGGER_ACTION, "exclude_link");
								LOGGER.info(objectMapper.writeValueAsString(loggerMap));

							} catch (IOException e1) {
								LOGGER.error("Failed to log excluded urls", e1);
							}
						});

						/** Send child pages to dead letter queue and Logging **/

						urlsToFetch.stream().forEach(url -> {

							loggerMap.put(LOGGER_URL, url);
							loggerMap.put(LOGGER_ACTION, "include_link");

							try {

								LOGGER.info(objectMapper.writeValueAsString(loggerMap));

								org.logstash.Event newEvent = new org.logstash.Event();
								eventData.entrySet().stream().forEach(ee -> newEvent.setField(ee.getKey(), ee.getValue()));
								newEvent.setField(PROPERTY_URLS, Arrays.asList(url));

								if (LOGGER.isDebugEnabled()) {
									LOGGER.debug("Adding child url : {}", Arrays.asList(url));
								}

								deadLetterQueueWriter.writeEntry(newEvent, this.getName(), this.getId(), "Child Page Found");

							} catch (IOException e1) {
								LOGGER.error("Failed to send items to dead letter queue", e1);
							}
						});

						DeadLetterQueueFactory.release(pipelineId);

					}

					/** Checking if url should be ingested **/

					try {

						if (excludedDataRegex.stream().noneMatch(ex -> result.getUrl().matches(ex)) && result.getUrl().startsWith(result.getRootUrl()) && result.getContent() != null) {

							loggerMap.put(LOGGER_URL, result.getUrl());
							loggerMap.put(LOGGER_ACTION, "include_data");
							LOGGER.info(objectMapper.writeValueAsString(loggerMap));

							parseContent(eventData, result);

						} else {

							loggerMap.put(LOGGER_URL, result.getUrl());
							loggerMap.put(LOGGER_ACTION, "exclude_data");
							LOGGER.info(objectMapper.writeValueAsString(loggerMap));

							eventData.remove(METADATA_URL);
							eventData.remove(METADATA_CONTENT);
						}

					} catch (JsonProcessingException e) {
						LOGGER.info("Failed to determine if url {} should send data", result.getUrl(), e);
					}

				});

			}

		});

		return events;

	}

	private void parseContent(Map<String, Object> eventData, Result result) {

		/** Adding basic events data **/

		eventData.put(METADATA_URL, result.getUrl());
		eventData.put(METADATA_CONTENT, result.getContent());
		eventData.put(METADATA_REFERENCE, result.getReference());
		eventData.put(METADATA_COMMAND, result.getCommand());

		/** Detects type if does not exist **/

		byte[] bytes = Base64.getDecoder().decode(result.getContent().getBytes());

		String type;

		if (eventData.containsKey(METADATA_TYPE)) {
			type = eventData.get(METADATA_TYPE).toString();

		} else {
			type = tika.detect(bytes);
			eventData.put(METADATA_TYPE, type);
		}

		/** Add the configured metadata **/

		metadataCustom.entrySet().stream().forEach(entry -> eventData.put(entry.getKey(), entry.getValue()));

		/**
		 * Only parse HTML here
		 */

		if (type.contains("html")) {

			try {

				/** Metadata Extraction **/

				Document document = Jsoup.parse(new String(bytes));
				Elements metadataElements = document.getElementsByTag("meta");

				/** Extract all metadata fields **/

				Map<String, Object> metadata = new HashMap<>();
				Map<String, Object> extractedMetadata = new HashMap<>();

				metadataElements.stream()
						.filter(m -> m.hasAttr("name") && m.hasAttr(METADATA_CONTENT))
						.forEach(m -> extractedMetadata.put(m.attr("name"), Arrays.asList(m.attr(METADATA_CONTENT))));

				metadataElements.stream()
						.filter(m -> m.hasAttr("property") && m.hasAttr(METADATA_CONTENT))
						.forEach(m -> extractedMetadata.put(m.attr("property"), Arrays.asList(m.attr(METADATA_CONTENT))));

				/** Add the custom metadata **/

				this.metadataCustom.entrySet().stream().forEach(m -> metadata.put(m.getKey(), Arrays.asList((String) m.getValue())));

				/** Map the custom metadata **/

				this.metadataMapping.entrySet().stream().forEach(m -> {

					if (extractedMetadata.containsKey(m.getValue())) {
						metadata.put(m.getKey(), extractedMetadata.get(m.getValue()));
					}

				});

				/** Send metadata to event **/

				for (Entry<String, Object> entry : metadata.entrySet()) {

					Entry<String, Object> entryTmp = eventData.entrySet().stream().filter(x -> x.getKey().equals(entry.getKey())).findFirst().orElse(null);

					if (entryTmp != null && entryTmp.getValue() instanceof List) {

						List<String> list = new ArrayList<>((List<String>) entryTmp.getValue());
						list.addAll((List) entry.getValue());
						eventData.put(entry.getKey(), list);

					} else {
						eventData.put(entry.getKey(), entry.getValue());
					}
				}

				/** Add HTML meta to the event **/

				extractedMetadata.entrySet().stream()
						.filter(m -> eventData.entrySet().stream().noneMatch(e -> e.getKey().equals(m.getKey())))
						.forEach(m -> eventData.put(m.getKey(), Arrays.asList(m.getValue())));

				/** Title extraction **/

				if (!metadata.containsKey(METADATA_TITLE)) {
					String newTitle = extractTitle(document, eventData.get(METADATA_URL).toString());
					eventData.put(METADATA_TITLE, Arrays.asList(newTitle));
				}

				/** Content Extraction **/

				String newContent = extractContent(document);

				for (String removeText : removeContent) {
					newContent = newContent.replace(removeText, " ");
				}

				eventData.put(METADATA_CONTENT, Base64.getEncoder().encodeToString(newContent.getBytes()));

				/** Detects the language if language is not specified **/

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

	private void writeToFile(String currentUrl) {

		String line = currentUrl + "\n";

		try {
			Files.write(pathListUrls, line.getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			LOGGER.error("Failed to write url list file", e);
		}
	}

	private List<String> getAllUrls() {

		try {
			return Files.readAllLines(pathListUrls);
		} catch (IOException e) {
			LOGGER.error("Failed to find url list file", e);
		}

		return new ArrayList<>();
	}

	private String extractContent(Document document) {

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

		Elements titleElements = document.getElementsByTag("title");
		return titleElements.stream().findFirst().map(Element::text).orElse(url.substring(url.lastIndexOf('/') + 1));

	}

	private Object getRubyObject(Object object) {

		if (object instanceof List) {

			List ll = new ArrayList<>();
			((List) object).stream().forEach(l -> ll.add(getRubyObject(l)));
			return ll;

		} else if (object instanceof RubyObject) {
			return JavaUtil.convertRubyToJava((RubyObject) object);
		}

		return object;

	}

}
