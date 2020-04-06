package eu.wajja.filter;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.wajja.web.fetcher.controller.URLController;
import eu.wajja.web.fetcher.model.Result;
import eu.wajja.web.fetcher.model.Robot;

public class HtmlRobotJob {

	private static final Logger LOGGER = LoggerFactory.getLogger(HtmlRobotJob.class);

	private static final String ROBOTS = "robots.txt";

	private HtmlRobotJob() {
	}

	public static Robot readRobot(URLController urlController, Boolean readRobot, String initialUrl) {

		Robot robot = new Robot();

		if (readRobot) {

			Pattern p = Pattern.compile("(http).*(\\/\\/)[^\\/]{2,}(\\/)");
			Matcher m = p.matcher(initialUrl);

			if (m.find()) {

				String robotUrl = m.group(0) + ROBOTS;
				Result result = urlController.getURL(robotUrl, initialUrl);

				if (result.getContent() != null) {
					readRobotContent(robot, robotUrl, result);
				} else {
					LOGGER.warn("Failed to read robot.txt url, status {}, {}, {}", result.getCode(), initialUrl, result.getMessage());
				}

			} else {
				LOGGER.warn("Failed to find robot.txt url {}", initialUrl);
			}

		}

		return robot;

	}

	private static void readRobotContent(Robot robot, String robotUrl, Result result) {

		byte[] content =  Base64.getDecoder().decode(result.getContent());
		
		try (Scanner scanner = new Scanner(IOUtils.toString(content, StandardCharsets.UTF_8.name()))) {

			String userAgent = "*";

			while (scanner.hasNextLine()) {

				String line = scanner.nextLine().trim();

				if (!line.startsWith("#") && !line.isEmpty()) {

					if (line.startsWith("User-agent:")) {
						userAgent = line.replace("User-agent:", "").trim();

					} else if (line.startsWith("Disallow:")) {

						readDisallowedLocation(robot, userAgent, line);

					} else if (line.startsWith("Allow:")) {

						readAllowedLocation(robot, userAgent, line);

					} else if (line.startsWith("Sitemap:")) {

						readSitemapLocation(robot, userAgent, line);
					}
				}
			}

		} catch (Exception e) {
			LOGGER.error("Failed to parse robots.txt from url {}", robotUrl, e);
		}
	}

	private static void readSitemapLocation(Robot robot, String userAgent, String line) {

		String perm = line.replace("Sitemap:", "").trim();

		if (!robot.getSitemapLocations().containsKey(userAgent)) {
			robot.getSitemapLocations().put(userAgent, new HashSet<>());
		}

		robot.getSitemapLocations().get(userAgent).add(perm);
	}

	private static void readAllowedLocation(Robot robot, String userAgent, String line) {

		String perm = line.replace("Allow:", "").trim();

		if (!robot.getAllowedLocations().containsKey(userAgent)) {
			robot.getAllowedLocations().put(userAgent, new HashSet<>());
		}

		robot.getAllowedLocations().get(userAgent).add(perm);
	}

	private static void readDisallowedLocation(Robot robot, String userAgent, String line) {

		String perm = line.replace("Disallow:", "").trim();

		if (!robot.getDisallowedLocations().containsKey(userAgent)) {
			robot.getDisallowedLocations().put(userAgent, new HashSet<>());
		}

		robot.getDisallowedLocations().get(userAgent).add(perm);
	}
}
