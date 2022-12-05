/**
 * Created by Cristiano Nattero on 2019-02-07
 * 
 * Fadeout software
 *
 */
package wasdi.shared.business;

import wasdi.shared.utils.Utils;

/**
 * @author c.nattero
 *
 */
public class AuthenticationCredentials {
	String m_sUser;
	String m_sPassword;
	String m_sApiKey;
	
	public AuthenticationCredentials(String sUser, String sPassword, String sApiKey) {
		if(!Utils.isNullOrEmpty(sUser) && !Utils.isNullOrEmpty(sPassword)) {
			this.m_sUser = sUser;
			this.m_sPassword = sPassword;
		}
		if(!Utils.isNullOrEmpty(sApiKey)) {
			this.m_sApiKey = sApiKey;
		} 
	}
	
	public String getUser() {
		return m_sUser;
	}
	public String getPassword() {
		return m_sPassword;
	}
	public String getApiKey() {
		return m_sApiKey;
	}
	
}
