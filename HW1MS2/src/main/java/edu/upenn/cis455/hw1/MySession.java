package edu.upenn.cis455.hw1;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import edu.upenn.cis455.hw1.interfaces.Session;

public class MySession extends Session {

	private String sessionID;
	private Instant creationTime = Instant.now();
	private Instant lastAccessedTime = Instant.now();
	private int maxInactiveInterval = (int) Duration.ofHours(3).toMillis();
	private Map<String, Object> attributes = new ConcurrentHashMap<String, Object>();
	
	public MySession(String sessionIDArg) {
		sessionID = sessionIDArg;
		attributes.put("valid", true);
	}
	
    public String id() {
      return sessionID;
    }
    
    public long creationTime() {
      return creationTime.toEpochMilli();
    }
    
    public long lastAccessedTime() {
      return lastAccessedTime.toEpochMilli();
    }
    
    public void invalidate() {
    	attributes.clear();
		attributes.put("valid", false);
    }
    
    public int maxInactiveInterval() {
      return maxInactiveInterval;
    }
    
    public void maxInactiveInterval(int interval) {
    	maxInactiveInterval = interval;
    }
    
    public void access() {
    	lastAccessedTime = Instant.now();
    }
    
    public void attribute(String name, Object value) {
    	attributes.put(name, value);
    }
    
    public Object attribute(String name) {
      return attributes.getOrDefault(name, null);
    }
    
    public Set<String> attributes() {
      return attributes.keySet();
    }
    
    public void removeAttribute(String name) {
    	attributes.remove(name);
    }
}
