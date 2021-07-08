/**
 * 
 */
package it.fadeout.services;
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

}
