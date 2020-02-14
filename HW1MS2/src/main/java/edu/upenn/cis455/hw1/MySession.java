package edu.upenn.cis455.hw1;

import java.util.Set;
import edu.upenn.cis455.hw1.interfaces.Session;

public class MySession extends Session {
    public String id() {
      return "";
    }
    
    public long creationTime() {
      return 0;
    }
    
    public long lastAccessedTime() {
      return 0;
    }
    
    public void invalidate() {
    }
    
    public int maxInactiveInterval() {
      return 0;
    }
    
    public void maxInactiveInterval(int interval) {
    }
    
    public void access() {
    }
    
    public void attribute(String name, Object value) {
    }
    
    public Object attribute(String name) {
      return null;
    }
    
    public Set<String> attributes() {
      return null;
    }
    
    public void removeAttribute(String name) {
    }
}
