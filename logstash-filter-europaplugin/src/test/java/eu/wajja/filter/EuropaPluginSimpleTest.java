package eu.wajja.filter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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

public class EuropaPluginSimpleTest {

	private static final String PDF1 = "applicationpdf_23903fb6-a0b2-4971-b111-bf0dc8addc7e";
	private static final String REFERENCE = "reference";
	private static final String CONTENT = "content";
	private static final String DATAFOLDER = "dataFolder";

	@SuppressWarnings("unchecked")
	@Test
	public void filterGeneralFilterBasicTest() throws IOException {

		Map<String, Object> configValues = new HashMap<>();

		URL url = this.getClass().getClassLoader().getResource(PDF1);
		File file = new File(url.getPath());
		configValues.put(DATAFOLDER, file.getParent());

		Configuration config = new ConfigurationImpl(configValues);

		EuropaPlugin europaFilter = new EuropaPlugin(UUID.randomUUID().toString(), config, null);

		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(PDF1);
		String encodedContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
		inputStream.close();

		Event e = new org.logstash.Event();
		e.setField(REFERENCE, REFERENCE);
		e.setField(CONTENT, encodedContent);
		e.setField("url", "http://test.ec.europa.eu/test");

		Collection<co.elastic.logstash.api.Event> results = europaFilter.filter(Collections.singletonList(e), null);
		Assert.assertFalse(results.isEmpty());

		Event eee = (Event) results.stream().findFirst().orElse(new Event());
		Map<String, Object> data = eee.getData();

		List<String> filters = (List<String>) data.get("GENERAL_FILTER");

		Assert.assertTrue(filters.size() == 2);
		Assert.assertTrue(filters.contains("European Commission"));
		Assert.assertTrue(filters.contains("General Information"));
		Assert.assertTrue(data.containsKey("DATE"));

	}
	
	   @SuppressWarnings("unchecked")
	    @Test
	    public void filterAdvancedTest() throws IOException {

	        Map<String, Object> configValues = new HashMap<>();

	        URL url = this.getClass().getClassLoader().getResource(PDF1);
	        File file = new File(url.getPath());
	        configValues.put(DATAFOLDER, file.getParent());

	        Configuration config = new ConfigurationImpl(configValues);

	        EuropaPlugin europaFilter = new EuropaPlugin(UUID.randomUUID().toString(), config, null);

	        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(PDF1);
	        String encodedContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
	        inputStream.close();

	        Event e = new org.logstash.Event();
	        e.setField(REFERENCE, REFERENCE);
	        e.setField(CONTENT, encodedContent);
	        e.setField("url", "https://ec.europa.eu/commission/commissioners/2014-2019/president/level2/president_juncker.html");

	        Collection<co.elastic.logstash.api.Event> results = europaFilter.filter(Collections.singletonList(e), null);
	        Assert.assertFalse(results.isEmpty());

	        Event eee = (Event) results.stream().findFirst().orElse(new Event());
	        Map<String, Object> data = eee.getData();

	        List<String> filters = (List<String>) data.get("GENERAL_FILTER");

	        Assert.assertTrue(filters.contains("FUNCTIONING OF THE EU"));
	        Assert.assertTrue(filters.contains("FUNCTIONING OF THE EU::INFORMATION AND COMMUNICATION OF THE EU"));
	        
	        Assert.assertTrue(data.containsKey("DATE"));

	    }

	@SuppressWarnings("unchecked")
	@Test
	public void filterGeneralFilterWithTopicsTest() throws IOException {

		Map<String, Object> configValues = new HashMap<>();

		URL url = this.getClass().getClassLoader().getResource(PDF1);
		File file = new File(url.getPath());
		configValues.put(DATAFOLDER, file.getParent());

		Configuration config = new ConfigurationImpl(configValues);

		EuropaPlugin europaFilter = new EuropaPlugin(UUID.randomUUID().toString(), config, null);

		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(PDF1);
		String encodedContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
		inputStream.close();

		Event e = new org.logstash.Event();
		e.setField(REFERENCE, REFERENCE);
		e.setField(CONTENT, encodedContent);
		e.setField("url", "http://ec.europa.eu/commission/commissioners/2014-2019/president/clinton");

		Collection<co.elastic.logstash.api.Event> results = europaFilter.filter(Collections.singletonList(e), null);
		Assert.assertFalse(results.isEmpty());

		Event eee = (Event) results.stream().findFirst().orElse(new Event());
		Map<String, Object> data = eee.getData();
		List<String> filters = (List<String>) data.get("GENERAL_FILTER");

		Assert.assertTrue(filters.size() == 5);
		Assert.assertTrue(filters.contains("European Commission"));
		Assert.assertTrue(filters.contains("General Information"));
		Assert.assertTrue(filters.contains("FUNCTIONING OF THE EU"));
		Assert.assertTrue(filters.contains("FUNCTIONING OF THE EU::INFORMATION AND COMMUNICATION OF THE EU"));
		Assert.assertTrue(filters.contains("FUNCTIONING OF THE EU::EU INSTITUTIONS ADMINISTRATION AND STAFF"));
		Assert.assertTrue(data.containsKey("DATE"));

	}

	@SuppressWarnings("unchecked")
	@Test
	public void filterRestrictedFilterTest() throws IOException {

		Map<String, Object> configValues = new HashMap<>();

		URL url = this.getClass().getClassLoader().getResource(PDF1);
		File file = new File(url.getPath());
		configValues.put(DATAFOLDER, file.getParent());

		Configuration config = new ConfigurationImpl(configValues);

		EuropaPlugin europaFilter = new EuropaPlugin(UUID.randomUUID().toString(), config, null);

		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(PDF1);
		String encodedContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
		inputStream.close();

		Event e = new org.logstash.Event();
		e.setField(REFERENCE, REFERENCE);
		e.setField(CONTENT, encodedContent);
		e.setField("url", "http://ec.europa.eu/agriculture/cap-overview/south");

		Collection<co.elastic.logstash.api.Event> results = europaFilter.filter(Collections.singletonList(e), null);
		Assert.assertFalse(results.isEmpty());

		Event eee = (Event) results.stream().findFirst().orElse(new Event());
		Map<String, Object> data = eee.getData();
		List<String> filters = (List<String>) data.get("RESTRICTED_FILTER");

		Assert.assertTrue(filters.size() == 2);
		Assert.assertTrue(filters.contains("AGRICULTURE::GENERAL INFORMATION"));
		Assert.assertTrue(filters.contains("AGRICULTURE::THE CAP"));
		Assert.assertTrue(data.containsKey("DATE"));

	}
	
	   @SuppressWarnings("unchecked")
	    @Test
	    public void filterRestrictedFilterWithLevelsTest() throws IOException {

	        Map<String, Object> configValues = new HashMap<>();

	        URL url = this.getClass().getClassLoader().getResource(PDF1);
	        File file = new File(url.getPath());
	        configValues.put(DATAFOLDER, file.getParent());

	        Configuration config = new ConfigurationImpl(configValues);

	        EuropaPlugin europaFilter = new EuropaPlugin(UUID.randomUUID().toString(), config, null);

	        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(PDF1);
	        String encodedContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
	        inputStream.close();

	        Event e = new org.logstash.Event();
	        e.setField(REFERENCE, REFERENCE);
	        e.setField(CONTENT, encodedContent);
	        e.setField("url", "http:////ec.europa.eu/agriculture/testw2filters/some/data/more.htm");

	        Collection<co.elastic.logstash.api.Event> results = europaFilter.filter(Collections.singletonList(e), null);
	        Assert.assertFalse(results.isEmpty());

	        Event eee = (Event) results.stream().findFirst().orElse(new Event());
	        Map<String, Object> data = eee.getData();
	        List<String> filters = (List<String>) data.get("RESTRICTED_FILTER");

	        Assert.assertTrue(filters.size() == 3);
	        Assert.assertTrue(filters.contains("AGRICULTURE::GENERAL INFORMATION"));
	        Assert.assertTrue(filters.contains("AGRICULTURE::FILTER1"));
	        Assert.assertTrue(filters.contains("AGRICULTURE::FILTER1::FILTER2"));
	        Assert.assertTrue(data.containsKey("DATE"));

	    }

	@SuppressWarnings("unchecked")
	@Test
	public void filtermetadataCustom1FilterTest() throws IOException {

		Map<String, Object> configValues = new HashMap<>();

		URL url = this.getClass().getClassLoader().getResource(PDF1);
		File file = new File(url.getPath());
		configValues.put(DATAFOLDER, file.getParent());

		Map<String, Object> map = new HashMap<>();
		map.put("http://#1", Arrays.asList("VALUE1", "VALUE2"));
		map.put("http://#2", Arrays.asList("VALUE3"));

		configValues.put("bestBetUrls", map);

		Configuration config = new ConfigurationImpl(configValues);

		EuropaPlugin europaFilter = new EuropaPlugin(UUID.randomUUID().toString(), config, null);

		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(PDF1);
		String encodedContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
		inputStream.close();

		Event e = new org.logstash.Event();
		e.setField(REFERENCE, REFERENCE);
		e.setField(CONTENT, encodedContent);
		e.setField("url", "http://#1");

		Collection<co.elastic.logstash.api.Event> results = europaFilter.filter(Collections.singletonList(e), null);
		Assert.assertFalse(results.isEmpty());

		Event eee = (Event) results.stream().findFirst().orElse(new Event());
		Map<String, Object> data = eee.getData();
		List<String> filters = (List<String>) data.get("SITETITLE");

		Assert.assertTrue(filters.size() == 2);
		Assert.assertTrue(filters.contains("VALUE1"));
		Assert.assertTrue(filters.contains("VALUE2"));

	}

	@SuppressWarnings("unchecked")
	@Test
	public void filtermetadataCustom2FilterTest() throws IOException {

		Map<String, Object> configValues = new HashMap<>();

		URL url = this.getClass().getClassLoader().getResource(PDF1);
		File file = new File(url.getPath());
		configValues.put(DATAFOLDER, file.getParent());

		Map<String, Object> map = new HashMap<>();
		map.put("http://#1", Arrays.asList("VALUE1", "VALUE2"));
		map.put("http://#2", Arrays.asList("VALUE3"));

		configValues.put("bestBetUrls", map);

		Configuration config = new ConfigurationImpl(configValues);

		EuropaPlugin europaFilter = new EuropaPlugin(UUID.randomUUID().toString(), config, null);

		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(PDF1);
		String encodedContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
		inputStream.close();

		Event e = new org.logstash.Event();
		e.setField(REFERENCE, REFERENCE);
		e.setField(CONTENT, encodedContent);
		e.setField("url", "http://#2");

		Collection<co.elastic.logstash.api.Event> results = europaFilter.filter(Collections.singletonList(e), null);
		Assert.assertFalse(results.isEmpty());

		Event eee = (Event) results.stream().findFirst().orElse(new Event());
		Map<String, Object> data = eee.getData();
		List<String> filters = (List<String>) data.get("SITETITLE");

		Assert.assertTrue(filters.size() == 1);
		Assert.assertTrue(filters.contains("VALUE3"));

	}

	@Test
	public void filtermetadataCustom3FilterTest() throws IOException {

		Map<String, Object> configValues = new HashMap<>();

		URL url = this.getClass().getClassLoader().getResource(PDF1);
		File file = new File(url.getPath());
		configValues.put(DATAFOLDER, file.getParent());

		Map<String, Object> map = new HashMap<>();
		map.put("http://#1", Arrays.asList("VALUE1", "VALUE2"));
		map.put("http://#2", Arrays.asList("VALUE3"));

		configValues.put("metadataCustom", map);

		Configuration config = new ConfigurationImpl(configValues);

		EuropaPlugin europaFilter = new EuropaPlugin(UUID.randomUUID().toString(), config, null);

		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(PDF1);
		String encodedContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
		inputStream.close();

		Event e = new org.logstash.Event();
		e.setField(REFERENCE, REFERENCE);
		e.setField(CONTENT, encodedContent);
		e.setField("url", "http://#3");

		Collection<co.elastic.logstash.api.Event> results = europaFilter.filter(Collections.singletonList(e), null);
		Assert.assertFalse(results.isEmpty());

		Event eee = (Event) results.stream().findFirst().orElse(new Event());
		Map<String, Object> data = eee.getData();
		Assert.assertFalse(data.containsKey("SITETITLE"));

	}
}
