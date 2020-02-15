package edu.upenn.cis.stormlite.spout;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.SequenceInputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.apache.log4j.Logger;

import edu.upenn.cis.stormlite.OutputFieldsDeclarer;
import edu.upenn.cis.stormlite.TopologyContext;
import edu.upenn.cis.stormlite.routers.StreamRouter;
import edu.upenn.cis.stormlite.spout.IRichSpout;
import edu.upenn.cis.stormlite.spout.SpoutOutputCollector;
import edu.upenn.cis.stormlite.tuple.Fields;
import edu.upenn.cis.stormlite.tuple.Values;
import edu.upenn.cis455.mapreduce.worker.WorkerAccountant;

/**
 * Simple word spout, largely derived from
 * https://github.com/apache/storm/tree/master/examples/storm-mongodb-examples
 * but customized to use a file called words.txt.
 * 
 */
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public abstract class FileSpout implements IRichSpout {
	static Logger log = Logger.getLogger(FileSpout.class);

    /**
     * To make it easier to debug: we have a unique ID for each
     * instance of the FileSpout, aka each "executor"
     */
    String executorId = UUID.randomUUID().toString();

    /**
	 * The collector is the destination for tuples; you "emit" tuples there
	 */
	SpoutOutputCollector collector;
	
	/**
	 * This is a Multi file reader
	 */
	String filename;
	File mainFile;
	ArrayList<FileReader> fileReaders = null;
    ArrayList<BufferedReader> bufferedReaders = null;
    Iterator<BufferedReader> bufferedReaderIterator;
    BufferedReader reader;
	Random r = new Random();
	
	int inx = 0;
	boolean sentEof = false;
	
    public FileSpout() {
    	filename = getFilename();
    }
    
    public abstract String getFilename();


    /**
     * Initializes the instance of the spout (note that there can be multiple
     * objects instantiated)
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
        this.collector = collector;
        filename = Paths.get(
        		(String)conf.get("storageDir"), 
        		(String)conf.get("inputDirectory")
    		).toAbsolutePath().toString();
        try {
        	log.debug("Starting spout for " + filename);
        	log.debug(getExecutorId() + " opening file reader");
        	mainFile = new File(filename);
        	if(mainFile.isDirectory()) {
        		fileReaders = new ArrayList<FileReader>();
                bufferedReaders = new ArrayList<BufferedReader>();
        		for(File f : mainFile.listFiles()) {
        			FileReader fr = new FileReader(f);
        			fileReaders.add(fr);
        			bufferedReaders.add(new BufferedReader(fr));
        		}
        	} else {
    			FileReader fr = new FileReader(filename);
    			fileReaders.add(fr);
    			bufferedReaders.add(new BufferedReader(fr));	
        	}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        bufferedReaderIterator = bufferedReaders.iterator();
    }

    /**
     * Shut down the spout
     */
    @Override
    public void close() {
//    	for(BufferedReader reader : bufferedReaders) {
//    		if (reader != null) {
//    	    	try {
//    	    		reader.close();
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//    		}
//    	}
    	for(FileReader reader : fileReaders) {
    		if (reader != null) {
    	    	try {
    	    		reader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		}
    	}
    }

    /**
     * The real work happens here, in incremental fashion.  We process and output
     * the next item(s).  They get fed to the collector, which routes them
     * to targets
     */
    @Override
    public synchronized void nextTuple() {
    	// TODO: Logic below is meant to read from a single file only - modify this to read from multiple files

    	String line = null;
    	while(line == null && !sentEof) {
    		
    		if(reader == null) {
    			if(bufferedReaderIterator.hasNext()) {
    				reader = bufferedReaderIterator.next();
    			} else {
        			this.collector.emitEndOfStream();
        	        sentEof = true;
        	        break;    				
    			}
    		}
	    	try {
	    		line = reader.readLine();
	    	} catch (IOException e) {
	    		e.printStackTrace();
	    	}
    		if (line != null) {
	        	// log.debug(getExecutorId() + " read from file " + getFilename() + ": " + line);
	        	log.debug(getExecutorId() + " emitting " + line);
				this.collector.emit(new Values<Object>(String.valueOf(inx++), line));
	    	} else {
	    		try {
					reader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    		reader = null;
	    	}
    	}    	
//    	
//    	if (reader != null && !sentEof) {
//	    	try {
//		    	String line = reader.readLine();
//		    	if (line != null) {
//		        	// log.debug(getExecutorId() + " read from file " + getFilename() + ": " + line);
//		        	log.debug(getExecutorId() + " emitting " + line);
//					this.collector.emit(new Values<Object>(String.valueOf(inx++), line));
//		    	} else {
//		    		if ()
//		    		if (!sentEof) {
//			        	// log.info(getExecutorId() + " finished file " + getFilename() + " and emitting EOS");
//		    	        this.collector.emitEndOfStream();
//		    	        sentEof = true;
//		    		}
//		    	}
//	    	} catch (IOException e) {
//	    		e.printStackTrace();
//	    	}
//    	}
        Thread.yield();
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("key", "value"));
    }


	@Override
	public String getExecutorId() {
		
		return executorId;
	}


	@Override
	public void setRouter(StreamRouter router) {
		this.collector.setRouter(router);
	}

}
