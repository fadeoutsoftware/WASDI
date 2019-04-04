/**
 * Created by Cristiano Nattero on 2019-03-07
 * 
 * Fadeout software
 *
 */
package it.fadeout.rest.resources.wps;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

import it.fadeout.Wasdi;

/**
 * @author c.nattero
 *
 */
public class WpsProxyBasicAuth extends WpsProxy {

	protected boolean m_bIsAuthenticatorSet;

	public WpsProxyBasicAuth() {
		m_bIsAuthenticatorSet = false;
	}

	@Override
	protected void setAuthenticator() {
		Wasdi.DebugLog("WpsProxyBasicAuth.authenticate");
		updateProviderUrlAsNeeded();
		if(null==m_oCredentials) {
			throw new NullPointerException("WpsProxyBNasicAuth.setAuthenticator: null credentials. Not initialized?");
		}
		Authenticator.setDefault(new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				try {
					return new PasswordAuthentication(m_oCredentials.getUser(), m_oCredentials.getPassword().toCharArray());
				} catch (Exception oEx) {
					Wasdi.DebugLog("WpsProxyBasicAuth.authenticate" + oEx.getMessage() );
					throw oEx;
				}
			}
		});
		m_bIsAuthenticatorSet = true;
	}

	@Override
	protected void authenticateAsNeeded() {
		Wasdi.DebugLog("WpsProxyBasicAuth.authenticateAsNeeded");
		if(!m_bIsAuthenticatorSet) {
			setAuthenticator();
			if(null==m_oCredentials) {
				throw new NullPointerException("WpsProxyBasicAuth.authenticateAsNeeded: credentials still null after initialization");
			}
		}
	}
}
