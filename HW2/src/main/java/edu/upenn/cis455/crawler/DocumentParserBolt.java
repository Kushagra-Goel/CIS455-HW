package edu.upenn.cis455.crawler;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import edu.upenn.cis.stormlite.OutputFieldsDeclarer;
import edu.upenn.cis.stormlite.TopologyContext;
import edu.upenn.cis.stormlite.bolt.IRichBolt;
import edu.upenn.cis.stormlite.bolt.OutputCollector;
import edu.upenn.cis.stormlite.routers.IStreamRouter;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis.stormlite.tuple.Tuple;
import edu.upenn.cis.stormlite.tuple.Values;
import edu.upenn.cis455.crawler.info.URLInfo;
import edu.upenn.cis455.storage.CrawledDocument;

public class DocumentParserBolt implements IRichBolt{
	static Logger logger = Logger.getLogger(DocumentParserBolt.class);
	
	Fields schema = new Fields("W3CDocument", "CrawledDoc", "URLs");
	
    
    /**
     * To make it easier to debug: we have a unique ID for each
     * instance of the WordCounter, aka each "executor"
     */
    String executorId = UUID.randomUUID().toString();
    
    /**
     * This is where we send our output stream
     */
    private OutputCollector collector;
    
    public DocumentParserBolt() {
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
		
		try {
			CrawledDocument crawledDoc = (CrawledDocument) input.getObjectByField("CrawledDoc");
			URLInfo url = (URLInfo) input.getObjectByField("URL");
			ArrayList<URLInfo> extractedUrls = new ArrayList<URLInfo>();
			W3CDom w3CDom = new W3CDom();
			org.w3c.dom.Document w3cDoc = null;
			
			// Convert JSOUP to W3CDOM
			if(crawledDoc.isHTML()) {
				Document jsoupDoc = Jsoup.parse(crawledDoc.getDoc(), url.getFullURL());
				for(Element link: jsoupDoc.select("a[href]")) {
					logger.info(String.format("Extracted url %s from %s", link.attr("abs:href"), url.getFullURL()));
					extractedUrls.add(new URLInfo(link.attr("abs:href")));					
				}
				w3cDoc = w3CDom.fromJsoup(jsoupDoc);
			}
			
			// Build W3CDOM from xml
			if(crawledDoc.isXML()) {
				DocumentBuilder documentBuilder;
				documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				InputSource is = new InputSource(new StringReader(crawledDoc.getDoc()));
				w3cDoc = documentBuilder.parse(is);
			}
			
			logger.info(String.format("Emitting %d urls from %s", extractedUrls.size(), crawledDoc.getUrl().getFullURL()));
			collector.emit(new Values<Object>(w3cDoc, crawledDoc, extractedUrls));
			
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
		} catch (IOException e) {
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
