package eu.cec.digit.search.logstash.filter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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

public class OfficeFilterTest {

	private Properties properties;

	@Before
	public void intialize() throws IOException {

		properties = new Properties();
		properties.load(this.getClass().getClassLoader().getResourceAsStream("test.properties"));
	}

	@Test
	public void filter1Test() throws IOException {

		Map<String, Object> configValues = new HashMap<>();

		Configuration config = new ConfigurationImpl(configValues);
		configValues.put("metadata", Arrays.asList("META1=VALUE1", "META2=VALUE2"));

		OfficeFilter officeFilter = new OfficeFilter(UUID.randomUUID().toString(), config, null);

		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("applicationvnd.openxmlformats-officedocument.wordprocessingml.document_1e1b8fdd-bd9e-409f-a287-6464a54a463f");
		String encodedContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
		inputStream.close();

		Event e = new org.logstash.Event();
		e.setField("reference", "reference");
		e.setField("content", encodedContent);
		e.setField("url", "http://localhost/test");

		Collection<co.elastic.logstash.api.Event> results = officeFilter.filter(Collections.singletonList(e), null);
		Assert.assertFalse(results.isEmpty());

		results.stream().forEach(eee -> {

			Map<String, Object> data = eee.getData();
			Assert.assertFalse(data.containsKey("TITLE"));
			Assert.assertTrue(data.get("DATE").equals("2015-10-01T07:08:00Z"));
			Assert.assertTrue(data.get("CONTENT-TYPE").equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));

			Map<String, List<String>> metadata = ((Map<String, List<String>>) data.get("metadata"));
			Assert.assertTrue(metadata.containsKey("META1") && metadata.get("META1").get(0).equals("VALUE1"));
			Assert.assertTrue(metadata.containsKey("META2") && metadata.get("META2").get(0).equals("VALUE2"));

			Assert.assertFalse(data.containsKey("languages"));
		});

	}

	@Test
	public void filter2Test() throws IOException {

		Map<String, Object> configValues = new HashMap<>();

		Configuration config = new ConfigurationImpl(configValues);
		configValues.put("metadata", Arrays.asList("META1=VALUE1", "META2=VALUE2"));

		OfficeFilter officeFilter = new OfficeFilter(UUID.randomUUID().toString(), config, null);

		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("applicationvnd.openxmlformats-officedocument.wordprocessingml.document_6d9006b5-e146-4550-aec6-eb728e69077f");
		String encodedContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
		inputStream.close();

		Event e = new org.logstash.Event();
		e.setField("reference", "reference");
		e.setField("content", encodedContent);
		e.setField("url", "http://localhost/test");

		Collection<co.elastic.logstash.api.Event> results = officeFilter.filter(Collections.singletonList(e), null);
		Assert.assertFalse(results.isEmpty());

		results.stream().forEach(eee -> {

			Map<String, Object> data = eee.getData();
			Assert.assertFalse(data.containsKey("TITLE"));
			Assert.assertTrue(data.get("DATE").equals("2015-03-23T15:19:00Z"));
			Assert.assertTrue(data.get("CONTENT-TYPE").equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));

			Map<String, List<String>> metadata = ((Map<String, List<String>>) data.get("metadata"));
			Assert.assertTrue(metadata.containsKey("META1") && metadata.get("META1").get(0).equals("VALUE1"));
			Assert.assertTrue(metadata.containsKey("META2") && metadata.get("META2").get(0).equals("VALUE2"));

			Assert.assertTrue(((List<String>) data.get("languages")).get(0).equals("da"));
		});

	}

}
