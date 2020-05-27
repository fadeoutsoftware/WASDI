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
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

import javax.annotation.PostConstruct;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;

import org.apache.commons.net.io.Util;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Config;
import org.esa.snap.runtime.Engine;
import org.glassfish.jersey.server.ResourceConfig;

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
			// If this is not the main node
			if (!s_sMyNodeCode.equals("wasdi")) {
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
				// ConsoleHandler handler = new ConsoleHandler();
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
				oWorkspace.setNodeCode("wasdi");
				
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
		Utils.debugLog("-------- 	   Welcome to space      -------");
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
		
		// Create Session Repository
		SessionRepository oSessionRepo = new SessionRepository();
		// Get The User Session
		UserSession oSession = oSessionRepo.getSession(sSessionId);

		if (Utils.isValidSession(oSession)) {
			// Create User Repo
			UserRepository oUserRepo = new UserRepository();
			// Get the user from the session
			User oUser = oUserRepo.getUser(oSession.getUserId());

			oSessionRepo.touchSession(oSession);

			return oUser;
		}

		// Session not valid
		oSessionRepo.deleteSession(oSession);

		// No Session, No User
		return null;
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
		
		// Get the Ob Id
		String sProcessObjId = oParameter.getProcessObjId();
		
		PrimitiveResult oResult = new PrimitiveResult();

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
			
			if (Utils.isNullOrEmpty(sMyNodeCode)) sMyNodeCode = "wasdi";
					
			// Is the workspace here?
			if (!oWorkspace.getNodeCode().equals(sMyNodeCode)) {
				
				// No: forward the call on the owner node
				Utils.debugLog("Wasdi.runProcess: forewarding request to [" + oWorkspace.getNodeCode()+"]");
				
				// Get the Node
				NodeRepository oNodeRepository = new NodeRepository();
				wasdi.shared.business.Node oDestinationNode = oNodeRepository.getNodeByCode(oWorkspace.getNodeCode());
				
				if (oDestinationNode==null) {
					Utils.debugLog("Wasdi.runProcess: Node [" + oWorkspace.getNodeCode()+"] not found. Return 500");
					oResult.setBoolValue(false);
					oResult.setIntValue(500);
					return oResult;									
				}
				
				// Get the JSON of the parameter
				//String sPayload = MongoRepository.s_oMapper.writeValueAsString(oParameter);
				String sPayload = SerializationUtils.serializeObjectToStringXML(oParameter);

				// Get the URL of the destination Node
				String sUrl = oDestinationNode.getNodeBaseAddress();
				if (sUrl.endsWith("/") == false) sUrl += "/";
				sUrl += "processing/run?sOperation=" + sOperationId + "&sProductName=" + URLEncoder.encode(sProductName);
				
				// Is there a parent?
				if (!Utils.isNullOrEmpty(sParentId)) {
					sUrl += "&parent=" + URLEncoder.encode(sParentId);
				}
				
				Utils.debugLog("Wasdi.runProcess: URL: " + sUrl);
				Utils.debugLog("Wasdi.runProcess: PAYLOAD: " + sPayload);
				
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
					if (!Utils.isNullOrEmpty(sParentId)) oProcess.setParentId(sParentId);
					oProcess.setStatus(ProcessStatus.CREATED.name());
					oProcess.setNodeCode(oWorkspace.getNodeCode());
					oRepository.insertProcessWorkspace(oProcess);
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
	
	public static void httpPostFile(String sUrl, String sFileName, Map<String, String> asHeaders) 
	{
		if(sFileName==null || sFileName.isEmpty()){
			throw new NullPointerException("Wasdi.httpPostFile: file name is null");
		}
		try {
			//local file
			File oFile = new File(sFileName);
			
			if(!oFile.exists()) {
				throw new IOException("Wasdi.httpPostFile: file not found");
			}
			
			InputStream oInputStream = new FileInputStream(oFile);
			
		    //request
			URL oURL = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();
			oConnection.setDoOutput(true);
			oConnection.setDoInput(true);
			oConnection.setUseCaches(false);
			int iBufferSize = 8192;//8*1024*1024;
			oConnection.setChunkedStreamingMode(iBufferSize);
			Long lLen = oFile.length();
			Utils.debugLog("Wasdi.httpPostFile: file length is: "+Long.toString(lLen));
			
			if (asHeaders != null) {
				for (String sKey : asHeaders.keySet()) {
					oConnection.setRequestProperty(sKey,asHeaders.get(sKey));
				}
			}

			String sBoundary = "**WASDIlib**" + UUID.randomUUID().toString() + "**WASDIlib**";
			oConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + sBoundary);
			oConnection.setRequestProperty("Connection", "Keep-Alive");
			oConnection.setRequestProperty("User-Agent", "WasdiLib.Java");


			oConnection.connect();
			DataOutputStream oOutputStream = new DataOutputStream(oConnection.getOutputStream());

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
			oOutputStream.close();

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
				String sMessage = oByteArrayOutputStream.toString();
				System.out.println(sMessage);
			} else {
				throw new NullPointerException("WasdiLib.uploadFile: stream is null");
			}

			oOutputStream.close();
			oConnection.disconnect();

		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Download a file on the local PC
	 * @param sWorkflowId File Name
	 * @return Full Path
	 */
	public static String downloadWorkflow(String sNodeUrl, String sWorkflowId, String sSessionId, ServletConfig oServletConfig) {
		try {
			
			if (sWorkflowId == null) {
				System.out.println("sFileName must not be null");
			}

			if (sWorkflowId.equals("")) {
				System.out.println("sFileName must not be empty");
			}
			
			String sBaseUrl = sNodeUrl;
			
			if (Utils.isNullOrEmpty(sNodeUrl)) sBaseUrl = "http://www.wasdi.net/wasdiwebserver/rest";

		    String sUrl = sBaseUrl + "/processing/downloadgraph?workflowId="+sWorkflowId;
		    
		    String sOutputFilePath = "";
		    
		    HashMap<String, String> asHeaders = getStandardHeaders(sSessionId);
			
			try {
				URL oURL = new URL(sUrl);
				HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();

				// optional default is GET
				oConnection.setRequestMethod("GET");
				
				if (asHeaders != null) {
					for (String sKey : asHeaders.keySet()) {
						oConnection.setRequestProperty(sKey,asHeaders.get(sKey));
					}
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
						System.out.println(sAttachmentName);
						
					}
					
					String sSavePath = getDownloadPath(oServletConfig) + "workflows/";
					sOutputFilePath = sSavePath + sWorkflowId+".xml";
					
					File oTargetFile = new File(sOutputFilePath);
					File oTargetDir = oTargetFile.getParentFile();
					oTargetDir.mkdirs();

					// opens an output stream to save into file
					FileOutputStream oOutputStream = new FileOutputStream(sOutputFilePath);

					InputStream oInputStream = oConnection.getInputStream();

					Util.copyStream(oInputStream, oOutputStream);

					if(null!=oOutputStream) {
						oOutputStream.close();
					}
					if(null!=oInputStream) {
						oInputStream.close();
					}
					
					return sOutputFilePath;
				} else {
					String sMessage = oConnection.getResponseMessage();
					System.out.println(sMessage);
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
