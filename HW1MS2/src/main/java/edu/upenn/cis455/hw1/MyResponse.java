package edu.upenn.cis455.hw1;

import edu.upenn.cis455.hw1.interfaces.Response;

public class MyResponse extends Response {
    public String getHeaders() {
      return "";
    }

    public void header(String header, String value) {
    }
    
    public void redirect(String location) {
    }
    
    public void redirect(String location, int httpStatusCode) {
    }
    
    public void cookie(String name, String value) {
    }
    
    public void cookie(String name, String value, int maxAge) {
    }

    public void cookie(String path, String name, String value) {
    }
    
    public void cookie(String path, String name, String value, int maxAge) {
    }

    public void removeCookie(String name) {
    }
    
    public void removeCookie(String path, String name) {
    }
}
