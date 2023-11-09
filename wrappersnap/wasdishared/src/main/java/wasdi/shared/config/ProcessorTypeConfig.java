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
	 * List of environment variables to pass when creating the container
	 */
	public List<EnvironmentVariableConfig> environmentVariables = new ArrayList<>();
	
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
