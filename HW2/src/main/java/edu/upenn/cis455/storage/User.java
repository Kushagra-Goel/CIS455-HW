package edu.upenn.cis455.storage;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

/*
 * User class
 */
public class User implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4107606447154401630L;
	private String firstName = null;
	private String lastName = null;
	private String userName = null;
	private byte[] passwordHash = null;
	private ArrayList<String> channelNames;
	
	
	public User(String firstNameArg, String lastNameArg, String userNameArg, String passwordArg, ArrayList<String> channelNamesArg) throws NoSuchAlgorithmException {
		firstName = firstNameArg;
		lastName = lastNameArg;
		userName = userNameArg;
		channelNames = channelNamesArg;
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		//SHA256 passwords only
		passwordHash = digest.digest(passwordArg.getBytes(StandardCharsets.UTF_8));
	}


	/**
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}


	/**
	 * @return the firstName
	 */
	public String getFirstName() {
		return firstName;
	}


	/**
	 * @return the lastName
	 */
	public String getLastName() {
		return lastName;
	}


	/**
	 * @return the passwordHash
	 */
	public byte[] getPasswordHash() {
		return passwordHash;
	}


	/**
	 * @return the channelNames
	 */
	public ArrayList<String> getChannelNames() {
		return channelNames;
	}
}
