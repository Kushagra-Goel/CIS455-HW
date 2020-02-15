package edu.upenn.cis455.storage;

import java.util.ArrayList;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;

/**
 * Class that handles communication between the front end and the BDB backend
 * @author cis455
 *
 */
public class ChannelStore {

	private static DBWrapper db;
	
	public ChannelStore(DBWrapper dbArg) {
		db = dbArg;
	}
	public synchronized void addChannel(Channel channel)
	{
		// Declare the database entry key-value pair that need to be stored in userDB
		DatabaseEntry theVal = new DatabaseEntry();
		DatabaseEntry theKey = new DatabaseEntry();
		// Associate the bindings with the database entries
		db.getChannelKeyBinding().objectToEntry(channel.getName(), theKey);
		db.getChannelValBinding().objectToEntry(channel, theVal);
		// Begin transaction
		Transaction txn = db.getEnv().beginTransaction(null, null);
		try {
			// Insert key-value pair in userDB
			db.getChannelDB().put(null, theKey, theVal);
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
	public synchronized void deleteChannel(Channel channel)
	{
		// Declare the database entry key-value pair that need to be stored in userDB
		DatabaseEntry theVal = new DatabaseEntry();
		DatabaseEntry theKey = new DatabaseEntry();
		// Associate the bindings with the database entries
		db.getChannelKeyBinding().objectToEntry(channel.getName(), theKey);
		db.getChannelValBinding().objectToEntry(channel, theVal);
		// Begin transaction
		Transaction txn = db.getEnv().beginTransaction(null, null);
		try {
			// Insert key-value pair in userDB
			db.getChannelDB().delete(null, theKey);
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

	public synchronized Channel getChannel(String name)
	{
		// Declare the database entry key-value pair that need to be stored in userDB
		DatabaseEntry theVal = new DatabaseEntry();
		DatabaseEntry theKey = new DatabaseEntry();
		// Associate the bindings with the database entries
		db.getChannelKeyBinding().objectToEntry(name, theKey);
		// Begin transaction
		Transaction txn = db.getEnv().beginTransaction(null, null);
		OperationStatus status = null;
		try {
			// Insert key-value pair in userDB
			status = db.getChannelDB().get(null, theKey, theVal, LockMode.DEFAULT);
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
			return (Channel)db.getChannelValBinding().entryToObject(theVal);
		}
		else {
			return null;
		}
	}
	
	public synchronized ArrayList<Channel> getAllChannels()
	{
		
		ArrayList<Channel> result = new ArrayList<Channel>();
		
		// Declare the database entry key-value pair that need to be stored in userDB
		DatabaseEntry theVal = new DatabaseEntry();
		DatabaseEntry theKey = new DatabaseEntry();

		Transaction txn = db.getEnv().beginTransaction(null, null);
		
		
		Cursor cursor = null;
		try {
			cursor = db.getChannelDB().openCursor(null, null);
			
			while(cursor.getNext(theKey, theVal, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
				result.add((Channel)db.getChannelValBinding().entryToObject(theVal));
			}
			cursor.close();
			txn.commit();
			return result;
		} catch(Exception e) {
			if(cursor != null)cursor.close();
			if (txn != null)
			{
				 txn.abort();
				 txn = null;
			 }
			return null;
		}
	}
	

	public void close() {
		db.close();
	}
	
	
}
