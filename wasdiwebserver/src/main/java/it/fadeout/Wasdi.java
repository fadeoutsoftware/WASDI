package it.fadeout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

import javax.annotation.PostConstruct;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Config;
import org.esa.snap.runtime.Engine;
import org.glassfish.jersey.server.ResourceConfig;

import it.fadeout.business.DownloadsThread;
import it.fadeout.business.IDLThread;
import it.fadeout.business.ProcessingThread;
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
import wasdi.shared.parameters.BaseParameter;
import wasdi.shared.rabbit.RabbitFactory;
import wasdi.shared.utils.CredentialPolicy;
import wasdi.shared.utils.SerializationUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.PrimitiveResult;

public class Wasdi extends ResourceConfig {
	@Context
	ServletConfig m_oServletConfig;

	@Context
	ServletContext m_oContext;

	/**
	 * Flag for Debug Log: if true Authentication is disabled
	 */
	private static boolean s_bDebug = false;

	/**
	 * Process queue scheduler
	 */
	private static ProcessingThread s_oProcessingThread = null;

	/**
	 * Downloads queue scheduler
	 */
	private static DownloadsThread s_oDownloadsThread = null;

	/**
	 * IDL Processors queue scheduler
	 */
	private static IDLThread s_oIDLThread = null;

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
	public static String s_sMyNodeCode = "wasdi";

	private static CredentialPolicy m_oCredentialPolicy;

	static {
		m_oCredentialPolicy = new CredentialPolicy();
	}

	public Wasdi() {
		register(new WasdiBinder());
		packages(true, "it.fadeout.rest.resources");
	}

	@PostConstruct
	public void initWasdi() {

		Utils.debugLog("-----------welcome to WASDI - Web Advanced Space Developer Interface");

		if (getInitParameter("DebugVersion", "false").equalsIgnoreCase("true")) {
			s_bDebug = true;
			Utils.debugLog("-------Debug Version on");
			s_sDebugUser = getInitParameter("DebugUser", "user");
			s_sDebugPassword = getInitParameter("DebugPassword", "password");
		}

		try {
			Utils.m_iSessionValidityMinutes = Integer
					.parseInt(getInitParameter("SessionValidityMinutes", "" + Utils.m_iSessionValidityMinutes));
			Utils.debugLog("-------Session Validity [minutes]: " + Utils.m_iSessionValidityMinutes);
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}

		// set nfs properties download
		String sUserHome = System.getProperty("user.home");
		String sNfsFolder = System.getProperty("nfs.data.download");
		if (sNfsFolder == null)
			System.setProperty("nfs.data.download", sUserHome + "/nfs/download");

		Utils.debugLog("-------nfs dir " + System.getProperty("nfs.data.download"));

		try {

			MongoRepository.SERVER_ADDRESS = getInitParameter("MONGO_ADDRESS", "127.0.0.1");
			MongoRepository.SERVER_PORT = Integer.parseInt(getInitParameter("MONGO_PORT", "27017"));
			MongoRepository.DB_NAME = getInitParameter("MONGO_DBNAME", "wasdi");
			MongoRepository.DB_USER = getInitParameter("MONGO_DBUSER", "mongo");
			MongoRepository.DB_PWD = getInitParameter("MONGO_DBPWD", "mongo");

			Utils.debugLog("-------Mongo db User " + MongoRepository.DB_USER);

		} catch (Throwable e) {
			e.printStackTrace();
		}
		
		try {

			s_sMyNodeCode = getInitParameter("NODECODE", "wasdi");
			Utils.debugLog("-------Node Code " + s_sMyNodeCode);

		} catch (Throwable e) {
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

		if (s_oProcessingThread == null) {
			try {

				Utils.debugLog("-------Starting Processing and Download Schedulers...");

				if (getInitParameter("EnableProcessingScheduler", "true").toLowerCase().equals("true")) {
					s_oProcessingThread = new ProcessingThread(m_oServletConfig);
					s_oProcessingThread.start();
					Utils.debugLog("-------processing thread STARTED");
				} else {
					Utils.debugLog("-------processing thread DISABLED");
				}

				if (getInitParameter("EnableDownloadScheduler", "true").toLowerCase().equals("true")) {
					s_oDownloadsThread = new DownloadsThread(m_oServletConfig);
					s_oDownloadsThread.start();
					Utils.debugLog("-------downloads thread STARTED");
				} else {
					Utils.debugLog("-------downloads thread DISABLED");
				}

				if (getInitParameter("EnableIDLScheduler", "true").toLowerCase().equals("true")) {
					s_oIDLThread = new IDLThread(m_oServletConfig);
					s_oIDLThread.start();
					Utils.debugLog("-------IDL thread STARTED");
				} else {
					Utils.debugLog("-------IDL thread DISABLED");
				}

			} catch (Exception e) {
				e.printStackTrace();
				Utils.debugLog("-------ERROR: CANNOT START PROCESSING THREAD!!!");
			}
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
				// ConsoleHandler handler = new ConsoleHandler();
				oFileHandler.setLevel(Level.ALL);
				SimpleFormatter oSimpleFormatter = new SimpleFormatter();
				oFileHandler.setFormatter(oSimpleFormatter);
				SystemUtils.LOG.setLevel(Level.ALL);
				SystemUtils.LOG.addHandler(oFileHandler);
			}
			Engine.start(false);

			// init HASH
			// initPasswordAuthenticationParameters();

		} catch (Throwable e) {
			e.printStackTrace();
		}

		Utils.debugLog("------- WASDI Init done ");
		Utils.debugLog("---------------------------------------------");
		Utils.debugLog("------- 	 Welcome to space     -------");
		Utils.debugLog("---------------------------------------------");
	}

	/**
	 * Server Shut down procedure
	 */
	public static void shutDown() {
		try {
			Utils.debugLog("-------Shutting Down Wasdi");

			s_oProcessingThread.stopThread();
			s_oDownloadsThread.stopThread();
			s_oIDLThread.stopThread();
			MongoRepository.shutDownConnection();
		} catch (Exception e) {
			Utils.debugLog("WASDI SHUTDOWN EXCEPTION: " + e.getMessage());
			e.printStackTrace();
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

	/*
	 * 
	 */
	/*
	 * private void initPasswordAuthenticationParameters() {
	 * PasswordAuthentication.setAlgorithm(getInitParameter("PWD_AUTHENTICATION",
	 * "PBKDF2WithHmacSHA1")); }
	 */
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
	public static String GetFormatDate(Date oDate) {
		return Utils.GetFormatDate(oDate);
	}

	/**
	 * Get the User object from the session Id
	 * 
	 * @param sSessionId
	 * @return
	 */
	public static User GetUserFromSession(String sSessionId) {

		// validate sSessionId
		if (!m_oCredentialPolicy.validSessionId(sSessionId)) {
			return null;
		}

		if (s_bDebug) {
			User oUser = new User();
			oUser.setId(1);
			oUser.setUserId(s_sDebugUser);
			oUser.setName("Name");
			oUser.setSurname("Surname");
			oUser.setPassword(s_sDebugPassword);
			return oUser;
		} else {
			// Create Session Repository
			SessionRepository oSessionRepo = new SessionRepository();
			// Get The User Session
			UserSession oSession = oSessionRepo.GetSession(sSessionId);

			if (Utils.isValidSession(oSession)) {
				// Create User Repo
				UserRepository oUserRepo = new UserRepository();
				// Get the user from the session
				User oUser = oUserRepo.GetUser(oSession.getUserId());

				oSessionRepo.TouchSession(oSession);

				return oUser;
			}

			// Session not valid
			oSessionRepo.DeleteSession(oSession);

			// No Session, No User
			return null;

		}
	}

	/**
	 * Get the OS PID of a process
	 * 
	 * @param oProc
	 * @return
	 */
	public static Integer getPIDProcess(Process oProc) {
		Integer oPID = null;

		if (oProc.getClass().getName().equals("java.lang.UNIXProcess")) {
			// get the PID on unix/linux systems
			try {
				Field oField = oProc.getClass().getDeclaredField("pid");
				oField.setAccessible(true);
				oPID = oField.getInt(oProc);
				Utils.debugLog("WASDI.getPIDProcess: found PID " + oPID);
			} catch (Throwable e) {
				Utils.debugLog("WASDI.getPIDProcess: Error getting PID " + e.getMessage());
			}
		}

		return oPID;
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

	/**
	 * Get The owner of a workspace starting from the workspace id
	 * 
	 * @param sWorkspaceId Id of the Workspace
	 * @return User Id of the owner
	 */
	public static String getWorkspaceOwner(String sWorkspaceId) {
		WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
		Workspace oWorkspace = oWorkspaceRepository.GetWorkspace(sWorkspaceId);
		if (oWorkspace == null)
			return "";
		String sWorkspaceOwner = oWorkspace.getUserId();
		return sWorkspaceOwner;
	}
	
	
	public static PrimitiveResult runProcess(String sUserId, String sSessionId, String sOperationId, String sProductName, String sSerializationPath, BaseParameter oParameter) throws IOException {
		
		// Get the Ob Id
		String sProcessObjId = oParameter.getProcessObjId();
		
		PrimitiveResult oResult = new PrimitiveResult();

		try {
			
			// Take the Workspace
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			Workspace oWorkspace = oWorkspaceRepository.GetWorkspace(oParameter.getWorkspace());
			
			if (oWorkspace == null) {
				Utils.debugLog("Wasdi.runProcess: ws not found. Return 500");
				oResult.setBoolValue(false);
				oResult.setIntValue(500);
				return oResult;				
			}
			
			// Check my node name
			String sMyNodeCode = Wasdi.s_sMyNodeCode;
			
			if (Utils.isNullOrEmpty(sMyNodeCode)) sMyNodeCode = "wasdi";
					
			// Is the worspace here?
			if (!oWorkspace.getNodeCode().equals(sMyNodeCode)) {
				
				// No: forward the call on the owner node
				Utils.debugLog("Wasdi.runProcess: forewarding request to [" + oWorkspace.getNodeCode()+"]");
				
				// Get the Node
				NodeRepository oNodeRepository = new NodeRepository();
				wasdi.shared.business.Node oDestinationNode = oNodeRepository.GetNodeByCode(oWorkspace.getNodeCode());
				
				if (oDestinationNode==null) {
					Utils.debugLog("Wasdi.runProcess: Node [" + oWorkspace.getNodeCode()+"] not found. Return 500");
					oResult.setBoolValue(false);
					oResult.setIntValue(500);
					return oResult;									
				}
				
				// Get the JSON of the parameter
				String sPayload = MongoRepository.s_oMapper.writeValueAsString(oParameter);

				// Get the URL of the destination Node
				String sUrl = oDestinationNode.getNodeBaseAddress();
				if (sUrl.endsWith("/") == false) sUrl += "/";
				sUrl += "processing/run?sProductName="+sProductName;
				
				// call the API on the destination node 
				String sResult = httpPost(sUrl, sPayload, getStandardHeaders(sSessionId));
				
				
		        try {
		        	// Get back the primitive result
		            PrimitiveResult oPrimitiveResult = MongoRepository.s_oMapper.readValue(sResult,PrimitiveResult.class);

		            return oPrimitiveResult;
		        } catch (Exception oEx) {
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

				if (!(sPath.endsWith("\\") || sPath.endsWith("/")))
					sPath += "/";
				sPath = sPath + sProcessObjId;

				SerializationUtils.serializeObjectToXML(sPath, oParameter);

				// Create the process Workspace
				ProcessWorkspaceRepository oRepository = new ProcessWorkspaceRepository();
				ProcessWorkspace oProcess = new ProcessWorkspace();

				try {
					oProcess.setOperationDate(Wasdi.GetFormatDate(new Date()));
					oProcess.setOperationType(sOperationId);
					oProcess.setProductName(sProductName);
					oProcess.setWorkspaceId(oParameter.getWorkspace());
					oProcess.setUserId(sUserId);
					oProcess.setProcessObjId(sProcessObjId);
					oProcess.setStatus(ProcessStatus.CREATED.name());
					oRepository.InsertProcessWorkspace(oProcess);
					Utils.debugLog("Wasdi.runProcess: Process Scheduled for Launcher");
				} catch (Exception oEx) {
					Utils.debugLog("Wasdi.runProcess: " + oEx);
					oResult.setBoolValue(false);
					oResult.setIntValue(500);
					return oResult;
				}				
			}

		} catch (IOException e) {
			Utils.debugLog("Wasdi.runProcess: " + e);
			oResult.setBoolValue(false);
			oResult.setIntValue(500);
			return oResult;
		} catch (Exception e) {
			Utils.debugLog("Wasdi.runProcess: " + e);
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
		HashMap<String, String> aoHeaders = new HashMap<String, String>();
		aoHeaders.put("x-session-token", sSessionId);
		aoHeaders.put("Content-Type", "application/json");
		
		return aoHeaders;
	}

	
	/**
	 * Standard http post utility function
	 * @param sUrl url to call
	 * @param sPayload payload of the post 
	 * @param asHeaders headers dictionary
	 * @return server response
	 */
	public static String httpPost(String sUrl, String sPayload, Map<String, String> asHeaders) {
		
		try {
			URL oURL = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();

			oConnection.setDoOutput(true);
			// Set POST
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

			BufferedReader oInputBuffer = new BufferedReader(new InputStreamReader(oConnection.getInputStream()));
			String sInputLine;
			StringBuffer sResponse = new StringBuffer();
	
			while ((sInputLine = oInputBuffer.readLine()) != null) {
				sResponse.append(sInputLine);
			}
			oInputBuffer.close();
			
			return sResponse.toString();
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}
	}	
	
		
}
