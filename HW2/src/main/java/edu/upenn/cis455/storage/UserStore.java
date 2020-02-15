package edu.upenn.cis455.storage;

import java.util.ArrayList;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;


/**
 * Class that handles communications between frontend and BDB backend
 * @author cis455
 *
 */
public class UserStore {
	private static DBWrapper db;
	
	public UserStore(DBWrapper dbArg) {
		db = dbArg;
	}
	public void addUserInfo(User user)
	{
		// Declare the database entry key-value pair that need to be stored in userDB
		DatabaseEntry theVal = new DatabaseEntry();
		DatabaseEntry theKey = new DatabaseEntry();
		// Associate the bindings with the database entries
		db.getUserKeyBinding().objectToEntry(user.getUserName(), theKey);
		db.getUserValBinding().objectToEntry(user, theVal);
		// Begin transaction
		Transaction txn = db.getEnv().beginTransaction(null, null);
		try {
			// Insert key-value pair in userDB
			db.getUserDB().put(null, theKey, theVal);
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
	public User getUserInfo(String username)
	{
		// Declare the database entry key-value pair that need to be stored in userDB
		DatabaseEntry theVal = new DatabaseEntry();
		DatabaseEntry theKey = new DatabaseEntry();
		// Associate the bindings with the database entries
		db.getUserKeyBinding().objectToEntry(username, theKey);
		// Begin transaction
		Transaction txn = db.getEnv().beginTransaction(null, null);
		OperationStatus status = null;
		try {
			// Insert key-value pair in userDB
			status = db.getUserDB().get(null, theKey, theVal, LockMode.DEFAULT);
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
			return (User)db.getUserValBinding().entryToObject(theVal);
		}
		else {
			return null;
		}
	}
	

	public synchronized ArrayList<User> getAllUsers()
	{
		
		ArrayList<User> result = new ArrayList<User>();
		
		// Declare the database entry key-value pair that need to be stored in userDB
		DatabaseEntry theVal = new DatabaseEntry();
		DatabaseEntry theKey = new DatabaseEntry();

		Transaction txn = db.getEnv().beginTransaction(null, null);
		
		
		Cursor cursor = null;
		try {
			cursor = db.getUserDB().openCursor(null, null);
			
			while(cursor.getNext(theKey, theVal, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
				result.add((User)db.getUserValBinding().entryToObject(theVal));
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
	
}
