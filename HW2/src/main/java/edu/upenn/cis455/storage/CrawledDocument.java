package edu.upenn.cis455.storage;

import java.io.Serializable;
import java.time.Instant;

import edu.upenn.cis455.crawler.info.URLInfo;

/**
 * The class that maintains the documents in BDB
 * @author cis455
 *
 */
public class CrawledDocument implements Serializable{
	private static final long serialVersionUID = 6672188242406189647L;
	private URLInfo url;
	private int docLength;
	private String doc;
	private String docType;
	private Instant crawlTimestamp;
	
	public CrawledDocument(URLInfo urlArg, int docLengthArg, String docTypeArg, String docArg, Instant crawlTimestampArg) {

		url = urlArg;
		docLength = docLengthArg;
		docType = docTypeArg.toLowerCase();
		doc = docArg;
		crawlTimestamp = crawlTimestampArg;
	}
	
	/**
	 * @return the url
	 */
	public URLInfo getUrl() {
		return url;
	}
	/**
	 * @param url the url to set
	 */
	public void setUrl(URLInfo url) {
		this.url = url;
	}
	/**
	 * @return the docLength
	 */
	public int getDocLength() {
		return docLength;
	}
	/**
	 * @param docLength the docLength to set
	 */
	public void setDocLength(int docLength) {
		this.docLength = docLength;
	}
	/**
	 * @return the doc
	 */
	public String getDoc() {
		return doc;
	}
	/**
	 * @param doc the doc to set
	 */
	public void setDoc(String doc) {
		this.doc = doc;
	}
	/**
	 * @return the crawlTimestamp
	 */
	public Instant getCrawlTimestamp() {
		return crawlTimestamp;
	}
	/**
	 * @param crawlTimestamp the crawlTimestamp to set
	 */
	public void setCrawlTimestamp(Instant crawlTimestamp) {
		this.crawlTimestamp = crawlTimestamp;
	}

	/**
	 * @return the docType
	 */
	public String getDocType() {
		return docType;
	}

	/**
	 * @param docType the docType to set
	 */
	public void setDocType(String docType) {
		this.docType = docType;
	}

	public boolean isHTML() {
		return docType.contains("html");
	}
	
	public boolean isXML() {
		return docType.contains("xml");
	}
}
