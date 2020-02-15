package edu.upenn.cis455.hw1;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//import org.apache.log4j.Logger;

import edu.upenn.cis455.hw1.interfaces.Response;

public class MyResponse extends Response {
//	static final Logger logger = Logger.getLogger(MyResponse.class);
	
	public static Map<String, String> httpStatusCodes;
	static {
	httpStatusCodes	= new ConcurrentHashMap<String, String>();

	httpStatusCodes.put("100", "Continue");
	httpStatusCodes.put("101", "Switching Protocols");
	
	httpStatusCodes.put("200", "OK");
	httpStatusCodes.put("201", "Created");
	httpStatusCodes.put("202", "Accepted");
	httpStatusCodes.put("203", "Non-Autoritative Information");
	httpStatusCodes.put("204", "No Content");
	httpStatusCodes.put("205", "Reset Content");
	httpStatusCodes.put("206", "Partial Content");
	
	httpStatusCodes.put("300", "Multiple Choices");
	httpStatusCodes.put("301", "Moved Permanently");
	httpStatusCodes.put("302", "Found");
	httpStatusCodes.put("303", "See Other");
	httpStatusCodes.put("304", "Not Modified");
	httpStatusCodes.put("305", "Use Proxy");
	httpStatusCodes.put("306", "(Unused)");
	httpStatusCodes.put("307", "Temporary Redirect");	

	httpStatusCodes.put("400", "Bad Request");
	httpStatusCodes.put("401", "Unauthorized");
	httpStatusCodes.put("402", "Payment Required");
	httpStatusCodes.put("403", "Forbidden");
	httpStatusCodes.put("404", "Not Found");
	httpStatusCodes.put("405", "Method Not Allowed");
	httpStatusCodes.put("406", "Not Acceptable");
	httpStatusCodes.put("407", "Proxy Authentication Required");
	httpStatusCodes.put("408", "Request Timeout");
	httpStatusCodes.put("409", "Conflict");
	httpStatusCodes.put("410", "Gone");
	httpStatusCodes.put("411", "Length Required");
	httpStatusCodes.put("412", "Precondition Failed");
	httpStatusCodes.put("413", "Request Entity Too Long");
	httpStatusCodes.put("414", "Request-URI Too Long");
	httpStatusCodes.put("415", "Unsupported Media Type");
	httpStatusCodes.put("416", "Request Range Not Satisfiable");
	httpStatusCodes.put("417", "Expectation Failed");	

	httpStatusCodes.put("500", "Internal Server Error");
	httpStatusCodes.put("501", "Not Implemented");
	httpStatusCodes.put("502", "Bad Gateway");
	httpStatusCodes.put("503", "Service Unavailable");
	httpStatusCodes.put("504", "Gateway Timeout");
	httpStatusCodes.put("505", "HTTP Version Not Supported");		
	}

	private Map<String, String> headers;
	private Map<String, Cookie> cookies;
	private int contentLength = 0;
	private String protocol;
	private MyRequest request;
	private int arbitraryChunkLength = 13;
	
	public MyResponse(MyRequest requestArg) {
		headers = new ConcurrentHashMap<String, String>();
		cookies = new ConcurrentHashMap<String, Cookie>();
		protocol = requestArg.protocol();
		request = requestArg;
	}

	
    public String getHeaders() {
      return "";
    }
	
    public boolean isPersistent() {
      return protocol.equals("HTTP/1.1") &&!headers.getOrDefault("Connection", "close").equals("close");
    }

    public void header(String header, String value) {
    	headers.put(header, value);
    }
    
    public void redirect(String location) {
    	redirect(location, 302);
    }
    
    public void redirect(String location, int httpStatusCode) {
    	
    	if(httpStatusCode < 300 || httpStatusCode > 307)throw new IllegalArgumentException("Redirect httpStatusCode outside acceptable range");
    	
    	statusCode = httpStatusCode;
    	headers.put("Location", location);
    	contentType = "text/html";
    	List<String> htmlString = new ArrayList<String>();

    	htmlString.add("<!DOCTYPE html>");
    	htmlString.add("<html>");
    	htmlString.add("<head>");
    	htmlString.add(String.format("<title>%d : %s</title>", httpStatusCode, httpStatusCodes.get(String.valueOf(httpStatusCode))));
    	htmlString.add("</head>");
    	htmlString.add("<body>");
	  	htmlString.add(String.format("<h3>You have been redirected to <a href = '%s'>%s/</a>", location, location));
	  	htmlString.add("</body>");
	  	htmlString.add("</html>");
	  	
	  	String joinedHtmlString = String.join("", htmlString);
	  	contentLength = joinedHtmlString.length();
//	  	logger.debug(joinedHtmlString);
	  	body = joinedHtmlString.getBytes();
    	
    }
    
    public void cookie(String name, String value) {
    	cookie(null, name, value, -1);
    }
    
    public void cookie(String name, String value, int maxAge) {
    	cookie(null, name, value, maxAge);
    }

    public void cookie(String path, String name, String value) {
    	cookie(path, name, value, -1);
    }
    
    public void cookie(String path, String name, String value, int maxAge) {
    	cookies.put(name, new Cookie(path, name, value, maxAge));
    }

    public void removeCookie(String name) {
    	removeCookie(null, name);
    }
    
    public void removeCookie(String path, String name) {
    	if(!cookies.containsKey(name)) {
    		cookies.put(name, new Cookie(path, name, "foobar", -1));
    	}
		Cookie cookie = cookies.get(name);
		cookie.setExpiry(Instant.EPOCH);
    }
    
    /**
     * Sends out data as per the request.
     * Can send chunked data if the header is specified (Transfer-Encoding: chunked)
     * Can persist connections if header is specified (Connection: keep-alive) by default, closes the connection
     * @param socket
     * @throws IOException
     */
    public void sendResponse(Socket socket) throws IOException {
      BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());
  	  PrintWriter p = new PrintWriter(out, true);
  	  p.print(String.format("HTTP/1.%d %d %s\r\n", (protocol.equals("HTTP/1.1"))?1:0, statusCode, httpStatusCodes.getOrDefault(String.valueOf(statusCode), "Unknown Status Code")));
  	  for(Map.Entry<String, String> entry : headers.entrySet()) {
  		  p.print(String.format("%s%s %s\r\n", entry.getKey(), (entry.getKey().endsWith(":"))?"":":", entry.getValue()));
  	  }
  	  
  	  String jsessionId = request.getnewSessionId();
  	  if(jsessionId != null) {
  		  cookie("JSESSIONID", jsessionId);
  	  }
  	  
  	  for(Map.Entry<String, Cookie>entry : cookies.entrySet()) {
  		  	Cookie cookie = entry.getValue();
  		  	if(cookie.getExpiry() != null)
  	  		  	if(cookie.getExpiry().isAfter(Instant.now())) {
	  	  		  	if(cookie.getPath() != null) {
	  	  		  		String path = request.pathInfo();
	  	  		  		String cookiePath = cookie.getPath();
	  			  		if(path.charAt(0) != '/')path = "/"+path;
	  					if(cookiePath.charAt(0) != '/')cookiePath = "/"+cookiePath;
	  					String[] cookiePathSplit = cookiePath.strip().split("/");
	  					String[] routePath = path.split("/");
	  					boolean subPath = true;
	  					for(int i = 0; i < cookiePathSplit.length && i < routePath.length; i++) {
	  						if(routePath[i].charAt(0) != ':' && routePath[i].charAt(0) != '*'  && !routePath[i].equals(cookiePathSplit[i])) {
	  							subPath = false;
	  							break;
	  						}
	  					}
	  					if(subPath == false)continue;
	  	  		  	}
  	  		  	}
	  		  	p.print(String.format("Set-Cookie: %s\r\n", cookie.toString()));
//	  		  	logger.debug("Sending cookie " + String.format("Set-Cookie: %s\r\n", cookie.toString()));
  		  	
  	  }
  	  if(contentType != null) {
	  	  p.print(String.format("Content-Type: %s\r\n", contentType));
	  	  if(contentLength == 0) {
	  		  if(contentType.toLowerCase().startsWith("text")) {
	  			  contentLength = body().length();
	  		  }
	  		  else {
	  	  		  contentLength = body.length;	
	  		  }
	  	  }
	  	  if(!headers.getOrDefault("Transfer-Encoding", "not chunked").equals("chunked"))
	  		  p.print(String.format("Content-Length: %s\r\n", contentLength));
  	  }
  	  p.print("\r\n");
  	  p.flush();	  

  	// Send body iff not HEAD request
  	  if(request.getRequestMethod() != HttpMethod.HEAD) {

  	  	  if(contentType != null) {
	  		  if(contentType.toLowerCase().startsWith("text")) {
	  		  	  if(headers.getOrDefault("Transfer-Encoding", "not chunked").equals("chunked") && protocol.equals("HTTP/1.1")) {
//	  		  		  logger.debug("Sending Chunked Response");
	  		  		  String sBody = body();
	  		  		  for(int i = 0; i < sBody.length(); i = i + arbitraryChunkLength) {
	  		  			  String chunk = sBody.substring(i, ((i + arbitraryChunkLength) < sBody.length())?(i + arbitraryChunkLength):sBody.length());
	  		  			  p.print(Integer.toHexString(chunk.length()));
	  		  			  p.print("\r\n");
//		  		  		  logger.debug(Integer.toHexString(chunk.length()));
	  		  			  p.print(chunk);
	  		  			  p.print("\r\n");
//		  		  		  logger.debug(chunk);
	  		  		  }
	  		  		  p.print("0");
  		  			  p.print("\r\n");
//	  		  		  logger.debug(0);
	  		  		  p.flush();
	  		  	  }else {
		  			  p.print(body());
		  		  	  p.flush();  
	  		  	  }	 
//	  	  		  logger.info("Text transfered : " + body());	
	  		  }
	  		  else {
		  			out.write(body, 0, body.length);  
//	    		  logger.info("Binary File transfered");	
	  		  }
  	  	  }
  	  }
  	  out.flush();
  	  if(!isPersistent()) {
	  	  out.close();
	  	  socket.close();
  	  }
//  	  logger.debug("DID NOT CLOSE CONNECTION");
    }
    
    
    
}
