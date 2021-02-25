/**
 * 
 */
package it.fadeout.services;

/**
 * @author c.nattero
 *
 */
public interface AuthProviderService {

	String getToken();
	String getUserData(String sKcTokenId, String sUserId);
	String login(String sUser, String sPassword);

}
