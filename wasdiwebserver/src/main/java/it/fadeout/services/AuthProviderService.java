package it.fadeout.services;
import wasdi.shared.business.users.User;
import wasdi.shared.viewmodels.PrimitiveResult;

/**
 * Interface of an Authorization Provider
 * @author c.nattero
 *
 */
public interface AuthProviderService {
	
	/**
	 * Get the user token
	 * @return
	 */
	String getToken();
	/**
	 * Get user data 
	 * @param sToken user token
	 * @param sUserId user id
	 * @return
	 */
	String getUserData(String sToken, String sUserId);
	/**
	 * Login the user
	 * @param sUser user id
	 * @param sPassword password
	 * @return
	 */
	String login(String sUser, String sPassword);
	
	/**
	 * Logs out the user
	 * @param sRefreshToken refresh token of the user
	 * @return
	 */
	boolean logout(String sRefreshToken);
	/**
	 * Get the user db id
	 * @param sUserId user id
	 * @return
	 */
	String getUserDbId(String sUserId);
	/**
	 * Requires a password update
	 * @param sUserId
	 * @return
	 */
	PrimitiveResult requirePasswordUpdateViaEmail(String sUserId);
	/**
	 * Checks the user exists in keycloak
	 * @param sUserId the id of the user (in most cases, her/his email)
	 * @return a User if it exists, null otherwise
	 * @throws NullPointerException if the response is null
	 * @throws IllegalStateException if the users found come in quantity different from 1
	 */
	User getUser(String sUserId);
	

}
