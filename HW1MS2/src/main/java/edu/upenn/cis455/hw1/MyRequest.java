package edu.upenn.cis455.hw1;

import edu.upenn.cis455.hw1.interfaces.HaltException;
import edu.upenn.cis455.hw1.interfaces.Request;
import edu.upenn.cis455.hw1.interfaces.Session;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URLDecoder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

public class MyRequest extends Request {
	

	static final Logger logger = Logger.getLogger(MyRequest.class);
	
	private int statusCode = 200;
	private String errorMessage = null;

	private HttpMethod requestMethod;
	private String host = null;
	private String userAgent = null;
	private Socket socket;
	private int port;
	private String pathInfo = null;
	private String url;
	private String uri;
	private String protocol;
	private String contentType = null;
	private String ip = null;
	private String body = null;
	private String queryString = null;
	private int contentLength = -1;

	private Map<String, Session> sessionTable;
	private Session session = null;
	

	private Map<String, String> headers;
	private Map<String, String> params;
	private List<String> splats;
	private Map<String, List<String>> queryParams;
	private Map<String, Object> attributes;
	private Map<String, String> cookies;
	
	private String newSessionId = null;
	
	public MyRequest(Socket currentSocket, Map<String, Session> sessionTableArg, int p, String IPAddress) throws Exception {
			
	  sessionTable = sessionTableArg;
	  socket = currentSocket;
	  headers = new ConcurrentHashMap<String, String>();
	  params = new ConcurrentHashMap<String, String>();
	  splats = new Vector<String>();
	  queryParams = new ConcurrentHashMap<String, List<String>>();
	  attributes = new ConcurrentHashMap<String, Object>();
	  cookies = new ConcurrentHashMap<String, String>();

	  BufferedReader in = new BufferedReader(new InputStreamReader(currentSocket.getInputStream()));
	  String status = in.readLine();
	  

//		logger.debug("Constructing Request Object");
	  
	  if(status == null || status.isEmpty()) {
//		  logger.debug(String.format("Recieved empty or null status in request"));
		  errorMessage = "Recieved empty or null status";
		  statusCode = 400;
		  return;
	  }
	  
	  // Assuming the requested file does not have space (or any special character) in it
	  String statusSplit[] = status.split("\\s");
	  if(statusSplit.length != 3) {
//		  logger.info(String.format("Recieved more than 3 items in status %s in request", status));
		  errorMessage = "Status contains incorrect number of words";
		  statusCode = 400;
		  throw new HaltException(statusCode, errorMessage);
	  }
	  
	  // Check method
	  switch(statusSplit[0].strip()){
	  case "GET":requestMethod = HttpMethod.GET;break;
	  case "HEAD":requestMethod = HttpMethod.HEAD;break;
	  case "PUT":requestMethod = HttpMethod.PUT;break;
	  case "DELETE":requestMethod = HttpMethod.DELETE;break;
	  case "OPTIONS":requestMethod = HttpMethod.OPTIONS;break;
	  case "POST":requestMethod = HttpMethod.POST;break;//errorMessage="POST Method has not been implemented";statusCode =  501;return;
	  default:
//		  logger.info(String.format("Recieved incorrect method %s in request", statusSplit[0].strip()));
		  errorMessage = "HTTP Method not valid";
		  statusCode = 400;
		  throw new HaltException(statusCode, errorMessage);
	  }

//		logger.debug(String.format("requestMethod = %s", statusSplit[0].strip()));
	  
	  // Check HTTP Version
	  
	  switch(statusSplit[2].strip()) {
	  case "HTTP/1.1":protocol = "HTTP/1.1";break;
	  case "HTTP/1.0":protocol = "HTTP/1.0";break;
	  default : 
//		  logger.info(String.format("Recieved incorrect HTTP version %s in request", statusSplit[2].strip()));
		  errorMessage = "Incorrect HTTP Version";
		  statusCode = 400;
		  throw new HaltException(statusCode, errorMessage);
	  }
//		logger.debug(String.format("protocol = %s", protocol));
//		logger.debug(String.format("Path = %s", statusSplit[1]));
	  
	  String[] statusSplits = statusSplit[1].strip().split("\\?");
	  pathInfo = statusSplits[0];
	  
	  // Handle both /foo/bar and http://localhost:XXXX/foo/bar types of request
	  if(pathInfo.charAt(0) != '/') {
		  try {
			  pathInfo = pathInfo.substring(uri.indexOf(':', 6));
			  pathInfo = pathInfo.substring(uri.indexOf('/'));
		  }catch (Exception e) {
			  errorMessage = "Illegal URI";
			  statusCode = 400;
			  throw new HaltException(statusCode, errorMessage);
		}
	  }
//		logger.debug(String.format("pathInfo = %s", pathInfo));
	  uri = "http://" + IPAddress + ":" + String.valueOf(p) + pathInfo;
	  url = uri;

//		logger.debug(String.format("url = %s", url));
//		logger.debug(String.format("uri = %s", uri));
		
		
	  if(statusSplits.length > 1) {
		  url = url + "?" + statusSplits[1];
		  queryString = statusSplits[1];
		  for(String queryParam: statusSplits[1].split("&")) {
			  if(!queryParam.contains("=")) {
				  errorMessage = "Illegal URI";
				  statusCode = 400;
				  throw new HaltException(statusCode, errorMessage);
			  }
			  String[] queryParamSplit = queryParam.split("=");
			  String key = URLDecoder.decode(queryParamSplit[0], "UTF-8");
			  List<String> values;
			  if(queryParams.containsKey(key)) {
				  values = queryParams.get(key);
			  }
			  else {
				  values = new ArrayList<String>();
			  }
			  values.add((queryParamSplit.length > 1)?URLDecoder.decode(queryParamSplit[1], "UTF-8"):"");
			  queryParams.put(key, values);
		  }
	  }
//		logger.debug(String.format("queryString = %s", queryString));
	  
	  String header = in.readLine();
	  String head = null;
	  while(!header.isEmpty()) {
		  if(header.charAt(0) == ' ' || header.charAt(0) == '\t') {// Continuation of a previous Header
			  if(head == null) {
//				  logger.info(String.format("Recieved multi line header %s with no previous corresponding header in request", header));
				  errorMessage = "Multi Line header corressponds to no header";
				  statusCode = 400;
				  throw new HaltException(statusCode, errorMessage);
			  }
			  if(headers.containsKey(head.toLowerCase())) { // Append attribute value
				  headers.put(head.toLowerCase(), String.join(headers.get(head), header.stripLeading()));
			  }
			  else {
//				  logger.info(String.format("Recieved multi line header %s with no previous corresponding header in request", header));
				  errorMessage = "Multi Line header corressponds to no header";
				  statusCode = 400;
				  throw new HaltException(statusCode, errorMessage);
			  }			  
		  }
		  else { //New header
			  int firstLine = header.indexOf(':');
			  if(firstLine > 0) {
				  head = header.substring(0, firstLine).toLowerCase();
				  if(head.charAt(head.length() - 1) == ' ' || head.charAt(head.length() - 1) == '\t') {
					  errorMessage = "Multi Line header corressponds to no header";
					  statusCode = 400;
					  throw new HaltException(statusCode, errorMessage);
				  }
				  headers.put(head.toLowerCase(), header.substring(firstLine + 1).strip());				  
			  }			  
		  }
		  header = in.readLine();
	  }
	  //Ensure header has host header if HTTP/1.1
	  if(protocol.equals("HTTP/1.1")) {
		  if(!headers.containsKey("host")) {
//			  logger.info(String.format("Recieved HTTP 1.1 request without host field"));
			  errorMessage = "HTTP1.1 does not have required host header";
			  statusCode = 400;
			  throw new HaltException(statusCode, errorMessage);
		  }
		  host = headers.get("host");
//		  logger.debug(String.format("host = %s", host));
	  }		
	  
	  
	  if(headers.containsKey("cookie")) {
		  String[] cookiesSplit = headers.get("cookie").strip().split(";");
		  for(String cookie: cookiesSplit) {
			  String[] cookieSplit = cookie.split("=");
			  if(cookies == null)cookies = new ConcurrentHashMap<String, String>();
			  cookies.put(cookieSplit[0], (cookieSplit.length > 1)?cookieSplit[1]:"");
		  }
		  if(cookies.containsKey("JSESSIONID")) {
			  session = sessionTableArg.getOrDefault(cookies.get("JSESSIONID"), null);  
			  if(session != null) {
				  if(!((boolean) session.attribute("valid"))) {
					  session = null;
				  }
			  }
		  }
	  }
		port = p;//currentSocket.getLocalPort();
		ip = currentSocket.getLocalAddress().getHostAddress();
//		logger.debug(String.format("ip = %s", ip));
//		logger.debug(String.format("port = %d", port));
		userAgent = headers.getOrDefault("user-agent", null);
//		logger.debug(String.format("userAgent = %s", userAgent));
		contentType = headers.getOrDefault("content-type", null);
//		logger.debug(String.format("contentType = %s", contentType));
		contentLength = Integer.valueOf(headers.getOrDefault("content-length", "-1"));
//		logger.debug(String.format("contentLength = %d", contentLength));
		
		body = "";

		if(headers.getOrDefault("transfer-encoding", "not Chunked").equals("chunked")) {
			String lengthLine = in.readLine();
			while(lengthLine.isEmpty()) {
				lengthLine = in.readLine();
			}
			int length = (int) Long.parseLong(lengthLine.split(";")[0], 16);
			while(length != 0) {
				char[] cbuf = new char[length];
				in.read(cbuf);
				String bodyLine = String.valueOf(cbuf);
				body = body + bodyLine;		
				lengthLine = in.readLine();
				while(lengthLine.isEmpty()) {
					lengthLine = in.readLine();
				}
				length = (int) Long.parseLong(lengthLine.split(";")[0], 16);	
			}
		}
		else {
			if(contentLength > 0) {
				char[] cbuf = new char[contentLength];
				in.read(cbuf);
				String bodyLine = String.valueOf(cbuf);
				body = body + bodyLine;
			}
		}
		
		logger.debug("Body = " + ((body == null)?"null":body));
		
		
		
		
		if(requestMethod == HttpMethod.POST && headers.containsKey("content-type") && headers.get("content-type").equals("application/x-www-form-urlencoded")) {
			for(String queryParam: body.split("&")) {
				  if(!queryParam.contains("=")) {
					  errorMessage = "Illegal URI";
					  statusCode = 400;
					  throw new HaltException(statusCode, errorMessage);
				  }
				  String[] queryParamSplit = queryParam.split("=");
				  String key = URLDecoder.decode(queryParamSplit[0], "UTF-8");
				  List<String> values;
				  if(queryParams.containsKey(key)) {
					  values = queryParams.get(key);
				  }
				  else {
					  values = new ArrayList<String>();
				  }
				  values.add((queryParamSplit.length > 1)?URLDecoder.decode(queryParamSplit[1], "UTF-8"):"");
				  queryParams.put(key, values);
			  }
	
		}
		
		
		if(headers.containsKey("connection")) {
			if(headers.get("connection").equals("keep-alive")) {
				persistent = true;
			}
			else {
				persistent  = false;
			}
		}
		
		logger.debug(String.format("statusCode = %d", statusCode));

		logger.debug("Headers");
		for(Map.Entry<String, String> entry : headers.entrySet()) {
			logger.debug(String.format("\t%s = %s", entry.getKey(), entry.getValue()));
		}
//		
		logger.debug("queryParams");
		for(Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
			logger.debug(String.format("\t%s = %s", entry.getKey(), String.join(",", entry.getValue())));
		}

		logger.debug("attributes");
		for(Map.Entry<String, Object> entry : attributes.entrySet()) {
			logger.debug(String.format("\t%s = %s", entry.getKey(), (entry.getValue() instanceof String)?(String)entry.getValue():""));
		}

		logger.debug("Cookies");
		for(Map.Entry<String, String> entry : cookies.entrySet()) {
			logger.debug(String.format("\t%s = %s", entry.getKey(), entry.getValue()));
		}
		
	
	  // Success
	}
	
	public Socket getSocket() {
		return socket;
	}

	public String getErrorMessage() {
		return errorMessage;
	}
	
	public String getnewSessionId() {
		return newSessionId;
	}

	public void updateParamsAndSplat(MyRoute route) {
		String routePath = route.getRoutePath();
		if(routePath.charAt(0) != '/')routePath = "/"+routePath;
		String[] pathSplit = pathInfo.strip().split("/");
		String[] routePathSplit = routePath.split("/");
		int i = 0;
		for(i = 0; i < pathSplit.length && i < routePathSplit.length; i++) {
			if(routePathSplit[i].length() == pathSplit[i].length() && pathSplit[i].length() == 0)continue;
			if(routePathSplit[i].charAt(0) == ':') {
				params.put(routePathSplit[i].toLowerCase(), pathSplit[i]);
			}
			if(routePathSplit[i].equals("*") && i != routePathSplit.length - 1) {
				splats.add(pathSplit[i]);
			}
		}
		if(i < pathSplit.length && routePathSplit.length > 0 && routePathSplit[routePathSplit.length - 1].equals("*")) {
			splats.add(String.join("/", Arrays.asList(pathSplit).subList(routePathSplit.length - 1, pathSplit.length)));
		}
		


//		logger.debug("Params");
//		for(Map.Entry<String, String> entry : params.entrySet()) {
//			logger.debug(String.format("\t%s = %s", entry.getKey(), entry.getValue()));
//		}
//		logger.debug("Splats = \n\t" + String.join("-", splats));
//		
	}
	
	public HttpMethod getRequestMethod() {
		return requestMethod;
	}
	
	public int getStatusCode() {
		return statusCode;
	}
	
    public String requestMethod() {
    	switch(requestMethod) {
    	case GET: return "GET";
    	case HEAD: return "HEAD";
    	case POST: return "POST";
    	case PUT: return "PUT";
    	case DELETE: return "DELETE";
    	case OPTIONS: return "OPTIONS";
    	default: return "";
    	}
    }

    public String host() {
      return host;
    }
    
    public String userAgent() {
      return userAgent;
    }

    public int port() {
      return port;
    }

    public String pathInfo() {
      return pathInfo;
    }
    
    public String url() {
      return url;
    }
    
    public String uri() {
      return uri;
    }
    
    public String protocol() {
      return protocol;
    }

    public String contentType() {
      return contentType;
    }
    
    public String ip() {
      return ip;
    }

    public String body() {
      return body;
    }

    public int contentLength() {
      return contentLength;
    }
    
    public String headers(String name) {
      if(headers == null)return null;
      return headers.getOrDefault(name.toLowerCase(), null);
    }
    
    public Set<String> headers() {
      if(headers == null)return null;
      return headers.keySet();
    }

    public boolean persistentConnection() {
      return persistent;
    }
    
    public Session session() {
    	return session(true);
    }
    
    public Session session(boolean create) {
    	if(session != null) {
    		if((boolean) session.attribute("valid")) {
    			if((Instant.now().toEpochMilli() - session.lastAccessedTime()) <= session.maxInactiveInterval())
    			return session;
    		}
    		session = null;
    	}
    	if(create) {
    		String sessId = String.valueOf(Math.random());
    		if(sessId.contains(".")) {
    			sessId = sessId.substring(sessId.indexOf(".") + 1);
    		}
    		while(sessionTable.keySet().contains(sessId)) {
    			sessId = String.valueOf(Math.random());
    		}
    		sessionTable.put(sessId, new MySession(sessId));
    		session = sessionTable.get(sessId);
    		newSessionId = sessId;
    	}
    	return session;
    }
    
    public Map<String, String> params() {
      return params;
    }
    
//    public String params(String param) {
//    }
    
    public String queryParams(String param) {
      if(queryParams == null)return null;
      return queryParams.containsKey(param.toLowerCase())?queryParams.get(param.toLowerCase()).get(0):null;
    }
    
    public List<String> queryParamsValues(String param) {
      if(queryParams == null)return null;
      return queryParams.getOrDefault(param.toLowerCase(), null);
    }
    
    public Set<String> queryParams() {
      if(queryParams == null)return null;
      return queryParams.keySet();
    }
    
    public String queryString() {
      return queryString;
    }

    public Object attribute(String attrib) {
      if(attributes == null)return null;
      return attributes.getOrDefault(attrib.toLowerCase(), null);
    }

    public void attribute(String attrib, Object val) {
      if(attributes == null)attributes = new ConcurrentHashMap<String, Object>();
      attributes.put(attrib.toLowerCase(), val);
    }

    public Set<String> attributes() {
      if(attributes == null)return null;
      return attributes.keySet();
    }
    
    public Map<String, String> cookies() {
      return cookies;
    }

	public List<String> splat() {
		return splats;
	}
}
