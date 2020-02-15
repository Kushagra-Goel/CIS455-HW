package edu.upenn.cis455.crawler;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.upenn.cis.stormlite.Config;
import edu.upenn.cis.stormlite.LocalCluster;
import edu.upenn.cis.stormlite.Topology;
import edu.upenn.cis.stormlite.TopologyBuilder;
import edu.upenn.cis455.crawler.info.RobotsTxtInfo;
import edu.upenn.cis455.crawler.info.URLFrontier;
import edu.upenn.cis455.crawler.info.URLInfo;
import edu.upenn.cis455.storage.ChannelStore;
import edu.upenn.cis455.storage.DBWrapper;
import edu.upenn.cis455.storage.DocStore;
import edu.upenn.cis455.storage.UserStore;


/** (MS1, MS2) The main class of the crawler.
  */
public class XPathCrawler {
	static final Logger logger = Logger.getLogger(XPathCrawler.class);

	private static String startUrl;
	private static String dbDirectory;
	private static int maxSize = 10 * 1000000;
	private static int numFiles = -1;

	private static volatile boolean firstCrawl = false; 
	
	private static volatile int currentBolts = 0;
	public static DocStore getDocStore() {
		return docStore;
	}


	public static void setNumFiles(int numFiles) {
		XPathCrawler.numFiles = numFiles;
	}


	public static int getMaxSize() {
		return maxSize;
	}


	public static int getNumFiles() {
		return numFiles;
	}

	private static InetAddress host;
	private static DatagramSocket dataGramSocket;
	private static URLFrontier<URLInfo> urlFrontier = new URLFrontier<URLInfo>(1024);
	public static URLFrontier<URLInfo> getUrlFrontier() {
		return urlFrontier;
	}

	private static ConcurrentHashMap<String, RobotsTxtInfo> robotMap = new ConcurrentHashMap<String, RobotsTxtInfo>();
	private static DocStore docStore = null;
	private static ChannelStore channelStore = null;
   

	private static final String CRAWLER_TOPOLOGY = "CRAWLER_TOPOLOGY";

	private static final String CRAWLER_SPOUT = "CRAWLER_SPOUT";
	private static final String CRAWLER_BOLT = "CRAWLER_BOLT";
	private static final String DOCUMENTPARSER_BOLT = "DOCUMENTPARSER_BOLT";
	private static final String CHANNELMATCHING_BOLT = "CHANNELMATCHING_BOLT";
	private static final String URLFILTER_BOLT = "URLFILTER_BOLT";
	
  public static void main(String args[])
  {
	  if(args.length < 3 || args.length > 5) {
		  throw new IllegalArgumentException("Argument : start_url database_directory max_size_in_megabytes <num_files> <monitoring_hostname>");
	  }

	  LocalCluster cluster = new LocalCluster();
	  try {
		  startUrl = args[0];
		  dbDirectory = args[1];
		  maxSize = (int) (Double.parseDouble(args[2]) * 1000000);


		  String monitoringHostName = "cis455.cis.upenn.edu";
		  if(args.length == 4) {
			  try{
				  numFiles = Integer.parseInt(args[3]);
			  }catch(NumberFormatException e) {
				  monitoringHostName = args[3];
			  }
		  }
		  if(args.length == 5) {
			  numFiles = Integer.parseInt(args[3]);
			  monitoringHostName = args[4];
		  }
		  
		  /* The first two commands need to be run only once */
		  host = InetAddress.getByName(monitoringHostName);
		  dataGramSocket = new DatagramSocket();
		  
		  
		  DBWrapper db = new DBWrapper(dbDirectory);
		  
		  docStore = new DocStore(db);
		  channelStore = new ChannelStore(db);
		  
		  urlFrontier.enqueue(new URLInfo(startUrl));
		  
		  
		  Config config = new Config();
	       
	       
		  CrawlerSpout crawlerSpout = new CrawlerSpout();
		  CrawlerBolt crawlerBolt = new CrawlerBolt();
		  DocumentParserBolt documentParserBolt = new DocumentParserBolt();
		  ChannelMatchingBolt channelMatchingBolt = new ChannelMatchingBolt();
		  URLFilterBolt urlFilterBolt = new URLFilterBolt();
				
		  TopologyBuilder builder = new TopologyBuilder();

		  builder.setSpout(CRAWLER_SPOUT, crawlerSpout, 1);
		  builder.setBolt(CRAWLER_BOLT, crawlerBolt, 4).shuffleGrouping(CRAWLER_SPOUT);
		  builder.setBolt(DOCUMENTPARSER_BOLT, documentParserBolt, 4).shuffleGrouping(CRAWLER_BOLT);
		  builder.setBolt(CHANNELMATCHING_BOLT, channelMatchingBolt, 4).shuffleGrouping(DOCUMENTPARSER_BOLT);
		  builder.setBolt(URLFILTER_BOLT, urlFilterBolt, 4).shuffleGrouping(CHANNELMATCHING_BOLT);
			
			
		  Topology topo = builder.createTopology();
	       
		  ObjectMapper mapper = new ObjectMapper();
		  try {
			  String str = mapper.writeValueAsString(topo);
				
			  System.out.println("The StormLite topology is:\n" + str);
		  } catch (JsonProcessingException e) {
			  // TODO Auto-generated catch block
    	 	 e.printStackTrace();
		  }
			
			
		  cluster.submitTopology(CRAWLER_TOPOLOGY, config, builder.createTopology());

		  logger.info("Submitted topology");
			
			while(getBoltCount() > 0 || !urlFrontier.isFrontierEmpty() || !isFirstCrawl()) {
				Thread.sleep(100);
			}
			
			
		  logger.info(String.format("Exiting after crawling %d documents", urlFrontier.getNumberOfURLCrawled()));
		  
	  }catch(Exception e) {
		  e.printStackTrace();
	  }	  


	  if(docStore != null)docStore.close();
	  cluster.killTopology(CRAWLER_TOPOLOGY);
	  cluster.shutdown();
	  System.exit(0);
	  
	  
  }
  
  /**
   * Method to keep track of running bolts
   * @param startingBolt
   */
  public static synchronized void boltLedger(boolean startingBolt) {
	  if(startingBolt) currentBolts++;
	  else currentBolts--;
  }
  
  public static synchronized int getBoltCount() {
	  return currentBolts;
  }

/**
 * @return the robotMap
 */
public static ConcurrentHashMap<String, RobotsTxtInfo> getRobotMap() {
	return robotMap;
}


/**
 * @return the dataGramSocket
 */
public static DatagramSocket getDataGramSocket() {
	return dataGramSocket;
}


/**
 * @return the host
 */
public static InetAddress getHost() {
	return host;
}


/**
 * @return the channelStore
 */
public static ChannelStore getChannelStore() {
	return channelStore;
}


/**
 * @return the firstCrawl
 */
public synchronized static boolean isFirstCrawl() {
	return firstCrawl;
}


/**
 * @param firstCrawl the firstCrawl to set
 */
public synchronized static void setFirstCrawl(boolean firstCrawl) {
	XPathCrawler.firstCrawl = firstCrawl;
}
  
//  
//  
//	public static void main(String args[]) throws NoSuchAlgorithmException
//	  {
//	    port(8080);
//
//	    get("/", (request,response) -> {
//	    	if(request.session(false) == null) {
//	        	return "<html><head></head><body>Please enter your username and password: <form action=\"/login\" method=\"POST\">\n" + 
//	        			"    <input type=\"text\" name=\"username\" placeholder=\"Username\"><br>\n" + 
//	        			"    <input type=\"password\" name=\"password\" placeholder=\"Password\"><br>\n" + 
//	        			"    <input type=\"submit\" value=\"Log in\">\n" + 
//	        			"</form></body></html>" + 
//	        			"<a href=\"/newaccount\">New Account Page</a>";
//	    	}
//	        String firstname = (String)(request.session().attribute("firstname"));
//	        String lastname = (String)(request.session().attribute("lastname"));
//	        String result = "<html><body>Welcome " + firstname + " " + lastname + "<br>\n";
//	        
//	        if(isCrawlerRunning) {
//	        	result = result + addStats();
//	        }else {
//	        	result = result + configureCrawler();
//	        }
//	        
//	        result = result  + "<a href=\"/logout\">Log out</a></body></html>";
//	        
//	        return result;
//	    });
//
//	    get("/newaccount", (request,response) -> {
//	    	return "<html><head></head><body>Please enter your firstname, lastname, username and password: <form action=\"/register\" method=\"POST\">\n" + 
//	    			"    <input type=\"text\" name=\"firstname\" placeholder=\"First Name\"><br>\n" + 
//	    			"    <input type=\"text\" name=\"lastname\" placeholder=\"Last Name\"><br>\n" + 
//	    			"    <input type=\"text\" name=\"username\" placeholder=\"Username\"><br>\n" + 
//	    			"    <input type=\"password\" name=\"password\" placeholder=\"Password\"><br>\n" + 
//	    			"    <input type=\"submit\" value=\"Register\">\n" + 
//	    			"</form></body></html>";
//	    });
//	    
//	    post("/register", (request, response) -> {
//	    	try {
//		    	User user = userStore.getUserInfo(request.queryParams("username"));
//		    	if(user != null) {
//		    		return "<html><head></head><body>Error : The user already exists.</body></html>";    		
//		    	}
//		    	userStore.addUserInfo(new User(request.queryParams("firstname"), request.queryParams("lastname"), request.queryParams("username"), request.queryParams("password")));
//		    	response.redirect("/");
//			} catch(Exception e) {
//				e.printStackTrace();
//			}
//	    	return null;
//	      });
//	    
//	    
//	    //Needs to get the bdb path from command line
//	    // and same should be given via the web interface too as we have only one DBWrapper.
//	    userStore = new UserStore(args[0]);
//	    userStore.addUserInfo(new User("Admin", "Strator", "root", "Alpine"));
//
//
//	    post("/login", (request, response) -> {
//	    	try {
//	    		if(!request.queryParams("username").equals("root"))return "<html><head></head><body>Error : Incorrect Credentials</body></html>"; 
//				MessageDigest digest = MessageDigest.getInstance("SHA-256");
//		    	if(!Arrays.equals(digest.digest("Alpine".getBytes()), digest.digest(request.queryParams("password").getBytes(StandardCharsets.UTF_8)))) {
//		    		return "<html><head></head><body>Error : Incorrect Credentials</body></html>";     		
//		    	}
//		    	User user = userStore.getUserInfo(request.queryParams("username"));
//		    	request.session().attribute("firstname", user.getFirstName());
//		    	request.session().attribute("lastname", user.getLastName());
//		    	response.redirect("/");
//	    	} catch(Exception e) {
//	    		e.printStackTrace();
//	    	}
//	    	return null;
//	    });
//	    
//	    
//
//
//	    post("/startCrawl", (request, response) -> {
//	    	if(request.session(false) == null) {
//	    		response.redirect("/");
//	    		return null;
//	    	}
//	    	try {
//	  		  startUrl = request.queryParams("startURL");
//	  		  dbDirectory = request.queryParams("bdbDirectory");
//	  		  
//	  		  if(!dbDirectory.equals(args[0]))halt(422, "DataBase needs to be started in the same path");
//	  		  maxSize = (int) (Double.parseDouble(request.queryParams("maxSize")) * 1000000);
//
//	  		  if(!request.queryParams("numFiles").isEmpty()) {
//	  			  try{
//	  				  numFiles = Integer.parseInt(request.queryParams("numFiles"));
//	  			  }catch(NumberFormatException e) {
//	  				numFiles = 50;
//	  			  }
//	  		  }
//	  		  
//	  		  if(!request.queryParams("hostName").isEmpty()) {
//	  				monitoringHostName = request.queryParams("hostName");
//	  		  }
//	  		  
//	  		  /* The first two commands need to be run only once */
//	  		  InetAddress host = InetAddress.getByName(monitoringHostName);
//	  		  DatagramSocket s = new DatagramSocket();
//	  		  
//	  		  
//	  		  docStore = new DocStore(dbDirectory);
//	  		  
//	  		  
//	  		  for(int i = 0; i < maxThreads; ++i) {
//	  			  threads.add(new CrawlerThreads(urlFrontier, maxSize, numFiles, host, s, docStore, robotMap ,i));
//	  		  }
//	  		  
//	  
//	  		  urlFrontier.enqueue(new URLInfo(startUrl));
//	  		  
//	  		  logger.debug("Starting all threads");
//	  		  for(CrawlerThreads t: threads) {
//	  			  t.start();
//	  		  }
//	  
//	  		  isCrawlerRunning = true;
//		    	response.redirect("/");
//	  		  
//	  	  }catch(Exception e) {
//	  		  halt(422);
//	  		  e.printStackTrace();
//	  	  }
//	    	return null;
//	    });
//
//	    
//	    get("/logout", (request, response) -> {
//	    	if(request.session(false) == null) {
//	    		response.redirect("/");
//	    		return null;
//	    	}
//	        request.session().invalidate();
//	      	response.redirect("/");
//	      	return null;
//	      });
//	    
//
//	    
//	    get("/shutdown", (request, response) -> {
//	    	if(request.session(false) == null) {
//	    		response.redirect("/");
//	    		return null;
//	    	}
//	    	if(isCrawlerRunning) {
//	    		terminateCrawl();
//	    		return "The crawler has terminated graciously.";
//	    	}
//	      	return "Crawler has not been started or has already terminated";
//	      });	 
//	  }
//	
//	
//
//	private static String addStats() {
//		
//		
//		// List of HTML page lines
//		List<String> statPage = new ArrayList<String>();
//		
//
//		statPage.add("<style>");
//		statPage.add("table {");
//		statPage.add("  font-family: arial, sans-serif;");
//		statPage.add("  border-collapse: collapse;");
//		statPage.add("  width: 100%;");
//		statPage.add("}");
//		statPage.add("td, th {");
//		statPage.add("  border: 1px solid #dddddd;");
//		statPage.add("  text-align: left;");
//		statPage.add("  padding: 8px;");
//		statPage.add("}");
//		statPage.add("tr:nth-child(even) {");
//		statPage.add("  background-color: #dddddd;");
//		statPage.add("}");
//		statPage.add("</style>");
//		statPage.add("</head>");
//		statPage.add("<body>");
//		statPage.add("<h2>Crawler State : Running</h2>");
//		statPage.add("<table>");
//		statPage.add("<tr>");
//		statPage.add("<th>Statistics</th>");
//		statPage.add("<th>Value</th>");
//		statPage.add("</tr>");
//		
//		
//		
//
//		int numHTML = 0;
//		int numXML = 0;
//		int dataDownloaded = 0;
//		int numServers = 0;
//		
//		
//		// Dynamically get thread statistics
//		for(CrawlerThreads t : threads) {
//			numHTML = numHTML + t.getNumHTML();
//			numXML = numXML + t.getNumXML();
//			dataDownloaded = dataDownloaded + t.getDataDownloaded();
//			numServers = numServers + t.getNumServers();
//		}
//
//		
//
//		statPage.add("<tr>");
//		statPage.add(String.format("<td>Number of HTML pages scanned</td>"));
//		statPage.add(String.format("<td>%d</td>",numHTML));
//		statPage.add("</tr>");
//		statPage.add("<tr>");
//		statPage.add(String.format("<td>Nnumber of XML documents retrieved</td>"));
//		statPage.add(String.format("<td>%d</td>",numXML));
//		statPage.add("</tr>");
//		statPage.add("<tr>");
//		statPage.add(String.format("<td>Amount of data downloaded in bytes</td>"));
//		statPage.add(String.format("<td>%d</td>",dataDownloaded));
//		statPage.add("</tr>");
//		statPage.add("<tr>");
//		statPage.add(String.format("<td>Number of servers visited</td>"));
//		statPage.add(String.format("<td>%d</td>",numServers));
//		statPage.add("</tr>");
//		statPage.add("<tr>");
//		statPage.add(String.format("<td>Number of XML documents that match each channel,</td>"));
//		statPage.add(String.format("<td>%d</td>",0));
//		statPage.add("</tr>");
//		statPage.add("<tr>");
//		statPage.add(String.format("<td>Servers with the most XML documents that match one of the channels</td>"));
//		statPage.add(String.format("<td>%d</td>",0));
//		statPage.add("</tr>");
//		
//		statPage.add("</table>");
//		
//		// Shutdown Button
//		statPage.add(String.format("<button onclick=\"window.location.href = 'http://localhost:%d/shutdown';\">Shutdown Server</button>", 8080));			
//		
//
//		
//		return String.join("", statPage);
//	}
//	/**
//	 * Configuration Form
//	 * @return
//	 */
//	private static String configureCrawler() {
//		String configuration = "<form action=\"/startCrawl\" method=\"POST\">\n" + 
//    			"    <input type=\"text\" name=\"startURL\" placeholder=\"Start URL\"><br>\n" + 
//    			"    <input type=\"text\" name=\"bdbDirectory\" placeholder=\"Berkeley DB Directory\"><br>\n" + 
//    			"    <input type=\"text\" name=\"maxSize\" placeholder=\"Maximum Size\"><br>\n" + 
//    			"    <input type=\"text\" name=\"numFiles\" placeholder=\"Number of Files\"><br>\n" + 
//    			"    <input type=\"text\" name=\"hostName\" placeholder=\"Host Name\"><br>\n" + 
//    			"    <input type=\"submit\" value=\"Start Crawler\">\n" + 
//    			"</form>\n";
//		return configuration;
//	}
  
	
}
