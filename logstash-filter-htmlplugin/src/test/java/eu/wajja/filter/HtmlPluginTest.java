//package eu.wajja.filter;
//
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.util.Arrays;
//import java.util.Base64;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.UUID;
//
//import org.apache.commons.io.IOUtils;
//import org.junit.Assert;
//import org.junit.Before;
//import org.junit.Test;
//import org.logstash.Event;
//import org.logstash.plugins.ConfigurationImpl;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//
//import co.elastic.logstash.api.Configuration;
//
//public class HtmlPluginTest {
//
//	private static final Logger LOGGER = LoggerFactory.getLogger(HtmlPluginTest.class);
//
//	private static Map<String, String> regexSet1 = new HashMap<>();
//	private static Map<String, String> regexSet2 = new HashMap<>();
//
//	private static final String SITENAME = "SITENAME";
//	private static final String SITEDESCRIPTION = "SITEDESCRIPTION";
//	private static final String SOURCE = "SOURCE";
//	private static final String THREAD_ID = "thread_id";
//	private static final String LOCALHOST = "http://localhost/test";
//	private static final String HTML_TITLE_1 = "html_title_1.html";
//	private static final String DC_TERMS_TITLE = "dcterms.title";
//
//	@Before
//	public void intialize() {
//
//		regexSet1.put(HtmlPlugin.METADATA_TITLE, DC_TERMS_TITLE);
//		regexSet1.put(HtmlPlugin.METADATA_CONTENT, DC_TERMS_TITLE);
//		regexSet2.put(SITENAME, "og:site_name");
//	}
//
//	@SuppressWarnings("unchecked")
//	@Test
//	public void filterHtmlMetadataTest() throws IOException {
//
//		Map<String, Object> configValues = new HashMap<>();
//
//		Map<String, Object> map1 = new HashMap<>();
//		map1.put(SOURCE, "AUDIOVISUAL-EC-EUROPA-EU");
//		map1.put(SITENAME, "Audiovisual Service");
//		map1.put(SITEDESCRIPTION, "European Commission Audiovisual Portal");
//		configValues.put(HtmlPlugin.PROPERTY_METADATA_CUSTOM, map1);
//
//		Configuration config = new ConfigurationImpl(configValues);
//		HtmlPlugin htmlFilter = new HtmlPlugin(UUID.randomUUID().toString(), config, null);
//
//		ObjectMapper mapper = new ObjectMapper();
//		String path = this.getClass().getClassLoader().getResource("15f5ad9b-ccd4-46dd-8889-679e3880d83d.json").getFile();
//		File file = new File(path);
//
//		try {
//			Map<String, Object> map = mapper.readValue(new FileInputStream(file), Map.class);
//			Event e = new org.logstash.Event();
//
//			map.entrySet().stream().forEach(ee -> e.setField(ee.getKey(), ee.getValue()));
//
//			e.setField(HtmlPlugin.METADATA_CONTEXT, "TEST_CONTEXT");
//			e.setField(HtmlPlugin.METADATA_TYPE, "html");
//
//			Collection<co.elastic.logstash.api.Event> results = htmlFilter.filter(Collections.singletonList(e), null);
//			Assert.assertFalse(results.isEmpty());
//
//			results.stream().forEach(eee -> {
//
//				Map<String, Object> data = eee.getData();
//
//				Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_URL));
//				Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_CONTENT));
//				Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_LANGUAGES));
//				Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_REFERENCE));
//				Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_TITLE));
//
//				Assert.assertTrue(data.containsKey(SOURCE) && ((List<String>) data.get(SOURCE)).get(0).equals("AUDIOVISUAL-EC-EUROPA-EU"));
//				Assert.assertTrue(data.containsKey(SITENAME) && ((List<String>) data.get(SITENAME)).get(0).equals("Audiovisual Service"));
//				Assert.assertTrue(data.containsKey(SITEDESCRIPTION) && ((List<String>) data.get(SITEDESCRIPTION)).get(0).equals("European Commission Audiovisual Portal"));
//
//			});
//
//		} catch (IOException e) {
//			LOGGER.info("ERROR", e);
//		}
//
//	}
//
//	@SuppressWarnings("unchecked")
//	@Test
//	public void filterHtmlBasicTest() throws IOException {
//
//		Map<String, Object> configValues = new HashMap<>();
//
//		Configuration config = new ConfigurationImpl(configValues);
//		HtmlPlugin htmlFilter = new HtmlPlugin(THREAD_ID, config, null);
//
//		ObjectMapper mapper = new ObjectMapper();
//		String path = this.getClass().getClassLoader().getResource("5bd7a6b6-ce3f-43f4-89cc-43052ceb6b2a.json").getFile();
//		File file = new File(path);
//
//		try {
//			Map<String, Object> map = mapper.readValue(new FileInputStream(file), Map.class);
//			Event e = new org.logstash.Event();
//
//			map.entrySet().stream().forEach(ee -> e.setField(ee.getKey(), ee.getValue()));
//			Collection<co.elastic.logstash.api.Event> results = htmlFilter.filter(Collections.singletonList(e), null);
//			Assert.assertFalse(results.isEmpty());
//
//			results.stream().forEach(eee -> {
//
//				Map<String, Object> data = eee.getData();
//
//				Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_URL));
//				Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_CONTENT));
//				Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_LANGUAGES));
//				Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_REFERENCE));
//				Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_CONTENT));
//				Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_TITLE));
//
//				String content = new String(Base64.getDecoder().decode(data.get(HtmlPlugin.METADATA_CONTENT).toString()));
//				Assert.assertTrue(content.contains("EURAXESS"));
//
//			});
//
//		} catch (IOException e) {
//			LOGGER.info("ERROR", e);
//		}
//
//	}
//
//	@Test
//	public void filterHtmlDateTest() throws IOException {
//
//		Map<String, Object> configValues = new HashMap<>();
//
//		configValues.put(HtmlPlugin.PROPERTY_METADATA_MAPPING, regexSet1);
//
//		Map<String, Object> map = new HashMap<>();
//		map.put("DATE", "article:published_time");
//		map.put("KEYWORDS", "keywords");
//		configValues.put(HtmlPlugin.PROPERTY_METADATA_MAPPING, map);
//
//		Configuration config = new ConfigurationImpl(configValues);
//		HtmlPlugin htmlFilter = new HtmlPlugin(THREAD_ID, config, null);
//
//		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("html_page_1.html");
//		String encodedContent = java.util.Base64.getEncoder().encodeToString(IOUtils.toByteArray(inputStream));
//		inputStream.close();
//
//		Event e = new org.logstash.Event();
//		e.setField(HtmlPlugin.METADATA_REFERENCE, HtmlPlugin.METADATA_REFERENCE);
//		e.setField(HtmlPlugin.METADATA_CONTENT, encodedContent);
//		e.setField(HtmlPlugin.METADATA_URL, LOCALHOST);
//
//		Collection<co.elastic.logstash.api.Event> results = htmlFilter.filter(Collections.singletonList(e), null);
//
//		Assert.assertFalse(results.isEmpty());
//		Map<String, Object> data = results.stream().findFirst().orElse(new Event()).getData();
//
//		Assert.assertTrue(data.containsKey("DATE"));
//
//		String title = data.get("DATE").toString();
//		Assert.assertTrue(title.equals("[2016-11-23T15:57:29+01:00]"));
//
//	}
//	
//	@Test
//	public void filterHtmlDate1Test() throws IOException {
//
//		Map<String, Object> configValues = new HashMap<>();
//
//		configValues.put(HtmlPlugin.PROPERTY_METADATA_MAPPING, regexSet1);
//
//		Map<String, Object> map = new HashMap<>();
//		map.put("DATE", "article:published_time");
//		configValues.put(HtmlPlugin.PROPERTY_METADATA_MAPPING, map);
//
//		Configuration config = new ConfigurationImpl(configValues);
//		HtmlPlugin htmlFilter = new HtmlPlugin(THREAD_ID, config, null);
//
//		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("html_page_3.html");
//		String encodedContent = java.util.Base64.getEncoder().encodeToString(IOUtils.toByteArray(inputStream));
//		inputStream.close();
//
//		Event e = new org.logstash.Event();
//		e.setField(HtmlPlugin.METADATA_REFERENCE, HtmlPlugin.METADATA_REFERENCE);
//		e.setField(HtmlPlugin.METADATA_CONTENT, encodedContent);
//		e.setField(HtmlPlugin.METADATA_URL, LOCALHOST);
//
//		Collection<co.elastic.logstash.api.Event> results = htmlFilter.filter(Collections.singletonList(e), null);
//
//		Assert.assertFalse(results.isEmpty());
//		Map<String, Object> data = results.stream().findFirst().orElse(new Event()).getData();
//
//		Assert.assertTrue(data.containsKey("DATE"));
//
//		String title = data.get("DATE").toString();
//		Assert.assertTrue(title.equals("[2016-05-04T15:25:45+02:00]"));
//
//	}
//
//	@Test
//	public void filterHtmlReferenceTest() throws IOException {
//
//		Map<String, Object> configValues = new HashMap<>();
//
//		Map<String, String> regexSet = new HashMap<>();
//		regexSet.put("TITLE", "dcterms.title");
//		regexSet.put("SITENAME", "og:site_name");
//		regexSet.put("DATE", "article:published_time");
//		regexSet.put("KEYWORDS", "keywords");
//
//		configValues.put(HtmlPlugin.PROPERTY_METADATA_MAPPING, regexSet);
//
//		Map<String, Object> map = new HashMap<>();
//		map.put("SOURCE", "EC-EUROPA-EU-BELGIUM");
//		map.put("SITENAME", "Belgium");
//		map.put("SITEDESCRIPTION", "Vertegenwoordiging in België");
//		map.put("SITEURL", "https://ec.europa.eu/belgium/home_nl");
//
//		configValues.put(HtmlPlugin.PROPERTY_METADATA_MAPPING, map);
//
//		Configuration config = new ConfigurationImpl(configValues);
//		HtmlPlugin htmlFilter = new HtmlPlugin(THREAD_ID, config, null);
//
//		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("html_page_2.html");
//		String encodedContent = java.util.Base64.getEncoder().encodeToString(IOUtils.toByteArray(inputStream));
//		inputStream.close();
//
//		Event e = new org.logstash.Event();
//		e.setField(HtmlPlugin.METADATA_REFERENCE, HtmlPlugin.METADATA_REFERENCE);
//		e.setField(HtmlPlugin.METADATA_CONTENT, encodedContent);
//		e.setField(HtmlPlugin.METADATA_URL, LOCALHOST);
//
//		Collection<co.elastic.logstash.api.Event> results = htmlFilter.filter(Collections.singletonList(e), null);
//
//		Assert.assertFalse(results.isEmpty());
//		Map<String, Object> data = results.stream().findFirst().orElse(new Event()).getData();
//		
//		Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_REFERENCE));
//		String reference = data.get(HtmlPlugin.METADATA_REFERENCE).toString();
//		Assert.assertTrue(reference.equals(HtmlPlugin.METADATA_REFERENCE));
//
//	}
//
//	@Test
//	public void filterHtmlTitleFromRegex1Test() throws IOException {
//
//		Map<String, Object> configValues = new HashMap<>();
//
//		configValues.put(HtmlPlugin.PROPERTY_METADATA_MAPPING, regexSet1);
//
//		Configuration config = new ConfigurationImpl(configValues);
//		HtmlPlugin htmlFilter = new HtmlPlugin(THREAD_ID, config, null);
//
//		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(HTML_TITLE_1);
//		String encodedContent = java.util.Base64.getEncoder().encodeToString(IOUtils.toByteArray(inputStream));
//		inputStream.close();
//
//		Event e = new org.logstash.Event();
//		e.setField(HtmlPlugin.METADATA_REFERENCE, HtmlPlugin.METADATA_REFERENCE);
//		e.setField(HtmlPlugin.METADATA_CONTENT, encodedContent);
//		e.setField(HtmlPlugin.METADATA_URL, LOCALHOST);
//
//		Collection<co.elastic.logstash.api.Event> results = htmlFilter.filter(Collections.singletonList(e), null);
//
//		Assert.assertFalse(results.isEmpty());
//
//		Map<String, Object> data = results.stream().findFirst().orElse(new Event()).getData();
//
//		Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_TITLE));
//
//		String title = data.get(HtmlPlugin.METADATA_TITLE).toString();
//		Assert.assertTrue(title.equals("[Together Against Trafficking in Human Beings 2]"));
//
//	}
//
//	@Test
//	public void filterHtmlTitleFromRegex2Test() throws IOException {
//
//		Map<String, Object> configValues = new HashMap<>();
//
//		configValues.put(HtmlPlugin.PROPERTY_METADATA_MAPPING, regexSet1);
//
//		Configuration config = new ConfigurationImpl(configValues);
//		HtmlPlugin htmlFilter = new HtmlPlugin(THREAD_ID, config, null);
//
//		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("html_title_2.html");
//		String encodedContent = java.util.Base64.getEncoder().encodeToString(IOUtils.toByteArray(inputStream));
//		inputStream.close();
//
//		Event e = new org.logstash.Event();
//		e.setField(HtmlPlugin.METADATA_REFERENCE, HtmlPlugin.METADATA_REFERENCE);
//		e.setField(HtmlPlugin.METADATA_CONTENT, encodedContent);
//		e.setField(HtmlPlugin.METADATA_URL, LOCALHOST);
//
//		Collection<co.elastic.logstash.api.Event> results = htmlFilter.filter(Collections.singletonList(e), null);
//		Assert.assertFalse(results.isEmpty());
//		Map<String, Object> data = results.stream().findFirst().orElse(new Event()).getData();
//
//		Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_TITLE));
//
//		String title = data.get(HtmlPlugin.METADATA_TITLE).toString();
//		Assert.assertTrue(title.equals("[Together Against Trafficking in Human Beings 2]"));
//
//	}
//
//	@Test
//	public void filterHtmlTitleFromRegex3Test() throws IOException {
//
//		Map<String, Object> configValues = new HashMap<>();
//
//		configValues.put(HtmlPlugin.PROPERTY_METADATA_MAPPING, regexSet1);
//
//		Configuration config = new ConfigurationImpl(configValues);
//		HtmlPlugin htmlFilter = new HtmlPlugin(THREAD_ID, config, null);
//
//		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("html_title_3.html");
//		String encodedContent = java.util.Base64.getEncoder().encodeToString(IOUtils.toByteArray(inputStream));
//		inputStream.close();
//
//		Event e = new org.logstash.Event();
//		e.setField(HtmlPlugin.METADATA_REFERENCE, HtmlPlugin.METADATA_REFERENCE);
//		e.setField(HtmlPlugin.METADATA_CONTENT, encodedContent);
//		e.setField(HtmlPlugin.METADATA_URL, LOCALHOST);
//
//		Collection<co.elastic.logstash.api.Event> results = htmlFilter.filter(Collections.singletonList(e), null);
//		Assert.assertFalse(results.isEmpty());
//		Map<String, Object> data = results.stream().findFirst().orElse(new Event()).getData();
//
//		Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_TITLE));
//
//		String title = data.get(HtmlPlugin.METADATA_TITLE).toString();
//		Assert.assertTrue(title.equals("[Together Against Trafficking in Human Beings | 3]"));
//
//	}
//
//	@Test
//	public void filterHtmlContentFromRegex1Test() throws IOException {
//
//		Map<String, Object> configValues = new HashMap<>();
//
//		configValues.put(HtmlPlugin.PROPERTY_METADATA_MAPPING, regexSet1);
//
//		Configuration config = new ConfigurationImpl(configValues);
//		HtmlPlugin htmlFilter = new HtmlPlugin(THREAD_ID, config, null);
//
//		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(HTML_TITLE_1);
//		String encodedContent = java.util.Base64.getEncoder().encodeToString(IOUtils.toByteArray(inputStream));
//		inputStream.close();
//
//		Event e = new org.logstash.Event();
//		e.setField(HtmlPlugin.METADATA_REFERENCE, HtmlPlugin.METADATA_REFERENCE);
//		e.setField(HtmlPlugin.METADATA_CONTENT, encodedContent);
//		e.setField(HtmlPlugin.METADATA_URL, LOCALHOST);
//
//		Collection<co.elastic.logstash.api.Event> results = htmlFilter.filter(Collections.singletonList(e), null);
//		Assert.assertFalse(results.isEmpty());
//		Map<String, Object> data = results.stream().findFirst().orElse(new Event()).getData();
//
//		Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_CONTENT));
//
//		String content = new String(Base64.getDecoder().decode(data.get(HtmlPlugin.METADATA_CONTENT).toString()));
//		Assert.assertTrue(content.contains("Together Against Trafficking in Human Beings | 3"));
//
//	}
//
//	@Test
//	public void filterHtmlContentFromRegex2Test() throws IOException {
//
//		Map<String, Object> configValues = new HashMap<>();
//
//		configValues.put(HtmlPlugin.PROPERTY_METADATA_MAPPING, regexSet1);
//
//		Configuration config = new ConfigurationImpl(configValues);
//		HtmlPlugin htmlFilter = new HtmlPlugin(THREAD_ID, config, null);
//
//		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("html_title_2.html");
//		String encodedContent = java.util.Base64.getEncoder().encodeToString(IOUtils.toByteArray(inputStream));
//		inputStream.close();
//
//		Event e = new org.logstash.Event();
//		e.setField(HtmlPlugin.METADATA_REFERENCE, HtmlPlugin.METADATA_REFERENCE);
//		e.setField(HtmlPlugin.METADATA_CONTENT, encodedContent);
//		e.setField(HtmlPlugin.METADATA_URL, LOCALHOST);
//
//		Collection<co.elastic.logstash.api.Event> results = htmlFilter.filter(Collections.singletonList(e), null);
//		Map<String, Object> data = results.stream().findFirst().orElse(new Event()).getData();
//
//		Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_CONTENT));
//
//		String content = new String(Base64.getDecoder().decode(data.get(HtmlPlugin.METADATA_CONTENT).toString()));
//		Assert.assertTrue(content.contains("Together Against Trafficking in Human Beings | 3"));
//
//	}
//
//	@Test
//	public void filterHtmlContentFromRegex4Test() throws IOException {
//
//		Map<String, Object> configValues = new HashMap<>();
//
//		configValues.put(HtmlPlugin.PROPERTY_EXTRACT_TITLE_CSS, Arrays.asList(".page-header__hero-title"));
//		configValues.put(HtmlPlugin.PROPERTY_EXTRACT_BODY_CSS, Arrays.asList(".page-content", "#main-content", "body .container", ".region-content"));
//
//		Map<String, String> map1 = new HashMap<>();
//		map1.put("TITLE", DC_TERMS_TITLE);
//		map1.put(SITENAME, "og:site_name");
//
//		Map<String, String> map3 = new HashMap<>();
//		map3.put(SOURCE, "EC-EUROPA-EU-BELGIUM");
//		map3.put(SITENAME, "Belgium");
//		map3.put(SITEDESCRIPTION, "Représentation en Belgique");
//		map3.put("SITEURL", "https://ec.europa.eu/belgium/home_nl");
//
//		configValues.put(HtmlPlugin.PROPERTY_METADATA_MAPPING, map1);
//		configValues.put(HtmlPlugin.PROPERTY_METADATA_CUSTOM, map3);
//
//		Configuration config = new ConfigurationImpl(configValues);
//		HtmlPlugin htmlFilter = new HtmlPlugin(THREAD_ID, config, null);
//
//		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("html_title_4.html");
//		String encodedContent = java.util.Base64.getEncoder().encodeToString(IOUtils.toByteArray(inputStream));
//		inputStream.close();
//
//		Event e = new org.logstash.Event();
//		e.setField(HtmlPlugin.METADATA_REFERENCE, HtmlPlugin.METADATA_REFERENCE);
//		e.setField(HtmlPlugin.METADATA_CONTENT, encodedContent);
//		e.setField(HtmlPlugin.METADATA_URL, LOCALHOST);
//
//		Collection<co.elastic.logstash.api.Event> results = htmlFilter.filter(Collections.singletonList(e), null);
//		Assert.assertFalse(results.isEmpty());
//		Map<String, Object> data = results.stream().findFirst().orElse(new Event()).getData();
//
//		Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_TITLE));
//		Assert.assertTrue(data.get(HtmlPlugin.METADATA_TITLE).toString().equals("[Economic forecast for Luxembourg]"));
//
//		Assert.assertTrue(data.containsKey(SITENAME));
//		Assert.assertTrue(data.get(SITENAME).toString().equals("[Europska komisija - European Commission]"));
//
//	}
//
//	@Test
//	public void filterMetadataContentFromRegex1Test() throws IOException {
//
//		Map<String, Object> configValues = new HashMap<>();
//
//		configValues.put(HtmlPlugin.PROPERTY_METADATA_MAPPING, regexSet2);
//
//		Configuration config = new ConfigurationImpl(configValues);
//		HtmlPlugin htmlFilter = new HtmlPlugin(THREAD_ID, config, null);
//
//		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(HTML_TITLE_1);
//		String encodedContent = java.util.Base64.getEncoder().encodeToString(IOUtils.toByteArray(inputStream));
//		inputStream.close();
//
//		Event e = new org.logstash.Event();
//		e.setField(HtmlPlugin.METADATA_REFERENCE, HtmlPlugin.METADATA_REFERENCE);
//		e.setField(HtmlPlugin.METADATA_CONTENT, encodedContent);
//		e.setField(HtmlPlugin.METADATA_URL, LOCALHOST);
//
//		Collection<co.elastic.logstash.api.Event> results = htmlFilter.filter(Collections.singletonList(e), null);
//		Assert.assertFalse(results.isEmpty());
//		Map<String, Object> data = results.stream().findFirst().orElse(new Event()).getData();
//
//		Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_CONTENT));
//
//		String siteName = data.get(SITENAME).toString();
//		Assert.assertTrue(siteName.equals("[Together Against Trafficking in Human Beings - European Commission]"));
//
//	}
//}
