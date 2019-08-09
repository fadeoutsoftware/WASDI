/**
 * Created by Cristiano Nattero on 2019-03-06
 * 
 * Fadeout software
 *
 */
package it.fadeout.rest.resources.wps;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.io.IOUtils;

import it.fadeout.Wasdi;
import wasdi.shared.business.WpsProvider;
import wasdi.shared.data.WpsProvidersRepository;
import wasdi.shared.utils.AuthenticationCredentials;
import wasdi.shared.utils.Utils;

/**
 * @author c.nattero
 *
 */
public class WpsProxy {

	protected String m_sProviderUrl;
	protected String m_sProviderName;
	
	protected AuthenticationCredentials m_oCredentials;

	public WpsProxy() {
		
	}


	public Response get(String sParam, HttpHeaders oHeaders) {
		Wasdi.debugLog("WpsProxy.get");
		updateProviderUrlAsNeeded();
		authenticateAsNeeded();
		try {
			String sUrl = m_sProviderUrl;
			if(!Utils.isNullOrEmpty(sParam)) {
				if(!sParam.startsWith("?")) {
					sParam = "?"+sParam;
				}
				sUrl +=sParam;
			}
			URL oUrl = new URL(sUrl);
			
			HttpURLConnection oHttpURLConnection = (HttpURLConnection) oUrl.openConnection();
			oHttpURLConnection.setRequestMethod("GET");
			MultivaluedMap<String, String> oHeadersMap = oHeaders.getRequestHeaders();
			Set<String> asKeys = oHeadersMap.keySet();
			for (String sKey : asKeys) {
				List<String> asValues = oHeadersMap.get(sKey);
				for (String sValue : asValues) {
					oHttpURLConnection.setRequestProperty(sKey, sValue);
				}
			}
			//TODO get rid of this if already set somehow else
			oHttpURLConnection.setRequestProperty("Content-Type", "application/xml");
			
			//TODO adopt basic auth, see ProviderAdapter.downloadViaHttp

			oHttpURLConnection.setDoOutput(true);
			oHttpURLConnection.setInstanceFollowRedirects(true);
			return performAndHandleResult(oHttpURLConnection);


		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	//TODO refactor with a method to get and post in order to get rid of redundancies
	public Response post(String sParam, HttpHeaders oHeaders, String sPayload) {
		Wasdi.debugLog("WpsProxy.post");
		updateProviderUrlAsNeeded();
		authenticateAsNeeded();
		Wasdi.debugLog("WpsProxy.get");
		if(Utils.isNullOrEmpty(m_sProviderUrl)) {
			throw new NullPointerException("WpsProxy.get: provider not initialized");
		}
		try {
			String sUrl = m_sProviderUrl;
			if(!Utils.isNullOrEmpty(sParam)) {
				if(!sParam.startsWith("?")) {
					sParam = "?"+sParam;
				}
				sUrl +=sParam;
			}
			URL oUrl = new URL(sUrl);
					
			
			HttpURLConnection oHttpURLConnection = (HttpURLConnection) oUrl.openConnection();			
			oHttpURLConnection.setRequestMethod("POST");
			MultivaluedMap<String, String> oHeadersMap = oHeaders.getRequestHeaders();
			Set<String> asKeys = oHeadersMap.keySet();
			for (String sKey : asKeys) {
				List<String> asValues = oHeadersMap.get(sKey);
				for (String sValue : asValues) {
					oHttpURLConnection.setRequestProperty(sKey, sValue);
				}
			}

			oHttpURLConnection.setDoOutput(true);
			oHttpURLConnection.setDoInput(true);
			oHttpURLConnection.setInstanceFollowRedirects(true);
			
//			//MAYBE improve this implementation which is inefficient, because it stores the entire payload before performing the request
			DataOutputStream oOutputStream = new DataOutputStream(oHttpURLConnection.getOutputStream());
			oOutputStream.writeBytes(sPayload);
			oOutputStream.flush();
			oOutputStream.close();
			
			return performAndHandleResult(oHttpURLConnection);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void setProviderUrl(String sUrl) {
		Wasdi.debugLog("WpsProxy.setProviderUrl");
		if(null == sUrl) {
			throw new NullPointerException("WpsProxy.setProviderUrl: null string passed");
		}
		if(!Utils.isPlausibleHttpUrl(sUrl)) {
			throw new IllegalArgumentException("WpsProxy.setProviderUrl: the provided provider URL is not plausible");
		}
		m_sProviderUrl = sUrl;
	}

	//TODO refactor with a common method to get rid of redundancies
	protected Response performAndHandleResult(HttpURLConnection oHttpURLConnection) {
		if(null==oHttpURLConnection) {
			throw new NullPointerException("WpsProxy.performAndHandleResult: null HttpURLConnection passed");
		}
		try {
			int iStatus = oHttpURLConnection.getResponseCode();
			if(200 <= iStatus && iStatus < 300 ) {
				//String sBody = getFullResponse(oHttpURLConnection);
				//ResponseBuilder oBuilder = Response.ok(sBody);
				ResponseBuilder oBuilder = Response.ok(oHttpURLConnection.getInputStream());
				if(200!=iStatus) {
					oBuilder.status(iStatus);
				}
				//oBuilder = addHeaders(oBuilder, oHttpURLConnection.getHeaderFields());
				return oBuilder.build();
			} else if(300 <= iStatus && iStatus < 400) {
				//TODO follow redirect
				//should be managed automatically
			} else if(400 <= iStatus && iStatus < 500 ) {
				ResponseBuilder oBuilder = Response.status(iStatus);
//				StringWriter oInputWriter = new StringWriter();
//				IOUtils.copy(oHttpURLConnection.getInputStream(), oInputWriter, "UTF-8");
//				String sPayload = oInputWriter.toString();
//				oInputWriter.flush();
//				oInputWriter.close();
				StringWriter oErrorWriter = new StringWriter();
				String sPayload = oErrorWriter.toString();
				oErrorWriter.flush();
				oErrorWriter.close();
				IOUtils.copy(oHttpURLConnection.getErrorStream(), oErrorWriter, "UTF-8");
				oBuilder.entity(sPayload);
				oBuilder = addHeaders(oBuilder, oHttpURLConnection.getHeaderFields());
				return oBuilder.build();
			} else if(500 <= iStatus) {
				//MAYBE try n (parameter) more times, then give up
				ResponseBuilder oBuilder = Response.status(iStatus);
//				StringWriter oInputWriter = new StringWriter();
//				IOUtils.copy(oHttpURLConnection.getInputStream(), oInputWriter, "UTF-8");
//				String sPayload = oInputWriter.toString();
//				oInputWriter.flush();
//				oInputWriter.close();
				StringWriter oErrorWriter = new StringWriter();
				String sPayload = oErrorWriter.toString();
				oErrorWriter.flush();
				oErrorWriter.close();
				IOUtils.copy(oHttpURLConnection.getErrorStream(), oErrorWriter, "UTF-8");
				oBuilder.entity(sPayload);
				oBuilder = addHeaders(oBuilder, oHttpURLConnection.getHeaderFields());
				return oBuilder.build();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	protected static String getFullResponse(HttpURLConnection oHttpURLConnection) {
		if(null==oHttpURLConnection) {
			throw new NullPointerException("WpsProxy.getFullResponse: null HttpURLConnection passed");
		}
		StringBuilder oFullResponseBuilder = new StringBuilder();
		try {
			BufferedReader oBufferedReader = new BufferedReader(
					new InputStreamReader(oHttpURLConnection.getInputStream()));
			String sInputLine = null;
			StringBuffer oStringBuffer = new StringBuffer();
			while ((sInputLine = oBufferedReader.readLine()) != null) {
				oStringBuffer.append(sInputLine);
			}
			oBufferedReader.close();
			oFullResponseBuilder.append(oStringBuffer);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return oFullResponseBuilder.toString();
	}

	protected ResponseBuilder addHeaders(ResponseBuilder oBuilder, Map<String, List<String>> aoHeaders) {
		if(null == oBuilder) {
			throw new NullPointerException("WpsProxy.addHeaders: null ResponseBuilder");
		}
		if(null == aoHeaders) {
			return oBuilder;
		}

		Set<String> asKeys = aoHeaders.keySet();
		if(null == asKeys) {
			return oBuilder;
		}
		for (String sKey : asKeys) {
			if(null!=sKey) {
				List<String> asValues = aoHeaders.get(sKey);
				if(null!=asValues) {
					for (String sValue : asValues) {
						if(null!=sValue) {
							oBuilder.header(sKey, sValue);
						}
					}
				}
			}
		}
		return oBuilder;

	}

	public void setCredentials(AuthenticationCredentials oCredentials) {
		if(null == oCredentials) {
			throw new NullPointerException("WpsProxy.setCredentials: null AuthenticationCredentials passed");
		}
		m_oCredentials = new AuthenticationCredentials(oCredentials.getUser(), oCredentials.getPassword());
	}
	
	protected void setAuthenticator() {
		Wasdi.debugLog("WpsProxy.authenticate");
		updateProviderUrlAsNeeded();
	}
	
	protected void authenticateAsNeeded() {
		Wasdi.debugLog("WpsProxy.authenticateAsNeeded");
	}

	public void setProviderName(String sWpsProvider) {
		if(null == sWpsProvider) {
			throw new NullPointerException("WpsProxy.setProviderName: passed a null String");
		}
		m_sProviderName = sWpsProvider;
	}

	protected void updateProviderUrl() {
		WpsProvidersRepository oRepo = new WpsProvidersRepository();
		String sUrl = oRepo.getProviderUrl(m_sProviderName);
		if(null!=sUrl) {
			m_sProviderUrl = sUrl;
		}
		if(null!=oRepo.getCredentials(m_sProviderName)) {
			setCredentials(oRepo.getCredentials(m_sProviderName));
		}
	}
	
	protected void updateProviderUrlAsNeeded() {
		if(null==m_sProviderUrl) {
			updateProviderUrl();
		}
	}
	
}
