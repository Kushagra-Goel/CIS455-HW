package edu.upenn.cis455.mapreduce.worker;

import java.util.Map;
import java.util.HashMap;

public class WorkerMain {
    public static void main(String[] args) {

      if (args.length != 3) {
        System.err.println("You need to provide: 1) the master's IP:port, 2) the path to the storage direcotry, and 3) the port number on which to listen for commands from the master.");
        System.exit(1);
      }

    	// TODO: Start WorkerServer from this class based on the command-line arguments provided

        Map<String, String> config = new HashMap<String, String>();
        config.put("masterIpPort", args[0]);
        config.put("storageDir", args[1]);
        config.put("workerList", "[localhost:"+args[2]+"]");
        config.put("workerIndex", "0");
        WorkerServer.createWorker(config);
        
        
        // Start the periodic thread
        WorkerAccountant.setMasterIpPort(args[0]);
        WorkerAccountant.setPort(Integer.parseInt(args[2]));
        WorkerAccountant wa = new WorkerAccountant();
        wa.start();
    }
}
