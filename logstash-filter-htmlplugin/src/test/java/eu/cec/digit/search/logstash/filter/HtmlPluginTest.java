package eu.cec.digit.search.logstash.filter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.jruby.RubyString;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.logstash.Event;
import org.logstash.plugins.ConfigurationImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rometools.rome.io.impl.Base64;

import co.elastic.logstash.api.Configuration;

public class HtmlPluginTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(HtmlPluginTest.class);
	private Properties properties;

	@Before
	public void intialize() throws IOException {

		properties = new Properties();
		properties.load(this.getClass().getClassLoader().getResourceAsStream("test.properties"));
	}

	@Test
	public void filterMetadataTest() throws IOException {

		Map<String, Object> configValues = new HashMap<>();
		configValues.put(HtmlPlugin.PROPERTY_METADATA, Arrays.asList("SOURCE=AUDIOVISUAL-EC-EUROPA-EU", "SITENAME=Audiovisual Service", "SITEDESCRIPTION=European Commission Audiovisual Portal"));
		configValues.put(HtmlPlugin.PROPERTY_EXPORT_CONTENT, true);
		configValues.put(HtmlPlugin.PROPERTY_EXTRACT_CONTENT, true);
		configValues.put(HtmlPlugin.PROPERTY_DATA_FOLDER, properties.get(HtmlPlugin.PROPERTY_DATA_FOLDER));

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

			e.setField(HtmlPlugin.METADATA_CONTEXT, "TEST_CONTEXT");
			e.setField(HtmlPlugin.METADATA_TYPE, "html");

			Collection<co.elastic.logstash.api.Event> results = htmlFilter.filter(Collections.singletonList(e), null);
			Assert.assertFalse(results.isEmpty());

			results.stream().forEach(eee -> {

				Map<String, Object> data = eee.getData();

				Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_URL));
				Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_CONTENT));
				Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_LANGUAGES));
				Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_REFERENCE));
				Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_ACL_USERS));
				Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_ACL_NO_USERS));
				Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_ACL_GROUPS));
				Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_ACL_NO_GROUPS));
				Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_TITLE));
				Assert.assertTrue(data.containsKey(HtmlPlugin.PROPERTY_METADATA));

				Map<String, List<String>> metadataMap = (Map) data.get(HtmlPlugin.PROPERTY_METADATA);

				Assert.assertTrue(metadataMap.containsKey("SOURCE") && metadataMap.get("SOURCE").get(0).equals("AUDIOVISUAL-EC-EUROPA-EU"));
				Assert.assertTrue(metadataMap.containsKey("SITENAME") && metadataMap.get("SITENAME").get(0).equals("Audiovisual Service"));
				Assert.assertTrue(metadataMap.containsKey("SITEDESCRIPTION") && metadataMap.get("SITEDESCRIPTION").get(0).equals("European Commission Audiovisual Portal"));

			});

		} catch (IOException e) {
			LOGGER.info("ERROR", e);
		}

	}

	@Test
	public void filterHtmlTest() throws IOException {

		Map<String, Object> configValues = new HashMap<>();
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

				Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_URL));
				Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_CONTENT));
				Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_LANGUAGES));
				Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_REFERENCE));
				Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_ACL_USERS));
				Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_ACL_NO_USERS));
				Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_ACL_GROUPS));
				Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_ACL_NO_GROUPS));
				Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_CONTENT));
				Assert.assertTrue(data.containsKey(HtmlPlugin.METADATA_TITLE));

				RubyString metadataContent = ((RubyString) data.get(HtmlPlugin.METADATA_CONTENT));
				String content = Base64.decode(metadataContent.toString());
				Assert.assertTrue(content.contains("EURAXESS"));

			});

		} catch (IOException e) {
			LOGGER.info("ERROR", e);
		}

	}

}
