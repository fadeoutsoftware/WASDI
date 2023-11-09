package wasdi.shared.config;

import java.util.ArrayList;
import java.util.List;

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
}
