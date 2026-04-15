package wasdi.shared.config;

import java.util.ArrayList;
import java.util.List;

import wasdi.shared.utils.Utils;

/**
 * Configuration of a processor type
 * @author p.campanella
 *
 */
public class ProcessorTypeConfig {
	
	/**
	 * Name of the processor Type
	 */
	public String processorType;
	
	/**
	 * List of strings that are additional moung points for this specific docker
	 */
	public ArrayList<String> additionalMountPoints = new ArrayList<>();

	/**
	 * List of strings that are additional commands for this specific docker
	 */
	public ArrayList<String> commands = new ArrayList<>();
	
	/**
	 * List of environment variables to pass when creating the container
	 */
	public List<EnvironmentVariableConfig> environmentVariables = new ArrayList<>();
	
	/**
	 * If needed, contains the name of the base image to use
	 */
	public String image;
	
	/**
	 * If needed, contains the version of the base image to use
	 */
	public String version;
	
	/**
	 * Personalized Extra Hosts
	 */
	public ArrayList<String> extraHosts = new ArrayList<>();
	
	/**
	 * Flag to decide if we need to mount on the docker 
	 * the all /data/wasdi folder
	 * or
	 * the /data/wasdi/[usr]/[wsid]/ folder
	 */
	public boolean mountOnlyWorkspaceFolder = false;
	
	/**
	 * List of names of files that must not be downloaded when zipping the processor
	 */
	public ArrayList<String> templateFilesToExcludeFromDownload = new ArrayList<>();
	
	/**
	 * Return the EnvironmentVariableConfig with the specified key
	 * @param sKey Key to search for
	 * @return EnvironmentVariableConfig or null
	 */
	public EnvironmentVariableConfig getEnvironmentVariableConfig(String sKey) {
		
		if (Utils.isNullOrEmpty(sKey)) return null;
		
		if (environmentVariables ==null) return null;
		
		for (EnvironmentVariableConfig oEnvironmentVariableConfig : environmentVariables) {
			if (Utils.isNullOrEmpty(oEnvironmentVariableConfig.key)) continue;
			if (oEnvironmentVariableConfig.key.equals(sKey)) return oEnvironmentVariableConfig;
		}
		return null;
	}
}
