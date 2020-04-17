package eu.wajja.filter;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.logstash.plugins.ConfigurationImpl;
import org.mockito.junit.MockitoJUnitRunner;

import co.elastic.logstash.api.Configuration;
import co.elastic.logstash.api.Event;

@RunWith(MockitoJUnitRunner.class)
public class ConfluenceTest {

	private static final String METADATA_TITLE = "title";
	private static final String METADATA_PAGE_NAME = "pageName";
	private static final String METADATA_FILE_NAME = "fileName";
	private static final String METADATA_REFERENCE = "reference";
	private static final String METADATA_CONTENT = "content";
	private static final String METADATA_URL = "url";
	private static final String METADATA_STATUS = "status";
	private static final String METADATA_CONTENT_TYPE = "contentType";
	private static final String METADATA_FETCHED_DATE = "fetchedDate";
	private static final String METADATA_MODIFIED_DATE = "modifiedDate";
	private static final String METADATA_MODIFIED_USER = "modifiedBy";
	private static final String METADATA_SPACE_ID = "spaceId";
	private static final String METADATA_SPACE_NAME = "spaceName";
	private static final String METADATA_SPACE_URL = "spaceUrl";
	private static final String METADATA_PARENT_ID = "parentId";
	private static final String METADATA_PARENT_NAME = "parentName";
	private static final String METADATA_PARENT_URL = "parentUrl";

	private static final String METADATA_MAP_AUTHOR = "AUTHOR";
	private static final String METADATA_MAP_CONFLUENCESPACE = "CONFLUENCESPACE";
	private static final String METADATA_MAP_DATAORIGIN = "DATAORIGIN";
	private static final String METADATA_MAP_DATE = "DATE";
	private static final String METADATA_MAP_DOCUMENT_TYPE = "DOCUMENT_TYPE";
	private static final String METADATA_MAP_PAGENAME = "PAGENAME";
	private static final String METADATA_MAP_PAGEURI = "PAGEURI";
	private static final String METADATA_MAP_SIMPLIFIED_CONTENT_TYPE = "SIMPLIFIED_CONTENT_TYPE";
	private static final String METADATA_MAP_SPACEURI = "SPACEURI";
	private static final String METADATA_MAP_SPACEHOME = "SPACEHOME";
	private static final String METADATA_MAP_TITLE = "TITLE";
	private static final String METADATA_MAP_CONTENT_TYPE = "CONTENT-TYPE";

	@Test
	public void filterConfluenceMappingTest() {

		Map<String, Object> configValues = new HashMap<>();
		configValues.put("simplifiedContentType", new HashMap<>());
		configValues.put("metadataCustom", new HashMap<>());

		Configuration config = new ConfigurationImpl(configValues);
		Confluence confluence = new Confluence(null, config, null);

		Collection<Event> events = new ArrayList<>();
		Map<String, Object> data = new HashMap<>();

		data.put(METADATA_URL, "http://localhost/test");
		data.put(METADATA_CONTENT, Base64.getEncoder().encodeToString("content".getBytes()));
		data.put(METADATA_CONTENT_TYPE, "confluence/test");

		data.put(METADATA_TITLE, METADATA_TITLE);
		data.put(METADATA_PAGE_NAME, METADATA_PAGE_NAME);
		data.put(METADATA_FILE_NAME, METADATA_FILE_NAME);
		data.put(METADATA_REFERENCE, METADATA_REFERENCE);
		data.put(METADATA_FETCHED_DATE, METADATA_FETCHED_DATE);
		data.put(METADATA_MODIFIED_DATE, METADATA_MODIFIED_DATE);
		data.put(METADATA_MODIFIED_USER, METADATA_MODIFIED_USER);
		data.put(METADATA_SPACE_ID, METADATA_SPACE_ID);
		data.put(METADATA_SPACE_NAME, METADATA_SPACE_NAME);
		data.put(METADATA_SPACE_URL, METADATA_SPACE_URL);
		data.put(METADATA_PARENT_ID, METADATA_PARENT_ID);
		data.put(METADATA_PARENT_NAME, METADATA_PARENT_NAME);
		data.put(METADATA_PARENT_URL, METADATA_PARENT_URL);

		events.add(new org.logstash.Event(data));
		events = confluence.filter(events, null);

		Map<String, Object> mapData = events.stream().findFirst().get().getData();

		Assert.assertTrue(mapData.get(METADATA_MAP_AUTHOR).toString().equals(METADATA_MODIFIED_USER));
		Assert.assertTrue(mapData.get(METADATA_MAP_CONFLUENCESPACE).toString().equals(METADATA_SPACE_NAME));
		Assert.assertTrue(mapData.get(METADATA_MAP_DATAORIGIN).toString().equals(METADATA_SPACE_ID));
		Assert.assertTrue(mapData.get(METADATA_MAP_DATE).toString().equals(METADATA_MODIFIED_DATE));
		Assert.assertTrue(mapData.get(METADATA_MAP_DOCUMENT_TYPE).toString().equals("confluence/test"));
		Assert.assertTrue(mapData.get(METADATA_MAP_PAGENAME).toString().equals(METADATA_PARENT_NAME));
		Assert.assertTrue(mapData.get(METADATA_MAP_PAGEURI).toString().equals(METADATA_PARENT_URL));
		Assert.assertTrue(mapData.get(METADATA_MAP_SPACEURI).toString().equals(METADATA_SPACE_URL));
		Assert.assertTrue(mapData.get(METADATA_MAP_SPACEHOME).toString().equals(METADATA_SPACE_NAME));
		Assert.assertTrue(mapData.get(METADATA_MAP_TITLE).toString().equals(METADATA_TITLE));
		Assert.assertTrue(mapData.get(METADATA_MAP_CONTENT_TYPE).toString().equals("text/plain"));
	}

	@Test
	public void filterConfluenceSimplifiedContentTypeTest() {

		Map<String, Object> configValues = new HashMap<>();
		Map<String, Object> map = new HashMap<>();
		map.put("plain", "Adobe PDF");

		configValues.put("metadataCustom", new HashMap<>());
		configValues.put("simplifiedContentType", map);

		Configuration config = new ConfigurationImpl(configValues);
		Confluence confluence = new Confluence(null, config, null);

		Collection<Event> events = new ArrayList<>();
		Map<String, Object> data = new HashMap<>();

		data.put(METADATA_URL, "http://localhost/test");
		data.put(METADATA_CONTENT, Base64.getEncoder().encodeToString("content".getBytes()));
		data.put(METADATA_CONTENT_TYPE, "confluence/test");

		data.put(METADATA_TITLE, METADATA_TITLE);
		data.put(METADATA_PAGE_NAME, METADATA_PAGE_NAME);
		data.put(METADATA_FILE_NAME, METADATA_FILE_NAME);
		data.put(METADATA_REFERENCE, METADATA_REFERENCE);
		data.put(METADATA_FETCHED_DATE, METADATA_FETCHED_DATE);
		data.put(METADATA_MODIFIED_DATE, METADATA_MODIFIED_DATE);
		data.put(METADATA_MODIFIED_USER, METADATA_MODIFIED_USER);
		data.put(METADATA_SPACE_ID, METADATA_SPACE_ID);
		data.put(METADATA_SPACE_NAME, METADATA_SPACE_NAME);
		data.put(METADATA_SPACE_URL, METADATA_SPACE_URL);
		data.put(METADATA_PARENT_ID, METADATA_PARENT_ID);
		data.put(METADATA_PARENT_NAME, METADATA_PARENT_NAME);
		data.put(METADATA_PARENT_URL, METADATA_PARENT_URL);

		events.add(new org.logstash.Event(data));
		events = confluence.filter(events, null);

		Map<String, Object> mapData = events.stream().findFirst().get().getData();

		Assert.assertTrue(mapData.get(METADATA_MAP_SIMPLIFIED_CONTENT_TYPE).toString().equals("Adobe PDF"));

	}

	@Test
	public void filterConfluenceCustomMetadataTest() {

		Map<String, Object> configValues = new HashMap<>();
		Map<String, Object> map = new HashMap<>();
		map.put("FIELD", "Adobe PDF");

		configValues.put("metadataCustom", map);
		configValues.put("simplifiedContentType", new HashMap<>());

		Configuration config = new ConfigurationImpl(configValues);
		Confluence confluence = new Confluence(null, config, null);
		
		Collection<Event> events = new ArrayList<>();
		Map<String, Object> data = new HashMap<>();

		data.put(METADATA_URL, "http://localhost/test");
		data.put(METADATA_CONTENT, Base64.getEncoder().encodeToString("content".getBytes()));
		data.put(METADATA_CONTENT_TYPE, "confluence/test");

		data.put(METADATA_TITLE, METADATA_TITLE);
		data.put(METADATA_PAGE_NAME, METADATA_PAGE_NAME);
		data.put(METADATA_FILE_NAME, METADATA_FILE_NAME);
		data.put(METADATA_REFERENCE, METADATA_REFERENCE);
		data.put(METADATA_FETCHED_DATE, METADATA_FETCHED_DATE);
		data.put(METADATA_MODIFIED_DATE, METADATA_MODIFIED_DATE);
		data.put(METADATA_MODIFIED_USER, METADATA_MODIFIED_USER);
		data.put(METADATA_SPACE_ID, METADATA_SPACE_ID);
		data.put(METADATA_SPACE_NAME, METADATA_SPACE_NAME);
		data.put(METADATA_SPACE_URL, METADATA_SPACE_URL);
		data.put(METADATA_PARENT_ID, METADATA_PARENT_ID);
		data.put(METADATA_PARENT_NAME, METADATA_PARENT_NAME);
		data.put(METADATA_PARENT_URL, METADATA_PARENT_URL);

		events.add(new org.logstash.Event(data));
		events = confluence.filter(events, null);

		Map<String, Object> mapData = events.stream().findFirst().get().getData();

		Assert.assertTrue(mapData.get("FIELD").toString().equals("Adobe PDF"));

	}

}
