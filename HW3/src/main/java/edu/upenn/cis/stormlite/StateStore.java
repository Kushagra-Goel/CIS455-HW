/**
 * 
 */
package edu.upenn.cis.stormlite;

import java.util.ArrayList;
import java.util.List;

import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.PrimaryIndex;

/**
 * Store to inteface with underlying berkeleyDB
 * @author cis455
 *
 */
public class StateStore {
	private DBWrapper db;
	
	public StateStore(DBWrapper dbArg) {
		db = dbArg;
	}
	/**
	 * Adds a state to the database
	 * @param state
	 */
	public synchronized void addState(State state)
	{
		PrimaryIndex<String, State> statePI = db.getStateStore().getPrimaryIndex(String.class, State.class);
		
		State oldState = statePI.get(state.getKey());
		if(oldState != null) {
			for(String value: oldState.getValues()) {
				state.addValues(value);
			}
		}
		statePI.put(state);
		
//		// Declare the database entry key-value pair that need to be stored in userDB
//		DatabaseEntry theVal = new DatabaseEntry();
//		DatabaseEntry theKey = new DatabaseEntry();
//		// Associate the bindings with the database entries
//		db.getStateKeyBinding().objectToEntry(state.getKey(), theKey);
//		db.getStateValBinding().objectToEntry(state, theVal);
//		// Begin transaction
//		Transaction txn = db.getEnv().beginTransaction(null, null);
//		try {
//			// Insert key-value pair in userDB
//			db.getStateDB().put(null, theKey, theVal);
//			// Commit the transaction
//			txn.commit();
//		} catch(Exception e) {
//			if (txn != null)
//			{
//				 txn.abort();
//				 txn = null;
//			 }
//		}
	}
	/**
	 * Gets the state from the database
	 * @param key
	 * @return
	 */
	public synchronized State getState(String key)
	{

		PrimaryIndex<String, State> statePI = db.getStateStore().getPrimaryIndex(String.class, State.class);
		return statePI.get(key);
//		// Declare the database entry key-value pair that need to be stored in userDB
//		DatabaseEntry theVal = new DatabaseEntry();
//		DatabaseEntry theKey = new DatabaseEntry();
//		// Associate the bindings with the database entries
//		db.getStateKeyBinding().objectToEntry(key, theKey);
//		// Begin transaction
//		Transaction txn = db.getEnv().beginTransaction(null, null);
//		OperationStatus status = null;
//		try {
//			// Insert key-value pair in userDB
//			status = db.getStateDB().get(null, theKey, theVal, LockMode.DEFAULT);
//			// Commit the transaction
//			txn.commit();
//		} catch(Exception e) {
//			if (txn != null)
//			{
//				 txn.abort();
//				 txn = null;
//			 }
//		}
//		// Convert database entry to UserVal
//		if(status == OperationStatus.SUCCESS) {
//			return (State)db.getStateValBinding().entryToObject(theVal);
//		}
//		else {
//			return null;
//		}
	}

	/**
	 * Gets all states from the database
	 * @return
	 */
	public synchronized List<State> getAllState()
	{
		List<State> states = new ArrayList<State>();
		PrimaryIndex<String, State> statePI = db.getStateStore().getPrimaryIndex(String.class, State.class);
		EntityCursor<State> cursor = statePI.entities();
		for(State state : cursor) {
			states.add(state);
		}
		cursor.close();
		return states;
	}
	

	public void close() {
		db.getEnv().sync();
		db.getStateStore().sync();
		db.close();
	}
	

}
