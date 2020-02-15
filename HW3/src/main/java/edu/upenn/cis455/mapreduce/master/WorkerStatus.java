/**
 * 
 */
package edu.upenn.cis455.mapreduce.master;

import java.time.Instant;

/**
 * Class that maintains the status by each worker
 * @author cis455
 */
public class WorkerStatus {
	private String ip;
	private int port;
	private String job;
	private String status;
	private int keysRead;
	private int keysWritten;
	private Instant timeStamp; // To see when was the last update
	
	/** Constructor
	 * @param ip
	 * @param port
	 * @param job
	 * @param status
	 * @param keysRead
	 * @param keysWritten
	 * @param timeStamp
	 */
	public WorkerStatus(String ip, int port, String job, String status, int keysRead, int keysWritten,
			Instant timeStamp) {
		this.ip = ip;
		this.port = port;
		this.job = job;
		this.status = status;
		this.keysRead = keysRead;
		this.keysWritten = keysWritten;
		this.timeStamp = timeStamp;
	}
	public String getIp() {
		return ip;
	}
	public int getPort() {
		return port;
	}
	public String getJob() {
		return job;
	}
	public String getStatus() {
		return status;
	}
	public int getKeysRead() {
		return keysRead;
	}
	public int getKeysWritten() {
		return keysWritten;
	}
	public Instant getTimeStamp() {
		return timeStamp;
	}
	public String getName() {
		return String.format("%s:%d", getIp(), getPort());
	}
}
