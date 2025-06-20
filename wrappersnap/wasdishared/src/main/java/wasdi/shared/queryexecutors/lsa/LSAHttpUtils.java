package wasdi.shared.queryexecutors.lsa;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.net.io.Util;

import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.StringUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;

public class LSAHttpUtils {
	private LSAHttpUtils() {
		// / private constructor to hide the public implicit one 
	}
	
    public static String s_sLoginBaseUrl = "https://ivv.collgs.lu/sso/realms/lucollgs/protocol/openid-connect/auth?";
    public static String s_sClientId = "client_id=account&response_mode=fragment&response_type=code&";
    public static String s_sRedirectUrl = "redirect_uri=https://collgs.lu/geocatalog.html";
    public static String s_sLogoutUrl = "https://ivv.collgs.lu/sso/realms/lucollgs/protocol/openid-connect/logout?redirect_uri=https://collgs.lu/";
    	
    public static void authenticate(String sUser, String sPassword) {
    	
    	try {
    		
    		WasdiLog.debugLog("LSAHttpUtils.authenticate: " + sUser);
    		
    		// Create the cookie manager
    		CookieManager oCookieManager = new CookieManager();
    		CookieHandler.setDefault(oCookieManager);
    		
    		// Login URL
    		String sLoginUrl = s_sLoginBaseUrl+s_sClientId+s_sRedirectUrl;
    		String sLoginPage = httpGetResults(sLoginUrl, oCookieManager);
    		if (Utils.isNullOrEmpty(sLoginPage)) {
    			throw new Exception("Authentication failed - httpGETResults returned NULL string");
    		}
    		
    		String sActionLink = getLoginAction(sLoginPage);
    		
    		/*
    		// Get the path of the login link
    		String sActionToSearch = "action=\"";
    		int iActionStart=sLoginPage.indexOf(sActionToSearch);
    		int iActionEnd = iActionStart+sActionToSearch.length();
    		int iLinkEnd= sLoginPage.indexOf("\" method=\"post\">");
    		
    		String sActionLink = sLoginPage.substring(iActionEnd, iLinkEnd);
    		sActionLink = sActionLink.replace("&amp;", "&");
    		 */
    		 
    		// Body with the login data
    		String sLoginData = "username=" + StringUtils.encodeUrl(sUser) + "&password=" + StringUtils.encodeUrl(sPassword);
    		// Log in
    		LSAHttpUtils.httpPostResults(sActionLink, sLoginData, oCookieManager);
    		
    	}
    	catch (Exception oEx) {
			WasdiLog.errorLog("LSAHttpUtils.authenticate: Exception  ", oEx);
		}
    }
    
    private static String getLoginAction(String sJavascripCode) {    	
    	try { 
	    	Pattern oPattern = Pattern.compile(
	                "\"loginAction\"\\s*:\\s*\"(https:\\/\\/ivv\\.collgs\\.lu\\/sso\\/realms\\/lucollgs\\/login-actions\\/authenticate\\?session_code=[^&\"]+&execution=[^&\"]+&client_id=account&tab_id=[^&\"]+&client_data=[^\"]+)\""
	            );
	
	
	        Matcher oMatcher = oPattern.matcher(sJavascripCode);
	
	        if (oMatcher.find()) {
	            String sLoginActionUrl = oMatcher.group(1);
	            return sLoginActionUrl;
	        } 
    	}
        catch (Exception oE) {
        	WasdiLog.errorLog("LSAHttpUtils.getLoginAction. impossible to extract the login action URL", oE);
        }
        
        return "";
    }
    
	/**
	 * Internal version of get 
	 * @param sUrl
	 * @param oCookieManager
	 * @return
	 */
	public static String httpGetResults(String sUrl, CookieManager oCookieManager) {
		WasdiLog.debugLog("LSAHttpUtils.httpGetResults( " + sUrl + " )");

		HashMap<String, String> asHeaders = new HashMap<String, String>();
		
		HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(sUrl, asHeaders);
		String sResult = oHttpCallResponse.getResponseBody();

		return sResult;
	}

	
	public static String httpPostResults(String sUrl, String sPayload, CookieManager oCookieManager) {
		WasdiLog.debugLog("LSAHttpUtils.httpPostResults( " + sUrl + " )");
		String sResult = null;
		long lStart = 0l;
		int iResponseSize = -1;
		try {
			URL oURL = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();
			oConnection.setRequestProperty("Accept", "*/*");
			oConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			oConnection.setRequestProperty("Connection", "keep-alive");
			oConnection.setRequestProperty("Accept-Encoding", "gzip, deflate");
			oConnection.setRequestProperty("User-Agent", "python-requests/2.22.0");
			
			byte[] ayBytes = sPayload.getBytes();
			oConnection.setFixedLengthStreamingMode(ayBytes.length);
			
			if (oCookieManager.getCookieStore().getCookies().size() > 0) {
				List<HttpCookie> aoCookieList = oCookieManager.getCookieStore().getCookies();
				
				String sCookie = "";
				
				for (HttpCookie oHttpCookie : aoCookieList) {
					sCookie += oHttpCookie.getName() + "=" + oHttpCookie.getValue();
					sCookie += "; ";
				}
				
				if (sCookie.length()>0) {
					sCookie = sCookie.substring(0, sCookie.length()-2);
				}
				
				oConnection.setRequestProperty("Cookie", sCookie);				
	        }
			
			oConnection.setDoOutput(true);
			oConnection.connect();
			try(OutputStream os = oConnection.getOutputStream()) {
			    os.write(ayBytes);
			}
			

			//WasdiLog.debugLog("QueryExecutorLSA.httpPostResults: Sending 'POST' request to URL : " + sUrl);

			lStart = System.nanoTime();
			try {
				int iResponseCode =  oConnection.getResponseCode();
				//WasdiLog.debugLog("QueryExecutorLSA.httpGetResults: Response Code : " + iResponseCode);
				String sResponseExtract = null;
				if(iResponseCode == 200) {
					InputStream oInputStream = oConnection.getInputStream();
					ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
					if(null!=oInputStream) {
						Util.copyStream(oInputStream, oBytearrayOutputStream);
						sResult = oBytearrayOutputStream.toString();
					}
				} else if (iResponseCode == 302) {
					
					String sNewUrl = oConnection.getHeaderField("Location");
					String sCookies = oConnection.getHeaderField("Set-Cookie");
					
					//WasdiLog.debugLog("QueryExecutorLSA.httpPostResults: redirect to " + sNewUrl);
					
					oConnection = (HttpURLConnection) new URL(sNewUrl).openConnection();
					
					oConnection.setRequestProperty("Cookie", sCookies);
					
					InputStream oInputStream = oConnection.getInputStream();
					ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
					if(null!=oInputStream) {
						Util.copyStream(oInputStream, oBytearrayOutputStream);
						sResult = oBytearrayOutputStream.toString();
					}					
				}
				else {
					
					WasdiLog.debugLog("LSAHttpUtils.httpPostResults: provider did not return 200 but "+iResponseCode+ " (1/2) and the following message:\n" + oConnection.getResponseMessage());
					ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
					InputStream oErrorStream = oConnection.getErrorStream();
					Util.copyStream(oErrorStream, oBytearrayOutputStream);
					String sMessage = oBytearrayOutputStream.toString();
					if(null!=sMessage) {
						sResponseExtract = sMessage.substring(0,  200) + "...";
						WasdiLog.debugLog("QueryExecutorLSA.httpPostResults: provider did not return 200 but "+iResponseCode+ " (2/2) and this is the content of the error stream:\n" + sResponseExtract);
						if(iResponseSize <= 0) {
							iResponseSize = sMessage.length();
						}						
					}
				}
			}catch (Exception oEint) {
				WasdiLog.debugLog("LSAHttpUtils.httpPostResults: Exception " + oEint);
			} finally {
				oConnection.disconnect();
			}
			
			long lEnd = System.nanoTime();
			long lTimeElapsed = lEnd - lStart;
			double dMillis = lTimeElapsed / (1000.0 * 1000.0);
			double dSpeed = 0;
			if(iResponseSize > 0) {
				dSpeed = ( (double) iResponseSize ) / dMillis;
				dSpeed *= 1000.0;
			}
			WasdiLog.debugLog("LSAHttpUtils.httpPostResults( " + sUrl + ") performance: " + dMillis + " ms, " + iResponseSize + " B (" + dSpeed + " B/s)");
		}
		catch (Exception oE) {
			WasdiLog.debugLog("LSAHttpUtils.httpPostResults: Exception " + oE);
		}
		return sResult;
	}
	
	public static void logout() {
    	try {
    		
    		WasdiLog.debugLog("LSAHttpUtils.logout ");
    		
    		// Create the cookie manager
    		CookieManager oCookieManager = (CookieManager) CookieHandler.getDefault();
    		
    		// Logout 
    		LSAHttpUtils.httpGetResults(s_sLogoutUrl, oCookieManager);
    		//String sLogoutPage = httpGetResults(s_sLogoutUrl, oCookieManager);
    		//WasdiLog.debugLog("LSAProviderAdapter.logout result: " + sLogoutPage);
    	}
    	catch (Exception oEx) {
			WasdiLog.debugLog("LSAHttpUtils.logout: Exception " + oEx.toString());
		}		
	}
	
}
