package eu.wajja.filter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.logstash.Event;
import org.logstash.plugins.ConfigurationImpl;

import co.elastic.logstash.api.Configuration;

public class EuropaPluginLanguageDetectionTest {

	private static final String METADATA_LANGUAGES = "languages";
	private static final String REFERENCE = "reference";
	private static final String CONTENT = "content";
	private static final String PDF1 = "applicationpdf_23903fb6-a0b2-4971-b111-bf0dc8addc7e";

	@SuppressWarnings("unchecked")
	@Test
	public void filterLanguageDetection1Test() throws IOException {

		Map<String, Object> configValues = new HashMap<>();
		Configuration config = new ConfigurationImpl(configValues);

		EuropaPlugin europaFilter = new EuropaPlugin(UUID.randomUUID().toString(), config, null);

		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(PDF1);
		String encodedContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
		inputStream.close();

		Event e = new org.logstash.Event();
		e.setField(REFERENCE, REFERENCE);
		e.setField(CONTENT, encodedContent);
		e.setField("url", "http://localhost/test/index_fr.html");

		Collection<co.elastic.logstash.api.Event> results = europaFilter.filter(Collections.singletonList(e), null);
		Assert.assertFalse(results.isEmpty());

		Event eee = (Event) results.stream().findFirst().orElse(new Event());
		Map<String, Object> data = eee.getData();
		List<String> languages = (List<String>) data.get(METADATA_LANGUAGES);

		Assert.assertTrue(languages.size() == 1);
		Assert.assertTrue(languages.get(0).equals("fr"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void filterLanguageDetection2Test() throws IOException {

		Map<String, Object> configValues = new HashMap<>();
		Configuration config = new ConfigurationImpl(configValues);

		EuropaPlugin europaFilter = new EuropaPlugin(UUID.randomUUID().toString(), config, null);

		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(PDF1);
		String encodedContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
		inputStream.close();

		Event e = new org.logstash.Event();
		e.setField(REFERENCE, REFERENCE);
		e.setField(CONTENT, encodedContent);
		e.setField("url", "http://localhost/test/en/index.html");

		Collection<co.elastic.logstash.api.Event> results = europaFilter.filter(Collections.singletonList(e), null);
		Assert.assertFalse(results.isEmpty());

		Event eee = (Event) results.stream().findFirst().orElse(new Event());
		Map<String, Object> data = eee.getData();
		List<String> languages = (List<String>) data.get(METADATA_LANGUAGES);

		Assert.assertTrue(languages.size() == 1);
		Assert.assertTrue(languages.get(0).equals("en"));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void filterLanguageDetection2Point1Test() throws IOException {

		Map<String, Object> configValues = new HashMap<>();
		Configuration config = new ConfigurationImpl(configValues);

		EuropaPlugin europaFilter = new EuropaPlugin(UUID.randomUUID().toString(), config, null);

		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(PDF1);
		String encodedContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
		inputStream.close();

		Event e = new org.logstash.Event();
		e.setField(REFERENCE, REFERENCE);
		e.setField(CONTENT, encodedContent);
		e.setField("url", "https://ec.europa.eu/maritimeaffairs/en/press/");

		Collection<co.elastic.logstash.api.Event> results = europaFilter.filter(Collections.singletonList(e), null);
		Assert.assertFalse(results.isEmpty());

		Event eee = (Event) results.stream().findFirst().orElse(new Event());
		Map<String, Object> data = eee.getData();
		List<String> languages = (List<String>) data.get(METADATA_LANGUAGES);

		Assert.assertTrue(languages.size() == 1);
		Assert.assertTrue(languages.get(0).equals("en"));
	}
	
	

	@SuppressWarnings("unchecked")
	@Test
	public void filterLanguageDetection3Test() throws IOException {

		Map<String, Object> configValues = new HashMap<>();
		Configuration config = new ConfigurationImpl(configValues);

		EuropaPlugin europaFilter = new EuropaPlugin(UUID.randomUUID().toString(), config, null);

		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(PDF1);
		String encodedContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
		inputStream.close();

		Event e = new org.logstash.Event();
		e.setField(REFERENCE, REFERENCE);
		e.setField(CONTENT, encodedContent);
		e.setField("url", "http://localhost/test/de");

		Collection<co.elastic.logstash.api.Event> results = europaFilter.filter(Collections.singletonList(e), null);
		Assert.assertFalse(results.isEmpty());

		Event eee = (Event) results.stream().findFirst().orElse(new Event());
		Map<String, Object> data = eee.getData();
		List<String> languages = (List<String>) data.get(METADATA_LANGUAGES);

		Assert.assertTrue(languages.size() == 1);
		Assert.assertTrue(languages.get(0).equals("de"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void filterLanguageDetection4Test() throws IOException {

		Map<String, Object> configValues = new HashMap<>();
		Configuration config = new ConfigurationImpl(configValues);

		EuropaPlugin europaFilter = new EuropaPlugin(UUID.randomUUID().toString(), config, null);

		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(PDF1);
		String encodedContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
		inputStream.close();

		Event e = new org.logstash.Event();
		e.setField(REFERENCE, REFERENCE);
		e.setField(CONTENT, encodedContent);
		e.setField("url", "http://localhost/test_nl/blah");

		Collection<co.elastic.logstash.api.Event> results = europaFilter.filter(Collections.singletonList(e), null);
		Assert.assertFalse(results.isEmpty());

		Event eee = (Event) results.stream().findFirst().orElse(new Event());
		Map<String, Object> data = eee.getData();
		List<String> languages = (List<String>) data.get(METADATA_LANGUAGES);

		Assert.assertTrue(languages.size() == 1);
		Assert.assertTrue(languages.get(0).equals("nl"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void filterLanguageDetection5Test() throws IOException {

		Map<String, Object> configValues = new HashMap<>();
		Configuration config = new ConfigurationImpl(configValues);
		EuropaPlugin europaFilter = new EuropaPlugin(UUID.randomUUID().toString(), config, null);

		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(PDF1);
		String encodedContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
		inputStream.close();

		Event e = new org.logstash.Event();
		e.setField(REFERENCE, REFERENCE);
		e.setField(CONTENT, encodedContent);
		e.setField("url", "http://localhost/test");
		e.setField(METADATA_LANGUAGES, Arrays.asList("sk"));

		Collection<co.elastic.logstash.api.Event> results = europaFilter.filter(Collections.singletonList(e), null);
		Assert.assertFalse(results.isEmpty());

		Event eee = (Event) results.stream().findFirst().orElse(new Event());
		Map<String, Object> data = eee.getData();
		List<String> languages = (List<String>) data.get(METADATA_LANGUAGES);

		Assert.assertTrue(languages.size() == 1);
		Assert.assertTrue(languages.toString().equals("ConvertedList{delegate=[sk]}"));
	}

	/**
	 * Let the API Detect the language
	 * 
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void filterLanguageDetection6Test() throws IOException {

		Map<String, Object> configValues = new HashMap<>();
		Configuration config = new ConfigurationImpl(configValues);

		EuropaPlugin europaFilter = new EuropaPlugin(UUID.randomUUID().toString(), config, null);

		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(PDF1);
		String encodedContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
		inputStream.close();

		Event e = new org.logstash.Event();
		e.setField(REFERENCE, REFERENCE);
		e.setField(CONTENT, encodedContent);
		e.setField("url", "http://localhost/test/blah");

		Collection<co.elastic.logstash.api.Event> results = europaFilter.filter(Collections.singletonList(e), null);
		Assert.assertFalse(results.isEmpty());

		Event eee = (Event) results.stream().findFirst().orElse(new Event());
		Map<String, Object> data = eee.getData();

		Assert.assertTrue(!data.containsKey(METADATA_LANGUAGES));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void filterLanguageDetection7Test() throws IOException {

		Map<String, Object> configValues = new HashMap<>();
		Configuration config = new ConfigurationImpl(configValues);

		EuropaPlugin europaFilter = new EuropaPlugin(UUID.randomUUID().toString(), config, null);

		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(PDF1);
		String encodedContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
		inputStream.close();

		Event e = new org.logstash.Event();
		e.setField(REFERENCE, REFERENCE);
		e.setField(CONTENT, encodedContent);
		e.setField("url", "https://ec.europa.eu/environment/nature/natura2000/platform/documents/first_marine_biogeographical_process_seminar/restitutions_day_3/c__group_1c_reporting_en.pdf");

		Collection<co.elastic.logstash.api.Event> results = europaFilter.filter(Collections.singletonList(e), null);
		Assert.assertFalse(results.isEmpty());

		Event eee = (Event) results.stream().findFirst().orElse(new Event());
		Map<String, Object> data = eee.getData();
		List<String> languages = (List<String>) data.get(METADATA_LANGUAGES);

		Assert.assertTrue(languages.size() == 1);
		Assert.assertTrue(languages.get(0).equals("en"));
	}
	
	
}
