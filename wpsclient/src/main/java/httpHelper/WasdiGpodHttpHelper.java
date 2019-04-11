/**
 * Created by Cristiano Nattero on 2019-01-25
 * 
 * Fadeout software
 *
 */
package httpHelper;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.cookie.Cookie;

/**
 * @author c.nattero
 *
 */
public class WasdiGpodHttpHelper extends WasdiWpsHttpHelper {


	public WasdiGpodHttpHelper() {
		super();
		m_aoAuthParams = new HashMap<>();
		m_aoAuthParams.put("URL", "http://gpod.eo.esa.int/services");
	}

	public WasdiGpodHttpHelper(String sProxy, int iPort ) {		
		super(sProxy,iPort);
	}
	
	@Override
	public void authenticate() {
		
		//TODO review these
		if (m_aoAuthParams == null) return;
		if (m_aoAuthParams.isEmpty()) return;
		if (!m_aoAuthParams.containsKey("URL")) {
			m_aoAuthParams.put("URL", "http://gpod.eo.esa.int/services");
		}
		
		
		String sUrl = (String) m_aoAuthParams.get("URL");
		httpGet(sUrl);
		
		
		if( isAuthenticationNeeded() ) {
			//TODO identify domain in cookie JSESSIONID
			String sDomain = "eo-sso-idp.eo.esa.int";
			//TODO identify action in body
			String sAction = "/idp/umsso20/login?conversation=e1s1";

			String sProt = "https://";
			sUrl = sProt + sDomain + sAction; 
			
			HashMap<String, String> asFormData = new HashMap<String, String>();
			asFormData.put("cn", "userNameHere");
			asFormData.put("password", "anAppropriatePasswordHere");
			asFormData.put("idleTime","halfaday");
			asFormData.put("sessionTime","untilbrowserclose");
			asFormData.put("loginFields","cn%40password2");
			asFormData.put("loginMethod","umsso");
			httpPost(sUrl, asFormData);
		}
		
		if(getLastResponse().getStatusLine().getStatusCode() == 302 ) {
			sUrl = getLastResponse().getFirstHeader("Location").getValue().toString();
			httpGet(sUrl);
		}	
	
		displayCookies();
		
		if(getLastResponse().getStatusLine().getStatusCode() == 200) {
			sUrl = "http://gpod.eo.esa.int/wps?identifier=ProductExtraction,SAROTEC_S1_WPS&Request=DescribeProcess&Service=WPS&version=1.0.0&";
			httpGet(sUrl);
		}		
	}
	
	private boolean isAuthenticationNeeded() {
		
		//FIXME adopt better conditions!
//		boolean bRedirect = (getLastResponse().getStatusLine().getStatusCode() == 302);
//		boolean bIsLogin = isLoginPage();
//		
//		return bRedirect && bIsLogin;
		return true;
	}
	
	private boolean isLoginPage() {
		return isLoginPage(getBodyAsString());
	}
	
	private boolean isLoginPage(String sBody ) {
		//TODO improve test
		boolean bContainsLogin = sBody.contains("login");
		boolean bContainsAction = sBody.contains("action");
		
		return bContainsAction && bContainsLogin;
	}
	
	//https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie
	private String displayCookies() {
		String sCookiesTest = "";
		
		int iSize = this.m_oContext.getCookieStore().getCookies().size();
		for (int iCookies = 0; iCookies<iSize; iCookies++ ) {
			Cookie oCookie = this.m_oContext.getCookieStore().getCookies().get(iCookies);
			
			sCookiesTest += oCookie.getName() + "=" +oCookie.getValue();
			//Expires
			if(null!= oCookie.getExpiryDate() ) {
				sCookiesTest += "; Expires=" + oCookie.getExpiryDate();
			}
			//Domain
			if(null!=oCookie.getDomain()) {
				sCookiesTest += "; Domain="+oCookie.getDomain();
			}
			//Path
			if(null!=oCookie.getPath() ) {
				sCookiesTest += "; Path=" + oCookie.getPath();
			}
			//Secure
			if(true == oCookie.isSecure()) {
				sCookiesTest += "; Secure";
			}
			if (iCookies<iSize-1) {
				sCookiesTest += ",";
			}
			
		}
		System.out.println(sCookiesTest);
		return sCookiesTest;
	}

}
