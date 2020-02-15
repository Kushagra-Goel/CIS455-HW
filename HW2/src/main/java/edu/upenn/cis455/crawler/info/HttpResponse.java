package edu.upenn.cis455.crawler.info;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;

/**
 * Traditional HTTP Response class to build response object
 * @author cis455
 *
 */
public class HttpResponse {

	static final Logger logger = Logger.getLogger(HttpResponse.class);
	private URLInfo url = null;
	private int statusCode;
	private String contentType = null;
	private int contentLength;
	private String content = null;
	private String location = null;
	private Instant lastModified = null;
	private boolean isValid = false;
	
	public HttpResponse(InputStream is, HttpsURLConnection connection, boolean hasBody) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(is, StandardCharsets.ISO_8859_1));	
		if(connection != null) {
			statusCode = connection.getResponseCode();
			contentLength = connection.getContentLength();
			contentType = connection.getContentType();
			if(connection.getLastModified() != 0) {
				lastModified = Instant.ofEpochMilli(connection.getLastModified());
			}
			else {
				if(connection.getDate() != 0) {
					lastModified = Instant.ofEpochMilli(connection.getLastModified());					
				}
			}
			location = connection.getHeaderField("Location");
		}else {		
			String line = in.readLine();
			if(line == null || line.isEmpty()) return;
			logger.debug(line);
			statusCode = Integer.parseInt(line.split(" ")[1]);
			while(!line.isEmpty()) {
				if(line.toLowerCase().contains("content-length")) {
					contentLength = Integer.parseInt(line.substring(line.indexOf(":") + 1).trim());
				}
				if(line.toLowerCase().contains("content-type")) {
					contentType = line.substring(line.indexOf(":") + 1).trim();
				}
				if(line.toLowerCase().contains("location")) {
					location = line.substring(line.indexOf(":") + 1).trim();
				}
				if(line.toLowerCase().contains("last-modified")) {
					lastModified = LocalDateTime.parse(line.substring(line.indexOf(":") + 1).trim(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant(ZoneOffset.UTC);
				}
				if(lastModified == null && line.toLowerCase().contains("date")) {
					lastModified = LocalDateTime.parse(line.substring(line.indexOf(":") + 1).trim(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant(ZoneOffset.UTC);
				}
				
				line = in.readLine();
			}
		}
		
		if(lastModified == null) {
			lastModified = Instant.EPOCH;
		}
		
		if(statusCode == 200 && hasBody && (isHTML() || isXML() || contentType.contains("text/plain"))) {
			StringBuilder b = new StringBuilder();
			for(int i = 0; i < contentLength; ++i) {
				b.append((char)in.read());
			}
			content = b.toString();
		}
		isValid = true;
	}
	
	/**
	 * @return the url
	 */
	public URLInfo getUrl() {
		return url;
	}
	/**
	 * @param url the url to set
	 */
	public void setUrl(URLInfo url) {
		this.url = url;
	}
	/**
	 * @return the statusCode
	 */
	public int getStatusCode() {
		return statusCode;
	}
	/**
	 * @param statusCode the statusCode to set
	 */
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}
	/**
	 * @return the contentType
	 */
	public String getContentType() {
		return contentType;
	}
	/**
	 * @param contentType the contentType to set
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	/**
	 * @return the contentLength
	 */
	public int getContentLength() {
		return contentLength;
	}
	/**
	 * @param contentLength the contentLength to set
	 */
	public void setContentLength(int contentLength) {
		this.contentLength = contentLength;
	}
	/**
	 * @return the content
	 */
	public String getContent() {
		return content;
	}
	/**
	 * @param content the content to set
	 */
	public void setContent(String content) {
		this.content = content;
	}

	/**
	 * @return the isValid
	 */
	public boolean isValid() {
		return isValid;
	}

	/**
	 * @return the location
	 */
	public String getLocation() {
		return location;
	}

	/**
	 * @return the lastModified
	 */
	public Instant getLastModified() {
		return lastModified;
	}

	public boolean isHTML() {
		return contentType.contains("text/html");
	}
	
	public boolean isXML() {
		return (contentType.contains("text/xml") || contentType.contains("application/xml") || contentType.endsWith("+xml"));
	}
}
