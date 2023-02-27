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
	 * User of the ades server
	 */
	public String adesUser;
	
	/**
	 * Password of the ades server
	 */
	public String adesPassword;
	
}
