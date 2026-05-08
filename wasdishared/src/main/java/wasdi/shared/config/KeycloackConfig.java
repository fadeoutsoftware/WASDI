package wasdi.shared.config;

/**
 * Keycloack authorization server Configuration
 * @author p.campanella
 *
 */
public class KeycloackConfig {
	
	/**
	 * Server Address
	 */
	public String address;
	
	/**
	 * CLI Secret
	 */
	public String cliSecret;
	
	/**
	 * Token API address
	 */
	public String authTokenAddress;
	
	/**
	 * Introspect APi address
	 */
	public String introspectAddress;
	
	/**
	 * Confidential client name
	 */
	public String confidentialClient;
	
	/**
	 * Client Name
	 */
	public String client;
	
	/**
	 * Client Secret
	 */
	public String clientSecret;
	
	/**
	 * Realm
	 */
	public String realm;
	
	/**
	 * Number of hours before a session expires. Used in the local db and not in keycloak at the moment
	 */
	public int sessionExpireHours = 24;
}
