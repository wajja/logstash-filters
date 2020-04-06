package eu.wajja.web.fetcher.controller;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.net.URL;

import javax.net.ssl.SSLException;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.wajja.web.fetcher.model.Result;

public class URLController {

	private static final Logger LOGGER = LoggerFactory.getLogger(URLController.class);

	private Proxy proxy;
	private Long timeout;
	private String userAgent;
	private String referer;
	private String chromeDriver;
	private WebDriverController webDriverController = new WebDriverController();

	public URLController(Proxy proxy, Long timeout, String userAgent, String referer, String chromeDriver) {

		this.proxy = proxy;
		this.timeout = timeout;
		this.userAgent = userAgent;
		this.referer = referer;
		this.chromeDriver = chromeDriver;
	}

	public Result getURL(String currentUrl, String initialUrl) {

		HttpURLConnection httpURLConnection = null;
		Result result = new Result();

		try {

			URL url = new URL(currentUrl);

			if (proxy == null) {
				httpURLConnection = (HttpURLConnection) url.openConnection();
			} else {
				httpURLConnection = (HttpURLConnection) url.openConnection(proxy);
			}

			httpURLConnection.setConnectTimeout(timeout.intValue());
			httpURLConnection.setReadTimeout(timeout.intValue());
			httpURLConnection.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
			httpURLConnection.addRequestProperty("User-Agent", userAgent);
			httpURLConnection.addRequestProperty("Referer", referer);

			httpURLConnection.connect();

			int code = httpURLConnection.getResponseCode();
			String message = httpURLConnection.getResponseMessage();
			result.setCode(code);
			result.setMessage(message);
			result.setUrl(currentUrl);
			result.setRootUrl(initialUrl);

			if (code == HttpURLConnection.HTTP_OK) {

				closeConnection(httpURLConnection);
				result.setHeaders(httpURLConnection.getHeaderFields());

				webDriverController.getURL(result, chromeDriver);

			} else if (code == HttpURLConnection.HTTP_MOVED_TEMP || code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_SEE_OTHER) {

				String newUrl = httpURLConnection.getHeaderField("Location");
				closeConnection(httpURLConnection);

				LOGGER.debug("Redirect needed to :  {}", newUrl);
				return getURL(newUrl, initialUrl);

			} else {
				LOGGER.warn("Failed To Read status {}, url {}, message {}", code, url, message);
			}

		} catch (SocketTimeoutException e) {

			LOGGER.warn("Thread url {}, sleeping and trying again", currentUrl);
			closeConnection(httpURLConnection);
			return result;

		} catch (SSLException e) {

			LOGGER.warn("Thread url {}, SSLException error, sleeping and trying again", currentUrl, e);
			closeConnection(httpURLConnection);
			return result;

		} catch (Exception e) {
			LOGGER.error("Failed to retrieve URL url {}", currentUrl, e);
		} finally {
			closeConnection(httpURLConnection);
		}

		return result;
	}

	private void closeConnection(HttpURLConnection httpURLConnection) {

		if (httpURLConnection != null) {
			httpURLConnection.disconnect();
		}
	}

	private byte[] getInputStream(HttpURLConnection httpURLConnection) throws IOException {

		try (InputStream inputStream = httpURLConnection.getInputStream()) {
			return IOUtils.toByteArray(inputStream);
		} catch (IOException e) {
			LOGGER.error("Failed to retrive url {}", httpURLConnection.getURL().toString(), e);
		}

		return new byte[0];
	}

}
