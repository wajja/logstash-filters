package eu.wajja.web.fetcher.exception;

public class WebDriverException extends Exception {

	private static final long serialVersionUID = -8084344161926301953L;

	public WebDriverException() {
		super();
	}

	public WebDriverException(String message) {
		super(message);
	}

	public WebDriverException(String message, Throwable cause) {
		super(message, cause);
	}
}
