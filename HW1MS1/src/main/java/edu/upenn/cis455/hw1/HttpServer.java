package edu.upenn.cis455.hw1;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.log4j.Logger;

/**
 * Main HTTPServer class to run the main server
 * @author Kushagra
 *
 */
class HttpServer {
	static final Logger logger = Logger.getLogger(HttpServer.class);
	
	private static int port;
	private static Path rootDirectory;
	private boolean shutDownFlag = false;
	private static ServerSocket serverSocket;
	private ThreadPool<Socket> threadPool = null; 
	
	

	public static final boolean useHTTPs = false;
	private static int maxNumberOfConnections = 10000;
	private static int maxNumberOfThreads = 10;
	
	/**
	 * Method to create SSL Context in case of HTTPS connection.
	 * Need to place the key in the resources folder.
	 * Command to use to make key "keytool -genkeypair -keyalg RSA -alias self_signed -keypass simulator -keystore lig.keystore -storepass simulator"
	 * @return SSL Context with pre-made key
	 */
    private SSLContext createSSLContext(){
  	  
  	  String ksName = "./src/main/resources/lig.keystore";
  	  char ksPass[] = "simulator".toCharArray();
  	  char ctPass[] = "simulator".toCharArray();
  	  
        try{
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(new FileInputStream(Paths.get(ksName).toString()),ksPass);
             
            //Generate Key Manager
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(keyStore, ctPass);
            KeyManager[] km = keyManagerFactory.getKeyManagers();
             

            //Generate Trust Manager
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
            trustManagerFactory.init(keyStore);
            TrustManager[] tm = trustManagerFactory.getTrustManagers();

            //Initialize SSL Context
            SSLContext sslContext = SSLContext.getInstance("TLSv1");
            sslContext.init(km,  tm, null);
             
            return sslContext;
        } catch (Exception ex){
        }
         
        return null;
    } 
    
    /**
     * Main method
     * @param args from the kernel
     * @throws Exception
     */
    public static void main(String[] args) throws Exception{
    	HttpServer server = new HttpServer();
        server.run(args);
    }
    
    /**
     * Dispatcher Thread
     * @param args from the kernel
     */
  public void run (String args[])
  {
	  if(args.length != 2) {
		  logger.error("Incorrect number of arguments given.");
		  throw new IllegalArgumentException("Illegal Arguments : HttpServer <Port> <Root Directory>");
	  }
	  
	  try {
		  port = Integer.parseInt(args[0]);
	  } catch(NumberFormatException e) {
		  logger.error("Illegal Port Address : " + args[0]);
		  throw new IllegalArgumentException("Port address " + args[0] + " is not a valid number");		  
	  }
	  String rootDirectoryString = args[1];
	  rootDirectory = Paths.get(rootDirectoryString);
	  rootDirectory = rootDirectory.normalize();	  
	  
	  
	  if(useHTTPs) {
		  SSLContext sslContext = this.createSSLContext();  
		  SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();
	      try {
			serverSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(port);
		} catch (IOException e) {
			logger.error("Error while creating SSL Server Socket");
			return;
		}
		  logger.info(String.format("HttpsServer running on port %d and root directory %s", port, rootDirectory));
	  }
	  else {
		  try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			logger.error("Error while creating Server Socket");
			return;
		}		  
		  logger.info(String.format("HttpServer running on port %d and root directory %s", port, rootDirectory));
	  }
	  
	  threadPool = new ThreadPool<Socket>(maxNumberOfConnections, maxNumberOfThreads, rootDirectory, port, serverSocket, this);
	  
	  while(!shutDownFlag) {
		  Socket clientSocket;
		try {
			//Accept Client connections
			clientSocket = serverSocket.accept();
			 logger.debug("Connection recieved from client");
			 // Put Client connection in the queue for a thread to pick up
			 threadPool.enqueueConnection(clientSocket);
		} catch (IOException e) {
			logger.info("Shutting down Server");
		}
		  
	  }
	  
  }
  
  /**
   * Sets the shutdown flag and closes the server socket
   * @throws Exception
   */
  public synchronized void shutdown() throws Exception {
	  shutDownFlag = true;
	  serverSocket.close();
  }
   
}
  
