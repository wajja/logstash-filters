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

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.logstash.Event;
import org.logstash.plugins.ConfigurationImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.elastic.logstash.api.Configuration;

public class FilterTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(FilterTest.class);

	@Test
	public void filterMetadataTest() {

		Map<String, Object> configValues = new HashMap<>();
		configValues.put(EuropaFilter.METADATA, Arrays.asList("SOURCE=AUDIOVISUAL-EC-EUROPA-EU", "SITENAME=Audiovisual Service", "SITEDESCRIPTION=European Commission Audiovisual Portal"));

		Configuration config = new ConfigurationImpl(configValues);
		EuropaFilter europaFilter = new EuropaFilter("thread_id", config, null);

		ObjectMapper mapper = new ObjectMapper();
		String path = this.getClass().getClassLoader().getResource("15f5ad9b-ccd4-46dd-8889-679e3880d83d.json").getFile();
		File file = new File(path);

		try {
			Map<String, Object> map = mapper.readValue(new FileInputStream(file), Map.class);
			Event e = new org.logstash.Event();

			map.entrySet().stream().forEach(ee -> {
				e.setField(ee.getKey(), ee.getValue());
			});

			Collection<co.elastic.logstash.api.Event> results = europaFilter.filter(Collections.singletonList(e), null);
			Assert.assertFalse(results.isEmpty());

			results.stream().forEach(eee -> {

				Map<String, Object> data = eee.getData();

				Assert.assertTrue(data.containsKey(EuropaFilter.METADATA_URL));
				Assert.assertTrue(data.containsKey(EuropaFilter.METADATA_CONTENT));
				Assert.assertTrue(data.containsKey(EuropaFilter.METADATA_LANGUAGES));
				Assert.assertTrue(data.containsKey(EuropaFilter.METADATA_REFERENCE));
				Assert.assertTrue(data.containsKey(EuropaFilter.METADATA_ACL_USERS));
				Assert.assertTrue(data.containsKey(EuropaFilter.METADATA_ACL_NO_USERS));
				Assert.assertTrue(data.containsKey(EuropaFilter.METADATA_ACL_GROUPS));
				Assert.assertTrue(data.containsKey(EuropaFilter.METADATA_ACL_NO_GROUPS));
				Assert.assertTrue(data.containsKey(EuropaFilter.METADATA_TITLE));
				Assert.assertTrue(data.containsKey(EuropaFilter.METADATA));

				Map<String, List<String>> metadataMap = (Map) data.get(EuropaFilter.METADATA);
				
				Assert.assertTrue(metadataMap.containsKey("SOURCE") && metadataMap.get("SOURCE").get(0).equals("AUDIOVISUAL-EC-EUROPA-EU"));
				Assert.assertTrue(metadataMap.containsKey("SITENAME") && metadataMap.get("SITENAME").get(0).equals("Audiovisual Service"));
				Assert.assertTrue(metadataMap.containsKey("SITEDESCRIPTION") && metadataMap.get("SITEDESCRIPTION").get(0).equals("European Commission Audiovisual Portal"));
				
			});

		} catch (IOException e) {
			LOGGER.info("ERROR", e);
		}

	}

	@Ignore
	@Test
	public void filterJsonTest() {

		Map<String, Object> configValues = new HashMap<>();
		Configuration config = new ConfigurationImpl(configValues);
		EuropaFilter europaFilter = new EuropaFilter("thread_id", config, null);

		ObjectMapper mapper = new ObjectMapper();
		String path = this.getClass().getClassLoader().getResource("15f5ad9b-ccd4-46dd-8889-679e3880d83d.json").getFile();
		File file = new File(path);

		try {
			Map<String, Object> map = mapper.readValue(new FileInputStream(file), Map.class);
			Event e = new org.logstash.Event();

			map.entrySet().stream().forEach(ee -> {
				e.setField(ee.getKey(), ee.getValue());
			});

			Collection<co.elastic.logstash.api.Event> results = europaFilter.filter(Collections.singletonList(e), null);

			Assert.assertFalse(results.isEmpty());

			results.stream().forEach(eee -> {

				Map<String, Object> data = eee.getData();

				Assert.assertTrue(data.containsKey(EuropaFilter.METADATA_URL));
				Assert.assertTrue(data.containsKey(EuropaFilter.METADATA_CONTENT));
				Assert.assertTrue(data.containsKey(EuropaFilter.METADATA_LANGUAGES));
				Assert.assertTrue(data.containsKey(EuropaFilter.METADATA_REFERENCE));
				Assert.assertTrue(data.containsKey(EuropaFilter.METADATA_ACL_USERS));
				Assert.assertTrue(data.containsKey(EuropaFilter.METADATA_ACL_NO_USERS));
				Assert.assertTrue(data.containsKey(EuropaFilter.METADATA_ACL_GROUPS));
				Assert.assertTrue(data.containsKey(EuropaFilter.METADATA_ACL_NO_GROUPS));
				Assert.assertTrue(data.containsKey(EuropaFilter.METADATA_TITLE));

				String rubyString = (String) data.get(EuropaFilter.METADATA_TITLE);
				Assert.assertTrue(rubyString.equals("1082"));

			});

		} catch (IOException e) {
			LOGGER.info("ERROR", e);
		}

	}

	@Ignore
	@Test
	public void filterHtmlTest() {

		Map<String, Object> configValues = new HashMap<>();
		Configuration config = new ConfigurationImpl(configValues);
		EuropaFilter europaFilter = new EuropaFilter("thread_id", config, null);

		ObjectMapper mapper = new ObjectMapper();
		String path = this.getClass().getClassLoader().getResource("5bd7a6b6-ce3f-43f4-89cc-43052ceb6b2a.json").getFile();
		File file = new File(path);

		try {
			Map<String, Object> map = mapper.readValue(new FileInputStream(file), Map.class);
			Event e = new org.logstash.Event();

			map.entrySet().stream().forEach(ee -> {
				e.setField(ee.getKey(), ee.getValue());
			});

			Collection<co.elastic.logstash.api.Event> results = europaFilter.filter(Collections.singletonList(e), null);

			Assert.assertFalse(results.isEmpty());

			results.stream().forEach(eee -> {

				Map<String, Object> data = eee.getData();

				Assert.assertTrue(data.containsKey(EuropaFilter.METADATA_URL));
				Assert.assertTrue(data.containsKey(EuropaFilter.METADATA_CONTENT));
				Assert.assertTrue(data.containsKey(EuropaFilter.METADATA_LANGUAGES));
				Assert.assertTrue(data.containsKey(EuropaFilter.METADATA_REFERENCE));
				Assert.assertTrue(data.containsKey(EuropaFilter.METADATA_ACL_USERS));
				Assert.assertTrue(data.containsKey(EuropaFilter.METADATA_ACL_NO_USERS));
				Assert.assertTrue(data.containsKey(EuropaFilter.METADATA_ACL_GROUPS));
				Assert.assertTrue(data.containsKey(EuropaFilter.METADATA_ACL_NO_GROUPS));
				Assert.assertTrue(data.containsKey(EuropaFilter.METADATA_CONTENT));
				Assert.assertTrue(data.containsKey(EuropaFilter.METADATA_TITLE));

				byte[] bytes = (byte[]) data.get(EuropaFilter.METADATA_CONTENT);
				Assert.assertTrue(new String(bytes).contains("EURAXESS"));

				String rubyString = (String) data.get(EuropaFilter.METADATA_TITLE);
				Assert.assertTrue(rubyString.equals("search"));

			});

		} catch (IOException e) {
			LOGGER.info("ERROR", e);
		}

	}

}
