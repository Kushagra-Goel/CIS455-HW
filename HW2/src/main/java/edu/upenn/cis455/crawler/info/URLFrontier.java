package edu.upenn.cis455.crawler.info;

import java.util.HashSet;
//import java.util.HashSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

import edu.upenn.cis455.crawler.CrawlerThreads;

/**
 * Frontier class that maintains a blockingqueue of the urls
 * and a set of seen urls
 * @author cis455
 *
 * @param <T>
 */
public class URLFrontier<T extends URLInfo> {	
	static final Logger logger = Logger.getLogger(URLFrontier.class);
	private BlockingQueue<T> frontierQueue;
	private HashSet<String> crawledUrl;
	private int capacity;
	private int numberOfURLCrawled = 0;
	
	public URLFrontier(int capacityArg) {
		capacity = capacityArg;
		frontierQueue = new ArrayBlockingQueue<T>(capacity);
		crawledUrl = new HashSet<String>();
	}
	
	public synchronized void enqueue(T s) throws InterruptedException {
		crawledUrl.add(s.getFullURL());
		frontierQueue.put(s);
	}
	
	public T deque() throws InterruptedException {
//		t.blockedAtDeque = true;
		T res =  frontierQueue.take();
//		t.blockedAtDeque = false;
		return res;
	}
	
	public synchronized void incrementNumURLCrawled() {
		numberOfURLCrawled = numberOfURLCrawled + 1;
	}
	
	public synchronized int getNumberOfURLCrawled() {
		return numberOfURLCrawled;
	}

	public synchronized boolean isFrontierEmpty() {
		return frontierQueue.isEmpty();
	}

	public synchronized boolean isCrawled(T s) {
		return crawledUrl.contains(s.getFullURL());
	}
	
}
