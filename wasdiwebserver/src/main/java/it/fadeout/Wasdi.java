package it.fadeout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;

import org.apache.commons.net.io.Util;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.json.JSONObject;

import it.fadeout.providers.JerseyMapperProvider;
import it.fadeout.rest.resources.AuthResource;
import it.fadeout.rest.resources.ProcessWorkspaceResource;
import wasdi.shared.business.Node;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.Workspace;
import wasdi.shared.business.users.User;
import wasdi.shared.business.users.UserApplicationRole;
import wasdi.shared.business.users.UserResourcePermission;
import wasdi.shared.business.users.UserSession;
import wasdi.shared.business.users.UserType;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.MetricsEntryRepository;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.NodeRepository;
import wasdi.shared.data.ParametersRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.SessionRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.data.UserResourcePermissionRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.rabbit.RabbitFactory;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.LauncherOperationsUtils;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.wasdiAPI.ProcessingAPIClient;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.monitoring.Disk;
import wasdi.shared.viewmodels.monitoring.License;
import wasdi.shared.viewmodels.monitoring.Memory;
import wasdi.shared.viewmodels.monitoring.MetricsEntry;
import wasdi.shared.viewmodels.monitoring.Timestamp;
import wasdi.shared.viewmodels.processworkspace.NodeScoreByProcessWorkspaceViewModel;
import wasdi.shared.viewmodels.processworkspace.ProcessWorkspaceAggregatedViewModel;
import wasdi.shared.viewmodels.users.RegistrationInfoViewModel;

/**
 * Main Class of the WASDI Web Server.
 * Initialises the system
 * Contains different Utility functions to make from this server http calls to the other servers
 * 
 * @author p.campanella
 *
 */
public class Wasdi extends ResourceConfig {
	
	/**
	 * Static name of the system property that hosts to the nfs (satellite path finder) download folder 
	 */
	private static final String s_SNFS_DATA_DOWNLOAD = "nfs.data.download";
		
	/**
	 * Servlet Config to access web.xml file
	 */
	@Context
	ServletConfig m_oServletConfig;

	/**
	 * User for debug mode auto login
	 */
	public static String s_sDebugUser = "user";

	/**
	 * Password for debug mode auto login
	 */
	public static String s_sDebugPassword = "password";
		
	/**
	 * Name of the Node-Specific Workspace
	 */
	public static String s_sLocalWorkspaceName = "wasdi_specific_node_ws_code";
	
	/**
	 * Actual node Object
	 */
	public static Node s_oMyNode = null;
	
	public static String s_sKeyCloakIntrospectionUrl = "";
	public static String s_sClientId = "";
	public static String s_sClientSecret = "";
	public static String s_KeyCloakUser = "";
	public static String s_KeyCloakPw = "";
	public static String s_KeyBearerSecret = "";
	
	/**
	 * Constructor: bind the classes and the resources classes
	 */
	public Wasdi() {
		register(new WasdiBinder());
		register(JacksonFeature.class);
		register(JerseyMapperProvider.class);
		packages(true, "it.fadeout.rest.resources");
	}

	/**
	 * Web Server initialisation: it loads the main web-server configuration
	 */
	@PostConstruct
	public void initWasdi() {

		WasdiLog.infoLog("----------- Welcome to WASDI - Web Advanced Space Developer Interface");

		String sConfigFilePath = "/etc/wasdi/wasdiConfig.json";

		if (Utils.isNullOrEmpty(m_oServletConfig.getInitParameter("ConfigFilePath")) == false){
			sConfigFilePath = m_oServletConfig.getInitParameter("ConfigFilePath");
		}
		
		if (!WasdiConfig.readConfig(sConfigFilePath)) {
			WasdiLog.warnLog("Wasdi.initWasdi: ERROR IMPOSSIBLE TO READ CONFIG FILE IN " + sConfigFilePath);
		}
		
		WasdiLog.initLogger(WasdiConfig.Current.logLevelServer);
		
		// set nfs properties download folder
		String sUserHome = System.getProperty("user.home");
		String sNfsFolder = System.getProperty(Wasdi.s_SNFS_DATA_DOWNLOAD);
		if (sNfsFolder == null)
			System.setProperty(Wasdi.s_SNFS_DATA_DOWNLOAD, sUserHome + "/nfs/download");

		WasdiLog.debugLog("------- NFS dir " + System.getProperty(Wasdi.s_SNFS_DATA_DOWNLOAD));
		
		// Read MongoDb Configuration
		try {

            MongoRepository.readConfig();

			WasdiLog.debugLog("------- Mongo db User " + MongoRepository.DB_USER);

		} catch (Throwable oEx) {
			WasdiLog.errorLog("Wasdi.initWasdi: Read MongoDb Configuration exception " + oEx.toString());
		}
		
		// Read the code of this Node
		try {
			WasdiLog.infoLog("------- Node Code " + WasdiConfig.Current.nodeCode);

		} catch (Throwable oEx) {
			WasdiLog.errorLog("Wasdi.initWasdi: Read the code of this Node exception " + oEx.toString());
		}
		
		// Read the configuration of KeyCloak		
		s_sKeyCloakIntrospectionUrl = WasdiConfig.Current.keycloack.introspectAddress;
		s_sClientId = WasdiConfig.Current.keycloack.confidentialClient;
		s_sClientSecret = WasdiConfig.Current.keycloack.clientSecret;
		
		// Computational nodes need to configure also the local dababase
		try {
			// If this is not the main node
			if (!WasdiConfig.Current.isMainNode()) {
				
				// Configure also the local connection: by default is the "wasdi" port + 1
				MongoRepository.addMongoConnection("local", WasdiConfig.Current.mongoLocal.user, WasdiConfig.Current.mongoLocal.password, WasdiConfig.Current.mongoLocal.address, WasdiConfig.Current.mongoLocal.replicaName, WasdiConfig.Current.mongoLocal.dbName);
				WasdiLog.debugLog("------- Addded Mongo Configuration local for " + WasdiConfig.Current.nodeCode);
			}			
		}
		catch (Throwable oEx) {
			WasdiLog.errorLog("Wasdi.initWasdi: Local Database config exception " + oEx.toString());
		}
		
		if (WasdiConfig.Current.mongoEcostress!=null) {
			MongoRepository.addMongoConnection("ecostress", WasdiConfig.Current.mongoEcostress.user, WasdiConfig.Current.mongoEcostress.password, WasdiConfig.Current.mongoEcostress.address, WasdiConfig.Current.mongoEcostress.replicaName, WasdiConfig.Current.mongoEcostress.dbName);	
		}
		
		
		// Configure Rabbit
		try {
			RabbitFactory.readConfig();
			
			WasdiLog.infoLog("------- Rabbit Initialized ");
		} catch (Throwable oEx) {
			WasdiLog.errorLog("Wasdi.initWasdi: Configure Rabbit exception " + oEx.toString());
		}
		
		// Each node must have a special workspace for depoy operations: here it check if exists: if not it will be created
		WasdiLog.debugLog("------- Initializing node local workspace...");
		
		try {
			// Check if it exists
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			Workspace oWorkspace = oWorkspaceRepository.getByNameAndNode(Wasdi.s_sLocalWorkspaceName, WasdiConfig.Current.nodeCode);
			
			if (oWorkspace == null) {
				
				// Create the working Workspace in this node
				WasdiLog.debugLog("Wasdi.initWasdi: Local Workpsace Node " + Wasdi.s_sLocalWorkspaceName + " does not exist create it");
				
				oWorkspace = new Workspace();
				// Default values
				oWorkspace.setCreationDate(Utils.nowInMillis());
				oWorkspace.setLastEditDate(Utils.nowInMillis());
				oWorkspace.setName(Wasdi.s_sLocalWorkspaceName);
				// Leave this at "no user"
				oWorkspace.setWorkspaceId(Utils.getRandomName());
				oWorkspace.setNodeCode(WasdiConfig.Current.nodeCode);
				
				// Insert in the db
				oWorkspaceRepository.insertWorkspace(oWorkspace);
				
				WasdiLog.debugLog("Wasdi.initWasdi: Workspace " + Wasdi.s_sLocalWorkspaceName + " created");
			}
						
		}
		catch (Throwable oEx) {
			WasdiLog.errorLog("Wasdi.initWasdi: Local Workspace Configuration Exception " + oEx.toString());
		}
		
		WasdiLog.infoLog("------- WASDI Init done\n\n");
		
		WasdiLog.infoLog("-----------------------------------------------");
		WasdiLog.infoLog("--------        Welcome to space        -------");
		WasdiLog.infoLog("-----------------------------------------------\n\n");
		
		try {
			// Can be useful for debug
			WasdiLog.debugLog("******************************Environment Vars*****************************"); 
			Map<String, String> aoEnviorntmentVars = System.getenv();
			for (String string : aoEnviorntmentVars.keySet()) { WasdiLog.debugLog(string + ": " + aoEnviorntmentVars.get(string)); }
			 			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("Environment Vars Exception " + oEx.toString());
		}
	}

	/**
	 * Server Shut down procedure
	 */
	public static void shutDown() {
		try {
			WasdiLog.debugLog("------- Shutting Down WASDI");
			
			// Stop mongo
			try {
				MongoRepository.shutDownConnection();
			} catch (Exception oE) {
				WasdiLog.debugLog("Wasdi.shutDown: could not shut down connection to DB: " + oE);
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("Wasdi.shutDown: WASDI SHUTDOWN EXCEPTION: " + oE);
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
		SessionRepository oSessionRepository = new SessionRepository();
		UserRepository oUserRepo = new UserRepository();
		
		try {			
			// Check The Session with Keycloak
			String sUserId = null;
			
			try  {
				// Try to Introspect the token
				String sPayload = "token=" + sSessionId;
				
				// Add headers
				Map<String,String> asHeaders = new HashMap<>();
				asHeaders.put("Content-Type", "application/x-www-form-urlencoded");
				
				// Post to keycloak server
				HttpCallResponse oHttpCallResponse = HttpUtils.httpPost(Wasdi.s_sKeyCloakIntrospectionUrl, sPayload, asHeaders, s_sClientId + ":" + s_sClientSecret); 
				String sResponse = oHttpCallResponse.getResponseBody();
				
				// Try to read the result
				JSONObject oJSON = null;
				
				if(!Utils.isNullOrEmpty(sResponse)) {
					oJSON = new JSONObject(sResponse);
					
					if(oJSON != null) {
						sUserId = oJSON.optString("preferred_username", null);
					}					
				}
				
			}
			catch (Exception oKeyEx) {
				WasdiLog.errorLog("Wasdi.getUserFromSession: exception contacting keycloak: " + oKeyEx.toString());
			}

			if(!Utils.isNullOrEmpty(sUserId)) {
				// Get the user from the repo
				oUser = oUserRepo.getUser(sUserId);
				
				if( oUser == null ) {
					// Probably we need to add the user to the db!
					WasdiLog.warnLog("Wasdi.getUserFromSession: the session is valid but the user does not exists, add it");
					
					AuthResource oAuthResource = new AuthResource();
					
					RegistrationInfoViewModel oRegistrationInfoViewModel = new RegistrationInfoViewModel();
					oRegistrationInfoViewModel.setUserId(sUserId);
					PrimitiveResult oRegistrationResult = oAuthResource.userRegistration(oRegistrationInfoViewModel);

					if (oRegistrationResult==null) {
						WasdiLog.warnLog("Wasdi.getUserFromSession: we had a problem registering the user, return invalid");
					} 
					else {
					
						if (oRegistrationResult.getBoolValue()==null) {
							WasdiLog.warnLog("Wasdi.getUserFromSession: we had a problem registering the user, return invalid");
						}
	
						if (oRegistrationResult.getBoolValue()==false) {
							WasdiLog.warnLog("Wasdi.getUserFromSession: we had a problem registering the user, return invalid");
						}
					}
					
					// Re-read the user: if it has been registered, oUser will be valid, if not still null
					oUser = oUserRepo.getUser(sUserId);
				}
			} 
			else {
				
				// Session not found in keyclock: check session against DB
				UserSession oUserSession = oSessionRepository.getSession(sSessionId);
				
				// We really do not have this session
				if(oUserSession == null) {
					return null;
				} 
								
				// Ok but the session itself, is expired or not?
				if (oSessionRepository.isNotExpiredSession(oUserSession)==false) {
					// Is expired. It has been deleted, and we return false
					return null;
				}
				
				// Ok we should have a user so
				sUserId = oUserSession.getUserId();				
				
				// Check if the user id is not null
				if(!Utils.isNullOrEmpty(sUserId)){
					
					// Try to read the user
					oUser = oUserRepo.getUser(sUserId);
					
					if (oUser!=null) {
						// If it is not null, we can touch the session
						safeTouchSession(oUserSession);
					}
					
				}
			}
		} catch (Exception oE) {
			WasdiLog.errorLog("Wasdi.getUserFromSession: something bad happened: ", oE);
		}

		return oUser;
	}
	

	/**
	 * Clear all the user expired sessions
	 * @param oUser
	 */
	public static void clearUserExpiredSessions(User oUser) {
		
		SessionRepository oSessionRepository = new SessionRepository();
		List<UserSession> aoEspiredSessions = oSessionRepository.getAllExpiredSessions(oUser.getUserId());
		
		for (UserSession oUserSession : aoEspiredSessions) {
			//delete data base sessions
			if (!oSessionRepository.deleteSession(oUserSession)) {

				WasdiLog.debugLog("AuthService.Login: Error deleting session.");
			}
		}
	}	
	
	/**
	 * Safe Touch Session: it really makes the touch only if the session is at least one minute old.
	 * 
	 * @param oSession Session to touch
	 * @return True if was more recent than one minute, or if was touched. False in case of error in the update of the session in the db
	 */
	public static boolean safeTouchSession(UserSession oSession) {
		double dNow = Utils.nowInMillis();
		double dSessionAge = dNow - oSession.getLastTouch();
		if (dSessionAge>60*1000) {
			SessionRepository oSessionRepository = new SessionRepository();
			return oSessionRepository.touchSession(oSession);
		}
		
		return true;		
	}

	/**
	 * Get The owner of a workspace starting from the workspace id
	 * 
	 * @param sWorkspaceId Id of the Workspace
	 * @return User Id of the owner
	 */
	public static String getWorkspaceOwner(String sWorkspaceId) {
		WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
		Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspaceId);
		if (oWorkspace == null)
			return "";
		String sWorkspaceOwner = oWorkspace.getUserId();
		return sWorkspaceOwner;
	}
	
	/**
	 * Run Process: trigger the execution of a WASDI Process. 
	 * If the workspace of the operation is the actual node this function
	 * creates the process workspace entry in the db that will be handled by the scheduler to run a process on a node.
	 * If the workspace it is in another node, it routes the request to the destination node calling 
	 * processing/run
	 * API
	 * This version assumes Sub Operation Id and Parent Id NULL
	 *  
	 * @param sUserId requesting the operation
	 * @param sSessionId User session
	 * @param sOperationType Id of the Launcher Operation
	 * @param sProductName Product name associated to the Process Workspace
	 * @param sSerializationPath Node Serialisation Path
	 * @param oParameter Parameter associated to the operation
	 * @return
	 * @throws IOException
	 */
	public static PrimitiveResult runProcess(String sUserId, String sSessionId, String sOperationType, String sProductName, BaseParameter oParameter) throws IOException {
		return runProcess(sUserId, sSessionId, sOperationType, sProductName, oParameter, null);
	}
	
	/**
	 * Run Process: trigger the execution of a WASDI Process. 
	 * If the workspace of the operation is the actual node this function
	 * creates the process workspace entry in the db that will be handled by the scheduler to run a process on a node.
	 * If the workspace it is in another node, it routes the request to the destination node calling 
	 * processing/run
	 * API	 
	 * This version assume Operation Sub Id = Null
	 *  
	 * @param sUserId User requesting the operation
	 * @param sSessionId User session
	 * @param sOperationType Id of the Launcher Operation
	 * @param sProductName Product name associated to the Process Workspace
	 * @param sSerializationPath Node Serialisation Path
	 * @param oParameter Parameter associated to the operation
	 * @param sParentId Id the the parent process
	 * @return Primitive Result with the output status of the operation
	 * @throws IOException
	 */
	public static PrimitiveResult runProcess(String sUserId, String sSessionId, String sOperationType, String sProductName, BaseParameter oParameter, String sParentId) throws IOException {
		return runProcess(sUserId, sSessionId, sOperationType, null, sProductName, oParameter, sParentId);
	}

	/**
	 * Run Process: trigger the execution of a WASDI Process. 
	 * If the workspace of the operation is the actual node this function
	 * creates the process workspace entry in the db that will be handled by the scheduler to run a process on a node.
	 * If the workspace it is in another node, it routes the request to the destination node calling 
	 * processing/run
	 * API
	 *
	 * @param sUserId User requesting the operation
	 * @param sSessionId User session
	 * @param sOperationType Id of the Launcher Operation
	 * @param sOperationSubId Sub Id of the Launcher Operation
	 * @param sProductName Product name associated to the Process Workspace
	 * @param sSerializationPath Node Serialisation Path
	 * @param oParameter Parameter associated to the operation
	 * @param sParentId Id the the parent process
	 * @return Primitive Result with the output status of the operation: boolValue = true, intValue = 200, stringValue = processId if the operation is fine. 
	 * 			otherwise boolValue = false, intValue = httpErrorCode.
	 * @throws IOException
	 */
	public static PrimitiveResult runProcess(String sUserId, String sSessionId, String sOperationType, String sOperationSubId, String sProductName, BaseParameter oParameter, String sParentId) throws IOException {

		if (!LauncherOperationsUtils.isValidLauncherOperation(sOperationType)) {
			// Bad request
			PrimitiveResult oResult = new PrimitiveResult();
			oResult.setIntValue(400);
			oResult.setBoolValue(false);
			return oResult;
		}
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setIntValue(500);

		//filter out invalid sessions
		User oUser = getUserFromSession(sSessionId);
		
		if(oUser == null) {
			WasdiLog.debugLog("Wasdi.runProcess( " + sUserId + ", " + sSessionId + ", " + sOperationType + ", " + sProductName + ", ... ): session not valid, aborting");
			oResult.setIntValue(401);
			oResult.setBoolValue(false);
			return oResult;
		}
		
		// Get the Ob Id
		String sProcessObjId = oParameter.getProcessObjId();

		try {
			
			// Take the Workspace
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			Workspace oWorkspace = oWorkspaceRepository.getWorkspace(oParameter.getWorkspace());
			
			if (oWorkspace == null) {
				WasdiLog.debugLog("Wasdi.runProcess: ws not found. Return 500");
				oResult.setBoolValue(false);
				oResult.setIntValue(500);
				return oResult;				
			}
			
			// Check my node name
			String sMyNodeCode = WasdiConfig.Current.nodeCode;
					
			// Is the workspace here?
			String sWsNodeCode = oWorkspace.getNodeCode();
			
			if (!sWsNodeCode.equals(sMyNodeCode)) {
				
				// No: forward the call on the owner node
				WasdiLog.debugLog("Wasdi.runProcess: forwarding request to [" + sWsNodeCode+"]");
				
				// Get the Node
				NodeRepository oNodeRepository = new NodeRepository();
				wasdi.shared.business.Node oDestinationNode = oNodeRepository.getNodeByCode(sWsNodeCode);
				
				if (oDestinationNode==null) {
					WasdiLog.debugLog("Wasdi.runProcess: Node [" + sWsNodeCode+"] not found. Return 500");
					oResult.setBoolValue(false);
					oResult.setIntValue(500);
					return oResult;									
				}
				
				WasdiLog.debugLog("Wasdi.runProcess: serializing parameter to XML");
				String sPayload = SerializationUtils.serializeObjectToStringXML(oParameter);

				HttpCallResponse oHttpCallResponse = ProcessingAPIClient.runProcess(oDestinationNode, sSessionId, sOperationType, sProductName, sParentId, sOperationSubId, sPayload);
				String sResult = oHttpCallResponse.getResponseBody();
				
		        try {
		        	// Get back the primitive result
		            PrimitiveResult oPrimitiveResult = MongoRepository.s_oMapper.readValue(sResult,PrimitiveResult.class);

		            return oPrimitiveResult;
		        } 
		        catch (Exception oEx) {
					WasdiLog.errorLog("Wasdi.runProcess: exception ", oEx);
					oResult.setBoolValue(false);
					oResult.setIntValue(500);
					return oResult;				
		        }
			}
			else {
				// The Workspace is here. Just add the Parameter and the ProcessWorkspace to the database 
				
				SessionRepository oSessionRepo = new SessionRepository();
				
				if (Utils.isNullOrEmpty(sParentId)) {
					
					//Create a WASDI session here
					UserSession oSession = new UserSession();
					oSession.setUserId(sUserId);
					
					sSessionId = UUID.randomUUID().toString();
					oSession.setSessionId(sSessionId);
					oSession.setLoginDate(Utils.nowInMillis());
					oSession.setLastTouch(Utils.nowInMillis());
					
					if (!oSessionRepo.insertSession(oSession)) {
						WasdiLog.warnLog("could not insert session " + oSession.getSessionId() + " in DB, try with the old one");						
					}
					else {
						// Assign this new session to the parameter
						oParameter.setSessionID(sSessionId);
					}
				}
				
				// Insert the parameter in mongo
				ParametersRepository oParametersRepository = new ParametersRepository();
				oParametersRepository.insertParameter(oParameter);

				// Create the process Workspace
				ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
				ProcessWorkspace oProcess = new ProcessWorkspace();

				try {
					
					oProcess.setOperationTimestamp(Utils.nowInMillis());
					oProcess.setOperationType(sOperationType);
					oProcess.setOperationSubType(sOperationSubId);
					oProcess.setProductName(sProductName);
					oProcess.setWorkspaceId(oParameter.getWorkspace());
					oProcess.setUserId(sUserId);
					oProcess.setNotifyOwnerByMail(oParameter.isNotifyOwnerByMail());

					//TODO - enforce the presence of a valid subscription and of an active project
					if (Utils.isNullOrEmpty(oUser.getActiveProjectId())
							|| Utils.isNullOrEmpty(oUser.getActiveSubscriptionId())) {
						WasdiLog.warnLog("Wasdi.runProcess: Process Scheduled for Launcher. The user does not have a valid subscription and an active project");
					}

					oProcess.setProjectId(oUser.getActiveProjectId());
					oProcess.setSubscriptionId(oUser.getActiveSubscriptionId());
					oProcess.setProcessObjId(sProcessObjId);
					if (!Utils.isNullOrEmpty(sParentId)) oProcess.setParentId(sParentId);
					oProcess.setStatus(ProcessStatus.CREATED.name());
					oProcess.setNodeCode(sWsNodeCode);
					oRepository.insertProcessWorkspace(oProcess);
					WasdiLog.debugLog("Wasdi.runProcess: Process Scheduled for Launcher");
				} 
				catch (Exception oEx) {
					WasdiLog.errorLog("Wasdi.runProcess: " + oEx);
					oResult.setBoolValue(false);
					oResult.setIntValue(500);
					return oResult;
				}				
			}
		} 
		catch (Exception oE) {
			WasdiLog.errorLog("Wasdi.runProcess: " + oE);
			oResult.setBoolValue(false);
			oResult.setIntValue(500);
			return oResult;
		}

		// Ok, operation triggered
		oResult.setBoolValue(true);
		oResult.setIntValue(200);
		oResult.setStringValue(sProcessObjId);

		return oResult;
	}
	
	/**
	 * Download a workflow on the local PC
	 * @param sWorkflowId File Name
	 * @return Full Path
	 */
	public static String downloadWorkflow(String sNodeUrl, String sWorkflowId, String sSessionId) {
		try {
			
			if (Utils.isNullOrEmpty(sWorkflowId)) {
				WasdiLog.debugLog("sWorkflowId is null or empty");
				return "";
			}
			
			String sBaseUrl = sNodeUrl;
			
			if (Utils.isNullOrEmpty(sNodeUrl)) { sBaseUrl = "https://www.wasdi.net/wasdiwebserver/rest"; }

		    String sUrl = sBaseUrl + "/workflows/download?workflowId="+sWorkflowId;
		    
		    String sOutputFilePath = "";
		    
			
			try {
				URL oURL = new URL(sUrl);
				HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();

				// optional default is GET
				oConnection.setRequestMethod("GET");
				Map<String, String> asHeaders = HttpUtils.getStandardHeaders(sSessionId);
				for (Entry<String,String> asEntry : asHeaders.entrySet()) {
						oConnection.setRequestProperty(asEntry.getKey(),asEntry.getValue());
				}
				
				
				int responseCode =  oConnection.getResponseCode();

 				if(responseCode == 200) {
							
					Map<String, List<String>> aoHeaders = oConnection.getHeaderFields();
					List<String> asContents = null;
					if(null!=aoHeaders) {
						asContents = aoHeaders.get("Content-Disposition");
					}
					String sAttachmentName = null;
					if(null!=asContents) {
						String sHeader = asContents.get(0);
						sAttachmentName = sHeader.split("filename=")[1];
						if(sAttachmentName.startsWith("\"")) {
							sAttachmentName = sAttachmentName.substring(1);
						}
						if(sAttachmentName.endsWith("\"")) {
							sAttachmentName = sAttachmentName.substring(0,sAttachmentName.length()-1);
						}
						WasdiLog.debugLog("Wasdi.downloadWorkflow: attachment name: " + sAttachmentName);
						
					}
					

					String sSavePath = PathsConfig.getWorkflowsPath();
					sOutputFilePath = sSavePath + sWorkflowId+".xml";
					
					File oTargetFile = new File(sOutputFilePath);
					File oTargetDir = oTargetFile.getParentFile();
					oTargetDir.mkdirs();
				
					
					try(FileOutputStream oOutputStream = new FileOutputStream(sOutputFilePath);
						InputStream oInputStream = oConnection.getInputStream()){
							// 	opens an output stream to save into file
							Util.copyStream(oInputStream, oOutputStream);
					}catch (Exception oEx) {
						WasdiLog.errorLog("Wasdi.downloadWorkflow: exception ", oEx);
					}
					return sOutputFilePath;
				} else {
					String sMessage = "Wasdi.downloadWorkflow: response message: " + oConnection.getResponseMessage();
					WasdiLog.debugLog(sMessage);
					return "";
				}
 				
			} catch (Exception oEx) {
				WasdiLog.errorLog("Wasdi.downloadWorkflow: exception ", oEx);
				return "";
			}
			
			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("Wasdi.downloadWorkflow: exception ", oEx);
			return "";
		}		
	}	
	
	/**
	 * Get the Node Object representing the node where the server is running.
	 * @return Actual Node Object
	 */
	public static Node getActualNode() {
		try {
			if (s_oMyNode == null) {
				NodeRepository oNodeRepository = new NodeRepository();
				s_oMyNode = oNodeRepository.getNodeByCode(WasdiConfig.Current.nodeCode);
			}
			
			return s_oMyNode;
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("Wasdi.getActualNode: " + oEx.toString());
		}
		
		return null;
	}
	
	/**
	 * Get the list of nodes in order of priority for a specific user and eventually a specific application
	 * @param sSessionId
	 * @param sApplication
	 * @return
	 */
	public static List<NodeScoreByProcessWorkspaceViewModel> getNodesSortedByScore(String sSessionId, String sApplication)  {
		
		// Return variable ready
		List<NodeScoreByProcessWorkspaceViewModel> aoOrderedNodeList = new ArrayList<>();
		// Backup option list, if at the end we do not have any valid node
		List<NodeScoreByProcessWorkspaceViewModel> aoExcludedNodeList = new ArrayList<>();
		
		// Check the user
		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.debugLog("ProcessWorkspaceResource.getQueuesStatus: invalid session");
			return aoOrderedNodeList;
		}
		
		
		try {
			
			// We need to get the list of Nodes involved
			NodeRepository oNodeRepository = new NodeRepository();
			List<Node> aoNodes = null;
			
			// Is this a professional user?
			if (PermissionsUtils.getUserType(oUser).equals(UserType.PROFESSIONAL.name()))  {
				
				WasdiLog.debugLog("Wasdi.getNodesSortedByScore: Search dedicated nodes for Professional User");
				
				// Try to get the dedicated node/nodes: we read node permissions
				UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
				List<UserResourcePermission> aoUserNodePermissions = oUserResourcePermissionRepository.getPermissionsByTypeAndUserId("node", oUser.getUserId());
				
				List<Node> aoDedicatedNodes = new ArrayList<>();
				
				// For each node-permission, we double check the node exists and is Active
				if (aoUserNodePermissions != null) {
					for (UserResourcePermission oPermission : aoUserNodePermissions) {
						String sNodeCode = oPermission.getResourceId();
						Node oNode = oNodeRepository.getNodeByCode(sNodeCode);
						
						if (oNode != null)   {
							if (oNode.getActive()) {
								aoDedicatedNodes.add(oNode);
							}
						}
					}
				}
				
				// If we found at least one node, lets use it
				if (aoDedicatedNodes.size() > 0) {
					aoNodes = aoDedicatedNodes;
				}
				else {
					WasdiLog.debugLog("Wasdi.getNodesSortedByScore: Professional User " + oUser.getUserId() + " does not have any node associated! Will fallback with shared nodes");
				}
			}
			
			// If we do not have the list, take the list of shared nodes. This works for free and standard users
			// This is also a fallback option for Professional users: what if also the professional does not has any node?
			if (aoNodes == null || UserApplicationRole.isAdmin(oUser.getUserId())) {
				
				if (aoNodes==null) {
					aoNodes = oNodeRepository.getSharedActiveNodesList();	
				}
				else {
					aoNodes.addAll(oNodeRepository.getSharedActiveNodesList());
				}
				
				
				// By config we can decide to use also the main node as computing node, or not
				if (WasdiConfig.Current.loadBalancer.includeMainClusterAsNode) {
					WasdiLog.debugLog("Wasdi.getNodesSortedByScore: Adding main node to the available list");
					Node oNodeWasdi = new Node();
					oNodeWasdi.setNodeCode("wasdi");
					aoNodes.add(oNodeWasdi);				
				}				
			}
			
			// This is the list of Nodes that the user can access
			for (Node oNode : aoNodes) {
				
				// This should not be needed: all here should be active. But just to be more sure
				if (!oNode.getActive()) {
					WasdiLog.debugLog("Wasdi.getNodesSortedByScore: node " + oNode.getNodeCode() + " not active");
					continue;
				}
				
				// Read the metrics of the node
				String sNodeCode = oNode.getNodeCode();
				
				
				if (WasdiConfig.Current.loadBalancer.activateMetrics) {
					MetricsEntryRepository oMetricsEntryRepository = new MetricsEntryRepository();
					MetricsEntry oMetricsEntry = oMetricsEntryRepository.getLatestMetricsEntryByNode(sNodeCode);

					if (oMetricsEntry == null) {
						WasdiLog.debugLog("Wasdi.getNodesSortedByScore: metrics are null for node " + sNodeCode + ". Skip.");
						continue;
					}


					// Check the metrics entry of the node. If the metrics are too old, the node is skipped.
					Timestamp oTimestamp = oMetricsEntry.getTimestamp();

					if (oTimestamp == null) {
						WasdiLog.debugLog("Wasdi.getNodesSortedByScore: metrics timestamp values are null for node " + sNodeCode + ". Skip.");
						continue;
					}

					Double oTimestampInMillis = oTimestamp.getMillis();

					if (oTimestampInMillis == null) {
						Double oTimestampInSeconds = oTimestamp.getSeconds();

						if (oTimestampInSeconds == null) {
							WasdiLog.debugLog("Wasdi.getNodesSortedByScore: metrics timestamp values are null for node " + sNodeCode + ". Skip.");
							continue;
						} else {
							oTimestampInMillis = BigDecimal.valueOf(oTimestampInSeconds).multiply(BigDecimal.valueOf(1000L)).doubleValue();
						}
					}

					long lMillisPassesSinceTheLastMetricsEntry = BigDecimal.valueOf(Utils.nowInMillis()).subtract(BigDecimal.valueOf(oTimestampInMillis)).longValue();
					long lMaximumAllowedAgeOfInformation = ((long)WasdiConfig.Current.loadBalancer.metricsMaxAgeSeconds) * 1000L;

					boolean bMetricsEntryTooOld = lMillisPassesSinceTheLastMetricsEntry > lMaximumAllowedAgeOfInformation;

					if (bMetricsEntryTooOld) {
						WasdiLog.debugLog("Wasdi.getNodesSortedByScore: metrics too old for node " + sNodeCode + ". Possibly, the node is down. Skip.");
						continue;
					}


					// Get the list of disks to estimate space
					List<Disk> aoDisks = oMetricsEntry.getDisks();

					if (aoDisks != null) {

						NodeScoreByProcessWorkspaceViewModel oViewModel = new NodeScoreByProcessWorkspaceViewModel();
						oViewModel.setNodeCode(sNodeCode);

						String sTimestampAsString = Utils.getFormatDate(oTimestampInMillis);
						oViewModel.setTimestampAsString(sTimestampAsString);

						List<License> asLicenses = oMetricsEntry.getLicenses();
						if (asLicenses != null) {
							String sLicenses = asLicenses.stream()
									.filter(License::getStatus)
									.map(License::getName)
									.sorted()
									.collect(Collectors.joining(", "));

							oViewModel.setLicenses(sLicenses);
						}

						Disk oDisk = null;
						Double oPercentageUsed = null;
						
						if (aoDisks.size() <= 0) {
							oViewModel.setDiskPercentageAvailable(0.0);
							oViewModel.setDiskPercentageUsed(0.0);
							oViewModel.setDiskAbsoluteAvailable(0L);
							oViewModel.setDiskAbsoluteUsed(0L);
							oViewModel.setDiskAbsoluteTotal(0L);
						}
						else {
							oDisk= aoDisks.get(0);
							
							oPercentageUsed = oDisk.getPercentageUsed();
							oViewModel.setDiskPercentageAvailable(oDisk.getPercentageAvailable());
							oViewModel.setDiskPercentageUsed(oPercentageUsed);
							oViewModel.setDiskAbsoluteAvailable(oDisk.getAbsoluteAvailable());
							oViewModel.setDiskAbsoluteUsed(oDisk.getAbsoluteUsed());
							oViewModel.setDiskAbsoluteTotal(oDisk.getAbsoluteTotal());						
						}


						// Get the memory info to estimate memory availability
						Memory oMemory = oMetricsEntry.getMemory();

						if (oMemory == null) {
							oViewModel.setMemoryPercentageAvailable(0.0);
							oViewModel.setMemoryPercentageUsed(0.0);
							oViewModel.setMemoryAbsoluteAvailable(0L);
							oViewModel.setMemoryAbsoluteUsed(0L);
							oViewModel.setMemoryAbsoluteTotal(0L);
						}
						else {
							oViewModel.setMemoryPercentageAvailable(oMemory.getPercentageAvailable());
							oViewModel.setMemoryPercentageUsed(oPercentageUsed);
							oViewModel.setMemoryAbsoluteAvailable(oMemory.getAbsoluteAvailable());
							oViewModel.setMemoryAbsoluteUsed(oMemory.getAbsoluteUsed());
							oViewModel.setMemoryAbsoluteTotal(oMemory.getAbsoluteTotal());						
						}
						
						if (oPercentageUsed != null && oPercentageUsed.doubleValue() <= WasdiConfig.Current.loadBalancer.diskOccupiedSpaceMaxPercentage) {
							aoOrderedNodeList.add(oViewModel);
						}
						else {
							WasdiLog.debugLog("Wasdi.getNodesSortedByScore: Node " + oNode.getNodeCode() + " excluded because of space problem ( full > " + WasdiConfig.Current.loadBalancer.diskOccupiedSpaceMaxPercentage + "%)");
							aoExcludedNodeList.add(oViewModel);
						}
					}				
				}
				else {
					// Metrics are disabled: just add 
					NodeScoreByProcessWorkspaceViewModel oViewModel = new NodeScoreByProcessWorkspaceViewModel();
					oViewModel.setNodeCode(sNodeCode);
					
					Date oNow = new Date();
					String sTimestampAsString = Utils.getFormatDate(oNow);
					oViewModel.setTimestampAsString(sTimestampAsString);

					aoOrderedNodeList.add(oViewModel);
				}

			}
			
			// Lets verify if we have at least one node, otherwise we relax the first filter
			if (aoOrderedNodeList.size() <= 0)  {
				WasdiLog.debugLog("Wasdi.getNodesSortedByScore: Impossible to find any node with given rules: try to recover excluded one");
				aoOrderedNodeList = aoExcludedNodeList;
			}
			
			// Here we should have a list of node accessable by the user and not excluded. 
			// Now get for all the picture of the actual load situation of the schedulers
			for (NodeScoreByProcessWorkspaceViewModel oCandidateNodeViewModel : aoOrderedNodeList) {
				
				// Get the actual status of the queue
				List<ProcessWorkspaceAggregatedViewModel> aoSchedulerStatusList = ProcessWorkspaceResource.getNodeQueuesStatus(sSessionId, oCandidateNodeViewModel.getNodeCode(), null);

				int iTotalNumberOfOngoingProcesses = 0;

				if (aoSchedulerStatusList != null) {
					for (ProcessWorkspaceAggregatedViewModel oNodeSchedulerStatus : aoSchedulerStatusList) {
						iTotalNumberOfOngoingProcesses += oNodeSchedulerStatus.getNumberOfUnfinishedProcesses();
					}
				}

				oCandidateNodeViewModel.setNumberOfProcesses(iTotalNumberOfOngoingProcesses);
			}
			
			// We order giving priority to "free" nodes and then the ones with more space
			Comparator<NodeScoreByProcessWorkspaceViewModel> oComparator = Comparator
					.comparing(NodeScoreByProcessWorkspaceViewModel::getNumberOfProcesses)
					.thenComparing(NodeScoreByProcessWorkspaceViewModel::getDiskAbsoluteAvailable, Comparator.reverseOrder());

			Collections.sort(aoOrderedNodeList, oComparator);
			
			// Ok. we should have finished. But did we find at least one node?
			if (aoOrderedNodeList.size() <= 0) {
				// No. We do not like this. Try to recover the old WASDI rules: user default node or generic WASDI default node
				WasdiLog.debugLog("Wasdi.getNodesSortedByScore: the list of nodes is empty!! fallback to defaults");
				
				String sDefaultNode = WasdiConfig.Current.usersDefaultNode;
				
				if (!Utils.isNullOrEmpty(oUser.getDefaultNode())) {
					sDefaultNode = oUser.getDefaultNode();
				}
				
				NodeScoreByProcessWorkspaceViewModel oViewModel = new NodeScoreByProcessWorkspaceViewModel();
				oViewModel.setNodeCode(sDefaultNode);
				aoOrderedNodeList.add(oViewModel);
			}
			else {
				
				try {
					
					if (WasdiConfig.Current.loadBalancer.activateMetrics) {
						// Review the list to "downgrade" low performance nodes
						ArrayList<NodeScoreByProcessWorkspaceViewModel> aoLowPerformanceNodes = new ArrayList<>();
						
						// For all the candidates
						for (NodeScoreByProcessWorkspaceViewModel oNodeCandiate : aoOrderedNodeList) {
							
							// Read also the absolute RAM available
							Long lGb = oNodeCandiate.getMemoryAbsoluteAvailable()/1073741824L;
							
							// If it is less of the "performance required" value
							if (lGb.intValue() < WasdiConfig.Current.loadBalancer.minTotalMemoryGBytes) {
								// Set it as low performance
								aoLowPerformanceNodes.add(oNodeCandiate);
							}
						}
						
						// Remove the low performance nodes from the ordered list
						for (NodeScoreByProcessWorkspaceViewModel oNode : aoLowPerformanceNodes) {
							aoOrderedNodeList.remove(oNode);
						}
						
						// Ad re-add so they will go at the bottom of the options
						for (NodeScoreByProcessWorkspaceViewModel oNode : aoLowPerformanceNodes) {
							aoOrderedNodeList.add(oNode);
						}
					}
					
				}
				catch (Exception oInnerEx) {
					WasdiLog.errorLog("Wasdi.getNodesSortedByScore: exception removing low performance nodes ", oInnerEx);
				}
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("Wasdi.getNodesSortedByScore: exception " + oEx.toString());
		}
		
		return aoOrderedNodeList;

	}
}
