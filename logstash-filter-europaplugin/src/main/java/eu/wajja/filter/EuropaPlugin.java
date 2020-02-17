package eu.wajja.filter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.tika.Tika;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

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
	private static final String PROPERTY_CUSTOM_METADATA = "customMetadata";
	private static final String PROPERTY_SIMPLIFIED_CONTENT_TYPE = "simplifiedContentType";

	private static final PluginConfigSpec<String> CONFIG_DATA_FOLDER = PluginConfigSpec.stringSetting(PROPERTY_DATA_FOLDER);
	private static final PluginConfigSpec<Map<String, Object>> CONFIG_CUSTOM_METADATA = PluginConfigSpec.hashSetting(PROPERTY_CUSTOM_METADATA, new HashMap<String, Object>(), false, false);
	private static final PluginConfigSpec<Map<String, Object>> CONFIG_SIMPLIFIED_CONTENT_TYPE = PluginConfigSpec.hashSetting(PROPERTY_SIMPLIFIED_CONTENT_TYPE, new HashMap<String, Object>(), false, false);

	private static final String ALLOWED_LANGUAGES = "(be)|(bg)|(bs)|(ca)|(cs)|(cy)|(da)|(de)|(el)|(en)|(es)|(et)|(eu)|(fi)|(fr)|(ga)|(hr)|(hu)|(is)|(it)|(lb)|(lt)|(lv)|(mk)|(mt)|(nl)|(no)|(pl)|(pt)|(ro)|(ru)|(sk)|(sl)|(sq)|(sr)|(sv)|(tr)|(uk)";
	private static final String METADATA_SIMPLIFIED_CONTENT_TYPE = "SIMPLIFIED_CONTENT_TYPE";
	private static final String METADATA_RESTRICTED_FILTER = "RESTRICTED_FILTER";
	private static final String METADATA_GENERAL_FILTER = "GENERAL_FILTER";
	private static final String METADATA_SITETITLE = "SITETITLE";
	private static final String METADATA_CONTENT = "content";
	private static final String METADATA_URL = "url";
	private static final String METADATA_DATE = "DATE";
	private static final String METADATA_KEYWORDS = "KEYWORDS";
	private static final String METADATA_LANGUAGES = "languages";

	private String threadId;
	private Map<String, String> mapGeneralFilters;
	private Map<String, String> mapGeneralFiltersIds;
	private Map<String, String> mapGeneralFiltersTopics;
	private Map<String, String> mapRestrictedFilters;
	private Map<String, Object> customMetadata;
	private Map<String, Object> simplifiedContentType;

	/**
	 * Mandatory constructor
	 * 
	 * @param id
	 * @param config
	 * @param context
	 * @throws IOException
	 */
	public EuropaPlugin(String id, Configuration config, Context context) throws IOException {

		if (context != null && LOGGER.isDebugEnabled()) {
			LOGGER.debug(context.toString());
		}

		this.threadId = id;

		String dataFolder = config.get(CONFIG_DATA_FOLDER) + "/europa-data/";
		this.customMetadata = config.get(CONFIG_CUSTOM_METADATA);
		this.simplifiedContentType = config.get(CONFIG_SIMPLIFIED_CONTENT_TYPE);

		/**
		 * General Filters
		 */

		Path pathGeneral = Paths.get(dataFolder + "/GENERAL_FILTER/");

		this.mapGeneralFilters = parseCsv(pathGeneral, "general_filters.csv");
		this.mapGeneralFiltersIds = parseCsv(pathGeneral, "general_filters_2013.csv");
		this.mapGeneralFiltersTopics = parseCsv(pathGeneral, "general_filters_2013_topics.csv");

		/**
		 * RestrictedFilters
		 */

		Path pathRestricted = Paths.get(dataFolder + "/RESTRICTED_FILTER/");
		this.mapRestrictedFilters = new HashedMap<>();

		if (pathRestricted.toFile().exists()) {

			Arrays.asList(pathRestricted.toFile().listFiles()).stream().forEach(file -> {

				try {

					List<String> lines = Files.readLines(file, Charset.defaultCharset());

					lines.stream().forEach(i -> {

						String[] cc = i.split("\\|");
						this.mapRestrictedFilters.put(cc[0], cc[1]);
					});

				} catch (IOException e) {
					LOGGER.error("Failed to read csv", e);
				}
			});

		}

	}

	/**
	 * Returns a list of all configuration
	 */
	@Override
	public Collection<PluginConfigSpec<?>> configSchema() {
		return Arrays.asList(CONFIG_DATA_FOLDER, CONFIG_CUSTOM_METADATA, CONFIG_SIMPLIFIED_CONTENT_TYPE);
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

				try {
					String contentString = eventData.get(METADATA_CONTENT).toString();
					byte[] bytes = Base64.getDecoder().decode(contentString);

					String url = eventData.get(METADATA_URL).toString();

					/**
					 * SIMPLIFIED CONTENT TYPE
					 */

					MediaType mediaType = tika.getDetector().detect(TikaInputStream.get(bytes), new Metadata());
					String contentType = mediaType.getSubtype();

					if (contentType != null && this.simplifiedContentType.containsKey(contentType)) {
						eventData.put(METADATA_SIMPLIFIED_CONTENT_TYPE, (String) simplifiedContentType.get(contentType));
					} else {
						LOGGER.info("Could not map simplified content type from url : {}, detected simple : {}", url, contentType);
					}

					/**
					 * KEYWORDS
					 */

					String keywordsUrl = url.toLowerCase().replaceAll("(http).*(\\/\\/)[a-z]{2,}(\\/)", "");
					if (keywordsUrl.endsWith("/")) {
						keywordsUrl = keywordsUrl.substring(0, keywordsUrl.length() - 1);
					}

					if (!keywordsUrl.isEmpty()) {

						String[] keywordUrls = keywordsUrl.split("/");

						for (int x = 0; x < keywordUrls.length; x++) {
							keywordUrls[x] = keywordUrls[x].replace("-", " ");
						}

						if (keywordUrls.length > 0) {
							eventData.put(METADATA_KEYWORDS, Arrays.asList(keywordUrls));
						}
					}

					/**
					 * LANGUAGES
					 */

					String languagesUrl = url.toLowerCase().replaceAll("(http).*(\\/\\/)[a-z]{2,}(\\/)", "");
					if (languagesUrl.endsWith("/")) {
						languagesUrl = languagesUrl.substring(0, languagesUrl.length() - 1);
					}

					if (!languagesUrl.isEmpty()) {

						String[] languagesUrls = languagesUrl.split("/");

						for (int x = 0; x < languagesUrls.length; x++) {

							String param = languagesUrls[x];

							if (param.matches(".*_(" + ALLOWED_LANGUAGES + ")..*")) {
								param = param.replaceAll(".*_", "").substring(0, 2);
								eventData.put(METADATA_LANGUAGES, Arrays.asList(param));

							} else if (param.matches(ALLOWED_LANGUAGES)) {
								eventData.put(METADATA_LANGUAGES, Arrays.asList(param));

							} else if (param.matches(".*_(" + ALLOWED_LANGUAGES + ")")) {
								param = param.replaceAll(".*_", "");
								eventData.put(METADATA_LANGUAGES, Arrays.asList(param));

							}
						}
					}

					/**
					 * RESTRICTED_FILTER
					 */

					List<String> restrictedFilters = getMatchingUrls(this.mapRestrictedFilters, url);
					eventData.put(METADATA_RESTRICTED_FILTER, restrictedFilters);

					/**
					 * GENERAL_FILTER
					 */

					List<String> generalFilters = getMatchingUrls(this.mapGeneralFilters, url);

					List<String> generalFiltersIds = getMatchingUrls(this.mapGeneralFiltersIds, url);
					List<String> ids = generalFiltersIds.stream().flatMap(x -> Arrays.asList(x.split("\\/")).stream()).collect(Collectors.toList());

					HashSet<String> set = new HashSet<>(ids);
					set.stream().forEach(s -> {

						if (this.mapGeneralFiltersTopics.containsKey(s)) {
							generalFilters.add(this.mapGeneralFiltersTopics.get(s));
						}

					});

					eventData.put(METADATA_GENERAL_FILTER, generalFilters);

					/**
					 * DATE
					 */

					String date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX").format(new Date());
					eventData.put(METADATA_DATE, Arrays.asList(date));

				} catch (IOException e) {
					LOGGER.error("Failed to detect content type", e);
				}

			}

			if (eventData.containsKey(METADATA_URL)) {

				String url = eventData.get(METADATA_URL).toString();

				if (this.customMetadata.containsKey(url)) {

					if (this.customMetadata.get(url) instanceof String[]) {

						String[] values = (String[]) this.customMetadata.get(url);
						eventData.put(METADATA_SITETITLE, Arrays.asList(values));

					} else {

						List<String> values = (List<String>) this.customMetadata.get(url);
						eventData.put(METADATA_SITETITLE, values);
					}

				}
			}

		});

		return events;

	}

	private List<String> getMatchingUrls(Map<String, String> urls, String url) {

		return urls.keySet().parallelStream().filter(k -> {

			StringBuilder stringBuilder = new StringBuilder();

			if (k.startsWith("*")) {
				stringBuilder.append(".");
			} else {
				stringBuilder.append(".*");
			}

			stringBuilder.append(k).append(".*");

			return Pattern.matches(stringBuilder.toString(), url);

		}).map(k -> urls.get(k)).collect(Collectors.toList());
	}

	private Map<String, String> parseCsv(Path pathGeneral, String name) throws IOException {

		Map<String, String> map = new HashedMap<>();

		if (pathGeneral.toFile().exists()) {

			File file = Arrays.asList(pathGeneral.toFile().listFiles()).stream().filter(f -> f.getName().equals(name)).findFirst().orElse(null);

			List<String> lines = Files.readLines(file, Charset.defaultCharset());
			lines.stream().forEach(i -> {

				String[] cc = i.split("\\|");
				map.put(cc[0], cc[1]);
			});

		}

		return map;
	}
}
