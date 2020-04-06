package eu.wajja.filter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.wajja.web.fetcher.controller.URLController;
import eu.wajja.web.fetcher.enums.Command;
import eu.wajja.web.fetcher.model.Result;
import eu.wajja.web.fetcher.model.Robot;

public class HtmlFetcherJob {

	private static final Logger LOGGER = LoggerFactory.getLogger(HtmlFetcherJob.class);

	private static final String HTTP = "http://";
	private static final String HTTPS = "https://";

	public Result fetchContent(URLController urlController, Robot robot, String currentUrl, String rootUrl, Long depth, String crawlerUserAgent) {

		try {
			String urlString = getUrlString(currentUrl, rootUrl);

			Set<String> disallowedList = new HashSet<>();

			if (robot.getDisallowedLocations().containsKey("*")) {
				disallowedList = robot.getDisallowedLocations().get("*");
			}

			if (robot.getAllowedLocations().containsKey(crawlerUserAgent)) {
				disallowedList.addAll(robot.getAllowedLocations().get(crawlerUserAgent));
			}

			if (!disallowedList.isEmpty()) {

				String regex = (".*(" + String.join(")|(", disallowedList).replace("*", ".*").replace("/", "\\/") + ").*").trim();
				Pattern p = Pattern.compile(regex);
				Matcher m = p.matcher(urlString);

				if (m.find()) {
					return null;
				}
			}

			Result result = urlController.getURL(urlString, rootUrl);
			result.setReference(Base64.getEncoder().encodeToString(currentUrl.getBytes()));
			result.setEpochSecond(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC));
			result.setUrl(urlString);
			result.setRootUrl(rootUrl);
			result.setCommand(Command.ADD.toString());
			result.setDepth(depth);

			extractContent(result);

			return result;

		} catch (Exception e) {
			LOGGER.error("Failed to retrieve URL from url {}", currentUrl, e);
		}

		LOGGER.info("Finished Thread {}", currentUrl);

		return null;

	}

	private Result extractContent(Result result) throws IOException {

		String urlRootTmp = result.getRootUrl();

		if (result.getHeaders().containsKey("Content-Type") && result.getHeaders().get("Content-Type").get(0).contains("html") && result.getContent() != null) {

			byte[] content = Base64.getDecoder().decode(result.getContent());
			org.jsoup.nodes.Document document = Jsoup.parse(IOUtils.toString(content, StandardCharsets.UTF_8.name()));
			Elements elements = document.getElementsByAttribute("href");

			String simpleUrlString = result.getRootUrl().replace(HTTP, "").replace(HTTPS, "");

			List<String> childPages = elements.stream().map(e -> e.attr("href"))
					.filter(href -> !href.equals("/") && !href.startsWith("//"))
					.map(url -> getUrlString(url, urlRootTmp))
					.filter(href -> href.startsWith(HTTP) || href.startsWith(HTTPS))
					.filter(href -> href.startsWith(HTTP + simpleUrlString) || href.startsWith(HTTPS + simpleUrlString))
					.sorted()
					.collect(Collectors.toList());

			result.setChildPages(childPages);

		}

		return result;

	}

	private String getUrlString(String urlString, String rootUrl) {

		urlString = urlString.trim();

		try {
			if (!urlString.startsWith("http") && urlString.startsWith("/")) {

				URL urlRoot = new URL(rootUrl);
				String path = urlRoot.getPath();

				if (StringUtils.isEmpty(path) || path.equals("/")) {

					if (urlRoot.toString().endsWith("/") && urlString.startsWith("/")) {
						urlString = urlRoot + urlString.substring(1);
					} else {
						urlString = urlRoot + urlString;
					}

				} else {
					urlString = rootUrl.replace(path, "") + urlString;
				}

			} else if (!urlString.startsWith("http") && !urlString.startsWith("/")) {

				URL urlRoot = new URL(rootUrl);
				String path = urlRoot.getPath();

				if (StringUtils.isEmpty(path) || path.equals("/")) {

					urlString = urlRoot + "/" + urlString;
				} else {
					urlString = urlRoot.toString().substring(0, urlRoot.toString().lastIndexOf('/') + 1) + urlString;
				}
			}

			if (!urlString.startsWith("http") && !urlString.startsWith("/")) {
				urlString = rootUrl + urlString;
			}

			if (urlString.endsWith("/") && !urlString.equals(rootUrl)) {
				urlString = urlString.substring(0, urlString.lastIndexOf('/'));
			}

		} catch (MalformedURLException e) {
			LOGGER.error("Failed to parse url {}", urlString, e);
		}

		if (urlString.endsWith("/")) {
			urlString = urlString.substring(0, urlString.length() - 1);
		}

		return urlString;
	}

//	private void deleteOldDocuments(String dataFolder, String id) {
//
//		if (dataFolder == null) {
//			return;
//		}
//
//		Path indexPath = Paths.get(new StringBuilder(dataFolder).append("/fetched-data/").append(id).toString());
//
//		if (!indexPath.toFile().exists()) {
//			indexPath.toFile().mkdirs();
//		}
//
//		Path pathFile = Paths.get(new StringBuilder(dataFolder).append("/fetched-data/").append(id).append("_tmp.txt").toString());
//
//		if (pathFile.toFile().exists()) {
//
//			try {
//				List<String> legacyFile = Arrays.asList(objectMapper.readValue(pathFile.toFile(), String[].class));
//				List<String> urlsForDeletion = legacyFile.stream().filter(l -> !processedSet.contains(l)).collect(Collectors.toList());
//
//				LOGGER.info("Thread deleting {}, deleting {} urls", threadId, urlsForDeletion.size());
//
//				urlsForDeletion.stream().forEach(url -> {
//
//					String reference = Base64.getEncoder().encodeToString(url.getBytes());
//					LOGGER.info("Thread Sending Deletion {}, reference {}", threadId, reference);
//
//					Map<String, Object> metadata = new HashMap<>();
//					metadata.put(METADATA_REFERENCE, reference);
//					metadata.put(METADATA_COMMAND, Command.DELETE.toString());
//
////					consumer.accept(metadata);
//
//				});
//
//				Files.delete(pathFile);
//
//			} catch (IOException e) {
//				LOGGER.warn("Failed to read legacy file", e);
//			}
//		}
//
//		try {
//			String json = objectMapper.writeValueAsString(processedSet);
//			Files.write(pathFile, json.getBytes());
//		} catch (IOException e) {
//			LOGGER.warn("Failed to write tmp file to disk {}", pathFile, e);
//		}
//
//	}
}
