package eu.wajja.filter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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

import co.elastic.logstash.api.Configuration;

public class EuropaPluginTest {

	private Properties properties;

	@Before
	public void intialize() throws IOException {

		properties = new Properties();
		properties.load(this.getClass().getClassLoader().getResourceAsStream("test.properties"));
	}

	@Test
	public void filterSimplifiedContentTypeTest() throws IOException {

		Map<String, Object> configValues = new HashMap<>();
		Configuration config = new ConfigurationImpl(configValues);
		EuropaPlugin europaFilter = new EuropaPlugin(UUID.randomUUID().toString(), config, null);

		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("applicationpdf_23903fb6-a0b2-4971-b111-bf0dc8addc7e");
		String encodedContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
		inputStream.close();

		Event e = new org.logstash.Event();
		e.setField("reference", "reference");
		e.setField("content", encodedContent);
		e.setField("url", "http://localhost/test");

		Collection<co.elastic.logstash.api.Event> results = europaFilter.filter(Collections.singletonList(e), null);
		Assert.assertFalse(results.isEmpty());

		results.stream().forEach(eee -> {

			Map<String, Object> data = eee.getData();
			Assert.assertTrue(data.get("SIMPLIFIED_CONTENT_TYPE").equals("application/pdf"));

		});

	}

	@Test
	public void filterGeneralFilterBasicTest() throws IOException {

		Map<String, Object> configValues = new HashMap<>();

		URL url = this.getClass().getClassLoader().getResource("applicationpdf_23903fb6-a0b2-4971-b111-bf0dc8addc7e");
		File file = new File(url.getPath());
		configValues.put("dataFolder", file.getParent());

		Configuration config = new ConfigurationImpl(configValues);

		EuropaPlugin europaFilter = new EuropaPlugin(UUID.randomUUID().toString(), config, null);

		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("applicationpdf_23903fb6-a0b2-4971-b111-bf0dc8addc7e");
		String encodedContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
		inputStream.close();

		Event e = new org.logstash.Event();
		e.setField("reference", "reference");
		e.setField("content", encodedContent);
		e.setField("url", "http://test.ec.europa.eu/test");

		Collection<co.elastic.logstash.api.Event> results = europaFilter.filter(Collections.singletonList(e), null);
		Assert.assertFalse(results.isEmpty());

		results.stream().forEach(eee -> {

			Map<String, Object> data = eee.getData();
			List<String> filters = (List<String>) data.get("GENERAL_FILTER");

			Assert.assertTrue(filters.size() == 2);
			Assert.assertTrue(filters.contains("European Commission"));
			Assert.assertTrue(filters.contains("General Information"));
		});

	}

	@Test
	public void filterGeneralFilterWithTopicsTest() throws IOException {

		Map<String, Object> configValues = new HashMap<>();

		URL url = this.getClass().getClassLoader().getResource("applicationpdf_23903fb6-a0b2-4971-b111-bf0dc8addc7e");
		File file = new File(url.getPath());
		configValues.put("dataFolder", file.getParent());

		Configuration config = new ConfigurationImpl(configValues);

		EuropaPlugin europaFilter = new EuropaPlugin(UUID.randomUUID().toString(), config, null);

		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("applicationpdf_23903fb6-a0b2-4971-b111-bf0dc8addc7e");
		String encodedContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
		inputStream.close();

		Event e = new org.logstash.Event();
		e.setField("reference", "reference");
		e.setField("content", encodedContent);
		e.setField("url", "http://ec.europa.eu/commission/commissioners/2014-2019/president/clinton");

		Collection<co.elastic.logstash.api.Event> results = europaFilter.filter(Collections.singletonList(e), null);
		Assert.assertFalse(results.isEmpty());

		results.stream().forEach(eee -> {

			Map<String, Object> data = eee.getData();
			List<String> filters = (List<String>) data.get("GENERAL_FILTER");

			Assert.assertTrue(filters.size() == 4);
			Assert.assertTrue(filters.contains("European Commission"));
			Assert.assertTrue(filters.contains("General Information"));
			Assert.assertTrue(filters.contains("FUNCTIONING OF THE EU/INFORMATION AND COMMUNICATION OF THE EU"));
			Assert.assertTrue(filters.contains("FUNCTIONING OF THE EU/EU INSTITUTIONS ADMINISTRATION AND STAFF"));
		});

	}

	@Test
	public void filterRestrictedFilterTest() throws IOException {

		Map<String, Object> configValues = new HashMap<>();

		URL url = this.getClass().getClassLoader().getResource("applicationpdf_23903fb6-a0b2-4971-b111-bf0dc8addc7e");
		File file = new File(url.getPath());
		configValues.put("dataFolder", file.getParent());

		Configuration config = new ConfigurationImpl(configValues);

		EuropaPlugin europaFilter = new EuropaPlugin(UUID.randomUUID().toString(), config, null);

		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("applicationpdf_23903fb6-a0b2-4971-b111-bf0dc8addc7e");
		String encodedContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
		inputStream.close();

		Event e = new org.logstash.Event();
		e.setField("reference", "reference");
		e.setField("content", encodedContent);
		e.setField("url", "http://ec.europa.eu/agriculture/cap-overview/south");

		Collection<co.elastic.logstash.api.Event> results = europaFilter.filter(Collections.singletonList(e), null);
		Assert.assertFalse(results.isEmpty());

		results.stream().forEach(eee -> {

			Map<String, Object> data = eee.getData();
			List<String> filters = (List<String>) data.get("RESTRICTED_FILTER");

			Assert.assertTrue(filters.size() == 2);
			Assert.assertTrue(filters.contains("GENERAL INFORMATION"));
			Assert.assertTrue(filters.contains("THE CAP"));

		});

	}
}
