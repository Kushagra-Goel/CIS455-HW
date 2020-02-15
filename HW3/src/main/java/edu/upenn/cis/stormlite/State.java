package edu.upenn.cis.stormlite;

import java.util.ArrayList;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

/**
 * State Entity Class
 * @author cis455
 *
 */
@Entity
public class State {
	@PrimaryKey
	private String key;
	private ArrayList<String> values;
	/**
	 * Default Constructor
	 */
	public State() {
		
	}
	/**
	 * @param key
	 * @param values
	 */
	public State(String key, ArrayList<String> values) {
		this.key = key;
		this.values = values;
	}
	/**
	 * @param key
	 * @param values
	 */
	public State(String key) {
		this.key = key;
		this.values = new ArrayList<String>();
	}
	/**
	 * @return the key
	 */
	public String getKey() {
		return key;
	}
	/**
	 * @param key the key to set
	 */
	public void setKey(String key) {
		this.key = key;
	}
	/**
	 * @return the values
	 */
	public ArrayList<String> getValues() {
		return values;
	}
	/**
	 * @param values the values to set
	 */
	public void setValues(ArrayList<String> values) {
		this.values = values;
	}
	/**
	 * @param value the value to add
	 */
	public void addValues(String value) {
		this.values.add(value);
	}
	/**
	 * @param value the value to remove
	 */
	public void removeValues(String value) {
		this.values.remove(value);
	}

}