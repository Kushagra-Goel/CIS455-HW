package edu.upenn.cis455.storage;

import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;

import edu.upenn.cis455.crawler.info.URLInfo;

/**
 * Class that handles communication between the front end and the BDB backend
 * @author cis455
 *
 */
public class DocStore {
	private static DBWrapper db;
	
	public DocStore(DBWrapper dbArg) {
		db = dbArg;
	}
	public synchronized void addDocument(CrawledDocument doc)
	{
		// Declare the database entry key-value pair that need to be stored in userDB
		DatabaseEntry theVal = new DatabaseEntry();
		DatabaseEntry theKey = new DatabaseEntry();
		// Associate the bindings with the database entries
		db.getDocKeyBinding().objectToEntry(doc.getUrl(), theKey);
		db.getDocValBinding().objectToEntry(doc, theVal);
		// Begin transaction
		Transaction txn = db.getEnv().beginTransaction(null, null);
		try {
			// Insert key-value pair in userDB
			db.getDocDB().put(null, theKey, theVal);
			// Commit the transaction
			txn.commit();
		} catch(Exception e) {
			if (txn != null)
			{
				 txn.abort();
				 txn = null;
			 }
		}
	}
	public synchronized CrawledDocument getDocument(URLInfo url)
	{
		// Declare the database entry key-value pair that need to be stored in userDB
		DatabaseEntry theVal = new DatabaseEntry();
		DatabaseEntry theKey = new DatabaseEntry();
		// Associate the bindings with the database entries
		db.getDocKeyBinding().objectToEntry(url, theKey);
		// Begin transaction
		Transaction txn = db.getEnv().beginTransaction(null, null);
		OperationStatus status = null;
		try {
			// Insert key-value pair in userDB
			status = db.getDocDB().get(null, theKey, theVal, LockMode.DEFAULT);
			// Commit the transaction
			txn.commit();
		} catch(Exception e) {
			if (txn != null)
			{
				 txn.abort();
				 txn = null;
			 }
		}
		// Convert database entry to UserVal
		if(status == OperationStatus.SUCCESS) {
			return (CrawledDocument)db.getDocValBinding().entryToObject(theVal);
		}
		else {
			return null;
		}
	}
	

	public void close() {
		db.close();
	}
	
	
}
