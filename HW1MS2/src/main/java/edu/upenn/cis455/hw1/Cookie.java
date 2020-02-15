package edu.upenn.cis455.hw1;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class Cookie {
	
	private String path = null;
	private String name = null;
	private String value = null;
	private Instant expiry = null;
	
	public Cookie(String pathArg, String nameArg, String valueArg, long ttlInMillis) {
		path = pathArg;
		name = nameArg;
		value = valueArg;
		if(ttlInMillis > 0) {
			expiry = Instant.now().plusMillis(ttlInMillis);
		}
	}
	
	public String getPath() {
		return path;
	}
	
	public String getName() {
		return name;
	}
	
	public String getValue() {
		return value;
	}
	
	public Instant getExpiry() {
		return expiry;
	}
	
	public void setExpiry(Instant newExpiry) {
		expiry = newExpiry;
	}
	
	public String toString() {
		String result = String.format("%s=%s", name, value);
		if(expiry != null)result = result + String.format(";%s=%s", "expires", expiry.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME));
		if(path != null)result = result + String.format(";%s=%s", "path", path);
		return result;
	}
	
	
}
