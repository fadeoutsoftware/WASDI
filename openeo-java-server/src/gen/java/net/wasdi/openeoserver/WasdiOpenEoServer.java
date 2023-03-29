package net.wasdi.openeoserver;

import java.util.Base64;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;

import org.glassfish.jersey.server.ResourceConfig;
import org.json.JSONObject;

import net.wasdi.openeoserver.providers.JacksonJsonProvider;
import net.wasdi.openeoserver.services.KeycloakService;
import wasdi.shared.business.PasswordAuthentication;
import wasdi.shared.business.User;
import wasdi.shared.business.UserSession;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.SessionRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

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
		
		String sConfigFilePath = "/data/wasdi/wasdiConfig.json"; 
		
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

		MongoRepository.addMongoConnection("ecostress", WasdiConfig.Current.mongoEcostress.user, WasdiConfig.Current.mongoEcostress.password, WasdiConfig.Current.mongoEcostress.address, WasdiConfig.Current.mongoEcostress.replicaName, WasdiConfig.Current.mongoEcostress.dbName);
		
		
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
	
	public static UserSession getSessionFromBasicAuth(String sAuthorization) {
		
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
		
}
