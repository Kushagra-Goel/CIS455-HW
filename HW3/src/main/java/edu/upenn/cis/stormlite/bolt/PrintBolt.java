package edu.upenn.cis.stormlite.bolt;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;

import edu.upenn.cis.stormlite.OutputFieldsDeclarer;
import edu.upenn.cis.stormlite.TopologyContext;
import edu.upenn.cis.stormlite.distributed.WorkerHelper;
import edu.upenn.cis.stormlite.routers.StreamRouter;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis.stormlite.tuple.Tuple;
import edu.upenn.cis455.mapreduce.worker.WorkerAccountant;
import edu.upenn.cis455.mapreduce.worker.WorkerServer;

public class PrintBolt implements IRichBolt {
    static Logger log = Logger.getLogger(PrintBolt.class);
    PrintWriter out = null;
    Fields myFields = new Fields();

/**
 * To make it easier to debug: we have a unique ID for each
 * instance of the PrintBolt, aka each "executor"
 */
String executorId = UUID.randomUUID().toString();

int neededVotesToComplete = 0;

    @Override
    public void cleanup() {
    	if(out != null) out.close();

    }

    @Override
    public void execute(Tuple input) {

            if (!input.isEndOfStream()) {
        		String key = input.getStringByField("key");
    	        String value = input.getStringByField("value");
    	        log.debug(getExecutorId() + " received " + key + " / " + value);
    	        out.println(String.format("%s / %s", key, value));
    	        out.flush();
            } else {
            	if(--neededVotesToComplete == 0)  {
            		System.out.println("Completed Job!");
	            	WorkerAccountant.reset();
	        	}
            }
    }

    @Override
    public void prepare(Map<String, String> stormConf, TopologyContext context, OutputCollector collector) {
    	Path filePath = Paths.get(
        		stormConf.get("storageDir"), 
        		stormConf.get("outputDirectory"),
        		"output.txt"
        		);
    	File output = filePath.toFile();
    	if(output.exists())output.delete();
    	try {
			out = new PrintWriter(new BufferedWriter(new FileWriter(filePath.toAbsolutePath().toString(), true)));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        // EOS gets sent to each senderbolt multiple (number of local bolts at current step) times 
        // due multiple senderbolt being duplicated in bolts list as specified in DistributedCluster :
        	// Create a bolt for each remote worker, give it the same # of entries
     		// as we had locally so round-robin and partitioning will be consistent
        neededVotesToComplete = Integer.parseInt(stormConf.get("reduceExecutors")) 
        		* (1 // Local Source
        				+ (Integer.parseInt(stormConf.get("printExecutors"))  // number of bolts per worker at current step
        						* (WorkerHelper.getWorkers(stormConf).length - 1))); // number of remote workers
    }

    @Override
    public String getExecutorId() {
            return executorId;
    }

    @Override
    public void setRouter(StreamRouter router) {
            // Do nothing
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
            declarer.declare(myFields);
    }

    @Override
    public Fields getSchema() {
            return myFields;
    }

}
