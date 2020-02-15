package edu.upenn.cis455.mapreduce.job;

import java.util.Iterator;

import edu.upenn.cis455.mapreduce.Context;
import edu.upenn.cis455.mapreduce.Job;

public class WordCount implements Job {

  public void map(String key, String value, Context context)
  {
    // Splits on space and sends
	  for(String v:value.split(" "))context.write(v, "1");
  }
  
  public void reduce(String key, Iterator<String> values, Context context)
  {
    // Adds all counts and sends
	  int count = 0;
	  while(values.hasNext()) {
		  count++;
		  values.next();
	  }
	  context.write(key, String.valueOf(count));
  }
  
}
