package edu.upenn.cis455.hw1;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Enum of Types of requests
 * @author Kushagra
 *
 */
enum RequestType{
	NONE,GET,HEAD,POST;
}

/**
 * Enum of Types of HTTP versions
 * @author Kushagra
 *
 */
enum HTTPType{
	NONE,HTTP10,HTTP11;
}

/**
 * Individual worker threads that actually send relevant response
 * @author Kushagra
 *
 * @param <T> derived from Socket
 */
public class WorkerThread<T extends Socket> extends Thread{
	static final Logger logger = Logger.getLogger(WorkerThread.class);
	private BlockingQueue<T> queue = null;
	private boolean stopFlag = false;
	private String state = "Waiting on connection";
	private T currentSocket = null;
	private int threadID;
	private Path rootDirectory;
	private ThreadPool<T> threadPool = null;
	private RequestType requestType = RequestType.NONE;
	private String requestURI;
	private Hashtable<String, String> headerAttributes;
	private int port;
	private String errorMessage = null;
	
	/**
	 * Constructor to instantiate worker thread
	 * @param q : the blocking queue
	 * @param p : path of root directory
	 * @param tid : Thread ID
	 * @param tp : Caller Threadpool
	 * @param portArg : Server Port
	 */
	public WorkerThread(BlockingQueue<T> q, Path p, int tid, ThreadPool<T> tp, int portArg) {
		queue = q;
		rootDirectory = p;
		threadPool = tp;
		threadID = tid;
		port = portArg;
		headerAttributes = new Hashtable<String, String>();	
	}
	
	/**
	 * The run method that verifies the HTTP request and takes corresponding action
	 */
	public void run() {
		
		// Run till stop flag is set
		while(getStopFlag() == false) {
			try {
				currentSocket = queue.dequeue(threadID);
				logger.debug(String.format("Thread ID %d successfully dequeued a request", threadID));
			} catch (InterruptedException e) {
				logger.debug(String.format("Thread %d was terminated", threadID));
				continue;
			}

		  try {
		  
		  BufferedReader in = new BufferedReader(new InputStreamReader(currentSocket.getInputStream()));	  
		  int statusCode = verifyHttpRequest(in);
		  
		  // Bad HTTP Request
		  if(statusCode != 200) {
			  sendErrorPage(statusCode, Instant.now());
			  resetThread();
			  currentSocket.close();
			  continue;
			  
		  }
		  
		  switch(requestURI) {
		  case "/control" :{  // Control Page
			  threadPool.sendControlPage(currentSocket, requestType == RequestType.HEAD);
			  logger.info(String.format("Thread ID : %d recieved control page request", threadID));
			  break;}
		  case "/shutdown" :{ // Shutdown request
			  logger.info(String.format("Thread ID : %d recieved shutdown request", threadID));
			  threadPool.shutdown(currentSocket, requestType == RequestType.HEAD);
			  break;}
		  default :{ // File or Directory
			  Path requestURIPath = Paths.get(rootDirectory.toString(), requestURI);
			  requestURIPath = requestURIPath.normalize();
			  File requestedFile = requestURIPath.toFile();
			  
			  // Check if File exists
			  if(!requestedFile.exists()) {
				  logger.debug(String.format("Thread ID : %d recieved request for non existent file %s", threadID, requestURI));
				  errorMessage = String.format("File %s not found", rootDirectory.relativize(requestURIPath));
				  sendErrorPage(404, Instant.now());
				  }
			  else {
				  if(requestedFile.isFile()) {
					  logger.info(String.format("Thread ID : %d recieved file request", threadID));
					  sendFile(requestURIPath);
					  }
				  if(requestedFile.isDirectory()) {
					  logger.info(String.format("Thread ID : %d recieved directory request", threadID));
					  sendDirectory(requestURIPath);
					  }				  
			  }
		  	}
		  }		  
		} catch (Exception e) {
		}
		  // Reset thread local states to original
			resetThread();
		}
	}

	/**
	 * Checks HTTP Request and updates state
	 * @param in : Socket stream to read request from
	 * @return Status code (and also sets errorMessage)
	 * @throws Exception
	 */
	private int verifyHttpRequest(BufferedReader in) throws Exception {
	  String status = in.readLine();
	  
	  if(status == null || status.isEmpty()) {
		  logger.debug(String.format("Thread ID : %d recieved empty or null status in request", threadID));
		  errorMessage = "Recieved empty or null status";
		  return 400;
	  }
	  
	  // Assuming the requested file does not have space (or any special character) in it
	  String statusSplit[] = status.split("\\s");
	  if(statusSplit.length != 3) {
		  logger.info(String.format("Thread ID : %d recieved more than 3 items in status %s in request", threadID, status));
		  errorMessage = "Status contains incorrect number of words";
		  return 400;
	  }
	  
	  // Check method
	  switch(statusSplit[0].strip()){
	  case "GET":requestType = RequestType.GET;break;
	  case "HEAD":requestType = RequestType.HEAD;break;
	  case "POST":requestType = RequestType.POST;errorMessage="POST Method has not been implemented";return 501;
	  default:
		  logger.info(String.format("Thread ID : %d recieved incorrect method %s in request", threadID, statusSplit[0].strip()));
		  errorMessage = "HTTP Method not valid";
		  return 400;
	  }
	  
	  // Check HTTP Version
	  HTTPType httpType = HTTPType.NONE;
	  
	  switch(statusSplit[2].strip()) {
	  case "HTTP/1.1":httpType = HTTPType.HTTP11;break;
	  case "HTTP/1.0":httpType = HTTPType.HTTP10;break;
	  default : 
		  logger.info(String.format("Thread ID : %d recieved incorrect HTTP version %s in request", threadID, statusSplit[2].strip()));
		  errorMessage = "Incorrect HTTP Version";
		  return 400;
	  }
	  
	  // Ignore the URI parameters
	  requestURI = statusSplit[1].strip().split("\\?")[0];
	  
	  // Handle both /foo/bar and http://localhost:XXXX/foo/bar types of request
	  if(requestURI.charAt(0) != '/') {
		  try {
		  requestURI = requestURI.substring(requestURI.indexOf(':'));
		  requestURI = requestURI.substring(requestURI.indexOf('/'));
		  }catch (Exception e) {
			  errorMessage = "Illegal URI";
			  return 400;
		}
	  }
	  
	  // Create absolute path and ensure root directory is a parent
	  // to prevent access outside the root subtree
	  Path requestURIPath = Paths.get(rootDirectory.toString(), requestURI);
	  requestURIPath = requestURIPath.normalize();
	  if(!requestURIPath.startsWith(rootDirectory)) {
		  logger.info(String.format("Thread ID : %d recieved inaccessible directory %s in request", threadID, requestURIPath));
		  errorMessage = "Trying to access a forbidden location";
		  return 403;
	  }
	  // Set thread State for control page
	  state = requestURI;
	  
	  
	  String header = in.readLine();
	  String head = null;
	  while(!header.isEmpty()) {
		  if(header.charAt(0) == ' ' || header.charAt(0) == '\t') {// Continuation of a previous Header
			  if(head == null) {
				  logger.info(String.format("Thread ID : %d recieved multi line header %s with no previous corresponding header in request", threadID, header));
				  errorMessage = "Multi Line header corressponds to no header";
				  return 400;
			  }
			  if(headerAttributes.containsKey(head)) { // Append attribute value
				  headerAttributes.put(head, String.join(headerAttributes.get(head), header.stripLeading()));
			  }
			  else {
				  logger.info(String.format("Thread ID : %d recieved multi line header %s with no previous corresponding header in request", threadID, header));
				  errorMessage = "Multi Line header corressponds to no header";
				  return 400;
			  }			  
		  }
		  else { //New header
			  int firstLine = header.indexOf(':');
			  if(firstLine > 0) {
				  head = header.substring(0, firstLine).toLowerCase();
				  if(head.charAt(head.length() - 1) == ' ' || head.charAt(head.length() - 1) == '\t') {
					  errorMessage = "Multi Line header corressponds to no header";
					  return 400;
				  }
				  headerAttributes.put(head, header.substring(firstLine + 1).strip());				  
			  }			  
		  }
		  header = in.readLine();
	  }
	  //Ensure header has host header if HTTP/1.1
	  if(httpType == HTTPType.HTTP11) {
		  if(!headerAttributes.containsKey("host")) {
			  logger.info(String.format("Thread ID : %d recieved HTTP 1.1 request without host field", threadID));
			  errorMessage = "HTTP1.1 does not have required host header";
			  return 400;
		  }
	  }		
	  // Success
		return 200;
	}
	
	/**
	 * Sends error page with a small HTML page explaining the error
	 * @param statusCode : error status code
	 * @param lastModified : last modified date of file in case of 403
	 * @throws IOException
	 */
	private void sendErrorPage(int statusCode, Instant lastModified) throws IOException {

	  BufferedOutputStream out = new BufferedOutputStream(currentSocket.getOutputStream());
	  PrintWriter p = new PrintWriter(out, true);

	  int contentLength = 0;

	  List<String> htmlErrorPage = new ArrayList<String>();

	  htmlErrorPage.add("<!DOCTYPE html>");
	  htmlErrorPage.add("<html>");
	  htmlErrorPage.add("<head>");
	  htmlErrorPage.add("</head>");
	  htmlErrorPage.add("<body>");
	  switch(statusCode) {
	  case 100: p.print("HTTP/1.1 100 Continue\r\n"); break;
	  case 200: p.print("HTTP/1.1 200 OK\r\n"); break;
	  case 304: p.print("HTTP/1.1 304 Not Modified\r\n"); htmlErrorPage.add(String.format("<b>Error : </b><p>%d</p>", 304)); break;
	  case 400: p.print("HTTP/1.1 400 Bad Request\r\n"); htmlErrorPage.add(String.format("<b>Error : </b><p>%d</p>", 400)); break;
	  case 403: p.print("HTTP/1.1 403 Forbidden\r\n"); htmlErrorPage.add(String.format("<b>Error : </b><p>%d</p>", 403)); break;
	  case 404: p.print("HTTP/1.1 404 Not Found\r\n"); htmlErrorPage.add(String.format("<b>Error : </b><p>%d</p>", 404)); break;
	  case 412: p.print("HTTP/1.1 412 Precondition Failed\r\n"); htmlErrorPage.add(String.format("<b>Error : </b><p>%d</p>", 412)); break;
	  case 501: p.print("HTTP/1.1 501 Not Implemented\r\n"); htmlErrorPage.add(String.format("<b>Error : </b><p>%d</p>", 501)); break;
	  }
	  htmlErrorPage.add(String.format("<h3>Error Message: </h3><p>%s</p>", errorMessage));
	  htmlErrorPage.add("</body>");
	  htmlErrorPage.add("</html>");
	  
	  

	for(String sentence : htmlErrorPage) {
		contentLength = contentLength + sentence.length();	
	}


	  OffsetDateTime odt = lastModified.atOffset(ZoneOffset.UTC);
	  p.print(String.format("Server: %s\r\n", "HttpServer/1.1"));
	  p.print(String.format("Date: %s\r\n", Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME)));
	  p.print(String.format("Last-Modified: %s\r\n", odt.format(DateTimeFormatter.RFC_1123_DATE_TIME)));
	  p.print(String.format("Content-Type: %s\r\n", "text/html"));
	  p.print(String.format("Content-Length: %d\r\n", contentLength));
	  p.print("Connection: close\r\n");
	  p.print("\r\n");    

	// Send body iff not HEAD request
	  if(requestType != RequestType.HEAD) {
		  for(String html: htmlErrorPage) {
			  p.print(html);
			  logger.debug(html);
		  }
	  }
	  p.flush();
	  out.close();
	  currentSocket.close();
	  resetThread();
	  logger.info(String.format("Thread ID : %d Closed Connection", threadID));
	}
	
	/**
	 * Send requested File
	 * @param requestURIPath : Path of requested file
	 * @throws Exception
	 */
	private void sendFile(Path requestURIPath) throws Exception {
	  File requestedFile = requestURIPath.toFile();
	  Instant lastModified = Instant.ofEpochMilli(requestedFile.lastModified());
	  OffsetDateTime odt = lastModified.atOffset(ZoneOffset.UTC);
	  
	  // Handle if-modified-since and if-unmodified-since headers
	  String lastModifiedHeader = null;
	  String lastUnmodifiedHeader = null;
	  lastModifiedHeader = headerAttributes.getOrDefault("if-modified-since", null);
	  lastUnmodifiedHeader = headerAttributes.getOrDefault("if-unmodified-since", null);
	  // 3 Acceptable date formats
	  String[] dateFormats = {"EEE, d MMM yyyy HH:mm:ss z", "EEEE, d-MMM-yy HH:mm:ss z", "EEE, MMM d HH:mm:ss yyyy"};
	  for(String format : dateFormats) {
		  try {
			  DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format);
			  if(lastModifiedHeader != null) {
				  if(lastModified.isBefore(LocalDateTime.parse(lastModifiedHeader.strip(), dtf).toInstant(ZoneOffset.UTC))) {
					  errorMessage = "File has not been modified since the provided date";
					  sendErrorPage(304, lastModified);
					  return;
				  }				  
			  }
			  if(lastUnmodifiedHeader != null) {
				  if(lastModified.isAfter(LocalDateTime.parse(lastUnmodifiedHeader.strip(), dtf).toInstant(ZoneOffset.UTC))) {
					  errorMessage = "File has been modified since the provided date";
					  sendErrorPage(412, lastModified);
					  return;
				  }				  
			  }
		  }catch(Exception e) {
			  errorMessage = "Illegal Date : " + e.getMessage();
			  sendErrorPage(400, lastModified);
			  return;
		  }
	  }
	  BufferedInputStream is = new BufferedInputStream(new FileInputStream(requestedFile));
	  BufferedOutputStream out = new BufferedOutputStream(currentSocket.getOutputStream());
	  PrintWriter p = new PrintWriter(out, true);
	  
	  p.print("HTTP/1.1 200 OK\r\n");
	  p.print(String.format("Server: %s\r\n", "HttpServer/1.1"));
	  p.print(String.format("Date: %s\r\n", Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME)));
	  p.print(String.format("Last-Modified: %s\r\n", odt.format(DateTimeFormatter.RFC_1123_DATE_TIME)));
	  p.print("Connection: close\r\n");
	  p.print(String.format("Content-Type: %s\r\n", Files.probeContentType(requestURIPath)));
	  p.print(String.format("Content-Length: %s\r\n", Long.toString(requestedFile.length())));
	  p.print("\r\n");
	  p.flush();	  

	// Send body iff not HEAD request
	  if(requestType != RequestType.HEAD) {
		  byte[] someBytes = new byte[8192];
		  int numBytes;
		  while((numBytes = is.read(someBytes)) > 0) {
		  out.write(someBytes, 0, numBytes);
	  }
		  logger.info("Binary File transfered");		
	  }
	  is.close();
	  out.flush();	  
	  out.close();
	  currentSocket.close();
	  resetThread();
	  logger.info(String.format("Thread ID : %d Closed Connection", threadID));
	}
	
	/**
	 * Method to send directory information
	 * @param requestURIPath
	 * @throws IOException
	 */
	private void sendDirectory(Path requestURIPath) throws IOException {
		
		
		List<String> htmlControlPage = new ArrayList<String>();
		int contentLength = 0;

		BufferedOutputStream out = new BufferedOutputStream(currentSocket.getOutputStream());
		PrintWriter p = new PrintWriter(out, true);
		String dateNow = Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME);

		htmlControlPage.add("<!DOCTYPE html>");
		htmlControlPage.add("<html>");
		htmlControlPage.add("<head>");
		htmlControlPage.add("<style>");
		htmlControlPage.add("table {");
		htmlControlPage.add("  font-family: arial, sans-serif;");
		htmlControlPage.add("  border-collapse: collapse;");
		htmlControlPage.add("  width: 100%;");
		htmlControlPage.add("}");
		htmlControlPage.add("td, th {");
		htmlControlPage.add("  border: 1px solid #dddddd;");
		htmlControlPage.add("  text-align: left;");
		htmlControlPage.add("  padding: 8px;");
		htmlControlPage.add("}");
		htmlControlPage.add("tr:nth-child(even) {");
		htmlControlPage.add("  background-color: #dddddd;");
		htmlControlPage.add("}");
		htmlControlPage.add("</style>");
		htmlControlPage.add("</head>");
		htmlControlPage.add("<body>");
		htmlControlPage.add(String.format("<h2>/%s</h2>", rootDirectory.relativize(requestURIPath)));
		htmlControlPage.add("<ul>");
		
		// Add ../ if not root directory
		if(!requestURIPath.equals(rootDirectory)) {
			htmlControlPage.add("<li>");
			if(HttpServer.useHTTPs) {
				htmlControlPage.add(String.format("<a href = 'https://localhost:%d/%s'>../</a>", port, rootDirectory.relativize(requestURIPath.getParent())));
				logger.debug(rootDirectory.relativize(requestURIPath.getParent()));
			}
			else {
				htmlControlPage.add(String.format("<a href = 'http://localhost:%d/%s'>../</a>", port, rootDirectory.relativize(requestURIPath.getParent())));	
				logger.debug(rootDirectory.relativize(requestURIPath.getParent()));			
			}
			htmlControlPage.add("</li>");			
		}
		
		
		// Adds file and folders with hyperlinks
		for(File f : requestURIPath.toFile().listFiles()) {
			htmlControlPage.add("<li>");
			if(HttpServer.useHTTPs) {
				htmlControlPage.add(String.format("<a href = 'https://localhost:%d/%s'>%s%s</a>", port, rootDirectory.relativize(Paths.get(f.getAbsolutePath())), f.getName(),(f.isDirectory())?"/":""));
				logger.debug(rootDirectory.relativize(Paths.get(f.getAbsolutePath())).toString());
			}
			else {
				htmlControlPage.add(String.format("<a href = 'http://localhost:%d/%s'>%s%s</a>", port, rootDirectory.relativize(Paths.get(f.getAbsolutePath())), f.getName(),(f.isDirectory())?"/":""));
				logger.debug(rootDirectory.relativize(Paths.get(f.getAbsolutePath())).toString());
			}
			htmlControlPage.add("</li>");
		}

		htmlControlPage.add("</ul>");
		htmlControlPage.add("</body>");
		htmlControlPage.add("</html>");
		
		for(String sentence : htmlControlPage) {
			contentLength = contentLength + sentence.length();	
		}
	
		
		p.print("HTTP/1.1 200 OK\r\n");
		p.print(String.format("Server: %s\r\n", "HttpServer/1.1"));
		p.print(String.format("Date: %s\r\n", dateNow));
		p.print(String.format("Last-Modified: %s\r\n", dateNow));
		p.print(String.format("Connection: %s\r\n", "close"));
		p.print(String.format("Content-Type: %s\r\n", "text/html"));
		p.print(String.format("Content-Length: %d\r\n", contentLength));
		p.print("\r\n");
		
		// Send body iff not HEAD request
		if(requestType != RequestType.HEAD) {
			for(String html: htmlControlPage) {
				p.print(html);
				logger.debug(html);
			}
		}
		p.flush();
		out.close();
		currentSocket.close();
		resetThread();
		logger.info(String.format("Thread ID : %d Closed Connection", threadID));
	}
	
	/**
	 * Method to set stop flag, close client socket and interrupt the thread
	 */
	public synchronized void stopThread(){
		stopFlag = true;
			try {
				if(currentSocket != null)
				{
					currentSocket.close();
				}
			} catch (IOException e) {
				logger.info(String.format("Exception closing client socket for thread %d", threadID));
				e.printStackTrace();
			}
		this.interrupt();
	}
	
	/**
	 * Reset threads local state (memory)
	 */
	private void resetThread() {

		state = "Waiting on connection";
		currentSocket = null;
		requestType = RequestType.NONE;
		requestURI = null;
		headerAttributes.clear();
		errorMessage = null;
	}
	
	
	private synchronized boolean getStopFlag() {
		return stopFlag;
	}
	
	public synchronized int getThreadID() {
		return threadID;
	}
	
	public synchronized String getThreadState() {
		return state;
	}
}
