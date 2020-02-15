package edu.upenn.cis455.storage;

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

import edu.upenn.cis455.crawler.info.URLInfo;

/** (MS1, MS2) A wrapper class which should include:
  * - Set up of Berkeley DB
  * - Saving and retrieving objects including crawled docs and user information
  */
public class DBWrapper {
	
	private static String envDirectory = null;
	
//	private static Environment myEnv;
//	private static EntityStore store;
	
	/* TODO: write object store wrapper for BerkeleyDB */
	
	private Environment env = null;
	/*
	* USER_STORE : Name used to identify userDB
	 * DOC_STORE : Name used to identify docDB
	* CLASS_CATALOG : Name used to identify the class catalog
	*/
	private static final String USER_STORE = "user_store";
	private static final String DOC_STORE = "doc_store";
	private static final String CHANNEL_STORE = "channel_store";
	private static final String CLASS_CATALOG = "class_catalog";
	private StoredClassCatalog catalog = null;
	private Database userDB = null;
	private Database docDB = null;
	private Database channelDB = null;

	EntryBinding userKeyBinding;
	EntryBinding userValBinding;
	EntryBinding docKeyBinding;
	EntryBinding docValBinding;
	EntryBinding channelKeyBinding;
	EntryBinding channelValBinding;
	
	public DBWrapper(String dir)
	{
		if(env == null) {
			envDirectory = dir;
			try
			{
				// Create EnvironmentConfig object
				EnvironmentConfig envConfig = new EnvironmentConfig();
				// Environment should be capable of performing transactions
				envConfig.setTransactional(true);
				// Create a database environment if it doesn’t already exist
				envConfig.setAllowCreate(true);
				// Instantiate environment
				this.env = new Environment(new File(dir), envConfig);
				// Create DatabaseConfig object
				DatabaseConfig dbConfig = new DatabaseConfig();
				
				// Encloses the database open within a transaction.
				dbConfig.setTransactional(true);
				
				// Create the database if it does not already exist
				dbConfig.setAllowCreate(true);
				// Instantiate a catalog database to keep track of the database’s metadata
				Database catalogDB = env.openDatabase(null, CLASS_CATALOG,dbConfig);
				this.catalog = new StoredClassCatalog(catalogDB);
				// Instantiate user database
				this.userDB = env.openDatabase(null,USER_STORE, dbConfig);
				this.docDB = env.openDatabase(null,DOC_STORE, dbConfig);
				this.channelDB = env.openDatabase(null,CHANNEL_STORE, dbConfig);
				// Instantiate the bindings for the key and value classes
				this.userKeyBinding = new SerialBinding(this.catalog, String.class);
				this.userValBinding = new SerialBinding(this.catalog, User.class);
				this.docKeyBinding = new SerialBinding(this.catalog, URLInfo.class);
				this.docValBinding = new SerialBinding(this.catalog, CrawledDocument.class);
				this.channelKeyBinding = new SerialBinding(this.catalog, String.class);
				this.channelValBinding = new SerialBinding(this.catalog, Channel.class);

			}
			catch(DatabaseException e)
			{
				throw e;
			}
		} else {
			if(dir != envDirectory) throw new IllegalArgumentException("BDB has already been instantiated with a different directory");
		}
	}
	// Return Environment instance
	public Environment getEnv()
	{
		return this.env;
	}
	// Return userDB instance
	public Database getUserDB()
	{
		return this.userDB;
	}
	// Return docDB instance
	public Database getDocDB()
	{
		return this.docDB;
	}
	// Return channelDB instance
	public Database getChannelDB()
	{
		return this.channelDB;
	}
	// Return userDB key binding
	public EntryBinding getUserKeyBinding()
	{
		return this.userKeyBinding;
	}
	// Return userDB val binding
	public EntryBinding getUserValBinding()
	{
		return this.userValBinding;
	}
	// Return docDB key binding
	public EntryBinding getDocKeyBinding()
	{
		return this.docKeyBinding;
	}
	// Return docDB val binding
	public EntryBinding getDocValBinding()
	{
		return this.docValBinding;
	}
	// Return channelDB key binding
	public EntryBinding getChannelKeyBinding()
	{
		return this.channelKeyBinding;
	}
	// Return channelDB val binding
	public EntryBinding getChannelValBinding()
	{
		return this.channelValBinding;
	}

	
	public void close() {
		docDB.close();
		userDB.close();
		channelDB.close();
		catalog.close();
		env.close();
	}
}
