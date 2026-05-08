package wasdi.shared.config;

public class EOEPCAConfig {	
	/**
	 * Folder that will be used in the docker to read-write files. 
	 * This folder is passed as args to the CWL file and used to set the 
	 * working dir of the Docker
	 */
	public String dockerWriteFolder;
	
	/**
	 * Address of the ades server
	 */
	public String adesServerAddress;
	
	/**
	 * Address of the authentication server
	 */
	public String authServerAddress;
	
	/**
	 * Client Id for authentication
	 */
	public String clientId;
	
	/**
	 * Client Secret for authentication
	 */
	public String clientSecret;
	
	/**
	 * User of the eoepca server
	 */
	public String user;
	
	/**
	 * Password of the eoepca server
	 */
	public String password;
	
}
