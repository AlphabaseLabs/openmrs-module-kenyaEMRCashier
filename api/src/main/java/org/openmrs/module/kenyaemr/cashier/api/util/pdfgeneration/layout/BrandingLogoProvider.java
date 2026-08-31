package org.openmrs.module.kenyaemr.cashier.api.util.pdfgeneration.layout;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.Base64;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.layout.element.Image;
import org.apache.commons.lang.StringUtils;
import org.openmrs.api.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Loads the shared branding logo with bounded file and network I/O. */
public final class BrandingLogoProvider {

	static final String GP_BRANDING_CONFIGURATION = "alphabaseadmin.configurations.branding";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private static final int MAX_LOGO_BYTES = 1024 * 1024;

	private static final int CONNECT_TIMEOUT_MILLIS = 3000;

	private static final int READ_TIMEOUT_MILLIS = 5000;

	private BrandingLogoProvider() {
	}

	public static Image loadConfiguredLogo() {
		return loadLocation(getConfiguredLogoLocation());
	}

	static String getConfiguredLogoLocation() {
		try {
			String json = Context.getAdministrationService().getGlobalProperty(GP_BRANDING_CONFIGURATION);
			return resolveLogoLocation(json);
		}
		catch (Exception e) {
			logger().warn("Failed to read the shared branding configuration", e);
			return "";
		}
	}

	public static Image loadLocation(String location) {
		if (StringUtils.isBlank(location)) {
			return null;
		}
		try {
			byte[] bytes = isHttpUrl(location) ? readUrl(new URL(location)) : readFileOrResource(location);
			return bytes == null ? null : new Image(ImageDataFactory.create(bytes));
		}
		catch (Exception e) {
			logger().warn("Failed to load logo from configured location: " + location, e);
			return null;
		}
	}

	public static Image loadBase64(String encodedLogo) {
		if (StringUtils.isBlank(encodedLogo)) {
			return null;
		}
		try {
			byte[] bytes = Base64.getDecoder().decode(encodedLogo);
			validateSize(bytes.length);
			return new Image(ImageDataFactory.create(bytes));
		}
		catch (Exception e) {
			logger().warn("Failed to decode configured base64 logo", e);
			return null;
		}
	}

	static String resolveLogoLocation(String json) throws IOException {
		if (StringUtils.isBlank(json)) {
			return "";
		}
		JsonNode root = OBJECT_MAPPER.readTree(json);
		if (root == null || !root.isObject()) {
			throw new IOException("Branding configuration must be a JSON object");
		}
		String logoPath = text(root, "logoPath");
		return StringUtils.isNotBlank(logoPath) ? logoPath : text(root, "logoUrl");
	}

	private static byte[] readFileOrResource(String location) throws IOException {
		File file = new File(location);
		if (file.isFile()) {
			validateSize(file.length());
			return Files.readAllBytes(file.toPath());
		}

		InputStream stream = BrandingLogoProvider.class.getResourceAsStream(location);
		if (stream == null && !location.startsWith("/")) {
			stream = BrandingLogoProvider.class.getClassLoader().getResourceAsStream(location);
		}
		return stream == null ? null : readBounded(stream);
	}

	private static byte[] readUrl(URL url) throws IOException {
		URLConnection connection = url.openConnection();
		connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
		connection.setReadTimeout(READ_TIMEOUT_MILLIS);
		connection.setUseCaches(false);
		if (connection instanceof HttpURLConnection) {
			((HttpURLConnection) connection).setInstanceFollowRedirects(true);
		}
		long contentLength = connection.getContentLengthLong();
		if (contentLength > MAX_LOGO_BYTES) {
			throw new IOException("Configured logo exceeds 1 MB");
		}
		return readBounded(connection.getInputStream());
	}

	private static byte[] readBounded(InputStream stream) throws IOException {
		try (InputStream input = stream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			byte[] buffer = new byte[8192];
			int total = 0;
			int count;
			while ((count = input.read(buffer)) != -1) {
				total += count;
				validateSize(total);
				output.write(buffer, 0, count);
			}
			if (total == 0) {
				throw new IOException("Configured logo is empty");
			}
			return output.toByteArray();
		}
	}

	private static void validateSize(long size) throws IOException {
		if (size <= 0 || size > MAX_LOGO_BYTES) {
			throw new IOException("Configured logo must be between 1 byte and 1 MB");
		}
	}

	private static boolean isHttpUrl(String location) {
		return location.regionMatches(true, 0, "http://", 0, 7)
		        || location.regionMatches(true, 0, "https://", 0, 8);
	}

	private static String text(JsonNode node, String field) {
		JsonNode value = node.get(field);
		return value != null && value.isTextual() ? value.asText().trim() : "";
	}

	private static Logger logger() {
		return LoggerHolder.LOG;
	}

	private static final class LoggerHolder {

		private static final Logger LOG = LoggerFactory.getLogger(BrandingLogoProvider.class);
	}
}
