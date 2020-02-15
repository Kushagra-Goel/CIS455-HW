package edu.upenn.cis455.crawler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import edu.upenn.cis455.crawler.info.HttpResponse;
import edu.upenn.cis455.crawler.info.RobotsTxtInfo;
import edu.upenn.cis455.crawler.info.URLFrontier;
import edu.upenn.cis455.crawler.info.URLInfo;
import edu.upenn.cis455.storage.CrawledDocument;
import edu.upenn.cis455.storage.DocStore;

/**
 * Threads that crawl the servers
 * @author cis455
 *
 */
public class CrawlerThreads extends Thread{
	
	static final Logger logger = Logger.getLogger(CrawlerThreads.class);

	private static URLFrontier<URLInfo> urlFrontier = null;

	private DocStore docStore = null;
	private static int maxSize;
	private static int numFiles;
	private static InetAddress hostName;
	private static DatagramSocket dataGramSocket;
	private static ConcurrentHashMap<String, RobotsTxtInfo> robotMap;
	
	private HttpsURLConnection connection = null;
	private Socket s = null;

	public volatile boolean blockedAtDeque = false;
	private volatile boolean done = false;
	private int threadID;
	
	

	private volatile int numHTML = 0;
	private volatile int numXML = 0;
	private volatile int dataDownloaded = 0;
	private volatile int numServers = 0;
	
	public CrawlerThreads(URLFrontier<URLInfo> urlFrontierArg, int maxSizeArg, int numFilesArg, InetAddress hostNameArg, DatagramSocket dataGramSocketArg, DocStore docStoreArg, ConcurrentHashMap<String, RobotsTxtInfo> robotMapArg ,int threadIDArg) {
		urlFrontier = urlFrontierArg;
		maxSize = maxSizeArg;
		numFiles = numFilesArg;
		hostName = hostNameArg;
		dataGramSocket = dataGramSocketArg; 
		docStore = docStoreArg;
		robotMap = robotMapArg;
		threadID = threadIDArg;
	}
	
	/**
	 * Generic send request function that returns the corresponding response.
	 * @param url
	 * @param type
	 * @param crawlTimeStamp
	 * @return
	 * @throws IOException
	 */
	private HttpResponse sendRequest(URLInfo url, String type, Instant crawlTimeStamp) throws IOException {
		HttpResponse response = null;
		if(url.getProtocol().equals("https")) {
			URL httpsURL = new URL(url.getFullURL());
			connection = (HttpsURLConnection) httpsURL.openConnection();
			connection.setInstanceFollowRedirects(false);
			connection.setRequestMethod(type);
			connection.setRequestProperty("Host", url.getHostName());
			connection.setRequestProperty("User-Agent", "cis455crawler");
			if(crawlTimeStamp != null) {
				connection.setIfModifiedSince(crawlTimeStamp.toEpochMilli());
			}
			response = new HttpResponse(connection.getInputStream(), connection, type.equals("GET"));
			connection.disconnect();
			connection = null;
	
			/* The commands below need to be run for every single URL */
			byte[] data = ("kgoel96;"+ url.getFullURL()).getBytes();
			DatagramPacket packet = new DatagramPacket(data, data.length, hostName, 10455);
			dataGramSocket.send(packet);
			  
			
		} else {
			if(url.getProtocol().equals("http")) {
				s = new Socket(InetAddress.getByName(url.getHostName()), 80);
				PrintWriter p = new PrintWriter(new BufferedWriter(new OutputStreamWriter(s.getOutputStream())), true);
				p.print(String.format("%s %s HTTP/1.1\r\n",type,  url.getFilePath()));
				p.print(String.format("Host: %s\r\n", url.getHostName()));
				p.print(String.format("User-Agent: %s\r\n", "cis455crawler"));
				if(crawlTimeStamp != null) {
					p.print(String.format("If-Modified-Since: %s\r\n", crawlTimeStamp.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME)));
				}
				p.print("\r\n");
				p.flush();	

				
				InputStream is = s.getInputStream();
				response = new HttpResponse(is, null, type.equals("GET"));
				p.close();
				is.close();
				s.close();
				s = null;
				
				/* The commands below need to be run for every single URL */
				byte[] data = ("kgoel96;"+ url.getFullURL()).getBytes();
//				logger.debug(String.format("Thread ID %d sent UDP packet %s", threadID, new String(data)));
				DatagramPacket packet = new DatagramPacket(data, data.length, hostName, 10455);
				dataGramSocket.send(packet);
				
			}
		}
		if(response.isValid()) return response;
		
		return null;
	}
	
	
	/**
	 * Like a thread in the threadpool, keeps extracting the URLs till either the max number of files are reached or 
	 * all threads except this one are blocked (frontier is completely empty)
	 */
	public void run() {
//		try {
//			
//			while(!done && (urlFrontier.getNumberOfURLCrawled() < numFiles || numFiles == -1) && !XPathCrawler.allBlockedAtDeque(threadID)) {
//
//				logger.debug(String.format("Thread ID %d went in the for loop", threadID));
//				
//				URLInfo url = urlFrontier.deque(this);
//
//				if(url.getHostName() == null)continue;
//				
//
//				RobotsTxtInfo robot;
//				
//				/**
//				 * Robots.txt logic
//				 */
//				if(!robotMap.containsKey(url.getHostName())) {
//					
//					numServers = numServers + 1;
//					
//					HttpResponse robotResponse = sendRequest(new URLInfo(String.format("%s://%s/robots.txt", url.getProtocol(), url.getHostName())), "GET", null);
//					
//					robot = new RobotsTxtInfo();	
//					BufferedReader robotReader = new BufferedReader(new StringReader(robotResponse.getContent()));
//					String robotLine = robotReader.readLine();
//					String agent = null;
//					if(robotLine != null) {
//						while(robotLine != null) {
//							if(robotLine.toLowerCase().startsWith("user-agent: ")) {
//								agent = robotLine.substring(robotLine.indexOf(":") + 1).trim();
//								robot.addUserAgent(agent);
//							}
//							if(robotLine.toLowerCase().startsWith("disallow: ")) {
//								robot.addDisallowedLink(agent, robotLine.substring(robotLine.indexOf(":") + 1).trim());
//							}
//							if(robotLine.toLowerCase().startsWith("allow: ")) {
//								robot.addAllowedLink(agent, robotLine.substring(robotLine.indexOf(":") + 1).trim());
//							}
//							if(robotLine.toLowerCase().startsWith("crawl-delay: ")) {
//								robot.addCrawlDelay(agent, Integer.parseInt(robotLine.substring(robotLine.indexOf(":") + 1).trim()));
//							}
//							robotLine = robotReader.readLine();
//						}
//					}
//					robotMap.put(url.getHostName(), robot);
//					
//				}else {
//					robot = robotMap.get(url.getHostName());					
//				}
//				
//				boolean allowed = true;
//				boolean pastDelay = true;
//				
//				if(robot.containsUserAgent("cis455crawler")) {
//					for(String link: robot.getDisallowedLinks("cis455crawler")) {
//						if(url.getFilePath().startsWith(link)) {
//							allowed = false;
//							break;
//						}
//					}
//					pastDelay = robot.ifPastDelay("cis455crawler");
//				}else {
//					if(robot.containsUserAgent("*")) {
//						for(String link: robot.getDisallowedLinks("*")) {
//							if(url.getFilePath().startsWith(link)) {
//								allowed = false;
//								break;
//							}
//					}
//					pastDelay = robot.ifPastDelay("*");
//					}
//				}
//					
//				/**
//				 * If the file path is not disallowed
//				 */
//				if(!allowed) {
//					logger.info(String.format("Thread ID %d is denied access to URL %s", threadID, url.getFullURL()));
//					continue;
//				}
//				
//				/**
//				 * If there is a crawl delay, then reinsert the URL to the frontier and continue
//				 */
//				if(!pastDelay) {
//					urlFrontier.enqueue(url);
//					logger.debug(String.format("Thread ID %d is gonna wait to access URL %s", threadID, url.getFullURL()));
//					continue;	
//				}
//
//				if(!(urlFrontier.getNumberOfURLCrawled() < numFiles || numFiles == -1))break;
//				
//				CrawledDocument doc = docStore.getDocument(url);
//							
//				
//				if(doc != null) logger.info(String.format("Thread ID %d is found URL %s in BDB created at %s", threadID, url.getFullURL(), doc.getCrawlTimestamp().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME)));
//				
//
//				
//				urlFrontier.incrementNumURLCrawled();
//				logger.debug(String.format("Thread ID %d is Parsing URL %s", threadID, url.getFullURL()));
//				
//				// No crawlTimestamp for document never crawled before
//				HttpResponse response = sendRequest(url, "HEAD", (doc == null)? null: doc.getCrawlTimestamp());
//
//				if(response == null)continue;
//
//				logger.info(String.format("Thread ID %d got Code %d for URL %s updated on %s", threadID, response.getStatusCode() ,url.getFullURL(), response.getLastModified().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME)));
//
//				
//				switch(response.getStatusCode()) {
//					case 301:{	//redirect; cascades down to 307					
//					}
//					case 302:{	//redirect; cascades down to 307					
//					}
//					case 307:{
//						if(response.getLocation() != null) urlFrontier.enqueue(new URLInfo(response.getLocation()));
//						logger.info(String.format("Thread ID %d was redirected by URL %s to %s", threadID, url.getFullURL(), response.getLocation()));
//						break;
//					}
//					case 200: { // new document or updated one; cascades to 304
//						if(response.getContentLength() > maxSize) break;
//						response = sendRequest(url, "GET", null);
//						Instant lastModified = Instant.EPOCH;
//						if(response.getLastModified() == null) {
//							if(doc != null) {
//								if(doc.getCrawlTimestamp() != null)
//									lastModified = doc.getCrawlTimestamp();
//							}
//						}else {
//							lastModified = response.getLastModified();
//						}
//						System.out.println(String.format("%s: Downloading", url.getFullURL()));
//						doc = new CrawledDocument(url, response.getContentLength(), response.getContentType(), response.getContent(), lastModified);
//						dataDownloaded = dataDownloaded + doc.getDocLength();
//						logger.debug(String.format("Recieved 200 for URL %s", url.getFullURL()));
//					}
//					case 304: { // Nothing changed, just extract links if HTML 
//						if(doc.getDocLength() > maxSize)continue;
//						
//						if(response.getStatusCode() == 304) {
//							System.out.println(String.format("%s: Not Modified", url.getFullURL()));
//						}
//						
//						if(doc.isHTML()) {
//							numHTML = numHTML + 1;
//							Document jsoupDoc = Jsoup.parse(doc.getDoc(), url.getFullURL());
//							for(Element link: jsoupDoc.select("a[href]")) {
//								URLInfo extractedURL = new URLInfo(link.attr("abs:href"));
//								// Check if the URI has already been seen or added to the frontier
//								if(!urlFrontier.isCrawled(extractedURL)) {
//									urlFrontier.enqueue(extractedURL);
//									logger.info(String.format("Thread ID %d is Adding URL %s with Base URL %s", threadID, link.attr("abs:href"), url.getFullURL()));	
//								}	
//								else {
//									logger.info(String.format("Thread ID %d has already crawled URL %s", threadID, link.attr("abs:href")));
//								}
//							}
//						}
//						if(doc.isXML()) {
//							numXML = numXML + 1;
//						}
//						
//						
//						docStore.addDocument(doc);
//						break;
//					}
//				}
//				
//			}
//			if(!done) {
//				logger.debug(String.format("Thread ID : %d is terminating", threadID));
//				XPathCrawler.terminateCrawl();
//				}
//			
//		}catch(InterruptedException e){
//			logger.debug(String.format("Thread ID %d was Interrupted", threadID));		
//		}
//		catch(SocketException e){
//			logger.error(String.format("Thread ID %d recieved SocketException: %s", threadID, e.getMessage()));			
//		} catch (IOException e) {
//			logger.error(String.format("Thread ID %d recieved IOException: %s", threadID, e.getMessage()));
//		}
	}

	public boolean isBlockedAtDeque() {
		return blockedAtDeque;
	}
	
	public int getThreadID() {
		return threadID;
	}
	
	public void close() {
		done = true;
		if(connection != null)connection.disconnect();
		if(s != null)
			try {
				s.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		if(blockedAtDeque)this.interrupt();
	}


	/**
	 * @return the numHTML
	 */
	public int getNumHTML() {
		return numHTML;
	}


	/**
	 * @return the numXML
	 */
	public int getNumXML() {
		return numXML;
	}


	/**
	 * @return the dataDownloaded
	 */
	public int getDataDownloaded() {
		return dataDownloaded;
	}


	/**
	 * @return the numServers
	 */
	public int getNumServers() {
		return numServers;
	}
}
