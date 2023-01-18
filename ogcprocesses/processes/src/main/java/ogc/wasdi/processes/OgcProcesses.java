package ogc.wasdi.processes;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;

import org.glassfish.jersey.server.ResourceConfig;
import org.json.JSONObject;

import wasdi.shared.business.User;
import wasdi.shared.business.UserSession;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.SessionRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class OgcProcesses extends ResourceConfig {
	
	
	/**
	 * Servlet Config to access web.xml file
	 */
	@Context
	ServletConfig m_oServletConfig;	
	
	public static String s_sBaseAddress = "";
	
	public OgcProcesses() {
		packages(true, "ogc.wasdi.processes.rest.resources");
	}

	/**
	 * Web Server intialization: it loads the main web-server configuration
	 */
	@PostConstruct
	public void initOgcProcesses() {
		WasdiLog.debugLog("WASDI OGC-Processes Server start");
		
		String sConfigFilePath = "/data/wasdi/wasdiConfig.json"; 
		
		if (Utils.isNullOrEmpty(m_oServletConfig.getInitParameter("ConfigFilePath")) == false){
			sConfigFilePath = m_oServletConfig.getInitParameter("ConfigFilePath");
		}
		
		if (!WasdiConfig.readConfig(sConfigFilePath)) {
			WasdiLog.errorLog("ERROR IMPOSSIBLE TO READ CONFIG FILE IN " + sConfigFilePath);
		}
		
		OgcProcesses.s_sBaseAddress = WasdiConfig.Current.ogcpProcessesApi.baseAddress;
		
		if (!OgcProcesses.s_sBaseAddress.endsWith("/")) OgcProcesses.s_sBaseAddress += "/";
		
		// Read MongoDb Configuration
		try {

            MongoRepository.readConfig();

			WasdiLog.debugLog("-------Mongo db User " + MongoRepository.DB_USER);

		} catch (Throwable e) {
			e.printStackTrace();
		}		
	}
	
	/**
	 * Get the User object from the session Id
	 * It checks first in Key Cloak and later on the local session mechanism.
	 * @param sSessionId
	 * @return
	 */
	public static User getUserFromSession(String sSessionId) {
		
		User oUser = null;
		
		try {			
			// Check The Session with Keycloak
			String sUserId = null;
			
			try  {
				//introspect
				String sPayload = "token=" + sSessionId;
				Map<String,String> asHeaders = new HashMap<>();
				asHeaders.put("Content-Type", "application/x-www-form-urlencoded");
				
				String sResponse = HttpUtils.httpPost(WasdiConfig.Current.keycloack.introspectAddress, sPayload, asHeaders, WasdiConfig.Current.keycloack.client + ":" + WasdiConfig.Current.keycloack.clientSecret);
				JSONObject oJSON = null;
				if(!Utils.isNullOrEmpty(sResponse)) {
					oJSON = new JSONObject(sResponse);
				}
				if(null!=oJSON) {
					sUserId = oJSON.optString("preferred_username", null);
				}				
			}
			catch (Exception oKeyEx) {
				WasdiLog.debugLog("OgcProcesses.getUserFromSession: exception contacting keycloak: " + oKeyEx.toString());
			}


			if(!Utils.isNullOrEmpty(sUserId)) {
				UserRepository oUserRepo = new UserRepository();
				oUser = oUserRepo.getUser(sUserId);
			} else {
				//check session against DB
				
				SessionRepository oSessionRepository = new SessionRepository();
				UserSession oUserSession = oSessionRepository.getSession(sSessionId);
				
				if(null==oUserSession) {
					return null;
				} else {
					sUserId = oUserSession.getUserId();
				}
				if(!Utils.isNullOrEmpty(sUserId)){
					UserRepository oUserRepository = new UserRepository();
					oUser = oUserRepository.getUser(sUserId);
				} else {
					return null;
				}
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("OgcProcesses.getUserFromSession: something bad happened: " + oE);
		}

		return oUser;
	}
	
	/**
	 * Get the wasdi session id from the basic authentication http header in the form: user:sessionId
	 * @param sAuthorization
	 * @return
	 */
	public static String getSessionIdFromBasicAuthentication(String sAuthorization) {
		try {
			sAuthorization = sAuthorization.replace("Basic ", "");
			byte[] ayDecodedBytes = Base64.getDecoder().decode(sAuthorization);
			String sDecodedString = new String(ayDecodedBytes);
			String [] asParts = sDecodedString.split(":");
			return asParts[1];
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("OgcProcesses.getSessionIdFromBasicAuthentication: something bad happened: " + oEx);
		}
		
		return "";
	}
}
