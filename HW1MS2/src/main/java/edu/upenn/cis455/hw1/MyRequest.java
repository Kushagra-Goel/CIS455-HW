package edu.upenn.cis455.hw1;

import edu.upenn.cis455.hw1.interfaces.Request;
import edu.upenn.cis455.hw1.interfaces.Session;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class MyRequest extends Request {
    public String requestMethod() {
      return "";
    }

    public String host() {
      return "";
    }
    
    public String userAgent() {
      return "";
    }

    public int port() {
      return 0;
    }

    public String pathInfo() {
      return "";
    }
    
    public String url() {
      return "";
    }
    
    public String uri() {
      return "";
    }
    
    public String protocol() {
      return "";
    }

    public String contentType() {
      return "";
    }
    
    public String ip() {
      return "";
    }

    public String body() {
      return "";
    }

    public int contentLength() {
      return 0;
    }
    
    public String headers(String name) {
      return "";
    }
    
    public Set<String> headers() {
      return null;
    }

    public boolean persistentConnection() {
      return persistent;
    }
    
    public Session session() {
      return null;
    }
    
    public Session session(boolean create) {
      return null;
    }
    
    public Map<String, String> params() {
      return null;
    }
    
    public String params(String param) {
      return "";
    }
    
    public String queryParams(String param) {
      return "";
    }
    
    public List<String> queryParamsValues(String param) {
      return null;
    }
    
    public Set<String> queryParams() {
      return null;
    }
    
    public String queryString() {
      return null;
    }

    public Object attribute(String attrib) {
      return null;
    }

    public void attribute(String attrib, Object val) {
    }

    public Set<String> attributes() {
      return null;
    }
    
    public Map<String, String> cookies() {
      return null;
    }
}
