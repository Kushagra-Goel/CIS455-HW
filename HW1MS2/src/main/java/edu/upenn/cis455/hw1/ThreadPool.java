package edu.upenn.cis455.hw1;

import java.io.BufferedOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//import org.apache.log4j.Logger;

import edu.upenn.cis455.hw1.interfaces.Session;

public class ThreadPool {

//	static final Logger logger = Logger.getLogger(ThreadPool.class);
	private BlockingQueue queue = null;
	private static List<WorkerThread> threads = new ArrayList<WorkerThread>();
	private static int port;
	private static ServerSocket serverSocket = null;
	private static MyWebService serverInstance = null;
	
	/**
	 * Constructor to instantiate the threadPool
	 * @param maxNumberOfActiveConnections : Maximum number of connections the queue can hold before blocking new connections
	 * @param maxNumberOfThreads : Maximum number of active threads
	 * @param rootDirectory : Root Directory
	 * @param p : Port address
	 * @param sc : Server Socket to initiate shutdown
	 * @param si : Caller HTTPServer to initiate shutdown
	 */
	public ThreadPool(int maxNumberOfActiveConnections, int maxNumberOfThreads, Path rootDirectory, int p, ServerSocket sc, MyWebService si, List<MyRoute> routeList, List<MyFilter> beforeList, List<MyFilter> afterList, InetAddress ipAddress, Map<String, Session> sessionTable) {
		port = p;
		serverSocket = sc;
		serverInstance = si;
		queue = new BlockingQueue(maxNumberOfActiveConnections);
		for(int i = 0; i < maxNumberOfThreads; i++) {
			threads.add(new WorkerThread(queue,  rootDirectory, i + 1, port, routeList, beforeList, afterList, ipAddress, sessionTable));
		}
		for(WorkerThread thread : threads) {
			thread.start();
		}
	}
	
	/**
	 * Adds new connection to the Blocking queue
	 * @param s connection to add
	 */
	public synchronized void enqueueConnection(Socket s) {
		try {
			queue.enqueue(s);
		} catch (InterruptedException e) {
//			logger.info("ThreadPool was interrupted");
			e.printStackTrace();
		}
	}
	
	/**
	 * Method to shutdown the server by showing a shutdown HTML confirmation page and then shutting down each thread
	 * @param s : Socket to send final shutdown confirmation to
	 * @param isGet : HEAD or GET command
	 * @throws Exception
	 */
	public synchronized static void shutdown(Socket s, boolean isHead) throws Exception {
		BufferedOutputStream out = new BufferedOutputStream(s.getOutputStream());
		PrintWriter p = new PrintWriter(out, true);
		String dateNow = Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME);
//		logger.info("Sending Control Page");
		
		// List of HTML page lines
		List<String> htmlControlPage = new ArrayList<String>();
		int contentLength = 0;
		

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
		htmlControlPage.add("<h2>Server has terminated, cya!</h2>");
		htmlControlPage.add("</body>");
		htmlControlPage.add("</html>");
		
		// Compute Content Length
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
		if(!isHead) {
			for(String html: htmlControlPage) {
				p.print(html);
//				logger.debug(html);
			}
		}
		p.flush();
		out.close();
		// Interrupt each worker thread
		for(WorkerThread thread : threads) {
			thread.stopThread();
		}
		// Call parent shutdown
		serverInstance.stop();
		
		// Close All sockets for good measure
		serverSocket.close();
	}
	
	public synchronized static void sendControlPage(Socket s, boolean isHead) throws Exception {
		BufferedOutputStream out = new BufferedOutputStream(s.getOutputStream());
		PrintWriter p = new PrintWriter(out, true);
		String dateNow = Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME);
//		logger.info("Sending Control Page");
		

		// List of HTML page lines
		List<String> htmlControlPage = new ArrayList<String>();
		int contentLength = 0;
		

		htmlControlPage.add("<!DOCTYPE html>");
		htmlControlPage.add("<html>");
		htmlControlPage.add("<body>");
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
		htmlControlPage.add("<h2>Student Information</h2>");
		htmlControlPage.add("<ul>");
		htmlControlPage.add("<li>Student Name : Kushagra Goel</li>");
		htmlControlPage.add("<li>Student Penn ID : kgoel96</li>");
		htmlControlPage.add("<li>Student Penn Key : 28300645</li>");
		htmlControlPage.add("</ul>");		
		htmlControlPage.add("<h2>Thread Information Table</h2>");
		htmlControlPage.add("<table>");
		htmlControlPage.add("<tr>");
		htmlControlPage.add("<th>Thread ID</th>");
		htmlControlPage.add("<th>Thread State</th>");
		htmlControlPage.add("</tr>");
		
		
		// Dynamically get thread IDs and States
		for(WorkerThread thread : threads) {
			htmlControlPage.add("<tr>");
			htmlControlPage.add(String.format("<td>%d</td>", thread.getThreadID()));
			htmlControlPage.add(String.format("<td>%s</td>",thread.getThreadState()));
			htmlControlPage.add("</tr>");
		}

		htmlControlPage.add("</table>");
		
		// Shutdown Button (depends on HTTP or HTTPS connection)
		htmlControlPage.add(String.format("<button onclick=\"window.location.href = 'http://localhost:%d/shutdown';\">Shutdown</button>", port));			
		
		htmlControlPage.add("</body>");
		htmlControlPage.add("</html>");
		
		//Dynamically compute Content Length
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
		if(!isHead) {
			for(String html: htmlControlPage) {
				p.print(html);
//				logger.debug(html);
			}
		}
		p.flush();
		out.close();
		
	}
}
