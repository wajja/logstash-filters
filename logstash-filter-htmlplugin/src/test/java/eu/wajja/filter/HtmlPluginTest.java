package eu.wajja.filter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.logstash.Event;
import org.logstash.plugins.ConfigurationImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.elastic.logstash.api.Configuration;

public class HtmlPluginTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(HtmlPluginTest.class);
	private Properties properties;

	private static final String METADATA_TITLE = "TITLE";
	private static final String METADATA_URL = "url";
	private static final String METADATA_CONTEXT = "context";
	private static final String METADATA_CONTENT = "content";
	private static final String METADATA_LANGUAGES = "languages";
	private static final String METADATA_TYPE = "type";
	private static final String METADATA_REFERENCE = "reference";
	private static final String PROPERTY_METADATA = "metadata";

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

	private static final List<String> REGEX_SET_1 = Arrays.asList("(content=\"(.*?)\".property=\"og:title\")", "(content=\"(.*?)\".name=\"dcterms.title\")", "<title>(.*?)</title>");
	private static final List<String> REGEX_SET_2 = Arrays.asList("SITENAME:(content=\"(.*?)\".property=\"og:site_name\")");

	@Before
	public void intialize() throws IOException {

		properties = new Properties();
		properties.load(this.getClass().getClassLoader().getResourceAsStream("test.properties"));
	}

	@Test
	public void filterHtmlMetadataTest() throws IOException {

		Map<String, Object> configValues = new HashMap<>();
		configValues.put(PROPERTY_METADATA, Arrays.asList("SOURCE=AUDIOVISUAL-EC-EUROPA-EU", "SITENAME=Audiovisual Service", "SITEDESCRIPTION=European Commission Audiovisual Portal"));
		configValues.put(PROPERTY_EXPORT_CONTENT, true);
		configValues.put(PROPERTY_EXTRACT_CONTENT, true);
		configValues.put(PROPERTY_DATA_FOLDER, properties.get(PROPERTY_DATA_FOLDER));

		Configuration config = new ConfigurationImpl(configValues);
		HtmlPlugin htmlFilter = new HtmlPlugin(UUID.randomUUID().toString(), config, null);

		ObjectMapper mapper = new ObjectMapper();
		String path = this.getClass().getClassLoader().getResource("15f5ad9b-ccd4-46dd-8889-679e3880d83d.json").getFile();
		File file = new File(path);

		try {
			Map<String, Object> map = mapper.readValue(new FileInputStream(file), Map.class);
			Event e = new org.logstash.Event();

			map.entrySet().stream().forEach(ee -> {
				e.setField(ee.getKey(), ee.getValue());
			});

			e.setField(METADATA_CONTEXT, "TEST_CONTEXT");
			e.setField(METADATA_TYPE, "html");

			Collection<co.elastic.logstash.api.Event> results = htmlFilter.filter(Collections.singletonList(e), null);
			Assert.assertFalse(results.isEmpty());

			results.stream().forEach(eee -> {

				Map<String, Object> data = eee.getData();

				Assert.assertTrue(data.containsKey(METADATA_URL));
				Assert.assertTrue(data.containsKey(METADATA_CONTENT));
				Assert.assertTrue(data.containsKey(METADATA_LANGUAGES));
				Assert.assertTrue(data.containsKey(METADATA_REFERENCE));
				Assert.assertTrue(data.containsKey(METADATA_TITLE));

				Assert.assertTrue(data.containsKey("SOURCE") && ((List<String>) data.get("SOURCE")).get(0).equals("AUDIOVISUAL-EC-EUROPA-EU"));
				Assert.assertTrue(data.containsKey("SITENAME") && ((List<String>) data.get("SITENAME")).get(0).equals("Audiovisual Service"));
				Assert.assertTrue(data.containsKey("SITEDESCRIPTION") && ((List<String>) data.get("SITEDESCRIPTION")).get(0).equals("European Commission Audiovisual Portal"));

			});

		} catch (IOException e) {
			LOGGER.info("ERROR", e);
		}

	}

	@Test
	public void filterHtmlBasicTest() throws IOException {

		Map<String, Object> configValues = new HashMap<>();
		configValues.put(PROPERTY_EXTRACT_CONTENT, true);

		Configuration config = new ConfigurationImpl(configValues);
		HtmlPlugin htmlFilter = new HtmlPlugin("thread_id", config, null);

		ObjectMapper mapper = new ObjectMapper();
		String path = this.getClass().getClassLoader().getResource("5bd7a6b6-ce3f-43f4-89cc-43052ceb6b2a.json").getFile();
		File file = new File(path);

		try {
			Map<String, Object> map = mapper.readValue(new FileInputStream(file), Map.class);
			Event e = new org.logstash.Event();

			map.entrySet().stream().forEach(ee -> {
				e.setField(ee.getKey(), ee.getValue());
			});

			Collection<co.elastic.logstash.api.Event> results = htmlFilter.filter(Collections.singletonList(e), null);

			Assert.assertFalse(results.isEmpty());

			results.stream().forEach(eee -> {

				Map<String, Object> data = eee.getData();

				Assert.assertTrue(data.containsKey(METADATA_URL));
				Assert.assertTrue(data.containsKey(METADATA_CONTENT));
				Assert.assertTrue(data.containsKey(METADATA_LANGUAGES));
				Assert.assertTrue(data.containsKey(METADATA_REFERENCE));
				Assert.assertTrue(data.containsKey(METADATA_CONTENT));
				Assert.assertTrue(data.containsKey(METADATA_TITLE));

				String content = new String(Base64.getDecoder().decode(data.get(METADATA_CONTENT).toString()));
				Assert.assertTrue(content.contains("EURAXESS"));

			});

		} catch (IOException e) {
			LOGGER.info("ERROR", e);
		}

	}

	@Test
	public void filterHtmlTitleFromRegex1Test() throws IOException {

		Map<String, Object> configValues = new HashMap<>();
		configValues.put(PROPERTY_EXTRACT_CONTENT, true);
		configValues.put(PROPERTY_EXTRACT_TITLE_REGEX, REGEX_SET_1);

		Configuration config = new ConfigurationImpl(configValues);
		HtmlPlugin htmlFilter = new HtmlPlugin("thread_id", config, null);

		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("html_title_1.html");
		String encodedContent = java.util.Base64.getEncoder().encodeToString(IOUtils.toByteArray(inputStream));
		inputStream.close();

		Event e = new org.logstash.Event();
		e.setField(METADATA_REFERENCE, METADATA_REFERENCE);
		e.setField(METADATA_CONTENT, encodedContent);
		e.setField(METADATA_URL, "http://localhost/test");

		Collection<co.elastic.logstash.api.Event> results = htmlFilter.filter(Collections.singletonList(e), null);

		Assert.assertFalse(results.isEmpty());

		results.stream().forEach(eee -> {

			Map<String, Object> data = eee.getData();

			Assert.assertTrue(data.containsKey(METADATA_TITLE));

			String title = data.get(METADATA_TITLE).toString();
			Assert.assertTrue(title.equals("Together Against Trafficking in Human Beings 1"));
		});

	}

	@Test
	public void filterHtmlTitleFromRegex2Test() throws IOException {

		Map<String, Object> configValues = new HashMap<>();
		configValues.put(PROPERTY_EXTRACT_CONTENT, true);
		configValues.put(PROPERTY_EXTRACT_TITLE_REGEX, REGEX_SET_1);

		Configuration config = new ConfigurationImpl(configValues);
		HtmlPlugin htmlFilter = new HtmlPlugin("thread_id", config, null);

		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("html_title_2.html");
		String encodedContent = java.util.Base64.getEncoder().encodeToString(IOUtils.toByteArray(inputStream));
		inputStream.close();

		Event e = new org.logstash.Event();
		e.setField(METADATA_REFERENCE, METADATA_REFERENCE);
		e.setField(METADATA_CONTENT, encodedContent);
		e.setField(METADATA_URL, "http://localhost/test");

		Collection<co.elastic.logstash.api.Event> results = htmlFilter.filter(Collections.singletonList(e), null);

		Assert.assertFalse(results.isEmpty());

		results.stream().forEach(eee -> {

			Map<String, Object> data = eee.getData();

			Assert.assertTrue(data.containsKey(METADATA_TITLE));

			String title = data.get(METADATA_TITLE).toString();
			Assert.assertTrue(title.equals("Together Against Trafficking in Human Beings 2"));
		});

	}

	@Test
	public void filterHtmlTitleFromRegex3Test() throws IOException {

		Map<String, Object> configValues = new HashMap<>();
		configValues.put(PROPERTY_EXTRACT_CONTENT, true);
		configValues.put(PROPERTY_EXTRACT_TITLE_REGEX, REGEX_SET_1);

		Configuration config = new ConfigurationImpl(configValues);
		HtmlPlugin htmlFilter = new HtmlPlugin("thread_id", config, null);

		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("html_title_3.html");
		String encodedContent = java.util.Base64.getEncoder().encodeToString(IOUtils.toByteArray(inputStream));
		inputStream.close();

		Event e = new org.logstash.Event();
		e.setField(METADATA_REFERENCE, METADATA_REFERENCE);
		e.setField(METADATA_CONTENT, encodedContent);
		e.setField(METADATA_URL, "http://localhost/test");

		Collection<co.elastic.logstash.api.Event> results = htmlFilter.filter(Collections.singletonList(e), null);

		Assert.assertFalse(results.isEmpty());

		results.stream().forEach(eee -> {

			Map<String, Object> data = eee.getData();

			Assert.assertTrue(data.containsKey(METADATA_TITLE));

			String title = data.get(METADATA_TITLE).toString();
			Assert.assertTrue(title.equals("Together Against Trafficking in Human Beings | 3"));
		});

	}

	@Test
	public void filterHtmlContentFromRegex1Test() throws IOException {

		Map<String, Object> configValues = new HashMap<>();
		configValues.put(PROPERTY_EXTRACT_CONTENT, true);
		configValues.put(PROPERTY_EXTRACT_CONTENT_REGEX, REGEX_SET_1);

		Configuration config = new ConfigurationImpl(configValues);
		HtmlPlugin htmlFilter = new HtmlPlugin("thread_id", config, null);

		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("html_title_1.html");
		String encodedContent = java.util.Base64.getEncoder().encodeToString(IOUtils.toByteArray(inputStream));
		inputStream.close();

		Event e = new org.logstash.Event();
		e.setField(METADATA_REFERENCE, METADATA_REFERENCE);
		e.setField(METADATA_CONTENT, encodedContent);
		e.setField(METADATA_URL, "http://localhost/test");

		Collection<co.elastic.logstash.api.Event> results = htmlFilter.filter(Collections.singletonList(e), null);

		Assert.assertFalse(results.isEmpty());

		results.stream().forEach(eee -> {

			Map<String, Object> data = eee.getData();

			Assert.assertTrue(data.containsKey(METADATA_CONTENT));

			String content = new String(Base64.getDecoder().decode(data.get(METADATA_CONTENT).toString()));
			Assert.assertTrue(content.equals("Together Against Trafficking in Human Beings 1"));
		});

	}

	@Test
	public void filterHtmlContentFromRegex2Test() throws IOException {

		Map<String, Object> configValues = new HashMap<>();
		configValues.put(PROPERTY_EXTRACT_CONTENT, true);
		configValues.put(PROPERTY_EXTRACT_CONTENT_REGEX, REGEX_SET_1);

		Configuration config = new ConfigurationImpl(configValues);
		HtmlPlugin htmlFilter = new HtmlPlugin("thread_id", config, null);

		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("html_title_2.html");
		String encodedContent = java.util.Base64.getEncoder().encodeToString(IOUtils.toByteArray(inputStream));
		inputStream.close();

		Event e = new org.logstash.Event();
		e.setField(METADATA_REFERENCE, METADATA_REFERENCE);
		e.setField(METADATA_CONTENT, encodedContent);
		e.setField(METADATA_URL, "http://localhost/test");

		Collection<co.elastic.logstash.api.Event> results = htmlFilter.filter(Collections.singletonList(e), null);

		Assert.assertFalse(results.isEmpty());

		results.stream().forEach(eee -> {

			Map<String, Object> data = eee.getData();

			Assert.assertTrue(data.containsKey(METADATA_CONTENT));

			String content = new String(Base64.getDecoder().decode(data.get(METADATA_CONTENT).toString()));
			Assert.assertTrue(content.equals("Together Against Trafficking in Human Beings 2"));
		});

	}

	@Test
	public void filterHtmlContentFromRegex3Test() throws IOException {

		Map<String, Object> configValues = new HashMap<>();
		configValues.put(PROPERTY_EXTRACT_CONTENT, true);
		configValues.put(PROPERTY_EXTRACT_CONTENT_REGEX, REGEX_SET_1);

		Configuration config = new ConfigurationImpl(configValues);
		HtmlPlugin htmlFilter = new HtmlPlugin("thread_id", config, null);

		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("html_title_3.html");
		String encodedContent = java.util.Base64.getEncoder().encodeToString(IOUtils.toByteArray(inputStream));
		inputStream.close();

		Event e = new org.logstash.Event();
		e.setField(METADATA_REFERENCE, METADATA_REFERENCE);
		e.setField(METADATA_CONTENT, encodedContent);
		e.setField(METADATA_URL, "http://localhost/test");

		Collection<co.elastic.logstash.api.Event> results = htmlFilter.filter(Collections.singletonList(e), null);

		Assert.assertFalse(results.isEmpty());

		results.stream().forEach(eee -> {

			Map<String, Object> data = eee.getData();

			Assert.assertTrue(data.containsKey(METADATA_CONTENT));

			String content = new String(Base64.getDecoder().decode(data.get(METADATA_CONTENT).toString()));
			Assert.assertTrue(content.equals("Together Against Trafficking in Human Beings | 3"));
		});

	}

	@Test
	public void filterMetadataContentFromRegex1Test() throws IOException {

		Map<String, Object> configValues = new HashMap<>();
		configValues.put(PROPERTY_EXTRACT_CONTENT, true);
		configValues.put(PROPERTY_EXTRACT_METADATA_REGEX, REGEX_SET_2);

		Configuration config = new ConfigurationImpl(configValues);
		HtmlPlugin htmlFilter = new HtmlPlugin("thread_id", config, null);

		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("html_title_1.html");
		String encodedContent = java.util.Base64.getEncoder().encodeToString(IOUtils.toByteArray(inputStream));
		inputStream.close();

		Event e = new org.logstash.Event();
		e.setField(METADATA_REFERENCE, METADATA_REFERENCE);
		e.setField(METADATA_CONTENT, encodedContent);
		e.setField(METADATA_URL, "http://localhost/test");

		Collection<co.elastic.logstash.api.Event> results = htmlFilter.filter(Collections.singletonList(e), null);

		Assert.assertFalse(results.isEmpty());

		results.stream().forEach(eee -> {

			Map<String, Object> data = eee.getData();

			Assert.assertTrue(data.containsKey(METADATA_CONTENT));

			String siteName = data.get("SITENAME").toString();
			Assert.assertTrue(siteName.equals("[Together Against Trafficking in Human Beings - European Commission]"));
		});

	}
}
