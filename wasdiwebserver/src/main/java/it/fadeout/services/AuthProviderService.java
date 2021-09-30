/**
 * 
 */
package it.fadeout.services;
import wasdi.shared.business.User;
import wasdi.shared.viewmodels.PrimitiveResult;

/**
 * @author c.nattero
 *
 */
public interface AuthProviderService {

	String getToken();
	String getUserData(String sToken, String sUserId);
	String login(String sUser, String sPassword);
	String getUserDbId(String sUserId);
	PrimitiveResult requirePasswordUpdateViaEmail(String sUserId);
	/**
	 * Checks the user exists in keycloak
	 * @param sUserId the id of the user (in most cases, her/his email)
	 * @return a User if it exists, null otherwise
	 * @throws NullPointerException if the response is null
	 * @throws IllegalStateException if the users found come in quantity different from 1
	 */
	User getUser(String sUserId);
	String insertOldStyleSession(String sUserId);
	String getOldStyleRandomSession();

}
