package it.fadeout;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;
import java.util.zip.ZipOutputStream;

import javax.annotation.PostConstruct;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;

import org.apache.commons.io.FileUtils;
import org.apache.commons.net.io.CopyStreamException;
import org.apache.commons.net.io.Util;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Config;
import org.esa.snap.runtime.Engine;
import org.glassfish.jersey.server.ResourceConfig;
import org.json.JSONObject;

import wasdi.shared.business.Node;
import wasdi.shared.business.ProcessStatus;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.business.User;
import wasdi.shared.business.UserSession;
import wasdi.shared.business.Workspace;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.NodeRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.SessionRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.launcherOperations.LauncherOperationsUtils;
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.rabbit.RabbitFactory;
import wasdi.shared.utils.CredentialPolicy;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.viewmodels.PrimitiveResult;

public class Wasdi extends ResourceConfig {
	
	private static final String s_SNFS_DATA_DOWNLOAD = "nfs.data.download";

	private static final String s_sWASDINAME = "wasdi";

	@Context
	ServletConfig m_oServletConfig;

	@Context
	ServletContext m_oContext;


	/**
	 * User for debug mode auto login
	 */
	public static String s_sDebugUser = "user";

	/**
	 * Password for debug mode auto login
	 */
	public static String s_sDebugPassword = "password";
	
	/**
	 * Code of the actual node
	 */
	public static String s_sMyNodeCode = Wasdi.s_sWASDINAME;
	
	/**
	 * Name of the Node-Specific Workpsace
	 */
	public static String s_sLocalWorkspaceName = "wasdi_specific_node_ws_code";
	
	/**
	 * Actual node Object
	 */
	public static Node s_oMyNode = null;
	
	/**
	 * Credential Policy Utility class
	 */
	private static CredentialPolicy m_oCredentialPolicy;

	public static String s_sKeyCloakIntrospectionUrl = "";
	public static String s_sClientId = "";
	public static String s_sClientSecret = "";
	public static String s_KeyCloakUser = "";
	public static String s_KeyCloakPw = "";
	public static String s_KeyBearerSecret = "";

	static {
		m_oCredentialPolicy = new CredentialPolicy();
	}

	public Wasdi() {
		register(new WasdiBinder());
		packages(true, "it.fadeout.rest.resources");
	}

	@PostConstruct
	public void initWasdi() {

		Utils.debugLog("----------- Welcome to WASDI - Web Advanced Space Developer Interface");

		try {
			Utils.s_iSessionValidityMinutes = Integer.parseInt(getInitParameter("SessionValidityMinutes", "" + Utils.s_iSessionValidityMinutes));
			Utils.debugLog("-------Session Validity [minutes]: " + Utils.s_iSessionValidityMinutes);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}

		// set nfs properties download
		String sUserHome = System.getProperty("user.home");
		String sNfsFolder = System.getProperty(Wasdi.s_SNFS_DATA_DOWNLOAD);
		if (sNfsFolder == null)
			System.setProperty(Wasdi.s_SNFS_DATA_DOWNLOAD, sUserHome + "/nfs/download");

		Utils.debugLog("-------nfs dir " + System.getProperty(Wasdi.s_SNFS_DATA_DOWNLOAD));

		try {

			MongoRepository.SERVER_ADDRESS = getInitParameter("MONGO_ADDRESS", "127.0.0.1");
			MongoRepository.SERVER_PORT = Integer.parseInt(getInitParameter("MONGO_PORT", "27017"));
			MongoRepository.DB_NAME = getInitParameter("MONGO_DBNAME", Wasdi.s_sWASDINAME);
			MongoRepository.DB_USER = getInitParameter("MONGO_DBUSER", "mongo");
			MongoRepository.DB_PWD = getInitParameter("MONGO_DBPWD", "mongo");

			Utils.debugLog("-------Mongo db User " + MongoRepository.DB_USER);

		} catch (Throwable e) {
			e.printStackTrace();
		}
		
		try {

			s_sMyNodeCode = getInitParameter("NODECODE", Wasdi.s_sWASDINAME);
			Utils.debugLog("-------Node Code " + s_sMyNodeCode);

		} catch (Throwable e) {
			e.printStackTrace();
		}
		
		s_sKeyCloakIntrospectionUrl = getInitParameter("keycloak_introspect", null);
		s_sClientId = getInitParameter("keycloak_confidentialClient", null);
		s_sClientSecret = getInitParameter("keycloak_clientSecret", null);
		s_KeyCloakUser = getInitParameter("keycloak_admin", null);
		s_KeyCloakPw = getInitParameter("keycloak_pw", null); // keycloak_brrrSecret
		s_KeyBearerSecret = getInitParameter("keycloak_brrrSecret", null); //

		try {
			// If this is not the main node
			if (!s_sMyNodeCode.equals(Wasdi.s_sWASDINAME)) {
				// Configure also the local connection: by default is the "wasdi" port + 1
				MongoRepository.addMongoConnection("local", MongoRepository.DB_USER, MongoRepository.DB_PWD, MongoRepository.SERVER_ADDRESS, MongoRepository.SERVER_PORT+1, MongoRepository.DB_NAME);
				
				Utils.debugLog("-------Addded Mongo Configuration local for " + s_sMyNodeCode);
			}			
		}
		catch (Throwable e) {
			e.printStackTrace();
		}

		try {

			RabbitFactory.s_sRABBIT_QUEUE_USER = getInitParameter("RABBIT_QUEUE_USER", "guest");
			RabbitFactory.s_sRABBIT_QUEUE_PWD = getInitParameter("RABBIT_QUEUE_PWD", "guest");
			RabbitFactory.s_sRABBIT_HOST = getInitParameter("RABBIT_HOST", "127.0.0.1");
			RabbitFactory.s_sRABBIT_QUEUE_PORT = getInitParameter("RABBIT_QUEUE_PORT", "5672");

			Utils.debugLog("-------Rabbit User " + RabbitFactory.s_sRABBIT_QUEUE_USER);

		} catch (Throwable e) {
			e.printStackTrace();
		}

		Utils.debugLog("-------initializing snap...");

		try {
			String snapAuxPropPath = getInitParameter("SNAP_AUX_PROPERTIES", null);
			Utils.debugLog("snap aux properties file: " + snapAuxPropPath);
			Path propFile = Paths.get(snapAuxPropPath);
			Config.instance("snap.auxdata").load(propFile);
			Config.instance().load();

			SystemUtils.init3rdPartyLibs(null);
			SystemUtils.LOG.setLevel(Level.ALL);

			String sSnapLogActive = getInitParameter("SNAP_LOG_ACTIVE", "1");

			if (sSnapLogActive.equals("1") || sSnapLogActive.equalsIgnoreCase("true")) {
				String sSnapLogFolder = getInitParameter("SNAP_LOG_FOLDER", "/usr/lib/wasdi/launcher/logs/snapweb.log");

				FileHandler oFileHandler = new FileHandler(sSnapLogFolder, true);
				oFileHandler.setLevel(Level.ALL);
				SimpleFormatter oSimpleFormatter = new SimpleFormatter();
				oFileHandler.setFormatter(oSimpleFormatter);
				SystemUtils.LOG.setLevel(Level.ALL);
				SystemUtils.LOG.addHandler(oFileHandler);
			}
			
			Engine.start(false);

		} catch (Throwable e) {
			e.printStackTrace();
		}
		
		
		Utils.debugLog("-------initializing node local workspace...");
		
		try {
			// Check if it exists
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			Workspace oWorkspace = oWorkspaceRepository.getByNameAndNode(Wasdi.s_sLocalWorkspaceName, s_sMyNodeCode);
			
			if (oWorkspace == null) {
				Utils.debugLog("Local Workpsace Node " + Wasdi.s_sLocalWorkspaceName + " does not exist create it");
				
				oWorkspace = new Workspace();
				
				// Create the working Workspace in this node

				// Default values
				oWorkspace.setCreationDate((double) new Date().getTime());
				oWorkspace.setLastEditDate((double) new Date().getTime());
				oWorkspace.setName(Wasdi.s_sLocalWorkspaceName);
				// Leave this at "no user"
				oWorkspace.setWorkspaceId(Utils.GetRandomName());
				oWorkspace.setNodeCode(Wasdi.s_sMyNodeCode);
				
				// Insert in the db
				oWorkspaceRepository.insertWorkspace(oWorkspace);
				
				Utils.debugLog("Workspace " + Wasdi.s_sLocalWorkspaceName + " created");
			}
			
		}
		catch (Throwable e) {
			e.printStackTrace();
		}
		
		Utils.debugLog("------- WASDI Init done\n\n");
		Utils.debugLog("-----------------------------------------------");
		Utils.debugLog("--------        Welcome to space        -------");
		Utils.debugLog("-----------------------------------------------\n\n");
		
		try {
			
			Utils.debugLog("******************************Environment Vars*****************************"); Map<String, String> enviorntmentVars = System.getenv();
			
			for (String string : enviorntmentVars.keySet()) { Utils.debugLog(string + ": " + enviorntmentVars.get(string)); }
			 			
		}
		catch (Exception e) {
			Utils.debugLog(e.toString());
			e.printStackTrace();
		}
	}

	/**
	 * Server Shut down procedure
	 */
	public static void shutDown() {
		try {
			Utils.debugLog("-------Shutting Down Wasdi");
			
			try {
				MongoRepository.shutDownConnection();
			} catch (Exception oE) {
				Utils.debugLog("Wasdi.shutDown: could not shut down connection to DB: " + oE);
			}
		} catch (Exception oE) {
			Utils.debugLog("WASDI SHUTDOWN EXCEPTION: " + oE);
			oE.printStackTrace();
		}
	}

	/**
	 * Safe Read Init Parameter
	 * 
	 * @param sParmaneter
	 * @param sDefault
	 * @return
	 */
	private String getInitParameter(String sParmaneter, String sDefault) {
		String sParameterValue = m_oServletConfig.getInitParameter(sParmaneter);
		return sParameterValue == null ? sDefault : sParameterValue;
	}

	/**
	 * Get Safe Random file name
	 * 
	 * @return
	 */
	public static String GetSerializationFileName() {
		return UUID.randomUUID().toString();
	}

	/**
	 * Get Common Date Time Format
	 * 
	 * @param oDate
	 * @return
	 */
	public static String getFormatDate(Date oDate) {
		return Utils.getFormatDate(oDate);
	}

	/**
	 * Get the User object from the session Id
	 * 
	 * @param sSessionId
	 * @return
	 */
	public static User getUserFromSession(String sSessionId) {
		User oUser = null;
		try {
			// validate sSessionId
			if (!m_oCredentialPolicy.validSessionId(sSessionId)) {
				return null;
			}

			String sUserId = null;
			//todo validate token with JWT
			
			try  {
				//introspect
				String sPayload = "token=" + sSessionId;
				Map<String,String> asHeaders = new HashMap<>();
				asHeaders.put("Content-Type", "application/x-www-form-urlencoded");
				String sResponse = httpPost(Wasdi.s_sKeyCloakIntrospectionUrl, sPayload, asHeaders, s_sClientId + ":" + s_sClientSecret);
				JSONObject oJSON = null;
				if(!Utils.isNullOrEmpty(sResponse)) {
					oJSON = new JSONObject(sResponse);
				}
				if(null!=oJSON) {
					sUserId = oJSON.optString("preferred_username", null);
				}				
			}
			catch (Exception oKeyEx) {
				Utils.debugLog("WAsdi.getUserFromSession: exception contacting keycloak: " + oKeyEx.toString());
			}


			if(!Utils.isNullOrEmpty(sUserId)) {
				UserRepository oUserRepo = new UserRepository();
				oUser = oUserRepo.getUser(sUserId);
				return oUser;
			} else {
				//check session against DB
				
				SessionRepository oSessionRepository = new SessionRepository();
				UserSession oUserSession = oSessionRepository.getSession(sSessionId);
				if(null==oUserSession) {
					throw new NullPointerException("checking against DB failed: session " + sSessionId + " is not valid");
				} else {
					sUserId = oUserSession.getUserId();
				}
				if(!Utils.isNullOrEmpty(sUserId)){
					UserRepository oUserRepository = new UserRepository();
					oUser = oUserRepository.getUser(sUserId);
				} else {
					throw new NullPointerException("User is null from WASDI session " + sSessionId );
				}
			}
		} catch (Exception oE) {
			Utils.debugLog("WAsdi.getUserFromSession: something bad happened: " + oE);
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
			//delete data base session
			if (!oSessionRepository.deleteSession(oUserSession)) {

				Utils.debugLog("AuthService.Login: Error deleting session.");
			}
		}
	}

	public static String getWorkspacePath(ServletConfig oServletConfig, String sUserId, String sWorkspace) {
		// Take path
		String sDownloadRootPath = oServletConfig.getInitParameter("DownloadRootPath");

		if (Utils.isNullOrEmpty(sDownloadRootPath)) {
			sDownloadRootPath = "/data/wasdi/";
		}

		if (!sDownloadRootPath.endsWith("/")) {
			sDownloadRootPath = sDownloadRootPath + "/";
		}
		String sPath = sDownloadRootPath + sUserId + "/" + sWorkspace + "/";

		return sPath;
	}


	public static String getDownloadPath(ServletConfig oServletConfig) {
		// Take path
		String sDownloadRootPath = oServletConfig.getInitParameter("DownloadRootPath");

		if (Utils.isNullOrEmpty(sDownloadRootPath)) {
			sDownloadRootPath = "/data/wasdi/";
		}

		if (!sDownloadRootPath.endsWith("/")) {
			sDownloadRootPath = sDownloadRootPath + "/";
		}

		return sDownloadRootPath;
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
	
	public static PrimitiveResult runProcess(String sUserId, String sSessionId, String sOperationId, String sProductName, String sSerializationPath, BaseParameter oParameter) throws IOException {
		return runProcess(sUserId, sSessionId, sOperationId, sProductName, sSerializationPath, oParameter, null);
	}
	
	public static PrimitiveResult runProcess(String sUserId, String sSessionId, String sOperationId, String sProductName, String sSerializationPath, BaseParameter oParameter, String sParentId) throws IOException {
		return runProcess(sUserId, sSessionId, sOperationId, null, sProductName, sSerializationPath, oParameter, sParentId);
	}

	/**
	 * Run Process: if the workspace of the operation is the actual node this function
	 * creates the process workspace entry that will be handled by the scheduler to run a process on a node.
	 * If it is in another node, it routes the request to the destination node.
	 *
	 * @param sUserId User requesting the operation
	 * @param sSessionId User session
	 * @param sOperationId Id of the Launcher Operation
	 * @param sOperationSubId Sub Id of the Launcher Operation
	 * @param sProductName Product name associated to the Process Workspace
	 * @param sSerializationPath Node Serialisation Path
	 * @param oParameter Parameter associated to the operation
	 * @param sParentId Id the the parent process
	 * @return Primitive Result with the output status of the operation
	 * @throws IOException
	 */
	public static PrimitiveResult runProcess(String sUserId, String sSessionId, String sOperationId, String sOperationSubId, String sProductName, String sSerializationPath, BaseParameter oParameter, String sParentId) throws IOException {

		if (!LauncherOperationsUtils.isValidLauncherOperation(sOperationId)) {
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
		if(null == oUser) {
			Utils.debugLog("Wasdi.runProcess( " + sUserId + ", " + sSessionId + ", " + sOperationId + ", " + sProductName + ", ... ): session not valid, aborting");
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
				Utils.debugLog("Wasdi.runProcess: ws not found. Return 500");
				oResult.setBoolValue(false);
				oResult.setIntValue(500);
				return oResult;				
			}
			
			// Check my node name
			String sMyNodeCode = Wasdi.s_sMyNodeCode;
			
			if (Utils.isNullOrEmpty(sMyNodeCode)) sMyNodeCode = Wasdi.s_sWASDINAME;
					
			// Is the workspace here?
			String sWsNodeCode = oWorkspace.getNodeCode();
			if (!sWsNodeCode.equals(sMyNodeCode)) {
				
				// No: forward the call on the owner node
				Utils.debugLog("Wasdi.runProcess: forwarding request to [" + sWsNodeCode+"]");
				
				// Get the Node
				NodeRepository oNodeRepository = new NodeRepository();
				wasdi.shared.business.Node oDestinationNode = oNodeRepository.getNodeByCode(sWsNodeCode);
				
				if (oDestinationNode==null) {
					Utils.debugLog("Wasdi.runProcess: Node [" + sWsNodeCode+"] not found. Return 500");
					oResult.setBoolValue(false);
					oResult.setIntValue(500);
					return oResult;									
				}
				
				Utils.debugLog("Wasdi.runProcess: serializing parameter to XML");
				String sPayload = SerializationUtils.serializeObjectToStringXML(oParameter);

				// Get the URL of the destination Node
				String sUrl = oDestinationNode.getNodeBaseAddress();
				Utils.debugLog("Wasdi.runProcess: base url is: " + sUrl );
				if (sUrl.endsWith("/") == false) sUrl += "/";
				sUrl += "processing/run?sOperation=" + sOperationId + "&sProductName=" + URLEncoder.encode(sProductName, java.nio.charset.StandardCharsets.UTF_8.toString());
				
				// Is there a parent?
				if (!Utils.isNullOrEmpty(sParentId)) {
					Utils.debugLog("Wasdi.runProcess: adding parent " + sParentId);
					sUrl += "&parent=" + URLEncoder.encode(sParentId, java.nio.charset.StandardCharsets.UTF_8.toString());
				}
				
				// Is there a subType?
				if (!Utils.isNullOrEmpty(sOperationSubId)) {
					Utils.debugLog("Wasdi.runProcess: adding sub type " + sOperationSubId);
					sUrl += "&subtype=" + sOperationSubId;
				}

				Utils.debugLog("Wasdi.runProcess: URL: " + sUrl);
				//Utils.debugLog("Wasdi.runProcess: PAYLOAD: " + sPayload);
				
				// call the API on the destination node 
				String sResult = httpPost(sUrl, sPayload, getStandardHeaders(sSessionId));
				
				
		        try {
		        	// Get back the primitive result
		            PrimitiveResult oPrimitiveResult = MongoRepository.s_oMapper.readValue(sResult,PrimitiveResult.class);

		            return oPrimitiveResult;
		        } 
		        catch (Exception oEx) {
		            oEx.printStackTrace();
					Utils.debugLog("Wasdi.runProcess: exception " + oEx);
					oResult.setBoolValue(false);
					oResult.setIntValue(500);
					return oResult;				
		        }
			}
			else {
				// The Workspace is here. Just add the ProcessWorkspace to the database 
				
				// Serialization Path
				String sPath = sSerializationPath;

				if (!(sPath.endsWith("\\") || sPath.endsWith("/"))) sPath += "/";
				sPath = sPath + sProcessObjId;
				
				
				//create a WASDI session here
				
				//maybe store original keycloak session id in WASDI DB, to keep more params, e.g., the user
				UserSession oSession = new UserSession();
				oSession.setUserId(sUserId);

				Boolean bNew = false;
				//store the keycloak access token instead, so we can retrieve the user and perform a further check
				if (Utils.isNullOrEmpty(sParentId)) {
					sSessionId = UUID.randomUUID().toString();
					bNew = true;
				}
				oSession.setSessionId(sSessionId);
				oSession.setLoginDate((double) new Date().getTime());
				oSession.setLastTouch((double) new Date().getTime());
				
				SessionRepository oSessionRepo = new SessionRepository();
				Boolean bRet = false;
				if(bNew) {
					bRet = oSessionRepo.insertSession(oSession);
				} else {
					bRet = oSessionRepo.touchSession(oSession);
				}
				if (bRet) {
					oParameter.setSessionID(oSession.getSessionId());
				} else {
					throw new IllegalArgumentException("could not insert session " + oSession.getSessionId() + " in DB, aborting");
				}

				SerializationUtils.serializeObjectToXML(sPath, oParameter);

				// Create the process Workspace
				ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
				ProcessWorkspace oProcess = new ProcessWorkspace();

				try {
					oProcess.setOperationDate(Wasdi.getFormatDate(new Date()));
					oProcess.setOperationType(sOperationId);
					oProcess.setOperationSubType(sOperationSubId);
					oProcess.setProductName(sProductName);
					oProcess.setWorkspaceId(oParameter.getWorkspace());
					oProcess.setUserId(sUserId);
					oProcess.setProcessObjId(sProcessObjId);
					if (!Utils.isNullOrEmpty(sParentId)) oProcess.setParentId(sParentId);
					oProcess.setStatus(ProcessStatus.CREATED.name());
					oProcess.setNodeCode(sWsNodeCode);
					oRepository.insertProcessWorkspace(oProcess);
					Utils.debugLog("Wasdi.runProcess: Process Scheduled for Launcher");
				} catch (Exception oEx) {
					Utils.debugLog("Wasdi.runProcess: " + oEx);
					oResult.setBoolValue(false);
					oResult.setIntValue(500);
					return oResult;
				}				
			}
		} catch (Exception oE) {
			Utils.debugLog("Wasdi.runProcess: " + oE);
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
	 * Get the standard headers for a WASDI call
	 * @return
	 */
	public static HashMap<String, String> getStandardHeaders(String sSessionId) {
		HashMap<String, String> asHeaders = new HashMap<String, String>();
		asHeaders.put("x-session-token", sSessionId);
		asHeaders.put("Content-Type", "application/json");
		
		return asHeaders;
	}
	
	/**
	 * Standard http post utility function
	 * @param sUrl url to call
	 * @param sPayload payload of the post 
	 * @param asHeaders headers dictionary
	 * @return server response
	 */
	public static String httpPost(String sUrl, String sPayload, Map<String, String> asHeaders) {
		return httpPost(sUrl, sPayload, asHeaders, null);
	}

	/**
	 * Standard http post utility function
	 * @param sUrl url to call
	 * @param sPayload payload of the post
	 * @param asHeaders headers dictionary
	 * @param sAuth in the form user:password (i.e., separated by a column: ':')
	 * @return server response
	 */
	public static String httpPost(String sUrl, String sPayload, Map<String, String> asHeaders, String sAuth) {

		try {
			URL oURL = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();

			if(!Utils.isNullOrEmpty(sAuth)) {
				String sEncodedAuth = Base64.getEncoder().encodeToString(sAuth.getBytes(StandardCharsets.UTF_8));
				String sAuthHeaderValue = "Basic " + sEncodedAuth;
				oConnection.setRequestProperty("Authorization", sAuthHeaderValue);

			}

			oConnection.setDoOutput(true);
			oConnection.setRequestMethod("POST");
			
			if (asHeaders != null) {
				for (String sKey : asHeaders.keySet()) {
					oConnection.setRequestProperty(sKey,asHeaders.get(sKey));
				}
			}
			
			OutputStream oPostOutputStream = oConnection.getOutputStream();
			OutputStreamWriter oStreamWriter = new OutputStreamWriter(oPostOutputStream, "UTF-8");  
			if (sPayload!= null) oStreamWriter.write(sPayload);
			oStreamWriter.flush();
			oStreamWriter.close();
			oPostOutputStream.close(); 
			
			oConnection.connect();

			String sMessage = readHttpResponse(oConnection);
			oConnection.disconnect();
			
			return sMessage;
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}
	}
		
	public static void httpPostFile(String sUrl, String sFileName, Map<String, String> asHeaders) throws IOException 
	{
		//local file -> automatically checks for null
		File oFile = new File(sFileName);
		if(!oFile.exists()) {
			throw new IOException("Wasdi.httpPostFile: file not found");
		}
		
		String sZippedFile = null;
		
		if (!oFile.getName().toUpperCase().endsWith("ZIP")) {
			
			Utils.debugLog("Wasdi.httpPostFile: File not zipped, zip it");
			
			int iRandom = new SecureRandom().nextInt() & Integer.MAX_VALUE;
			
			String sTemp = "tmp-" + iRandom + File.separator;
			String sTempPath = WasdiFileUtils.fixPathSeparator(oFile.getParentFile().getPath());
			
			if(!sTempPath.endsWith(File.separator)) {
				sTempPath += File.separator;
			}
			sTempPath += sTemp;
			
			Path oPath = Paths.get(sTempPath).toAbsolutePath().normalize();
			if (oPath.toFile().mkdir()) {
				Utils.debugLog("Wasdi.httpPostFile: Temporary directory created");
			} else {
				throw new IOException("Wasdi.httpPostFile: Can't create temporary dir " + sTempPath);
			}
			
			sZippedFile = sTempPath+iRandom + ".zip";
			
			File oZippedFile = new File(sTempPath+iRandom + ".zip");
			ZipOutputStream oOutZipStream = new ZipOutputStream(new FileOutputStream(oZippedFile));
			WasdiFileUtils.zipFile(oFile, oFile.getName(), oOutZipStream);
			
			oOutZipStream.close();
			
			String sOldFileName = oFile.getName();
			
			oFile = new File(sZippedFile);
			
			sUrl = sUrl.replace(sOldFileName, oFile.getName());
			
			sFileName = oFile.getName();
		}

		String sBoundary = "**WASDIlib**" + UUID.randomUUID().toString() + "**WASDIlib**";
		try(FileInputStream oInputStream = new FileInputStream(oFile)){
		    //request
			URL oURL = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();
			oConnection.setDoOutput(true);
			oConnection.setDoInput(true);
			oConnection.setUseCaches(false);
			int iBufferSize = 8192;//8*1024*1024
			oConnection.setChunkedStreamingMode(iBufferSize);
			Long lLen = oFile.length();
			Utils.debugLog("Wasdi.httpPostFile: file length is: "+Long.toString(lLen));
			
			if (asHeaders != null) {
				for (String sKey : asHeaders.keySet()) {
					oConnection.setRequestProperty(sKey,asHeaders.get(sKey));
				}
			}
			oConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + sBoundary);
			oConnection.setRequestProperty("Connection", "Keep-Alive");
			oConnection.setRequestProperty("User-Agent", "WasdiLib.Java");
			oConnection.connect();
			
			try(DataOutputStream oOutputStream = new DataOutputStream(oConnection.getOutputStream())){

				oOutputStream.writeBytes( "--" + sBoundary + "\r\n" );
				oOutputStream.writeBytes( "Content-Disposition: form-data; name=\"" + "file" + "\"; filename=\"" + sFileName + "\"" + "\r\n");
				oOutputStream.writeBytes( "Content-Type: " + URLConnection.guessContentTypeFromName(sFileName) + "\r\n");
				oOutputStream.writeBytes( "Content-Transfer-Encoding: binary" + "\r\n");
				oOutputStream.writeBytes("\r\n");

				Util.copyStream(oInputStream, oOutputStream);

				oOutputStream.flush();
				oInputStream.close();
				oOutputStream.writeBytes("\r\n");
				oOutputStream.flush();
				oOutputStream.writeBytes("\r\n");
				oOutputStream.writeBytes("--" + sBoundary + "--"+"\r\n");

				// response
				int iResponse = oConnection.getResponseCode();
				Utils.debugLog("Wasdi.httpPostFile: server returned " + iResponse);
				
				InputStream oResponseInputStream = null;
				
				ByteArrayOutputStream oByteArrayOutputStream = new ByteArrayOutputStream();
				
				if( 200 <= iResponse && 299 >= iResponse ) {
					oResponseInputStream = oConnection.getInputStream();
				} else {
					oResponseInputStream = oConnection.getErrorStream();
				}
				if(null!=oResponseInputStream) {
					Util.copyStream(oResponseInputStream, oByteArrayOutputStream);
					String sMessage = "WasdiLib.uploadFile: " + oByteArrayOutputStream.toString();
					Utils.debugLog(sMessage);
				} else {
					throw new NullPointerException("WasdiLib.uploadFile: stream is null");
				}

				oConnection.disconnect();

			} catch(Exception oE) {
				Utils.debugLog("WasdiLib.uploadFile( " + sUrl + ", " + sFileName + ", ...): internal exception: " + oE);
				throw oE;
			}
		} catch (Exception oE) {
			Utils.debugLog("Wasdi.httpPostFile( " + sUrl + ", " + sFileName + ", ...): could not open file due to: " + oE + ", aborting");
			throw oE;
		}
		
		if (!Utils.isNullOrEmpty(sZippedFile)) {
			try {
				FileUtils.deleteDirectory(new File(sZippedFile).getParentFile());
			}
			catch (Exception oE) {
				Utils.debugLog("Wasdi.httpPostFile( " + sUrl + ", " + sFileName + ", ...): could not delete temp zip file: " + oE + "");
			}			
		}
	}
	
	
	/**
	 * Standard http put utility function
	 * @param sUrl url to call
	 * @param sPayload payload of the post
	 * @param asHeaders headers dictionary
	 * @return server response
	 */
	public static String httpPut(String sUrl, String sPayload, Map<String, String> asHeaders) {
		return httpPut(sUrl, sPayload, asHeaders, null);
	}
	
	/**
	 * Standard http put utility function
	 * @param sUrl url to call
	 * @param sPayload payload of the post
	 * @param asHeaders headers dictionary
	 * @param sAuth in the form user:password (i.e., separated by a column: ':')
	 * @return server response
	 */
	public static String httpPut(String sUrl, String sPayload, Map<String, String> asHeaders, String sAuth) {

		try {
			URL oURL = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();

			if(!Utils.isNullOrEmpty(sAuth)) {
				String sEncodedAuth = Base64.getEncoder().encodeToString(sAuth.getBytes(StandardCharsets.UTF_8));
				String sAuthHeaderValue = "Basic " + sEncodedAuth;
				oConnection.setRequestProperty("Authorization", sAuthHeaderValue);

			}

			oConnection.setDoOutput(true);
			oConnection.setRequestMethod("PUT");
			
			if (asHeaders != null) {
				for (String sKey : asHeaders.keySet()) {
					oConnection.setRequestProperty(sKey,asHeaders.get(sKey));
				}
			}
			
			OutputStream oPostOutputStream = oConnection.getOutputStream();
			OutputStreamWriter oStreamWriter = new OutputStreamWriter(oPostOutputStream, "UTF-8");  
			if (sPayload!= null) oStreamWriter.write(sPayload);
			oStreamWriter.flush();
			oStreamWriter.close();
			oPostOutputStream.close(); 
			
			oConnection.connect();

			String sMessage = readHttpResponse(oConnection);
			oConnection.disconnect();
			
			return sMessage;
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}
	}
	
	/**
	 * Standard http get utility function
	 * @param sUrl url to call
	 * @param sPayload payload of the post 
	 * @param asHeaders headers dictionary
	 * @return server response
	 */
	public static String httpGet(String sUrl, Map<String, String> asHeaders) {
		
		try {
			URL oURL = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();
			oConnection.setConnectTimeout(2000);
			oConnection.setReadTimeout(2000);

			oConnection.setDoOutput(true);
			oConnection.setRequestMethod("GET");
			
			if (asHeaders != null) {
				for (String sKey : asHeaders.keySet()) {
					oConnection.setRequestProperty(sKey,asHeaders.get(sKey));
				}
			}
						
			oConnection.connect();
			
			return readHttpResponse(oConnection);
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}
	}

	/**
	 * @param oConnection
	 * @throws IOException
	 * @throws CopyStreamException
	 */
	public static String readHttpResponse(HttpURLConnection oConnection) {
		try {
			// response
			int iResponseCode = oConnection.getResponseCode();
			//Utils.debugLog("Wasdi.readHttpResponse: server returned " + iResponseCode);

			InputStream oResponseInputStream = null;

			ByteArrayOutputStream oByteArrayOutputStream = new ByteArrayOutputStream();

			if( 200 <= iResponseCode && 299 >= iResponseCode ) {
				oResponseInputStream = oConnection.getInputStream();
			} else {
				Utils.debugLog("Wasdi.readHttpResponse: failed with error " + iResponseCode);
				oResponseInputStream = oConnection.getErrorStream();
			}
			if(null!=oResponseInputStream) {
				Util.copyStream(oResponseInputStream, oByteArrayOutputStream);
				String sMessage = oByteArrayOutputStream.toString();
				if( 200 <= iResponseCode && 299 >= iResponseCode ) {
					return sMessage;
				} else {
					Utils.debugLog("Wasdi.readHttpResponse: error message: " + sMessage);
					return "";
				}
			} else {
				throw new NullPointerException("WasdiLib.uploadFile: stream is null");
			}
		} catch (Exception oE) {
			Utils.debugLog("Wasdi.readHttpResponse: " + oE );
		}
		return "";
	}

	
	/**
	 * Download a file on the local PC
	 * @param sWorkflowId File Name
	 * @return Full Path
	 */
	public static String downloadWorkflow(String sNodeUrl, String sWorkflowId, String sSessionId, ServletConfig oServletConfig) {
		try {
			
			if (Utils.isNullOrEmpty(sWorkflowId)) {
				Utils.debugLog("sWorkflowId is null or empty");
				return "";
			}
			
			String sBaseUrl = sNodeUrl;
			
			if (Utils.isNullOrEmpty(sNodeUrl)) { sBaseUrl = "https://www.wasdi.net/wasdiwebserver/rest"; }

		    String sUrl = sBaseUrl + "/processing/downloadgraph?workflowId="+sWorkflowId;
		    
		    String sOutputFilePath = "";
		    
		    
		    
		    
			
			try {
				URL oURL = new URL(sUrl);
				HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();

				// optional default is GET
				oConnection.setRequestMethod("GET");
				HashMap<String, String> asHeaders = getStandardHeaders(sSessionId);
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
						Utils.debugLog("Wasdi.downloadWorkflow: attachment name: " + sAttachmentName);
						
					}
					

					String sSavePath = getDownloadPath(oServletConfig) + "workflows/";
					sOutputFilePath = sSavePath + sWorkflowId+".xml";
					
					File oTargetFile = new File(sOutputFilePath);
					File oTargetDir = oTargetFile.getParentFile();
					oTargetDir.mkdirs();
				
					
					try(FileOutputStream oOutputStream = new FileOutputStream(sOutputFilePath);
						InputStream oInputStream = oConnection.getInputStream()){
							// 	opens an output stream to save into file
							Util.copyStream(oInputStream, oOutputStream);
					}catch (Exception oEx) {
						oEx.printStackTrace();
					}
					return sOutputFilePath;
				} else {
					String sMessage = "Wasdi.downloadWorkflow: response message: " + oConnection.getResponseMessage();
					Utils.debugLog(sMessage);
					return "";
				}
 				
			} catch (Exception oEx) {
				oEx.printStackTrace();
				return "";
			}
			
			
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
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
				s_oMyNode = oNodeRepository.getNodeByCode(s_sMyNodeCode);
			}
			
			return s_oMyNode;
		}
		catch (Exception oEx) {
			Utils.debugLog("Wasdi.getActualNode: " + oEx.toString());
		}
		
		return null;
	}


}