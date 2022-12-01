package wasdi.shared.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import wasdi.shared.utils.Utils;

public class EOEPCAConfig {
	/**
	 * List of docker registries supported
	 */
	public ArrayList<DockerRegistryConfig> registers;
	
	/**
	 * Get the list of registers ordered by priority
	 * @return Ordered list of registers
	 */
	public ArrayList<DockerRegistryConfig> getRegisters() {
		try {
			Collections.sort(registers, Comparator.comparing(DockerRegistryConfig::getPriority));
		}		
		catch (Exception oEx) {
			Utils.debugLog("DockersConfig.getRegisters: Exception ordering the registers list");
		}
		
		return registers;
	}
	
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
