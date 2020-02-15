package edu.upenn.cis455.hw1;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
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
import java.util.List;
import java.util.Map;

//import org.apache.log4j.Logger;

import edu.upenn.cis455.hw1.interfaces.HaltException;
import edu.upenn.cis455.hw1.interfaces.Session;

public class WorkerThread extends Thread{
//	static final Logger logger = Logger.getLogger(WorkerThread.class);
	private BlockingQueue queue = null;
	private boolean stopFlag = false;
	private Socket currentSocket = null;
	private int threadID;
	private Path rootDirectory;
	private int port;
	private InetAddress ipAddress;
	private String errorMessage = null;
	private List<MyRoute> routeList;
	private List<MyFilter> beforeList, afterList;
	private Map<String, Session> sessionTable;
	private MyRequest request;
	private MyResponse response;
	
	/**
	 * The flag to set the connection to persistent.
	 * The route/filter must set the appropriate header in the response to make the connection persistent
	 */
	private boolean isPersistent = false;
	
	
	/**
	 * Constructor to instantiate worker thread
	 * @param q : the blocking queue
	 * @param p : path of root directory
	 * @param tid : Thread ID
	 * @param tp : Caller Threadpool
	 * @param portArg : Server Port
	 */
	public WorkerThread(BlockingQueue q, Path p, int tid, int portArg, List<MyRoute> routeListArg, List<MyFilter> beforeListArg, List<MyFilter> afterListArg, InetAddress ipAddressArg, Map<String, Session> sessionTableArg) {
		queue = q;
		rootDirectory = p;
		threadID = tid;
		port = portArg;
		ipAddress = ipAddressArg;
		routeList = routeListArg;
		beforeList = beforeListArg;
		afterList = afterListArg;
		sessionTable = sessionTableArg;
	}
	
	/**
	 * The run method that verifies the HTTP request and takes corresponding action
	 */
	public void run() {
		
		// Run till stop flag is set
		while(getStopFlag() == false) {
//			logger.debug(currentSocket == null || currentSocket.isClosed());
			if(currentSocket == null || currentSocket.isClosed()) {
				try {
					currentSocket = queue.dequeue(threadID);
//					logger.debug(String.format("Thread ID %d successfully dequeued a request", threadID));
				} catch (InterruptedException e) {
//					logger.debug(String.format("Thread %d was terminated", threadID));
					continue;
				}
			}

		  try {
			  request = new MyRequest(currentSocket, sessionTable, port, ipAddress.getCanonicalHostName());
			  
			  // Bad HTTP Request
			  if(request.getStatusCode() != 200) {
				  errorMessage = request.getErrorMessage();
				  sendErrorPage(request.getStatusCode(), Instant.now());
				  resetThread();
				  continue;
			  }
			  
			  MyRoute matchingRoute = null;
			  for(MyRoute route: routeList) {
				  if(route.match(request.getRequestMethod(), request)) {
					  matchingRoute = route;
					  break;
				  }
			  }
			  
			  if(matchingRoute != null) {
//				  logger.debug("Found Suitable route for request : " + matchingRoute.getRoutePath());
				  try {
					  response = new MyResponse(request);
					  request.updateParamsAndSplat(matchingRoute);
					  for(MyFilter filter: beforeList) {
						  if(filter.match(request)) {
							  filter.handle(request, response);
						  }
					  }
					  
					  Object result = matchingRoute.handle(request, response);
//					  logger.debug("Handled request");
					  if(result != null) {
						  if(result instanceof String) {
							  response.type("text/html");
							  response.body((String) result);
						  }
//						  if(response.type() == null) {
//							  throw new HaltException(500, "Response Content-Type not set");
//						  }
						  if(result instanceof byte[]) {
							  response.bodyRaw((byte[]) result);
						  }
					  }
					  
					  for(MyFilter filter: afterList) {
						  if(filter.match(request)) {
							  filter.handle(request, response);
						  }
					  }
					  
					  if(request.attribute("Shutdown") != null) {
						  if((boolean)request.attribute("Shutdown") == true) {
//							  logger.info(String.format("Thread ID : %d recieved shutdown request via route", threadID));
							  ThreadPool.shutdown(currentSocket, request.getRequestMethod() == HttpMethod.HEAD);
						  }
					  }
					  else {
						  if(request.attribute("Control") != null) {
							  if((boolean)request.attribute("Control") == true) {
//								  logger.info(String.format("Thread ID : %d recieved control page request via route", threadID));
								  ThreadPool.sendControlPage(currentSocket, request.getRequestMethod() == HttpMethod.HEAD);
							  }							  
						  }
						  else {
							  isPersistent = response.isPersistent();
							  response.sendResponse(currentSocket);							  
						  }
					  }
					  
				  } catch(HaltException e) {
//					  logger.debug("Halt Exception" + e.body());
					  errorMessage = e.body();
					  sendErrorPage(e.statusCode(), Instant.now());
				  }
			  }else {
				  
				  if(rootDirectory == null) {
					  errorMessage = "Root Directory wasn't initialized";
					  sendErrorPage(404, Instant.now());
				  }
				  
				  switch(request.pathInfo()) {
				  case "/control" :{  // Control Page
					  ThreadPool.sendControlPage(currentSocket, request.getRequestMethod() == HttpMethod.HEAD);
//					  logger.info(String.format("Thread ID : %d recieved control page request", threadID));
					  break;}
				  case "/shutdown" :{ // Shutdown request
//					  logger.info(String.format("Thread ID : %d recieved shutdown request", threadID));
					  ThreadPool.shutdown(currentSocket, request.getRequestMethod() == HttpMethod.HEAD);
					  break;}
				  default :{ // File or Directory
					  Path requestURIPath = Paths.get(rootDirectory.toString(), request.pathInfo());
					  requestURIPath = requestURIPath.normalize();
					  File requestedFile = requestURIPath.toFile();
					  
					  // Check if File exists
					  if(!requestedFile.exists()) {
//						  logger.debug(String.format("Thread ID : %d recieved request for non existent file %s", threadID, request.pathInfo()));
						  errorMessage = String.format("File %s not found", rootDirectory.relativize(requestURIPath));
						  sendErrorPage(404, Instant.now());
						  }
					  else {
						  if(requestedFile.isFile()) {
//							  logger.info(String.format("Thread ID : %d recieved file request", threadID));
							  sendFile(requestURIPath);
							  }
						  if(requestedFile.isDirectory()) {
//							  logger.info(String.format("Thread ID : %d recieved directory request", threadID));
							  sendDirectory(requestURIPath);
						}				  
					  }
				  	}
				  }
			  }
			  
			  		  
		} catch(HaltException e) {
//			  logger.debug("Halt Exception" + e.body());
			  errorMessage = e.body();
			  try {
				sendErrorPage(e.statusCode(), Instant.now());
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		  }		  
		  catch (Exception e) {
			e.printStackTrace();
			errorMessage = e.getMessage();
			try {
				sendErrorPage(500, Instant.now());
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
		}
		  // Reset thread local states to original
			resetThread();
		}
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
	  

  	  if(!MyResponse.httpStatusCodes.containsKey(String.valueOf(statusCode))) {
  		  statusCode = 500;
  	  }

	  int contentLength = 0;

	  List<String> htmlErrorPage = new ArrayList<String>();

	  htmlErrorPage.add("<!DOCTYPE html>");
	  htmlErrorPage.add("<html>");
	  htmlErrorPage.add("<head>");
	  htmlErrorPage.add("</head>");
	  htmlErrorPage.add("<body>");
	  htmlErrorPage.add(String.format("<b>Error : </b><p>%d</p>", statusCode));
	  htmlErrorPage.add(String.format("<h3>Error Message: </h3><p>%s</p>", errorMessage));
	  htmlErrorPage.add("</body>");
	  htmlErrorPage.add("</html>");
	  
	  

	for(String sentence : htmlErrorPage) {
		contentLength = contentLength + sentence.length();	
	}


	  OffsetDateTime odt = lastModified.atOffset(ZoneOffset.UTC);
	  p.print(String.format("HTTP/1.1 %d %s\r\n", statusCode, MyResponse.httpStatusCodes.get(String.valueOf(statusCode))));
	  p.print(String.format("Server: %s\r\n", "HttpServer/1.1"));
	  p.print(String.format("Date: %s\r\n", Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME)));
	  p.print(String.format("Last-Modified: %s\r\n", odt.format(DateTimeFormatter.RFC_1123_DATE_TIME)));
	  p.print(String.format("Content-Type: %s\r\n", "text/html"));
	  p.print(String.format("Content-Length: %d\r\n", contentLength));
	  if(!isPersistent) {
		  p.print("Connection: close\r\n");		  
	  }
	  p.print("\r\n");    

	// Send body iff not HEAD request
	  if(request.getRequestMethod() != HttpMethod.HEAD) {
		  for(String html: htmlErrorPage) {
			  p.print(html);
//			  logger.debug(html);
		  }
	  }
	  p.flush();
	  if(!isPersistent) {
		  out.close();
		  currentSocket.close();
//		  logger.info(String.format("Thread ID : %d Closed Connection", threadID));
	  }
	  resetThread();
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
	  lastModifiedHeader = request.headers("if-modified-since");
	  lastUnmodifiedHeader = request.headers("if-unmodified-since");
	  // 3 Acceptable date formats
	  boolean validDateFormat = false;
	  String[] dateFormats = {"EEE, d MMM yyyy HH:mm:ss z", "EEEE, d-MMM-yy HH:mm:ss z", "EEE, MMM d HH:mm:ss yyyy"};
	  for(String format : dateFormats) {
		  try {
			  DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format);
			  if(lastModifiedHeader != null && !validDateFormat) {
				  if(lastModified.isBefore(LocalDateTime.parse(lastModifiedHeader.strip(), dtf).toInstant(ZoneOffset.UTC))) {
					  errorMessage = "File has not been modified since the provided date";
					  sendErrorPage(304, lastModified);
					  return;
				  }				  
			  }
			  validDateFormat = true;
		  }catch(Exception e) {
			  errorMessage = "Illegal Date : " + e.getMessage();
		  }
	  }
	  
	  if(!validDateFormat) {
		  sendErrorPage(400, lastModified);
		  return;		  
	  }
	  
	  validDateFormat = false;
	  for(String format : dateFormats) {
		  try {
			  DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format);
			  if(lastUnmodifiedHeader != null && !validDateFormat) {
				  if(lastModified.isAfter(LocalDateTime.parse(lastUnmodifiedHeader.strip(), dtf).toInstant(ZoneOffset.UTC))) {
					  errorMessage = "File has been modified since the provided date";
					  sendErrorPage(412, lastModified);
					  return;
				  }				  
			  }
			  validDateFormat = true;
		  }catch(Exception e) {
			  errorMessage = "Illegal Date : " + e.getMessage();
		  }
	  }

	  if(!validDateFormat) {
		  sendErrorPage(400, lastModified);
		  return;		  
	  }
	  
	  BufferedInputStream is = new BufferedInputStream(new FileInputStream(requestedFile));
	  BufferedOutputStream out = new BufferedOutputStream(currentSocket.getOutputStream());
	  PrintWriter p = new PrintWriter(out, true);
	  
	  p.print("HTTP/1.1 200 OK\r\n");
	  p.print(String.format("Server: %s\r\n", "HttpServer/1.1"));
	  p.print(String.format("Date: %s\r\n", Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME)));
	  p.print(String.format("Last-Modified: %s\r\n", odt.format(DateTimeFormatter.RFC_1123_DATE_TIME)));
	  if(!isPersistent) {
		  p.print("Connection: close\r\n");		  
	  }
	  p.print(String.format("Content-Type: %s\r\n", Files.probeContentType(requestURIPath)));
	  p.print(String.format("Content-Length: %s\r\n", Long.toString(requestedFile.length())));
	  p.print("\r\n");
	  p.flush();	  

	// Send body iff not HEAD request
	  if(request.getRequestMethod() != HttpMethod.HEAD) {
		  byte[] someBytes = new byte[8192];
		  int numBytes;
		  while((numBytes = is.read(someBytes)) > 0) {
		  out.write(someBytes, 0, numBytes);
	  }
//		  logger.info("Binary File transfered");		
	  }
	  is.close();
	  out.flush();
	  if(!isPersistent) {	  
		  out.close();
		  currentSocket.close();
	  }
	  resetThread();
//	  logger.info(String.format("Thread ID : %d Closed Connection", threadID));
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
			htmlControlPage.add(String.format("<a href = 'http://localhost:%d/%s'>../</a>", port, rootDirectory.relativize(requestURIPath.getParent())));	
//			logger.debug(rootDirectory.relativize(requestURIPath.getParent()));			
			htmlControlPage.add("</li>");			
		}
		
		
		// Adds file and folders with hyperlinks
		for(File f : requestURIPath.toFile().listFiles()) {
			htmlControlPage.add("<li>");
			htmlControlPage.add(String.format("<a href = 'http://localhost:%d/%s'>%s%s</a>", port, rootDirectory.relativize(Paths.get(f.getAbsolutePath())), f.getName(),(f.isDirectory())?"/":""));
//			logger.debug(rootDirectory.relativize(Paths.get(f.getAbsolutePath())).toString());
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
		  if(!isPersistent) {
			  p.print("Connection: close\r\n");		  
		  }
		p.print(String.format("Content-Type: %s\r\n", "text/html"));
		p.print(String.format("Content-Length: %d\r\n", contentLength));
		p.print("\r\n");
		
		// Send body iff not HEAD request
		if(request.getRequestMethod() != HttpMethod.HEAD) {
			for(String html: htmlControlPage) {
				p.print(html);
//				logger.debug(html);
			}
		}
		p.flush();

		if(!isPersistent) {
			out.close();
			currentSocket.close();
//			logger.info(String.format("Thread ID : %d Closed Connection", threadID));
		}
		resetThread();
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
//				logger.info(String.format("Exception closing client socket for thread %d", threadID));
				e.printStackTrace();
			}
		this.interrupt();
	}
	
	/**
	 * Reset threads local state (memory)
	 */
	private void resetThread() {

		if(!isPersistent) {
			currentSocket = null;
			request = null;
			response = null;
		}
		isPersistent = false;
		errorMessage = null;
	}
	
	
	private synchronized boolean getStopFlag() {
		return stopFlag;
	}
	
	public synchronized int getThreadID() {
		return threadID;
	}
	
	public synchronized String getThreadState() {
		return (request == null)?"Waiting on a connection":request.pathInfo();
	}

}
