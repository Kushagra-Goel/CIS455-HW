package edu.upenn.cis455.hw1;

import edu.upenn.cis455.hw1.interfaces.WebService;
import edu.upenn.cis455.hw1.interfaces.Route;
import edu.upenn.cis455.hw1.interfaces.Filter;

public class MyWebService extends WebService implements Runnable {

  public MyWebService()
  {
  }

  public void run() {
    // TODO: Your server code should go here
  }

  public void start()
  {
  }
    
  public void stop()
  {
  }

  public void staticFileLocation(String directory)
  {
  }

  public void get(String path, Route route)
  {
  }

  public void ipAddress(String ipAddress)
  {
  }
    
  public void port(int port)
  {
  }
    
  public void threadPool(int threads)
  {
  }
    
  public void post(String path, Route route)
  {
  }

  public void put(String path, Route route)
  {
  }

  public void delete(String path, Route route)
  {
  }

  public void head(String path, Route route)
  {
  }

  public void options(String path, Route route)
  {
  }
    
  public void before(Filter filter)
  {
  }

  public void after(Filter filter)
  {
  }
    
  public void before(String path, String acceptType, Filter filter)
  {
  }
    
  public void after(String path, String acceptType, Filter filter)
  {
  }

}
