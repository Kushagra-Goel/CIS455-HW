package edu.upenn.cis455.crawler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;

import edu.upenn.cis.stormlite.OutputFieldsDeclarer;
import edu.upenn.cis.stormlite.TopologyContext;
import edu.upenn.cis.stormlite.bolt.IRichBolt;
import edu.upenn.cis.stormlite.bolt.OutputCollector;
import edu.upenn.cis.stormlite.routers.IStreamRouter;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis.stormlite.tuple.Tuple;
import edu.upenn.cis.stormlite.tuple.Values;
import edu.upenn.cis455.crawler.info.HttpResponse;
import edu.upenn.cis455.crawler.info.RobotsTxtInfo;
import edu.upenn.cis455.crawler.info.URLFrontier;
import edu.upenn.cis455.crawler.info.URLInfo;
import edu.upenn.cis455.storage.CrawledDocument;

public class CrawlerBolt implements IRichBolt{
	static Logger logger = Logger.getLogger(CrawlerBolt.class);
	
	Fields schema = new Fields("CrawledDoc", "URL");
	
    
    /**
     * To make it easier to debug: we have a unique ID for each
     * instance of the WordCounter, aka each "executor"
     */
    String executorId = UUID.randomUUID().toString();
    
    /**
     * This is where we send our output stream
     */
    private OutputCollector collector;

	private ConcurrentHashMap<String, RobotsTxtInfo> robotMap;
	private static URLFrontier<URLInfo> urlFrontier;
    
    public CrawlerBolt() {
    }
    
    /**
     * Initialization, just saves the output stream destination
     */
    @Override
    public void prepare(Map<String,String> stormConf, 
    		TopologyContext context, OutputCollector collector) {
        this.collector = collector;
        robotMap = XPathCrawler.getRobotMap();
    	urlFrontier = XPathCrawler.getUrlFrontier();
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
			HttpsURLConnection connection = (HttpsURLConnection) httpsURL.openConnection();
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
			DatagramPacket packet = new DatagramPacket(data, data.length, XPathCrawler.getHost(), 10455);
			XPathCrawler.getDataGramSocket().send(packet);
			  
			
		} else {
			if(url.getProtocol().equals("http")) {
				Socket s = new Socket(InetAddress.getByName(url.getHostName()), 80);
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
//				logger.info(String.format("Thread ID %s sent UDP packet %s", threadID, new String(data)));
				DatagramPacket packet = new DatagramPacket(data, data.length, XPathCrawler.getHost(), 10455);
				XPathCrawler.getDataGramSocket().send(packet);
				
			}
		}
		if(response.isValid()) return response;
		
		return null;
	}
	
	

    /**
     * Process a tuple received from the stream, incrementing our
     * counter and outputting a result
     */
    @Override
    public void execute(Tuple input) {
    	// Increment number of bolts in running
		XPathCrawler.boltLedger(true);
		// Signal that crawling has started 
		XPathCrawler.setFirstCrawl(true);
		
		try {
	        URLInfo url = (URLInfo) input.getObjectByField("URL");
	        
			if(url.getHostName() == null) {
				logger.info("HostName is null");
				XPathCrawler.boltLedger(false);
				return;
			}

	
			RobotsTxtInfo robot;
			
			/**
			 * Robots.txt logic
			 */
			if(!robotMap.containsKey(url.getHostName())) {
				
				HttpResponse robotResponse;
				robotResponse = sendRequest(new URLInfo(String.format("%s://%s/robots.txt", url.getProtocol(), url.getHostName())), "GET", null);
				robot = new RobotsTxtInfo();	
				BufferedReader robotReader = new BufferedReader(new StringReader(robotResponse.getContent()));
				String robotLine = robotReader.readLine();
				String agent = null;
				if(robotLine != null) {
					while(robotLine != null) {
						if(robotLine.toLowerCase().startsWith("user-agent: ")) {
							agent = robotLine.substring(robotLine.indexOf(":") + 1).trim();
							robot.addUserAgent(agent);
						}
						if(robotLine.toLowerCase().startsWith("disallow: ")) {
							robot.addDisallowedLink(agent, robotLine.substring(robotLine.indexOf(":") + 1).trim());
						}
						if(robotLine.toLowerCase().startsWith("allow: ")) {
							robot.addAllowedLink(agent, robotLine.substring(robotLine.indexOf(":") + 1).trim());
						}
						if(robotLine.toLowerCase().startsWith("crawl-delay: ")) {
							robot.addCrawlDelay(agent, Integer.parseInt(robotLine.substring(robotLine.indexOf(":") + 1).trim()));
						}
						robotLine = robotReader.readLine();
					}
				}
				robotMap.put(url.getHostName(), robot);
				
			}else {
				robot = robotMap.get(url.getHostName());					
			}
			
			boolean allowed = true;
			boolean pastDelay = true;
			
			if(robot.containsUserAgent("cis455crawler")) {
				for(String link: robot.getDisallowedLinks("cis455crawler")) {
					if(url.getFilePath().startsWith(link)) {
						allowed = false;
						break;
					}
				}
				pastDelay = robot.ifPastDelay("cis455crawler");
			}else {
				if(robot.containsUserAgent("*")) {
					for(String link: robot.getDisallowedLinks("*")) {
						if(url.getFilePath().startsWith(link)) {
							allowed = false;
							break;
						}
				}
				pastDelay = robot.ifPastDelay("*");
				}
			}

			/**
			 * If the file path is not disallowed
			 */
			if(!allowed) {
				logger.info(String.format("Denied access to URL %s", url.getFullURL()));
				XPathCrawler.boltLedger(false);
				return;
			}
			
			
			/**
			 * If there is a crawl delay, then reinsert the URL to the frontier and continue
			 */
			if(!pastDelay) {
				urlFrontier.enqueue(url);
				logger.info(String.format("Thread ID %s is gonna wait to access URL %s", getExecutorId(), url.getFullURL()));
				XPathCrawler.boltLedger(false);
				return;	
			}
	
			if(!(urlFrontier.getNumberOfURLCrawled() < XPathCrawler.getNumFiles() || XPathCrawler.getNumFiles() == -1)) {
				XPathCrawler.boltLedger(false);
				return;
			}
			
			CrawledDocument doc = XPathCrawler.getDocStore().getDocument(url);
						
			
			if(doc != null) logger.info(String.format("Found URL %s in BDB created at %s", url.getFullURL(), doc.getCrawlTimestamp().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME)));
			
	
			
			urlFrontier.incrementNumURLCrawled();
			logger.info(String.format("Parsing URL %s", url.getFullURL()));
			
			// No crawlTimestamp for document never crawled before
			HttpResponse response = sendRequest(url, "HEAD", (doc == null)? null: doc.getCrawlTimestamp());
	
			if(response == null) {
				XPathCrawler.boltLedger(false);
				return;
			}
	
			logger.info(String.format("Got Code %d for URL %s updated on %s", response.getStatusCode() ,url.getFullURL(), response.getLastModified().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME)));
	
			
			switch(response.getStatusCode()) {
				case 301:{	//redirect; cascades down to 307					
				}
				case 302:{	//redirect; cascades down to 307					
				}
				case 307:{
					if(response.getLocation() != null) urlFrontier.enqueue(new URLInfo(response.getLocation()));
					logger.info(String.format("Was redirected by URL %s to %s", url.getFullURL(), response.getLocation()));
					break;
				}
				case 200: { // new document or updated one; cascades to 304
					if(response.getContentLength() > XPathCrawler.getMaxSize()) break;
					response = sendRequest(url, "GET", null);
					Instant lastModified = Instant.EPOCH;
					if(response.getLastModified() == null) {
						if(doc != null) {
							if(doc.getCrawlTimestamp() != null)
								lastModified = doc.getCrawlTimestamp();
						}
					}else {
						lastModified = response.getLastModified();
					}
					System.out.println(String.format("%s: Downloading", url.getFullURL()));
					doc = new CrawledDocument(url, response.getContentLength(), response.getContentType(), response.getContent(), lastModified);
					logger.info(String.format("Recieved 200 for URL %s", url.getFullURL()));
				}
				case 304: { // Nothing changed, just extract links if HTML 
					if(doc.getDocLength() > XPathCrawler.getMaxSize()) {

						XPathCrawler.boltLedger(false);
						return;
					}
					
					if(response.getStatusCode() == 304) {
						System.out.println(String.format("%s: Not Modified", url.getFullURL()));
					}
					
					logger.info(String.format("Emitting : %s", doc.getUrl().getFullURL()));
					collector.emit(new Values<Object>(doc, url));
					break;
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
		} catch (NullPointerException e) {
//			e.printStackTrace();
		}
		//Decrement number of running bolts
		XPathCrawler.boltLedger(false);
    }

    /**
     * Shutdown, just frees memory
     */
    @Override
    public void cleanup() {
    }

    /**
     * Lets the downstream operators know our schema
     */
    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(schema);
    }

    /**
     * Used for debug purposes, shows our exeuctor/operator's unique ID
     */
	@Override
	public String getExecutorId() {
		return executorId;
	}

	/**
	 * Called during topology setup, sets the router to the next
	 * bolt
	 */
	@Override
	public void setRouter(IStreamRouter router) {
		this.collector.setRouter(router);
	}

	/**
	 * The fields (schema) of our output stream
	 */
	@Override
	public Fields getSchema() {
		return schema;
	}

}
