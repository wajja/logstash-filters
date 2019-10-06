package eu.cec.digit.search.logstash.filter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.langdetect.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;
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
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.tokenize.WhitespaceTokenizer;
import opennlp.tools.util.Span;

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
	protected static final String METADATA_TYPE = "type";
	protected static final String METADATA_REFERENCE = "reference";
	protected static final String METADATA = "metadata";
	protected static final String METADATA_ACL_USERS = "aclUsers";
	protected static final String METADATA_ACL_NO_USERS = "aclNoUsers";
	protected static final String METADATA_ACL_GROUPS = "aclGroups";
	protected static final String METADATA_ACL_NO_GROUPS = "aclNoGroups";
	protected static final String OPEN_NLP_BIN = "nlpBin";
	protected static final String EXTRACT_CONTENT = "extractContent";

	public static final PluginConfigSpec<List<Object>> CONFIG_METADATA = PluginConfigSpec.arraySetting(METADATA);
	public static final PluginConfigSpec<String> CONFIG_OPEN_NLP = PluginConfigSpec.stringSetting(OPEN_NLP_BIN);
	public static final PluginConfigSpec<Boolean> CONFIG_EXTRACT_CONTENT = PluginConfigSpec.booleanSetting(EXTRACT_CONTENT, false);

	private final Tika tika = new Tika();
	private final CountDownLatch done = new CountDownLatch(1);
	private volatile boolean stopped;
	private Boolean extractContent;
	private NameFinderME finder;
	private LanguageDetector detector;

	private String threadId;
	private Map<String, List<String>> metadataMap = new HashMap<>();

	/**
	 * Mandatory constructor
	 * 
	 * @param id
	 * @param config
	 * @param context
	 * @throws IOException
	 */
	public EuropaFilter(String id, Configuration config, Context context) throws IOException {

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
		this.detector = new OptimaizeLangDetector().loadModels();
	}

	/**
	 * Returns a list of all configuration
	 */
	@Override
	public Collection<PluginConfigSpec<?>> configSchema() {
		return Arrays.asList(CONFIG_METADATA, CONFIG_OPEN_NLP, CONFIG_EXTRACT_CONTENT);
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

				try {

					/**
					 * Extracts the content if needed
					 */

					String contentString = ((RubyString) eventData.get(METADATA_CONTENT)).toString();
					byte[] bytes = Base64.getDecoder().decode(contentString);
					String newContent = null;

					if (extractContent) {
						newContent = tika.parseToString(new ByteArrayInputStream(bytes));
						eventData.put(METADATA_CONTENT, newContent);
					}

					/**
					 * Detects type if does not exist
					 */

					if (!eventData.containsKey(METADATA_TYPE)) {
						eventData.put(METADATA_TYPE, tika.detect(bytes));
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

					List<DetectedEntity> entities = getEntities(newContent);

					entities.stream().forEach(e -> {
						eventData.put(e.getEntity(), e.getValue());

					});

				} catch (IOException | TikaException e) {
					LOGGER.error("Failed to extract content from file", e);
				}

				eventData.put(METADATA_TITLE, urlString.substring(urlString.lastIndexOf('/') + 1));

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

	public List<DetectedEntity> getEntities(String source) throws IOException {

		List<DetectedEntity> detectedEntities = new ArrayList<>();

		if (finder != null) {

			String[] whitespaceTokenizerLine = WhitespaceTokenizer.INSTANCE.tokenize(source);
			Span[] spans = finder.find(whitespaceTokenizerLine);
			String[] strings = Span.spansToStrings(spans, whitespaceTokenizerLine);

			for (int i = 0; i < spans.length; i++) {
				detectedEntities.add(new DetectedEntity(spans[i].getType(), strings[i]));
			}

		}
		
		return detectedEntities;

	}

}
