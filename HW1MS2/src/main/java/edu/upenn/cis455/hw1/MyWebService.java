package edu.upenn.cis455.hw1;

import edu.upenn.cis455.hw1.interfaces.WebService;
import edu.upenn.cis455.hw1.interfaces.Route;
import edu.upenn.cis455.hw1.interfaces.Session;


import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

//import org.apache.log4j.Logger;

import edu.upenn.cis455.hw1.interfaces.Filter;

public class MyWebService extends WebService implements Runnable {

//	static final Logger logger = Logger.getLogger(MyWebService.class);

	
	private static int portArg = 8080;
	private static Path staticFileLocation;
	private boolean shutDownFlag = false;
	private static ServerSocket serverSocket;
	private ThreadPool threadPool = null; 
	private InetAddress ipAddressField;
	private static int maxNumberOfConnections = 1000;
	private static int maxNumberOfThreads = 100;
	private List<MyRoute> routeList;
	private List<MyFilter> beforeList, afterList;
	private Map<String, Session> sessionTable;

  public MyWebService()
  {
	  try {
		  ipAddressField = InetAddress.getByName("0.0.0.0");
	} catch (UnknownHostException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	  routeList = new Vector<MyRoute>();
	  beforeList = new Vector<MyFilter>();
	  afterList = new Vector<MyFilter>();
	  sessionTable = new ConcurrentHashMap<String, Session>();
//	  logger.debug("Constructed MyWebService Object");
  }

  public void run() {	  
	  try {
		serverSocket = new ServerSocket(portArg, maxNumberOfConnections, ipAddressField);
	} catch (IOException e) {
		e.printStackTrace();
//		logger.error("Error while creating Server Socket");
		return;
	}		  
//	  logger.info(String.format("HttpServer running at ip %s on port %d and root directory %s", serverSocket.getInetAddress().getCanonicalHostName(), portArg, staticFileLocation));
	  threadPool = new ThreadPool(maxNumberOfConnections, maxNumberOfThreads, staticFileLocation, portArg, serverSocket, this, routeList, beforeList, afterList, ipAddressField, sessionTable);

	    get("/shutdown", (request, response) -> {
	    	request.attribute("Shutdown", true);
	      return null;
	    });
	    get("/control", (request, response) -> {
	    	request.attribute("Control", true);
	      return null;
	    });	    
	    
	  while(!shutDownFlag) {
		  Socket clientSocket;
		try {
			//Accept Client connections
			clientSocket = serverSocket.accept();
//			 logger.debug("Connection recieved from client");
			 // Put Client connection in the queue for a thread to pick up
			 threadPool.enqueueConnection(clientSocket);
		} catch (IOException e) {
//			logger.info("Shutting down Server");
		}
		  
	  }
	  
  }

  public void start()
  {
  }
    
  public void stop()
  {
	  shutDownFlag = true;
	  try {
		serverSocket.close();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
  }

  public void staticFileLocation(String directory)
  {
	  staticFileLocation = Paths.get(directory);
	  staticFileLocation = staticFileLocation.normalize();
  }

  public void get(String path, Route route)
  {
	  routeList.add(new MyRoute(HttpMethod.GET, path, null, route));
  }

  public void ipAddress(String ipAddress)
  {
	  try {
		ipAddressField = InetAddress.getByName(ipAddress);
	} catch (UnknownHostException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
  }
    
  public void port(int port)
  {
	  portArg = port;
  }
    
  public void threadPool(int threads)
  {
	  maxNumberOfThreads = threads;
  }
    
  public void post(String path, Route route)
  {
	  routeList.add(new MyRoute(HttpMethod.POST, path, null, route));
  }

  public void put(String path, Route route)
  {
	  routeList.add(new MyRoute(HttpMethod.PUT, path, null, route));
  }

  public void delete(String path, Route route)
  {
	  routeList.add(new MyRoute(HttpMethod.DELETE, path, null, route));
  }

  public void head(String path, Route route)
  {
	  routeList.add(new MyRoute(HttpMethod.HEAD, path, null, route));
  }

  public void options(String path, Route route)
  {
	  routeList.add(new MyRoute(HttpMethod.OPTIONS, path, null, route));
  }
    
  public void before(Filter filter)
  {
	  before(null, null, filter);
  }

  public void after(Filter filter)
  {
	  after(null, null, filter);
  }
    
  public void before(String path, String acceptType, Filter filter)
  {
	  beforeList.add(new MyFilter(path, acceptType, filter));
  }
    
  public void after(String path, String acceptType, Filter filter)
  {
	  afterList.add(new MyFilter(path, acceptType, filter));
  }

  
  
}
