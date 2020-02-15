package edu.upenn.cis455.mapreduce.master;

import static spark.Spark.*;


import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.upenn.cis.stormlite.Config;
import edu.upenn.cis.stormlite.Topology;
import edu.upenn.cis.stormlite.TopologyBuilder;
import edu.upenn.cis.stormlite.bolt.MapBolt;
import edu.upenn.cis.stormlite.bolt.PrintBolt;
import edu.upenn.cis.stormlite.bolt.ReduceBolt;
import edu.upenn.cis.stormlite.distributed.WorkerHelper;
import edu.upenn.cis.stormlite.distributed.WorkerJob;
import edu.upenn.cis.stormlite.spout.FileSpout;
import edu.upenn.cis.stormlite.spout.FileSpoutImpl;
import edu.upenn.cis.stormlite.tuple.Fields;
import spark.Request;
import spark.Response;

/**
 * Class to start Master
 * @author cis455
 *
 */
class MasterApp {


	private static final String WORD_SPOUT = "WORD_SPOUT";
	private static final String MAP_BOLT = "MAP_BOLT";
	private static final String REDUCE_BOLT = "REDUCE_BOLT";
	private static final String PRINT_BOLT = "PRINT_BOLT";


	static Logger logger = Logger.getLogger(MasterApp.class);
	static Config config = new Config();

	private static Map<String, WorkerStatus> workers = new ConcurrentHashMap<String, WorkerStatus>();

	public static void main(String args[]) {

		port(8000);
		config.put("workerList", "[]");

		/* Just in case someone opens the root URL, without /status... */

		get("/", (request,response) -> {
			return "Please go to the <a href=\"/status\">status page</a>!";
		});

		/* Status page, for launching jobs and for viewing the current status */

		get("/status", (request,response) -> {
			response.type("text/html");

			return getStatus();
		});

		post("/newjob", (request,response) -> {
			postNewJob(request, response);
			response.redirect("/status");
			return null;
		});

		/* Workers submit requests for /workerstatus; human users don't normally look at this */

		get("/workerstatus", (request,response) -> {
			try {
				Instant tos = Instant.now();
				logger.info("Got status update from " + request.ip() + ":" + request.queryParams("port"));
				WorkerStatus workerupdate = new WorkerStatus(
						(request.ip().startsWith("http"))?request.ip():String.format("http://%s", request.ip()), 
						Integer.parseInt(request.queryParams("port")), 
						request.queryParams("job"), 
						request.queryParams("status"), 
						Integer.parseInt(request.queryParams("keysread")), 
						Integer.parseInt(request.queryParams("keyswritten")), 
						Instant.ofEpochMilli(Long.parseLong(request.queryParams("timestamp"))));
				boolean isNewerEntry = true;
				synchronized (workers) {
					//					WorkerStatus lastupdate = workers.getOrDefault(workerupdate.getName(), null);
					//					if(lastupdate != null)if(lastupdate.getTimeStamp().isAfter(workerupdate.getTimeStamp())) isNewerEntry = false;
					if(isNewerEntry) {
						workers.put(workerupdate.getName(), workerupdate);
						if(config.get("workerList").length() < 3) {
							synchronized (config) {
								config.put("workerList", String.format("[%s]", workerupdate.getName()));	
							}							
						}
						else {
							
							// This part removes workers that didn't update in last 30 seconds from the config
							
							Set<String> newWorkerNames = new HashSet(Arrays.asList(WorkerHelper.getWorkers(config)));
							newWorkerNames.add(workerupdate.getName());
							Set<String> activeWorkerNames = new HashSet<String>();
							synchronized (config) {
								for(String workerName : newWorkerNames) {
									WorkerStatus ws = workers.get(workerName);
									if(ws.getTimeStamp().isAfter(tos.minusSeconds(30))){
										activeWorkerNames.add(ws.getName());
									}
								}
								config.put("workerList", String.format("[%s]", String.join(",", activeWorkerNames)));
							}
						}
					}
				}
				logger.debug(config.get("workerList"));
				return (isNewerEntry)?"1":"0";
			}catch (Exception e) {
				e.printStackTrace();
			}
			return "0";
		});

		/* Shutdown page, for launching jobs and for viewing the current status */

		get("/shutdown", (request,response) -> {
			if(config.get("workerList").length() > 3) {
				for(String dest : WorkerHelper.getWorkers(config)) {
					try {
						sendJob(dest, "POST", null, "shutdown", "");
					} catch(Exception e) {
						e.printStackTrace();
					}
				}			
			}
			response.type("text/html");
			stop();
			return "<html><body>Workers Shutdown</body></html>";
		});
	}
	/**
	 * Renders the status page for the user
	 * @return HTML String
	 */
	public static String getStatus() {
		StringBuilder sb = new StringBuilder();
		sb.append("<!DOCTYPE html>");
		sb.append("<html>");
		sb.append("<body>");
		sb.append("<style>");
		sb.append("table {");
		sb.append("  font-family: arial, sans-serif;");
		sb.append("  border-collapse: collapse;");
		sb.append("  width: 100%;");
		sb.append("}");
		sb.append("td, th {");
		sb.append("  border: 1px solid #dddddd;");
		sb.append("  text-align: left;");
		sb.append("  padding: 8px;");
		sb.append("}");
		sb.append("tr:nth-child(even) {");
		sb.append("  background-color: #dddddd;");
		sb.append("}");
		sb.append("</style>");
		sb.append("<title>Master Status Page</title>");
		sb.append("</head>");
		sb.append("<body>");
		sb.append("<h2>Worker Information Table</h2>");
		sb.append("<table>");
		sb.append("<tr>");
		sb.append("<th>IP:Port</th>");
		sb.append("<th>Status</th>");
		sb.append("<th>Job</th>");
		sb.append("<th>Keys Read</th>");
		sb.append("<th>Keys Written</th>");
		sb.append("</tr>");

		for(WorkerStatus ws : workers.values()) {
			if(ws.getTimeStamp().isAfter(Instant.now().minusSeconds(30))) {
				sb.append("<tr>");
				sb.append(String.format("<td>%s</td>", ws.getName()));
				sb.append(String.format("<td>%s</td>", ws.getStatus()));
				sb.append(String.format("<td>%s</td>", ws.getJob()));
				sb.append(String.format("<td>%d</td>", ws.getKeysRead()));
				sb.append(String.format("<td>%d</td>", ws.getKeysWritten()));
				sb.append("</tr>");
			}
		}
		sb.append("</table><br>");

		sb.append("<form action=\"/newjob\" method=\"post\">\n"); 
		sb.append("<label>Class Name of the Job:</label><br><input type=\"text\" name=\"classname\"><br>\n");
		sb.append("<label>Input Directory:</label><br><input type=\"text\" name=\"inputdirectory\"><br>\n");
		sb.append("<label>Output Directory:</label><br><input type=\"text\" name=\"outputdirectory\"><br>\n");
		sb.append("<label>Number of Map threads per worker:</label><br><input type=\"text\" name=\"mapthreads\"><br>\n");
		sb.append("<label>Number of Reduce threads per worker:</label><br><input type=\"text\" name=\"reducethreads\"><br>\n");
		sb.append("<input type=\"submit\" value=\"Submit\">\n");
		sb.append("</form><br>");


		sb.append("<p>Written by: <font color=\"red\">Kushagra Goel</font></body></html>");
		return sb.toString();
	}

	
	/**
	 * Sends job definition to workers and then runs them
	 * @param request
	 * @param response
	 */
	public static void postNewJob(Request request, Response response) {

		try {
			// Job name
			config.put("job", request.queryParams("classname"));

			// IP:port for /workerstatus to be sent
			config.put("master", "127.0.0.1:8080");

			// Class with map and reduce function
			config.put("jobClass", request.queryParams("classname"));

			// Input Directory
			config.put("inputDirectory", request.queryParams("inputdirectory"));
			// Output Directory
			config.put("outputDirectory", request.queryParams("outputdirectory"));

			// Numbers of executors (per node)
			config.put("spoutExecutors", "1");
			config.put("mapExecutors", request.queryParams("mapthreads"));
			config.put("reduceExecutors", request.queryParams("reducethreads"));
			config.put("printExecutors", "1");

			FileSpout spout = new FileSpoutImpl();
			MapBolt bolt = new MapBolt();
			ReduceBolt bolt2 = new ReduceBolt();
			PrintBolt printer = new PrintBolt();

			TopologyBuilder builder = new TopologyBuilder();

			// Only one source ("spout") for the words
			builder.setSpout(WORD_SPOUT, spout, Integer.valueOf(config.get("spoutExecutors")));

			// Parallel mappers, each of which gets specific words
			builder.setBolt(MAP_BOLT, bolt, Integer.valueOf(config.get("mapExecutors"))).fieldsGrouping(WORD_SPOUT, new Fields("value"));

			// Parallel reducers, each of which gets specific words
			builder.setBolt(REDUCE_BOLT, bolt2, Integer.valueOf(config.get("reduceExecutors"))).fieldsGrouping(MAP_BOLT, new Fields("key"));

			// Only use the first printer bolt for reducing to a single point
			builder.setBolt(PRINT_BOLT, printer, 1).firstGrouping(REDUCE_BOLT);

			Topology topo = builder.createTopology();

			WorkerJob job = new WorkerJob(topo, config);

			ObjectMapper mapper = new ObjectMapper();
			mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
			
			
			if(config.get("workerList").length() < 3) {
				halt(503, "No worker has reported yet");
			}
			
			String[] workers = WorkerHelper.getWorkers(config);
			
			
			

			int i = 0;
			for (String dest: workers) {
				config.put("workerIndex", String.valueOf(i++));
				if (sendJob(dest, "POST", config, "definejob", 
						mapper.writerWithDefaultPrettyPrinter().writeValueAsString(job)).getResponseCode() != 
						HttpURLConnection.HTTP_OK) {
					throw new RuntimeException("Job definition request failed");
				}
			}
			for (String dest: workers) {
				if (sendJob(dest, "POST", config, "runjob", "").getResponseCode() != 
						HttpURLConnection.HTTP_OK) {
					throw new RuntimeException("Job execution request failed");
				}
			}
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Method to send data as HTTP request
	 * @param dest
	 * @param reqType
	 * @param config
	 * @param job
	 * @param parameters
	 * @return
	 * @throws IOException
	 */
	static HttpURLConnection sendJob(String dest, String reqType, Config config, String job, String parameters) throws IOException {
		URL url = new URL(dest + "/" + job);

		logger.info("Sending request to " + url.toString());

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

}