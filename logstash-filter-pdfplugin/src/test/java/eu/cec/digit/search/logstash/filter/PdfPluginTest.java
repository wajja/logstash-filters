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

public class PdfPluginTest {

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

		PdfPlugin pdfFilter = new PdfPlugin(UUID.randomUUID().toString(), config, null);

		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("applicationpdf_23903fb6-a0b2-4971-b111-bf0dc8addc7e");
		String encodedContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
		inputStream.close();

		Event e = new org.logstash.Event();
		e.setField("reference", "reference");
		e.setField("content", encodedContent);
		e.setField("url", "http://localhost/test");

		Collection<co.elastic.logstash.api.Event> results = pdfFilter.filter(Collections.singletonList(e), null);
		Assert.assertFalse(results.isEmpty());

		results.stream().forEach(eee -> {

			Map<String, Object> data = eee.getData();
			Assert.assertTrue(data.get("TITLE").equals("Crosswalks between European marine habitat typologies_10.04.14_V3"));
			Assert.assertTrue(data.get("DATE").equals("2014-05-19T14:17:36Z"));
			Assert.assertTrue(data.get("CONTENT-TYPE").equals("application/pdf"));

			Map<String, List<String>> metadata = ((Map<String, List<String>>) data.get("metadata"));
			Assert.assertTrue(metadata.containsKey("META1") && metadata.get("META1").get(0).equals("VALUE1"));
			Assert.assertTrue(metadata.containsKey("META2") && metadata.get("META2").get(0).equals("VALUE2"));

			Assert.assertTrue(((List<String>) data.get("languages")).get(0).equals("en"));
		});

	}

	@Test
	public void filter2Test() throws IOException {

		Map<String, Object> configValues = new HashMap<>();

		Configuration config = new ConfigurationImpl(configValues);
		configValues.put("metadata", Arrays.asList("META1=VALUE1", "META2=VALUE2"));

		PdfPlugin pdfFilter = new PdfPlugin(UUID.randomUUID().toString(), config, null);

		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("applicationpdf_74173610-9cbc-477e-8974-4b7ea332e56a");
		String encodedContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
		inputStream.close();

		Event e = new org.logstash.Event();
		e.setField("reference", "reference");
		e.setField("content", encodedContent);
		e.setField("url", "http://localhost/test");

		Collection<co.elastic.logstash.api.Event> results = pdfFilter.filter(Collections.singletonList(e), null);
		Assert.assertFalse(results.isEmpty());

		results.stream().forEach(eee -> {

			Map<String, Object> data = eee.getData();
			Assert.assertFalse(data.containsKey("TITLE"));
			Assert.assertTrue(data.get("DATE").equals("2013-09-17T07:55:52Z"));
			Assert.assertTrue(data.get("CONTENT-TYPE").equals("application/pdf"));

			Map<String, List<String>> metadata = ((Map<String, List<String>>) data.get("metadata"));
			Assert.assertTrue(metadata.containsKey("META1") && metadata.get("META1").get(0).equals("VALUE1"));
			Assert.assertTrue(metadata.containsKey("META2") && metadata.get("META2").get(0).equals("VALUE2"));

			Assert.assertTrue(((List<String>) data.get("languages")).get(0).equals("en"));
		});

	}

}
