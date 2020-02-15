package edu.upenn.cis455.storage;

import java.io.Serializable;
import java.util.ArrayList;

import edu.upenn.cis455.crawler.info.URLInfo;

/**
 * Channel class that keeps track of matched documents
 * @author cis455
 *
 */
public class Channel implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -541070576588113688L;
	private String name;
	private String xpath;
	private String ownerUserName;
	private ArrayList<URLInfo> docUrls;
	
	public Channel(String nameArg, String xpathArg, String ownerUserNameArg, ArrayList<URLInfo> docUrlsArg) {
		name = nameArg;
		xpath = xpathArg;
		ownerUserName = ownerUserNameArg;
		docUrls = docUrlsArg;
	}

	public String getName() {
		return name;
	}

	public String getXpath() {
		return xpath;
	}

	public String getOwnerUserName() {
		return ownerUserName;
	}

	public ArrayList<URLInfo> getDocUrls() {
		return docUrls;
	}

}