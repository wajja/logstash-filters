//package eu.wajja.filter;
//
//import java.net.MalformedURLException;
//import java.util.Arrays;
//import java.util.Base64;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import org.junit.Test;
//import org.mockito.Mockito;
//
//import eu.wajja.web.fetcher.controller.URLController;
//import eu.wajja.web.fetcher.controller.WebDriverController;
//import eu.wajja.web.fetcher.exception.WebDriverException;
//import eu.wajja.web.fetcher.model.Result;
//import eu.wajja.web.fetcher.model.Robot;
//
//public class HtmlFetcherJobTest {
//
//	private HtmlFetcherJob htmlFetcherJobTest = new HtmlFetcherJob();
//
//	private String webdriver = "http://10.69.105.17:3000/webdriver";
//	private Long timeout = 30000l;
//
//	@Test
//	public void filterHtmlMetadataTest() throws MalformedURLException, WebDriverException {
//
//		String currentUrl = "https://audiovisual.ec.europa.eu/en";
//
//		WebDriverController webDriverController = Mockito.mock(WebDriverController.class);
//		URLController urlController = Mockito.mock(URLController.class);
//
//		Result result = new Result();
//		result.setContent(Base64.getEncoder().encodeToString("<html><html>".getBytes()));
//
//		Map<String, List<String>> headers = new HashMap<>();
//		headers.put("Content-Type", Arrays.asList("html"));
//		result.setHeaders(headers);
//
//		Mockito.when(urlController.getWebDriverController()).thenReturn(webDriverController);
//		Mockito.when(urlController.getURL(currentUrl, currentUrl)).thenReturn(result);
//		Mockito.when(webDriverController.getURL(result)).thenReturn(result);
//
//		Robot robot = new Robot();
//
//		String rootUrl = currentUrl;
//		Long depth = null;
//		Long maxPagesCount = 100l;
//		String crawlerUserAgent = "testCrawler";
//		List<String> excludedDataRegex = Arrays.asList(".*.((PNG)|(png)|(ico)|(RSS)|(rss.xml)|(rss)|(mp3)|(png)|(PNG)|(xml)|(ico)|(css)|(js)|(mp4)|(jpg)|(JPG)|(jpeg)|(gif)|(zip)|(svg)|(splash_)|(2nd-language)|(site-language_)|(whatsapp)).*", ".*(page=).*", ".*search?.*]");
//		List<String> excludedLinkRegex = Arrays.asList(".*.((PNG)|(png)|(ico)|(mp3)|(png)|(PNG)|(ico)|(css)|(js)|(mp4)|(jpg)|(JPG)|(jpeg)|(gif)|(zip)|(svg)|(whatsapp)).*]");
//
//		result = htmlFetcherJobTest.fetchContent(urlController, robot, currentUrl, rootUrl, depth, maxPagesCount, crawlerUserAgent, excludedDataRegex, excludedLinkRegex);
//
//	}
//}