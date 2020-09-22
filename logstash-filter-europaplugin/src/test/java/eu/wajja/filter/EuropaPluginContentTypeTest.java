package eu.wajja.filter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.logstash.Event;
import org.logstash.plugins.ConfigurationImpl;

import co.elastic.logstash.api.Configuration;

public class EuropaPluginContentTypeTest {

	@Test
	public void filterSimplifiedContentTypeTest() throws IOException {

		Map<String, Object> configValues = new HashMap<>();
		Configuration config = new ConfigurationImpl(configValues);

		Map<String, Object> map = new HashMap<>();
		map.put("pdf", "Adobe PDF");
		configValues.put("simplifiedContentType", map);

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

		Event eee = (Event) results.stream().findFirst().orElse(new Event());
		Map<String, Object> data = eee.getData();

		Assert.assertTrue(data.get("SIMPLIFIED_CONTENT_TYPE").equals("Adobe PDF"));

	}

}
