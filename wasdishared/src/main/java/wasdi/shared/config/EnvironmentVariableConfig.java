package wasdi.shared.config;

/**
 * Represents an Environment Variable.
 * Used when starting a Container
 * At the beginning introduced to configure the ProcessorTypes
 * 
 * @author p.campanella
 *
 */
public class EnvironmentVariableConfig {
	
	/**
	 * Name of the environment variable
	 */
	public String key;
	
	/**
	 * Value of the environment variable
	 */
	public String value;

}
