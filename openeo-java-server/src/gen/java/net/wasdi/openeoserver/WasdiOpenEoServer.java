package net.wasdi.openeoserver;

import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;

import org.glassfish.jersey.server.ResourceConfig;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import net.wasdi.openeoserver.providers.JacksonJsonProvider;
import net.wasdi.openeoserver.services.KeycloakService;
import wasdi.shared.business.PasswordAuthentication;
import wasdi.shared.business.users.*;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.SessionRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;

public class WasdiOpenEoServer extends ResourceConfig {
	
	/**
	 * Servlet Config to access web.xml file
	 */
	@Context
	ServletConfig m_oServletConfig;
	
	/**
	 * Code of the actual node
	 */
	public static String s_sMyNodeCode = "wasdi";
	
	/**
	 * Name of the Node-Specific Workpsace
	 */
	public static String s_sLocalWorkspaceName = "wasdi_specific_node_ws_code";
	
	
	public static String s_sKeyCloakIntrospectionUrl = "";
	public static String s_sClientId = "";
	public static String s_sClientSecret = "";
	public static String s_KeyCloakUser = "";
	public static String s_KeyCloakPw = "";
	public static String s_KeyBearerSecret = "";	
	
	public WasdiOpenEoServer()  {
		//register(JacksonFeature.class);
		register(JacksonJsonProvider.class);
		packages(true, "it.fadeout.rest.resources");
	}
	
	@PostConstruct
	public void initWasdiOpenEoServer() {
		WasdiLog.debugLog("----------- Welcome to WASDI OPEN EO SERVER - The WASDI OpenEo Backend");
		
		String sConfigFilePath = "/etc/wasdi/wasdiConfig.json";
		
		if (Utils.isNullOrEmpty(m_oServletConfig.getInitParameter("ConfigFilePath")) == false){
			sConfigFilePath = m_oServletConfig.getInitParameter("ConfigFilePath");
		}
		
		if (!WasdiConfig.readConfig(sConfigFilePath)) {
			WasdiLog.debugLog("ERROR IMPOSSIBLE TO READ CONFIG FILE IN " + sConfigFilePath);
		}
				
		// Read MongoDb Configuration
		try {
            MongoRepository.readConfig();
			WasdiLog.debugLog("-------Mongo db User " + MongoRepository.DB_USER);
		} catch (Throwable oEx) {
			WasdiLog.errorLog("Read MongoDb Configuration exception " + oEx.toString());
		}
		
		try {
			ch.qos.logback.classic.Logger oMongoLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.mongodb.driver.cluster");
			oMongoLogger.setLevel(ch.qos.logback.classic.Level.WARN);	
			
			oMongoLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("org.mongodb.driver");
			oMongoLogger.setLevel(ch.qos.logback.classic.Level.WARN);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("Disabling mongo driver logging exception " + oEx.toString());
		}
		
		
		// Read the code of this Node
		try {

			s_sMyNodeCode = WasdiConfig.Current.nodeCode;
			WasdiLog.debugLog("-------Node Code " + s_sMyNodeCode);

		} catch (Throwable oEx) {
			WasdiLog.errorLog("Read the code of this Node exception " + oEx.toString());
		}
		
		// Read the configuration of KeyCloak		
		s_sKeyCloakIntrospectionUrl = WasdiConfig.Current.keycloack.introspectAddress;
		s_sClientId = WasdiConfig.Current.keycloack.confidentialClient;
		s_sClientSecret = WasdiConfig.Current.keycloack.clientSecret;
		
		// Computational nodes need to configure also the local dababase
		try {
			// If this is not the main node
			if (!s_sMyNodeCode.equals("wasdi")) {
				
				// Configure also the local connection: by default is the "wasdi" port + 1
				MongoRepository.addMongoConnection("local", WasdiConfig.Current.mongoLocal.user, WasdiConfig.Current.mongoLocal.password, WasdiConfig.Current.mongoLocal.address, WasdiConfig.Current.mongoLocal.replicaName, WasdiConfig.Current.mongoLocal.dbName);
				WasdiLog.debugLog("-------Addded Mongo Configuration local for " + s_sMyNodeCode);
			}			
		}
		catch (Throwable oEx) {
			WasdiLog.errorLog("Local Database config exception " + oEx.toString());
		}		
		
		WasdiLog.debugLog("-------- WASDI Open EO Server Init done -------\n\n");
		WasdiLog.debugLog("-----------------------------------------------");
		WasdiLog.debugLog("--------        Welcome to space        -------");
		WasdiLog.debugLog("-----------------------------------------------\n\n");
		
	}
	
	/**
	 * Server Shut down procedure
	 */
	public static void shutDown() {
		try {
			WasdiLog.debugLog("-------Shutting Down WasdiOpenEoServer");
			
			// Stop mongo
			try {
				MongoRepository.shutDownConnection();
			} catch (Exception oE) {
				WasdiLog.debugLog("WasdiOpenEoServer.shutDown: could not shut down connection to DB: " + oE);
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("WasdiOpenEoServer SHUTDOWN EXCEPTION: " + oE);
		}
	}
	
	/**
	 * Get the wasdi session id from the basic authentication http header in the form: user:sessionId
	 * @param sAuthorization
	 * @return
	 */
	public static String [] decodeBasicAuthentication(String sAuthorization) {
		
		try {
			if (Utils.isNullOrEmpty(sAuthorization)) return null;
			sAuthorization = sAuthorization.replace("Basic ", "");
			byte[] ayDecodedBytes = Base64.getDecoder().decode(sAuthorization);
			
			if (ayDecodedBytes==null) return null;
			
			String sDecodedString = new String(ayDecodedBytes);
			String [] asParts = sDecodedString.split(":");
			return asParts;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("WasdiOpenEoServer.decodeBasicAuthentication: something bad happened: " + oEx);
		}
		
		return null;
	}
	
	/**
	 * Get the WASDI Session from a Basic Authentication Header userid:session
	 * @param sAuthorization
	 * @return
	 */
	public static UserSession getLoginFromBasicAuth(String sAuthorization) {
		
		User oUser = null;
		
		try {
			
			String [] asUserPw = decodeBasicAuthentication(sAuthorization);
			
			if (asUserPw == null) return null;
			
			// Check The Session with Keycloak
			String sUserId = asUserPw[0];
			String sPwd = asUserPw[1];
			
			// Check if the user exists
			UserRepository oUserRepository = new UserRepository();
			oUser = oUserRepository.getUser(sUserId);
			
			if (oUser == null) return null;
			
			KeycloakService oKeycloakService = new KeycloakService();
			
			// First try to Authenticate using keycloak
			String sAuthResult = oKeycloakService.login(sUserId, sPwd);
			
			boolean bLoginSuccess = false;

			if(!Utils.isNullOrEmpty(sAuthResult)) { 
				bLoginSuccess = true;
				
				try {
					JSONObject oAuthResponse = new JSONObject(sAuthResult);
					
					String sRefreshToken = oAuthResponse.optString("refresh_token", null);
					
					oKeycloakService.logout(sRefreshToken);
				} catch (Exception oE) {
					WasdiLog.errorLog("WasdiOpenEoServer.login: could not parse response due to " + oE + ", aborting");
				}
				
				
			} else {
				PasswordAuthentication oPasswordAuthentication = new PasswordAuthentication();
				// Try to log in with the WASDI old password
				bLoginSuccess = oPasswordAuthentication.authenticate(sPwd.toCharArray(), oUser.getPassword() );
			}
			
			
			if(bLoginSuccess) {
				// If the user is logged, update last login
				oUser.setLastLogin((new Date()).toString());
				oUserRepository.updateUser(oUser);

				
				//Clear all old, expired sessions
				//Wasdi.clearUserExpiredSessions(oUser);
				
				// Create a new session
				SessionRepository oSessionRepository = new SessionRepository();
				UserSession oSession = oSessionRepository.insertUniqueSession(oUser.getUserId());
				
				if(null==oSession || Utils.isNullOrEmpty(oSession.getSessionId())) {
					WasdiLog.debugLog("WasdiOpenEoServer.login: could not insert session in DB, aborting");
					return null;
				}
				
				return oSession;
				
			} else {
				WasdiLog.debugLog("WasdiOpenEoServer.login: access failed");
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("WasdiOpenEoServer.login: something bad happened: " + oE);
		}

		return null;
	}
	
	/**
	 * Get the User object from the session Id
	 * It checks first in Key Cloak and later on the local session mechanism.
	 * @param sSessionId
	 * @return
	 */
	public static User getUserFromAuthenticationHeader(String sAuthentication) {
		
		User oUser = null;
		
		if (Utils.isNullOrEmpty(sAuthentication)) return null;
		
		try {			
			// Check The Session with Keycloak
			String sUserId = null;
			
			String sSessionId = sAuthentication.substring("Bearer basic//".length());
			
			try  {
				//introspect
				String sPayload = "token=" + sSessionId;
				Map<String,String> asHeaders = new HashMap<String,String>();
				asHeaders.put("Content-Type", "application/x-www-form-urlencoded");
				HttpCallResponse oHttpCallResponse = HttpUtils.httpPost(WasdiConfig.Current.keycloack.introspectAddress, sPayload, asHeaders, s_sClientId + ":" + s_sClientSecret); 
				String sResponse = oHttpCallResponse.getResponseBody();
				JSONObject oJSON = null;
				if(!Utils.isNullOrEmpty(sResponse)) {
					oJSON = new JSONObject(sResponse);
				}
				if(null!=oJSON) {
					sUserId = oJSON.optString("preferred_username", null);
				}				
			}
			catch (Exception oKeyEx) {
				WasdiLog.errorLog("WAsdi.getUserFromSession: exception contacting keycloak: " + oKeyEx.toString());
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
			WasdiLog.errorLog("WAsdi.getUserFromSession: something bad happened: " + oE);
		}

		return oUser;
	}	
		
}
