/**
 * 
 */
package edu.upenn.cis455.mapreduce.worker;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;

import org.apache.log4j.Logger;

import edu.upenn.cis.stormlite.Config;
import edu.upenn.cis.stormlite.spout.FileSpout;
import spark.Spark;

/**
 * Class to periodically send update to Master and keep record of status
 * @author cis455
 */
public class WorkerAccountant extends Thread{
	static Logger log = Logger.getLogger(WorkerAccountant.class);
	private static String job = "NOP";
	private static String status = "IDLE";
	private static int keysRead = 0;
	private static int keysWritten = 0;
	private static String masterIpPort = "";
	private static int port = 0;
	private static boolean shutdownFlag = false;
	/**
	 * @return the job
	 */
	public static String getJob() {
		return job;
	}
	/**
	 * @param job the job to set
	 */
	public static void setJob(String job) {
		WorkerAccountant.job = job;
	}
	/**
	 * @return the status
	 */
	public static String getStatus() {
		return status;
	}
	/**
	 * @param status the status to set
	 */
	public static void setStatus(String status) {
		WorkerAccountant.status = status;
	}
	/**
	 * @return the keysRead
	 */
	public static int getKeysRead() {
		return keysRead;
	}
	/**
	 * @param keysRead the keysRead to set
	 */
	public static void setKeysRead(int keysRead) {
		WorkerAccountant.keysRead = keysRead;
	}
	public static synchronized void readKey() {
		keysRead++;
	}
	/**
	 * @return the keysWritten
	 */
	public static int getKeysWritten() {
		return keysWritten;
	}
	public static synchronized void wroteKey() {
		keysWritten++;
	}
	/**
	 * @param keysWritten the keysWritten to set
	 */
	public static void setKeysWritten(int keysWritten) {
		WorkerAccountant.keysWritten = keysWritten;
	}
	/**
	 * @param masterIpPort the masterIpPort to set
	 */
	public static void setMasterIpPort(String masterIpPort) {
		WorkerAccountant.masterIpPort = masterIpPort;
	}
	/**
	 * @param shutdownFlag the shutdownFlag to set
	 */
	public static void setShutdownFlag(boolean shutdownFlag) {
		WorkerAccountant.shutdownFlag = shutdownFlag;
	}
	/**
	 * resets the state
	 */
	public static synchronized void reset() {
		job = "NOP";
		status = "IDLE";
		keysRead = 0;
		keysWritten = 0;		
	}
	/**
	 * resets the counters as next bolt stage has started
	 */
	public static synchronized void resetForNextBolt(String state) {
		if(status.equals("WAITING")) {
			status = state;
			keysRead = 0;
			keysWritten = 0;		
		}
	}
	
	/**
	 * Sends status update every 30 seconds
	 */
	@Override
	public void run() {
		while(!shutdownFlag) {
			try {
				String query = String.format("?port=%d&job=%s&status=%s&keysread=%d&keyswritten=%d&timestamp=%d", 
						port,
						job,
						status,
						keysRead,
						keysWritten,
						Instant.now().toEpochMilli());
				sendWorkerStatus(masterIpPort, "GET", null, query, "");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}
		Spark.stop();
        System.exit(0);
	}

	/**
	 * Sends status as HTTP request
	 * @param dest
	 * @param reqType
	 * @param config
	 * @param query
	 * @param parameters
	 * @return
	 * @throws IOException
	 */
	static HttpURLConnection sendWorkerStatus(String dest, String reqType, Config config, String query, String parameters) throws IOException {
		URL url = new URL(dest + "/workerstatus" + query);
		log.debug(dest + "/workerstatus" + query);


		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod(reqType);

		
		if (reqType.equals("POST")) {
			conn.setRequestProperty("Content-Type", "application/json");

			OutputStream os = conn.getOutputStream();
			byte[] toSend = parameters.getBytes();
			os.write(toSend);
			os.flush();
		}
		
		conn.getResponseCode();
		conn.getResponseMessage();
		
		conn.disconnect();

		return conn;
	}
	/**
	 * @param port the port to set
	 */
	public static void setPort(int port) {
		WorkerAccountant.port = port;
	}
}
