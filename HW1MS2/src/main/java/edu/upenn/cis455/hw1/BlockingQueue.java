package edu.upenn.cis455.hw1;

import java.net.Socket;
import java.util.Vector;

//import org.apache.log4j.Logger;

public class BlockingQueue {

//	static final Logger logger = Logger.getLogger(BlockingQueue.class);
	private Vector<Socket> queue;
	private int capacity;
	
	/**
	 * Constructor to initialize the internal vector based container
	 * @param limit is the max number of elements the queue can hold
	 */
	public BlockingQueue(int limit) {
		queue = new Vector<Socket>();
		this.capacity = limit;
	}

	/**
	 * Method to add an object
	 * @param s Object to add
	 * @throws InterruptedException if waiting thread is unusually interrupted
	 */
	public synchronized void enqueue(Socket s) throws InterruptedException{
		while(queue.size() == capacity) {
//			logger.debug("Queue at max capacity");
			wait();
		}
		queue.add(s);
		if(queue.size() == 1) {
			notifyAll();
		}
	}

	/**
	 * Method to remove an object
	 * @param threadID of the thread removing the object
	 * @return the object
	 * @throws InterruptedException if waiting thread is unusually interrupted
	 */
	public synchronized Socket dequeue(int threadID) throws InterruptedException{
		while(queue.isEmpty()) {
//			logger.debug(String.format("Queue is Empty; Thread %d is blocked", threadID));
			wait();
		}
		notifyAll();
		return queue.remove(0);
	}
	
}
