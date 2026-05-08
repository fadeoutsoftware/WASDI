package wasdi.shared.config;

/**
 * Represents the configuration a Docker Registry 
 * @author p.campanella
 *
 */
public class DockerRegistryConfig {
	/**
	 * Unique identifier of the registry
	 */
	public String id;
	/**
	 * User of the registry
	 */
	public String user;
	/**
	 * Password of the above user
	 */
	public String password;
	/**
	 * http address of the registry
	 */
	public String address;
	/**
	 * Priority of the register
	 */
	public int priority;
	/**
	 * http address of the API of the register
	 */
	public String apiAddress;
	/**
	 * Name of the repository inside the Registry
	 */
	public String repositoryName="wasdi-docker";
	
	public int getPriority() {
		return priority;
	}
}
