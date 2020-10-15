package eu.wajja.filter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import org.apache.commons.io.IOUtils;
import org.apache.tika.Tika;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.jruby.RubyString;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.logstash.ConvertedList;
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
    private static final String PROPERTY_CUSTOM_METADATA = "metadataCustom";
    private static final String PROPERTY_SIMPLIFIED_CONTENT_TYPE = "simplifiedContentType";
    private static final String PROPERTY_BEST_BET_URLS = "bestBetUrls";
    private static final String PROPERTY_BEST_BET_FIELD = "bestBetField";
    private static final String PROPERTY_METADATA_URL = "metadataUrl";
    private static final String PROPERTY_TIMEOUT = "timeout";
    private static final String PROPERTY_PROXY_HOST = "proxyHost";
    private static final String PROPERTY_PROXY_PORT = "proxyPort";
    private static final String PROPERTY_PROXY_USER = "proxyUser";
    private static final String PROPERTY_PROXY_PASS = "proxyPass";
    private static final String PROPERTY_CRAWLER_USER_AGENT = "crawlerUserAgent";
    private static final String PROPERTY_CRAWLER_REFERER = "crawlerReferer";
    private static final String PROPERTY_DATE_FORMAT = "dateFormat";

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
    private static final String METADATA_TITLE = "TITLE";

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSX";

    private static final PluginConfigSpec<String> CONFIG_DATA_FOLDER = PluginConfigSpec.stringSetting(PROPERTY_DATA_FOLDER);
    private static final PluginConfigSpec<Map<String, Object>> CONFIG_CUSTOM_METADATA = PluginConfigSpec.hashSetting(PROPERTY_CUSTOM_METADATA, new HashMap<>(), false, false);
    private static final PluginConfigSpec<Map<String, Object>> CONFIG_SIMPLIFIED_CONTENT_TYPE = PluginConfigSpec.hashSetting(PROPERTY_SIMPLIFIED_CONTENT_TYPE, new HashMap<>(), false, false);
    private static final PluginConfigSpec<Map<String, Object>> CONFIG_BEST_BET_URLS = PluginConfigSpec.hashSetting(PROPERTY_BEST_BET_URLS, new HashMap<>(), false, false);
    private static final PluginConfigSpec<String> CONFIG_BEST_BET_FIELD = PluginConfigSpec.stringSetting(PROPERTY_BEST_BET_FIELD, METADATA_SITETITLE);
    private static final PluginConfigSpec<String> CONFIG_METADATA_URL = PluginConfigSpec.stringSetting(PROPERTY_METADATA_URL, null, false, false);
    private static final PluginConfigSpec<String> CONFIG_DATE_FORMAT = PluginConfigSpec.stringSetting(PROPERTY_DATE_FORMAT, null, false, false);

    public static final PluginConfigSpec<Long> CONFIG_TIMEOUT = PluginConfigSpec.numSetting(PROPERTY_TIMEOUT, 8000);
    public static final PluginConfigSpec<String> CONFIG_PROXY_HOST = PluginConfigSpec.stringSetting(PROPERTY_PROXY_HOST);
    public static final PluginConfigSpec<Long> CONFIG_PROXY_PORT = PluginConfigSpec.numSetting(PROPERTY_PROXY_PORT, 80);
    public static final PluginConfigSpec<String> CONFIG_PROXY_USER = PluginConfigSpec.stringSetting(PROPERTY_PROXY_USER);
    public static final PluginConfigSpec<String> CONFIG_PROXY_PASS = PluginConfigSpec.stringSetting(PROPERTY_PROXY_PASS);
    public static final PluginConfigSpec<String> CONFIG_CRAWLER_USER_AGENT = PluginConfigSpec.stringSetting(PROPERTY_CRAWLER_USER_AGENT, "Wajja Europa Plugin");
    public static final PluginConfigSpec<String> CONFIG_CRAWLER_REFERER = PluginConfigSpec.stringSetting(PROPERTY_CRAWLER_REFERER, "http://wajja.eu/");

    private String threadId;
    private Map<String, String> mapGeneralFilters;
    private Map<String, String> mapGeneralFiltersIds;
    private Map<String, String> mapGeneralFiltersTopics;
    private Map<String, String> mapRestrictedFilters;
    private Map<String, Object> customMetadata;
    private Map<String, Object> simplifiedContentType;
    private Map<String, Object> bestBetUrls;
    private String bestBetField;
    private String metadataUrl;
    private Proxy proxy;
    private Long timeout;
    private String userAgent;
    private String referer;
    private String dateFormat;
    private ProxyController proxyController;

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
        this.bestBetField = config.get(CONFIG_BEST_BET_FIELD);
        this.bestBetUrls = config.get(CONFIG_BEST_BET_URLS);
        this.metadataUrl = config.get(CONFIG_METADATA_URL);
        this.timeout = config.get(CONFIG_TIMEOUT);
        this.referer = config.get(CONFIG_CRAWLER_REFERER);
        this.userAgent = config.get(CONFIG_CRAWLER_USER_AGENT);
        this.dateFormat = config.get(CONFIG_DATE_FORMAT);

        proxyController = new ProxyController(config.get(CONFIG_PROXY_USER), config.get(CONFIG_PROXY_PASS), config.get(CONFIG_PROXY_HOST), config.get(CONFIG_PROXY_PORT), false);

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

                String collectionName = file.getAbsoluteFile().getName().split("_")[1].replace(".csv", "");

                String[] cc = i.split("\\|");
                this.mapRestrictedFilters.put(cc[0], collectionName + "::" + cc[1].replace("/", "::"));
                });

            } catch (Exception e) {
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

        return Arrays.asList(CONFIG_DATA_FOLDER,
                CONFIG_CUSTOM_METADATA,
                CONFIG_SIMPLIFIED_CONTENT_TYPE,
                CONFIG_BEST_BET_FIELD,
                CONFIG_BEST_BET_URLS,
                CONFIG_METADATA_URL,
                CONFIG_TIMEOUT,
                CONFIG_PROXY_HOST,
                CONFIG_PROXY_PORT,
                CONFIG_PROXY_USER,
                CONFIG_PROXY_PASS,
                CONFIG_DATE_FORMAT,
                CONFIG_CRAWLER_USER_AGENT,
                CONFIG_CRAWLER_REFERER);
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
                        eventData.put(METADATA_SIMPLIFIED_CONTENT_TYPE, simplifiedContentType.get(contentType));
                    } else {
                        LOGGER.info("Could not map simplified content type from url : {}, detected simple : {}", url, contentType);
                    }

                    /**
                     * KEYWORDS
                     */

                    String keywordsUrl = url.toLowerCase().replaceAll("(http).*(\\/\\/)[^\\/]{2,}(\\/)", "");
                    if (keywordsUrl.endsWith("/")) {
                        keywordsUrl = keywordsUrl.substring(0, keywordsUrl.length() - 1);
                    }

                    if (!keywordsUrl.isEmpty()) {

                        String[] keywordUrls = keywordsUrl.split("/");

                        for (int x = 0; x < keywordUrls.length; x++) {
                            keywordUrls[x] = keywordUrls[x].replace("-", " ");
                        }

                        if (keywordUrls.length > 0) {
                            eventData.put(METADATA_KEYWORDS, Arrays.asList(keywordUrls).stream().filter(f -> !f.isEmpty()).collect(Collectors.toList()));
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

                            try {
                                String param = languagesUrls[x];

                                if (param.matches(".*_(" + ALLOWED_LANGUAGES + ")\\..*")) {
                                    param = param.replaceAll(".*_", "").substring(0, 2);
                                    eventData.put(METADATA_LANGUAGES, Arrays.asList(param));

                                } else if (param.matches(ALLOWED_LANGUAGES)) {
                                    eventData.put(METADATA_LANGUAGES, Arrays.asList(param));

                                } else if (param.matches(".*_(" + ALLOWED_LANGUAGES + ")")) {
                                    param = param.replaceAll(".*_", "");
                                    eventData.put(METADATA_LANGUAGES, Arrays.asList(param));

                                }

                            } catch (StringIndexOutOfBoundsException e) {
                                LOGGER.error("Failed to detect language from url {}", languagesUrl, e);
                            }
                        }
                    }
                    /**
                     * RESTRICTED_FILTER
                     */

                    List<String> restrictedFilters = getMatchingUrls(this.mapRestrictedFilters, url);

                    List<String> parentLevelFilters = restrictedFilters.stream().filter(rf -> rf.split("::").length > 2).map(rf -> rf.split("::")[0] + "::" + rf.split("::")[1]).collect(Collectors.toList());
                    if (!parentLevelFilters.isEmpty()) {
                    restrictedFilters.addAll(parentLevelFilters);
                    }
                    eventData.put(METADATA_RESTRICTED_FILTER, restrictedFilters.stream().distinct().collect(Collectors.toList()));

                    /**
                     * GENERAL_FILTER
                     */

                    List<String> generalFilters = getMatchingUrls(this.mapGeneralFilters, url);

                    List<String> generalFiltersIds = getMatchingUrls(this.mapGeneralFiltersIds, url);
                    List<String> ids = generalFiltersIds.stream().flatMap(x -> Arrays.asList(x.split("\\/")).stream()).collect(Collectors.toList());

                    HashSet<String> set = new HashSet<>(ids);
                    set.stream().forEach(s -> {

                    if (this.mapGeneralFiltersTopics.containsKey(s)) {

                        String[] parts = this.mapGeneralFiltersTopics.get(s).split("/");
                        if (!generalFilters.contains(parts[0])) {
                        generalFilters.add(parts[0]);
                        }

                        if (parts.length > 1) {
                        generalFilters.add(this.mapGeneralFiltersTopics.get(s).replace("/", "::"));
                        }
                    }

                    });

                    eventData.put(METADATA_GENERAL_FILTER, generalFilters.stream().distinct().collect(Collectors.toList()));


                    /**
                     * DATE
                     */

                    if (!eventData.containsKey(METADATA_DATE)) {

                        String date = new SimpleDateFormat(DATE_FORMAT).format(new Date());
                        eventData.put(METADATA_DATE, Arrays.asList(date));

                    } else if (eventData.containsKey(METADATA_DATE) && dateFormat != null) {

                        List<String> dates = getPropertyList(eventData, METADATA_DATE);

                        dates = dates.stream().map(date -> {

                            try {

                                Date dateObject = new SimpleDateFormat(dateFormat).parse(date);
                                return new SimpleDateFormat(DATE_FORMAT).format(dateObject);

                            } catch (ParseException e) {
                                LOGGER.error("Failed to parse date format", e);
                            }

                            return null;

                        }).collect(Collectors.toList());

                        eventData.put(METADATA_DATE, dates);

                    }

                    /**
                     * Extra metadata from url
                     */

                    if (this.metadataUrl != null && !this.metadataUrl.isEmpty()) {

                        String httpUrl = url.replace("https", "http");
                        HttpURLConnection httpURLConnection = null;

                        try {

                            URL fullUrl = new URL(this.metadataUrl + httpUrl);

                            LOGGER.info("Extracting metadata from {}", fullUrl);

                            if (proxy == null) {
                                httpURLConnection = (HttpURLConnection) fullUrl.openConnection();
                            } else {
                                httpURLConnection = (HttpURLConnection) fullUrl.openConnection(proxy);
                            }

                            httpURLConnection.setConnectTimeout(timeout.intValue());
                            httpURLConnection.setReadTimeout(timeout.intValue());
                            httpURLConnection.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
                            httpURLConnection.addRequestProperty("User-Agent", userAgent);
                            httpURLConnection.addRequestProperty("Referer", referer);

                            httpURLConnection.connect();

                            if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                extractMetadata(eventData, httpURLConnection, fullUrl);
                            }

                        } catch (Exception e) {
                            LOGGER.error("Failed to retrieve metadata for url : {}", httpUrl);

                        } finally {

                            if (httpURLConnection != null) {
                                httpURLConnection.disconnect();
                            }
                        }
                    }

                } catch (IOException e) {
                    LOGGER.error("Failed to detect content type", e);
                }

            }

            /**
             * Add the configured metadata
             */
            customMetadata.entrySet().stream().forEach(entry -> eventData.put(entry.getKey(), entry.getValue()));

            /**
             * Mapping URLS to BestBets
             */
            if (eventData.containsKey(METADATA_URL)) {

                String url = eventData.get(METADATA_URL).toString();

                if (this.bestBetUrls.containsKey(url)) {

                    if (this.bestBetUrls.get(url) instanceof String[]) {
                        String[] values = (String[]) this.bestBetUrls.get(url);
                        eventData.put(bestBetField, Arrays.asList(values));

                    } else {
                        List<String> values = (List<String>) this.bestBetUrls.get(url);
                        eventData.put(bestBetField, values);
                    }
                }
            }

        });

        return events;

    }

    @SuppressWarnings("unchecked")
    private List<String> getPropertyList(Map<String, Object> eventData, String property) {

        Object object = eventData.get(property);

        if (object instanceof ConvertedList) {

            ConvertedList list = (ConvertedList) object;
            return list.stream().map(x -> ((RubyString) x).toString()).collect(Collectors.toList());

        } else if (object instanceof List) {

            List list = (List) object;

            if (!list.isEmpty() && list.get(0) instanceof String) {
                return list;
            }

        } else if (object != null) {
            return Arrays.asList(object.toString());
        }

        return new ArrayList<>();
    }

    private void extractMetadata(Map<String, Object> eventData, HttpURLConnection httpURLConnection, URL fullUrl) {

        try (InputStream inputStream = httpURLConnection.getInputStream()) {

            Document document = Jsoup.parse(IOUtils.toString(inputStream, StandardCharsets.UTF_8));
            Elements metadataElements = document.getElementsByTag("meta");

            metadataElements.stream().forEach(element -> {

                String attrValue = element.attr("name");
                String content = element.attr(METADATA_CONTENT);
                LOGGER.info("metadataElements found {} : {} ", attrValue, content);

                if (attrValue.equals("Docsroom_DocumentTitle")) {
                    eventData.put(METADATA_TITLE, Arrays.asList(content));

                } else if (attrValue.equals("Docsroom_DocumentLanguage")) {
                    eventData.put(METADATA_LANGUAGES, Arrays.asList(content.toLowerCase()));

                } else if (attrValue.equals("Docsroom_DocumentKeywords")) {
                    eventData.put(METADATA_KEYWORDS, content.split(","));

                } else if (attrValue.equals("Docsroom_DocumentDate")) {

                    Date date = new Date(Long.getLong(content));
                    String dateFormatted = new SimpleDateFormat(DATE_FORMAT).format(date);
                    eventData.put(METADATA_DATE, Arrays.asList(dateFormatted));
                }

            });

        } catch (Exception e) {
            LOGGER.error("Failed to read content from url : {}", fullUrl);
        }
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

            if (file != null) {

                List<String> lines = Files.readLines(file, Charset.defaultCharset());
                lines.stream().forEach(i -> {

                    String[] cc = i.split("\\|");
                    map.put(cc[0], cc[1]);
                });
            }

        }

        return map;
    }
}
