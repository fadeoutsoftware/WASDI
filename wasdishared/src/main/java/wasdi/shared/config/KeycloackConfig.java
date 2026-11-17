package wasdi.shared.config;

import java.util.ArrayList;

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
	 * Issuer expected in locally validated JWTs. If empty, address and realm are used.
	 */
	public String issuer;

	/**
	 * External OAuth clients allowed to call APIs with bearer tokens.
	 */
	public ArrayList<String> acceptedClientIds = new ArrayList<>();
	
	/**
	 * Realm
	 */
	public String realm;
	
	/**
	 * Number of hours before a session expires. Used in the local db and not in keycloak at the moment
	 */
	public int sessionExpireHours = 24;

	/**
	 * Return local sessions at login and validate them without Keycloak introspection.
	 * Credential authentication is unchanged.
	 */
	public boolean useLegacySessions = false;
	
	/**
	 * Optional internal address of the keycloak service to use instead of the public one to
	 * get the certificates to check JWT tokens
	 */
	public String jwksUrl = null;
}
