package eu.cec.digit.search.logstash.filter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.logstash.Event;
import org.logstash.plugins.ConfigurationImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import co.elastic.logstash.api.Configuration;

public class OfficeFilterTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(OfficeFilterTest.class);
	private Properties properties;

	@Before
	public void intialize() throws IOException {

		properties = new Properties();
		properties.load(this.getClass().getClassLoader().getResourceAsStream("test.properties"));
	}

	@Ignore
	@Test
	public void filterTest() throws IOException {

		Map<String, Object> configValues = new HashMap<>();

		Configuration config = new ConfigurationImpl(configValues);
		OfficeFilter htmlFilter = new OfficeFilter(UUID.randomUUID().toString(), config, null);

		ObjectMapper mapper = new ObjectMapper();
		String path = this.getClass().getClassLoader().getResource("15f5ad9b-ccd4-46dd-8889-679e3880d83d.json").getFile();
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
				// Assert Here

			});

		} catch (IOException e) {
			LOGGER.info("ERROR", e);
		}

	}

}
