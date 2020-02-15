package edu.upenn.cis455.crawler;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import edu.upenn.cis.stormlite.OutputFieldsDeclarer;
import edu.upenn.cis.stormlite.TopologyContext;
import edu.upenn.cis.stormlite.bolt.IRichBolt;
import edu.upenn.cis.stormlite.bolt.OutputCollector;
import edu.upenn.cis.stormlite.routers.IStreamRouter;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis.stormlite.tuple.Tuple;
import edu.upenn.cis.stormlite.tuple.Values;
import edu.upenn.cis455.crawler.info.URLInfo;
import edu.upenn.cis455.storage.Channel;
import edu.upenn.cis455.storage.CrawledDocument;
import edu.upenn.cis455.xpathengine.XPathEngineImpl;

public class ChannelMatchingBolt implements IRichBolt{
	static Logger logger = Logger.getLogger(ChannelMatchingBolt.class);
	
	Fields schema = new Fields("URLs");
	
    
    /**
     * To make it easier to debug: we have a unique ID for each
     * instance of the WordCounter, aka each "executor"
     */
    String executorId = UUID.randomUUID().toString();
    
    /**
     * This is where we send our output stream
     */
    private OutputCollector collector;
    
    public ChannelMatchingBolt() {
    }
    
    /**
     * Initialization, just saves the output stream destination
     */
    @Override
    public void prepare(Map<String,String> stormConf, 
    		TopologyContext context, OutputCollector collector) {
        this.collector = collector;
    }
    
	

    /**
     * Process a tuple received from the stream, incrementing our
     * counter and outputting a result
     */
    @Override
    public void execute(Tuple input) {
		XPathCrawler.boltLedger(true);
    	
    	Document doc = (Document) input.getObjectByField("W3CDocument");
    	CrawledDocument crawledDoc = (CrawledDocument) input.getObjectByField("CrawledDoc");
    	ArrayList<URLInfo> urls = (ArrayList<URLInfo>) input.getObjectByField("URLs");
    	
    	XPathCrawler.getDocStore().addDocument(crawledDoc);
    	
    	ArrayList<Channel> allChannels = XPathCrawler.getChannelStore().getAllChannels();
    	
    	// Add xpaths to XPathEngineImpl
    	ArrayList<String> xpaths = new ArrayList<String>();
    	for(Channel channel : allChannels) {
    		xpaths.add(channel.getXpath());
    	}
    	XPathEngineImpl xpel = new XPathEngineImpl();
    	xpel.setXPaths(xpaths.toArray(new String[xpaths.size()]));
    	
    	
    	// Evaluate doc for all xpaths
    	boolean[] evaluation = xpel.evaluate(doc);
    	
    	for(int i = 0; i < allChannels.size(); ++i) {
    		logger.info(String.format("Checking for Channel : %s and doc %s ",allChannels.get(i).getName(), crawledDoc.getUrl().getFullURL()));
    		if(evaluation[i] == true) { // Doc matches with channel and therefore should be added to channel
    			if(!allChannels.get(i).getDocUrls().contains(crawledDoc.getUrl())) {
    				allChannels.get(i).getDocUrls().add(crawledDoc.getUrl());
    				XPathCrawler.getChannelStore().addChannel(allChannels.get(i));
    				logger.info(String.format("URL : %s matched Channel : %s", crawledDoc.getUrl().getFullURL(), allChannels.get(i).getName()));
    			}
    		} else {// Doc does not match with channel and therefore should be removed from the channel if previously matched
    			if(allChannels.get(i).getDocUrls().contains(crawledDoc.getUrl())) {
    				allChannels.get(i).getDocUrls().remove(crawledDoc.getUrl());
    				XPathCrawler.getChannelStore().addChannel(allChannels.get(i));
    			}    			
    		}
    	}
    	
    	XPathCrawler.getDocStore().addDocument(crawledDoc);
    	
    	collector.emit(new Values<Object>(urls));
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
