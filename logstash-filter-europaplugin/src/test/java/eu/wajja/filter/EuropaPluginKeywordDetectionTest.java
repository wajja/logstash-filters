package eu.wajja.filter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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

public class EuropaPluginKeywordDetectionTest {

	private static final String REFERENCE = "reference";
	private static final String CONTENT = "content";
	private static final String PDF1 = "applicationpdf_23903fb6-a0b2-4971-b111-bf0dc8addc7e";
	private static final String KEYWORDS = "KEYWORDS";
	private static final String KEYWORDS1 = "keyword001";
	private static final String KEYWORDS2 = "keyword002";
	private static final String LOCALHOST = "localhost";

	@SuppressWarnings("unchecked")
	@Test
	public void keywordSimpleHttpsDetectionTest() throws IOException {

		Map<String, Object> configValues = new HashMap<>();
		Configuration config = new ConfigurationImpl(configValues);
		EuropaPlugin europaFilter = new EuropaPlugin(UUID.randomUUID().toString(), config, null);

		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(PDF1);
		String encodedContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
		inputStream.close();

		Event e = new org.logstash.Event();
		e.setField(REFERENCE, REFERENCE);
		e.setField(CONTENT, encodedContent);
		e.setField("url", "https://localhost/keyword001/keyword002");

		Collection<co.elastic.logstash.api.Event> results = europaFilter.filter(Collections.singletonList(e), null);
		Assert.assertFalse(results.isEmpty());

		Event eee = (Event) results.stream().findFirst().orElse(new Event());
		Map<String, Object> data = eee.getData();

		Assert.assertTrue(data.containsKey(KEYWORDS));
		List<String> keywords = (List<String>) data.get(KEYWORDS);

		Assert.assertTrue(keywords.contains(KEYWORDS1));
		Assert.assertTrue(keywords.contains(KEYWORDS2));
		Assert.assertFalse(keywords.contains(LOCALHOST));

	}

	@SuppressWarnings("unchecked")
	@Test
	public void keywordSimpleDetectionTest() throws IOException {

		Map<String, Object> configValues = new HashMap<>();
		Configuration config = new ConfigurationImpl(configValues);
		EuropaPlugin europaFilter = new EuropaPlugin(UUID.randomUUID().toString(), config, null);

		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(PDF1);
		String encodedContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
		inputStream.close();

		Event e = new org.logstash.Event();
		e.setField(REFERENCE, REFERENCE);
		e.setField(CONTENT, encodedContent);
		e.setField("url", "http://localhost/keyword001/keyword002");

		Collection<co.elastic.logstash.api.Event> results = europaFilter.filter(Collections.singletonList(e), null);
		Assert.assertFalse(results.isEmpty());

		Event eee = (Event) results.stream().findFirst().orElse(new Event());
		Map<String, Object> data = eee.getData();

		Assert.assertTrue(data.containsKey(KEYWORDS));
		List<String> keywords = (List<String>) data.get(KEYWORDS);

		Assert.assertTrue(keywords.contains(KEYWORDS1));
		Assert.assertTrue(keywords.contains(KEYWORDS2));
		Assert.assertFalse(keywords.contains(LOCALHOST));

	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void keywordSimpleDetectionWithEuropaHostnameTest() throws IOException {

		Map<String, Object> configValues = new HashMap<>();
		Configuration config = new ConfigurationImpl(configValues);
		EuropaPlugin europaFilter = new EuropaPlugin(UUID.randomUUID().toString(), config, null);

		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(PDF1);
		String encodedContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
		inputStream.close();

		Event e = new org.logstash.Event();
		e.setField(REFERENCE, REFERENCE);
		e.setField(CONTENT, encodedContent);
		e.setField("url", "https://ec.europa.eu/maritimeaffairs/press/");

		Collection<co.elastic.logstash.api.Event> results = europaFilter.filter(Collections.singletonList(e), null);
		Assert.assertFalse(results.isEmpty());

		Event eee = (Event) results.stream().findFirst().orElse(new Event());
		Map<String, Object> data = eee.getData();

		Assert.assertTrue(data.containsKey(KEYWORDS));
		List<String> keywords = (List<String>) data.get(KEYWORDS);

		Assert.assertTrue(keywords.contains("maritimeaffairs"));
		Assert.assertTrue(keywords.contains("press"));
		Assert.assertFalse(keywords.contains("ec.europa.eu"));

	}

	@SuppressWarnings("unchecked")
	@Test
	public void keywordSimpleCaseSensitiveDetectionTest() throws IOException {

		Map<String, Object> configValues = new HashMap<>();
		Configuration config = new ConfigurationImpl(configValues);
		EuropaPlugin europaFilter = new EuropaPlugin(UUID.randomUUID().toString(), config, null);

		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(PDF1);
		String encodedContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
		inputStream.close();

		Event e = new org.logstash.Event();
		e.setField(REFERENCE, REFERENCE);
		e.setField(CONTENT, encodedContent);
		e.setField("url", "hTtp://locAlhost/keywoRd001/keywOrd002");

		Collection<co.elastic.logstash.api.Event> results = europaFilter.filter(Collections.singletonList(e), null);
		Assert.assertFalse(results.isEmpty());

		Event eee = (Event) results.stream().findFirst().orElse(new Event());
		Map<String, Object> data = eee.getData();

		Assert.assertTrue(data.containsKey(KEYWORDS));
		List<String> keywords = (List<String>) data.get(KEYWORDS);

		Assert.assertTrue(keywords.contains(KEYWORDS1));
		Assert.assertTrue(keywords.contains(KEYWORDS2));
		Assert.assertFalse(keywords.contains(LOCALHOST));

	}

	@SuppressWarnings("unchecked")
	@Test
	public void keywordSimpleTrailingSlashDetectionTest() throws IOException {

		Map<String, Object> configValues = new HashMap<>();
		Configuration config = new ConfigurationImpl(configValues);
		EuropaPlugin europaFilter = new EuropaPlugin(UUID.randomUUID().toString(), config, null);

		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(PDF1);
		String encodedContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
		inputStream.close();

		Event e = new org.logstash.Event();
		e.setField(REFERENCE, REFERENCE);
		e.setField(CONTENT, encodedContent);
		e.setField("url", "http://localhost/keyword001/keyword002/");

		Collection<co.elastic.logstash.api.Event> results = europaFilter.filter(Collections.singletonList(e), null);
		Assert.assertFalse(results.isEmpty());

		Event eee = (Event) results.stream().findFirst().orElse(new Event());
		Map<String, Object> data = eee.getData();

		Assert.assertTrue(data.containsKey(KEYWORDS));
		List<String> keywords = (List<String>) data.get(KEYWORDS);

		Assert.assertTrue(keywords.contains(KEYWORDS1));
		Assert.assertTrue(keywords.contains(KEYWORDS2));
		Assert.assertFalse(keywords.contains(LOCALHOST));

	}

	@Test
	public void keywordSimpleNoKeywordDetectionTest() throws IOException {

		Map<String, Object> configValues = new HashMap<>();
		Configuration config = new ConfigurationImpl(configValues);
		EuropaPlugin europaFilter = new EuropaPlugin(UUID.randomUUID().toString(), config, null);

		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(PDF1);
		String encodedContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
		inputStream.close();

		Event e = new org.logstash.Event();
		e.setField(REFERENCE, REFERENCE);
		e.setField(CONTENT, encodedContent);
		e.setField("url", "http://localhost/");

		Collection<co.elastic.logstash.api.Event> results = europaFilter.filter(Collections.singletonList(e), null);
		Assert.assertFalse(results.isEmpty());

		Event eee = (Event) results.stream().findFirst().orElse(new Event());
		Map<String, Object> data = eee.getData();

		Assert.assertFalse(data.containsKey(KEYWORDS));

	}

	@SuppressWarnings("unchecked")
	@Test
	public void keywordSimplePhraseDetectionTest() throws IOException {

		Map<String, Object> configValues = new HashMap<>();
		Configuration config = new ConfigurationImpl(configValues);
		EuropaPlugin europaFilter = new EuropaPlugin(UUID.randomUUID().toString(), config, null);

		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(PDF1);
		String encodedContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
		inputStream.close();

		Event e = new org.logstash.Event();
		e.setField(REFERENCE, REFERENCE);
		e.setField(CONTENT, encodedContent);
		e.setField("url", "http://localhost/the-keyword-001/keyword002");

		Collection<co.elastic.logstash.api.Event> results = europaFilter.filter(Collections.singletonList(e), null);
		Assert.assertFalse(results.isEmpty());

		Event eee = (Event) results.stream().findFirst().orElse(new Event());
		Map<String, Object> data = eee.getData();

		Assert.assertTrue(data.containsKey(KEYWORDS));
		List<String> keywords = (List<String>) data.get(KEYWORDS);

		Assert.assertTrue(keywords.contains("the keyword 001"));
		Assert.assertTrue(keywords.contains(KEYWORDS2));
		Assert.assertFalse(keywords.contains(LOCALHOST));

	}

}
