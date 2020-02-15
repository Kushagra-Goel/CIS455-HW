package edu.upenn.cis455.crawler;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;

import edu.upenn.cis.stormlite.OutputFieldsDeclarer;
import edu.upenn.cis.stormlite.TopologyContext;
import edu.upenn.cis.stormlite.bolt.IRichBolt;
import edu.upenn.cis.stormlite.bolt.OutputCollector;
import edu.upenn.cis.stormlite.routers.IStreamRouter;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis.stormlite.tuple.Tuple;
import edu.upenn.cis455.crawler.info.URLFrontier;
import edu.upenn.cis455.crawler.info.URLInfo;

public class URLFilterBolt implements IRichBolt{
	static Logger logger = Logger.getLogger(URLFilterBolt.class);
	
	Fields schema = new Fields();
	
    
    /**
     * To make it easier to debug: we have a unique ID for each
     * instance of the WordCounter, aka each "executor"
     */
    String executorId = UUID.randomUUID().toString();
    
    /**
     * This is where we send our output stream
     */
    private OutputCollector collector;
	private static URLFrontier<URLInfo> urlFrontier;
    
    public URLFilterBolt() {
    }
    
    /**
     * Initialization, just saves the output stream destination
     */
    @Override
    public void prepare(Map<String,String> stormConf, 
    		TopologyContext context, OutputCollector collector) {
        this.collector = collector;
    	urlFrontier = XPathCrawler.getUrlFrontier();
    }
    
	

    /**
     * Process a tuple received from the stream, incrementing our
     * counter and outputting a result
     */
    @Override
    public void execute(Tuple input) {
		XPathCrawler.boltLedger(true);
		try {
	    	ArrayList<URLInfo> urls = (ArrayList<URLInfo>) input.getObjectByField("URLs");
	    	for(URLInfo extractedURL : urls) {
				if(!urlFrontier.isCrawled(extractedURL)) { // If not already processed or in processing
						urlFrontier.enqueue(extractedURL);
					logger.info(String.format("Adding URL %s", extractedURL.getFullURL()));	
				}	
				else {
					logger.info(String.format("Already crawled URL %s", extractedURL.getFullURL()));
				}
	    	}
		} catch (Exception e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
		}
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
