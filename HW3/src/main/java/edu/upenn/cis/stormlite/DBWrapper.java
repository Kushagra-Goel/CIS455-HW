package edu.upenn.cis.stormlite;

import java.io.File;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.collections.StoredSortedMap;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.StoreConfig;

/** HW3 A wrapper class which should include:
  * - Set up of Berkeley DB
  * - Saving and retrieving reduce operation state
  */
public class DBWrapper {
	
	private Environment env = null;
	private EntityStore stateStore = null;
	/*
	* STATE_STORE : Name used to identify stateDB
	*/
	private static final String STATE_STORE = "state_store";
//
//	EntryBinding stateKeyBinding;
//	EntryBinding stateValBinding;
	
	public DBWrapper(String envDirectory)
	{
		try
		{
			// Create EnvironmentConfig object
			EnvironmentConfig envConfig = new EnvironmentConfig();
			// Environment should be capable of performing transactions
			envConfig.setTransactional(true);
			// Create a database environment if it doesn’t already exist
			envConfig.setAllowCreate(true);
			File dir = new File(envDirectory);
			if(!dir.exists()) {
				dir.mkdir();
				dir.setReadable(true);
				dir.setWritable(true);
			}
			// Instantiate environment
			this.env = new Environment(dir, envConfig);
			

			// Create StoreConfig object
			StoreConfig storeConfig = new StoreConfig();
			// Store should be capable of performing transactions
			storeConfig.setTransactional(true);
			// Create a Store if it doesn’t already exist
			storeConfig.setAllowCreate(true);
			stateStore = new EntityStore(env, STATE_STORE, storeConfig);
			
//			// Create DatabaseConfig object
//			DatabaseConfig dbConfig = new DatabaseConfig();
//			
//			// Encloses the database open within a transaction.
//			dbConfig.setTransactional(true);
//			
//			// Create the database if it does not already exist
//			dbConfig.setAllowCreate(true);
//			// Instantiate a catalog database to keep track of the database’s metadata
//			Database catalogDB = env.openDatabase(null, CLASS_CATALOG,dbConfig);
//			this.catalog = new StoredClassCatalog(catalogDB);
//			// Instantiate user database
//			this.stateDB = env.openDatabase(null,STATE_STORE, dbConfig);
//			// Instantiate the bindings for the key and value classes
//			this.stateKeyBinding = new SerialBinding(this.catalog, String.class);
//			this.stateValBinding = new SerialBinding(this.catalog, State.class);

		}
		catch(DatabaseException e)
		{
			throw e;
		}
	}
	// Return Environment instance
	public Environment getEnv()
	{
		return this.env;
	}
	// Return stateDB instance
	public EntityStore getStateStore()
	{
		return this.stateStore;
	}
//	// Return userDB key binding
//	public EntryBinding getStateKeyBinding()
//	{
//		return this.stateKeyBinding;
//	}
//	// Return userDB val binding
//	public EntryBinding getStateValBinding()
//	{
//		return this.stateValBinding;
//	}

	
	public void close() {
		stateStore.close();
		env.close();
	}
}
