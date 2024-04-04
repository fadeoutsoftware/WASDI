package wasdi.shared.config;

/**
 * Configuration specific of the Meluxina Super Computer environment
 */
public class MeluxinaConfig {
	
	/**
	 * Declared Slurm Version for the API
	 */
	public String apiVersion = "v0.0.38";
	/**
	 * Base Address of the API
	 */
	public String apiUrl = "http://172.42.0.1:6820/slurm/";
	/**
	 * Meluxina Token 
	 */
	public String token = "";
	/**
	 * Meluxina User
	 */
	public String user = "u101504";
	/**
	 * Meluxina Project
	 */
	public String project = "p200333";
	
	/**
	 * Home folder of the user in Meluxina
	 */
	public String home = "/home/users/";
	
	/**
	 * Address of the registry where Meluxina will pull the image
	 */
	public String registryAddress = "test-registry-processors.wasdi.net";
	
	/**
	 * User Name of the registry
	 */
	public String registryUserName = "external-meluxina-ro";
	
	/**
	 * Password for the registry
	 */
	public String registryPassword;

}
