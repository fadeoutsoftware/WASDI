package wasdi.jwasdilib;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
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
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.net.io.Util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import wasdi.jwasdilib.utils.MosaicSetting;

/**
 * @author c.nattero
 *
 */
public class WasdiLib {

	protected static ObjectMapper s_oMapper = new ObjectMapper();

	/**
	 * Wasdi User
	 */
	private String m_sUser = "";

	/**
	 * Wasdi Password
	 */
	private String m_sPassword = "";

	/**
	 * Wasdi Active Workspace
	 */
	private String m_sActiveWorkspace = "";

	/*
	 * Url associated with the workspace
	 */
	private String m_sWorkspaceBaseUrl = "";

	/**
	 * Wasdi Workspace Owner
	 */
	private String m_sWorkspaceOwner = "";	

	/**
	 * Wasdi Active Session
	 */
	private String m_sSessionId = "";

	/**
	 * Data Base Url
	 */
	private String m_sBaseUrl = "https://www.wasdi.net/wasdiwebserver/rest";

	/**
	 * Flag to know if we are on the real server
	 */
	private Boolean m_bIsOnServer = false;

	/**
	 * Flag to activate the automatic local download
	 */
	private Boolean m_bDownloadActive = true;

	/**
	 * Flag to activate the automatic upload of locally created files
	 */
	private Boolean m_bUploadActive = true;

	/**
	 * Base Folder Path
	 */
	private String m_sBasePath = "";

	/**
	 * Own Proc Id
	 */
	private String m_sMyProcId = "";

	/**
	 * Flag to set if the lib has to be verbose or not
	 */
	private Boolean m_bVerbose = true;

	/**
	 * Params dictionary
	 */
	private Map<String, String> m_aoParams = new HashMap<String, String>();

	/**
	 * Utility paramters reader
	 */
	private ParametersReader m_oParametersReader = null;

	/**
	 * Path of the Parameters file
	 */
	private String m_sParametersFilePath = "";

	/*
	 * Default data provider to be used for search and import operations 
	 */
	private String m_sDefaultProvider = "LSA";

	
	/**
	 * Self constructor. If there is a config file initilizes the class members
	 */
	public WasdiLib() {
		log("WasdiLib.WasdiLib()");
	}

	public String getDefaultProvider() {
		return m_sDefaultProvider;
	}

	public void setDefaultProvider(String sProvider) {
		log("WasdiLib.setDefaultProvider( " + sProvider + " )");
		if(sProvider==null || sProvider.isEmpty()) {
			System.out.println("WasdiLib.setDefaultProvider: the provider cannot be null or empty, aborting");
		} else {
			this.m_sDefaultProvider = sProvider;
		}
	}


	/**
	 * Get User 
	 * @return User
	 */
	public String getUser() {
		return m_sUser;
	}

	/**
	 * Set User
	 * @param sUser User
	 */
	public void setUser(String sUser) {
		log("WasdiLib.setUser( " + sUser + " )");
		this.m_sUser = sUser;
	}

	/**
	 * Get Password
	 * @return
	 */
	public String getPassword() {
		return m_sPassword;
	}

	/**
	 * Set Password
	 * @param sPassword
	 */
	public void setPassword(String sPassword) {
		log("WasdiLib.setPassword( ********** )");
		this.m_sPassword = sPassword;
	}

	/**
	 * Get Active Workspace
	 * @return
	 */
	public String getActiveWorkspace() {
		return m_sActiveWorkspace;
	}

	/**
	 * Set Active Workspace
	 * @param sNewActiveWorkspaceId
	 */
	public void setActiveWorkspace(String sNewActiveWorkspaceId) {
		log("WasdiLib.setActiveWorkspace( " + sNewActiveWorkspaceId + " )");
		this.m_sActiveWorkspace = sNewActiveWorkspaceId;

		if (m_sActiveWorkspace != null && !m_sActiveWorkspace.equals("")) {
			m_sWorkspaceOwner = getWorkspaceOwnerByWSId(sNewActiveWorkspaceId);
		}
	}

	/**
	 * Get Session Id
	 * @return
	 */
	public String getSessionId() {
		return m_sSessionId;
	}

	/**
	 * Set Session Id
	 * @param sSessionId
	 */
	public void setSessionId(String sSessionId) {
		log("WasdiLib.setSessionId( " + sSessionId + " )");
		this.m_sSessionId = sSessionId;
	}

	/**
	 * Get Base Url
	 * @return
	 */
	public String getBaseUrl() {
		return m_sBaseUrl;
	}

	/**
	 * Set Base URl
	 * @param sBaseUrl
	 */
	public void setBaseUrl(String sBaseUrl) {
		log("WasdiLib.setBaseUrl( " + sBaseUrl + " )");
		this.m_sBaseUrl = sBaseUrl;
	}

	/**
	 * Get is on server flag
	 * @return
	 */
	public Boolean getIsOnServer() {
		return m_bIsOnServer;
	}

	/**
	 * Set is on server flag
	 * @param bIsOnServer
	 */
	public void setIsOnServer(Boolean bIsOnServer) {
		log("WasdiLib.setIsOnServer( " + bIsOnServer + " )");
		this.m_bIsOnServer = bIsOnServer;
	}

	/**
	 * Get Download Active flag
	 * @return
	 */
	public Boolean getDownloadActive() {
		return m_bDownloadActive;
	}

	/**
	 * Set Download Active Flag
	 * @param bDownloadActive
	 */
	public void setDownloadActive(Boolean bDownloadActive) {
		log("WasdiLib.setDownloadActive( " + bDownloadActive + " )");
		this.m_bDownloadActive = bDownloadActive;
	}

	public Boolean getUploadActive() {
		return m_bUploadActive;
	}

	public void setUploadActive(Boolean bUploadActive) {
		log("WasdiLib.setUploadActive( " + bUploadActive + " )");
		this.m_bUploadActive = bUploadActive;
	}

	/**
	 * Set Base Path
	 * @return
	 */
	public String getBasePath() {
		return m_sBasePath;
	}

	/**
	 * Get Base Path
	 * @param sBasePath
	 */
	public void setBasePath(String sBasePath) {
		log("WasdiLib.setBasePath ( " + sBasePath + " )");
		this.m_sBasePath = sBasePath;
	}

	/**
	 * Get my own Process Id
	 * @return
	 */
	public String getMyProcId() {
		return m_sMyProcId;
	}

	/**
	 * Set My own process ID
	 * @param m_sMyProcId
	 */
	public void setMyProcId(String sMyProcId) {
		log("WasdiLib.setMyProcId( " + sMyProcId + " )");
		this.m_sMyProcId = sMyProcId;
	}

	/**
	 * Get Verbose Flag
	 * @return
	 */
	public Boolean getVerbose() {
		return m_bVerbose;
	}

	/**
	 * Set Verbose flag
	 * @param bVerbose
	 */
	public void setVerbose(Boolean bVerbose) {
		log("WasdiLib.setVerbose( " + bVerbose +" )" );
		this.m_bVerbose = bVerbose;
	}

	/**
	 * Get Params HashMap
	 * @return Params Dictionary
	 */
	public Map<String, String> getParams() {
		return m_aoParams;
	}

	/**
	 * Add Param
	 * @param sKey
	 * @param sParam
	 */
	public void addParam(String sKey, String sParam) {
		log("WasdiLib.addParam( " + sKey + ", " + sParam + " )");
		m_aoParams.put(sKey, sParam);
	}

	/**
	 * Get Param
	 * @param sKey
	 * @return
	 */
	public String getParam(String sKey) {
		if (m_aoParams.containsKey(sKey))
			return m_aoParams.get(sKey);
		return "";
	}

	/**
	 * Log
	 * @param sLog Log row
	 */
	protected void log(String sLog) {
		if (m_bVerbose == false) return;

		System.out.println(sLog);
	}

	/**
	 * Get Parameters file path
	 * @return parameters file path
	 */
	public String getParametersFilePath() {
		return m_sParametersFilePath;
	}

	/**
	 * Set Parameters file path
	 * @param sParametersFilePath parameters file path
	 */
	public void setParametersFilePath(String sParametersFilePath) {
		log("WasdiLib.setParametersFilePath( " + sParametersFilePath + " )");
		this.m_sParametersFilePath = sParametersFilePath;
	}


	/**
	 * Init the WASDI Library starting from a configuration file
	 * @param sConfigFilePath full path of the configuration file
	 * @return True if the system is initialized, False if there is any error
	 */
	public Boolean init(String sConfigFilePath) {
		log("WasdiLib.init( " + sConfigFilePath + " )");
		try {

			if (sConfigFilePath != null) {
				if (!sConfigFilePath.equals("")) {
					File oConfigFile = new File(sConfigFilePath);
					if (oConfigFile.exists()) {
						ConfigReader.setConfigFilePath(sConfigFilePath);
					}
				}
			}

			log("Config File Path " + sConfigFilePath);

			m_sUser = ConfigReader.getPropValue("USER", "");
			m_sPassword = ConfigReader.getPropValue("PASSWORD", "");
			m_sBasePath = ConfigReader.getPropValue("BASEPATH", "");
			m_sBaseUrl = ConfigReader.getPropValue("BASEURL", "https://www.wasdi.net/wasdiwebserver/rest");
			m_sSessionId = ConfigReader.getPropValue("SESSIONID","");
			m_sActiveWorkspace = ConfigReader.getPropValue("WORKSPACEID","");
			m_sMyProcId = ConfigReader.getPropValue("MYPROCID","");
			m_sParametersFilePath = ConfigReader.getPropValue("PARAMETERSFILEPATH","./parameters.txt");
			String sVerbose = ConfigReader.getPropValue("VERBOSE","");
			if (sVerbose.equals("1") || sVerbose.toUpperCase().equals("TRUE")) {
				m_bVerbose = true;
			}

			log("SessionId from config " + m_sSessionId);

			String sDownloadActive = ConfigReader.getPropValue("DOWNLOADACTIVE", "1");

			if (sDownloadActive.equals("0") || sDownloadActive.toUpperCase().equals("FALSE")) {
				m_bDownloadActive = false;
			}

			String sUploadactive = ConfigReader.getPropValue("UPLOADACTIVE", "1");

			if (sUploadactive.equals("0") || sUploadactive.toUpperCase().equals("FALSE")) {
				setUploadActive(false);
			}

			String sIsOnServer = ConfigReader.getPropValue("ISONSERVER", "0");

			if (sIsOnServer.equals("1") || sIsOnServer.toUpperCase().equals("TRUE")) {
				m_bIsOnServer = true;
				// On Server Force Download to false
				m_bDownloadActive = false;
				m_bVerbose = true;
			} else {
				m_bIsOnServer = false;
			}

			if (m_sBasePath.equals("")) {

				if (!m_bIsOnServer ) {
					String sUserHome = System.getProperty("user.home");
					String sWasdiHome = sUserHome + "/.wasdi/";

					File oFolder = new File(sWasdiHome);
					oFolder.mkdirs();

					m_sBasePath = sWasdiHome;					
				}
				else {
					m_sBasePath = "/data/wasdi/";
				}
			}

			// Read the parameters file
			m_oParametersReader = new ParametersReader(m_sParametersFilePath);
			m_aoParams = m_oParametersReader.getParameters();

			if (internalInit()) {

				if (m_sActiveWorkspace == null || m_sActiveWorkspace.equals("")) {

					String sWorkspaceName = ConfigReader.getPropValue("WORKSPACE","");

					log("Workspace to open: " + sWorkspaceName);

					if (!sWorkspaceName.equals("")) {
						openWorkspace(sWorkspaceName);						
					}
				}
				else {
					openWorkspaceById(m_sActiveWorkspace);					
					log("Active workspace set " + m_sActiveWorkspace);
				}

				return true;
			}
			else {
				return false;
			}

		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public Boolean init() {
		return init(null);
	}

	/**
	 * Call this after base parameters settings to init the system
	 * Needed at least:
	 * Base Path
	 * User
	 * Password or SessionId
	 * @return
	 */
	public Boolean internalInit() {
		log("WasdiLib.internalInit");
		try {

			log("jWASDILib Init");

			// User Name Needed 
			if (m_sUser == null) return false;

			log("User not null " + m_sUser);

			// Is there a password?
			if (m_sPassword != null && !m_sPassword.equals("")) {

				log("Password not null. Try to login");

				// Try to log in
				String sResponse = login(m_sUser, m_sPassword);

				// Get JSON
				Map<String, Object> aoJSONMap = s_oMapper.readValue(sResponse, new TypeReference<Map<String,Object>>(){});

				if (aoJSONMap == null) return false;

				if (aoJSONMap.containsKey("sessionId")) {
					// Got Session
					m_sSessionId = (String) aoJSONMap.get("sessionId");

					log("User logged: session ID " + m_sSessionId);

					return true;
				}				
			}
			else if (m_sSessionId != null) {

				log("Check Session: session ID " + m_sSessionId);

				// Check User supplied Session
				String sResponse = checkSession(m_sSessionId);

				// Get JSON
				Map<String, Object> aoJSONMap = s_oMapper.readValue(sResponse, new TypeReference<Map<String,Object>>(){});
				if (aoJSONMap == null) {
					log("Check Session: session ID not valid");
					return false;
				}

				// Check if session and user id are the same
				if (aoJSONMap.containsKey("userId")) {
					if (((String)aoJSONMap.get("userId")).equals(m_sUser)) {
						log("Check Session: session ID OK");
						return true;
					}
					else {
						log("Check Session: session Wrong User " + ((String)aoJSONMap.get("userId")));
					}
				}

				log("Session invalid or not of the user ");
			}


			return false;

		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return false;
		}
	}

	/**
	 * Call CheckSession API
	 * @param sSessionID actual session Id
	 * @return Session Id or "" if there are problems
	 */
	public String checkSession(String sSessionID) {
		try {

			String sUrl = m_sBaseUrl + "/auth/checksession";

			Map<String, String> aoHeaders = new HashMap<String, String>();
			aoHeaders.put("x-session-token", sSessionID);
			aoHeaders.put("Content-Type", "application/json");

			String sResult = httpGet(sUrl, aoHeaders);

			return sResult;

		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}
	}

	/**
	 * Call Login API
	 * @param sUser
	 * @param sPassword
	 * @return
	 */
	public String login(String sUser, String sPassword) {
		try {
			String sUrl = m_sBaseUrl + "/auth/login";

			String sPayload = "{\"userId\":\"" + m_sUser + "\",\"userPassword\":\"" + m_sPassword + "\" }";

			Map<String, String> aoHeaders = new HashMap<String, String>();

			aoHeaders.put("Content-Type", "application/json");

			String sResult = httpPost(sUrl, sPayload, aoHeaders);

			return sResult;

		} catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}
	}

	/**
	 * Get the standard headers for a WASDI call
	 * @return
	 */
	protected Map<String, String> getStandardHeaders() {
		Map<String, String> aoHeaders = new HashMap<String, String>();
		aoHeaders.put("x-session-token", m_sSessionId);
		aoHeaders.put("Content-Type", "application/json");

		return aoHeaders;
	}

	/**
	 * Get the headers for a Streming POST call
	 * @return
	 */
	protected Map<String, String> getStreamingHeaders() {
		Map<String, String> aoHeaders = new HashMap<String, String>();
		aoHeaders.put("x-session-token", m_sSessionId);
		aoHeaders.put("Content-Type", "multipart/form-data");

		return aoHeaders;
	}

	/**
	 * get the list of workspaces of the logged user
	 * @return List of Workspace as JSON representation
	 */
	public List<Map<String, Object>> getWorkspaces() {

		try {
			String sUrl = m_sBaseUrl + "/ws/byuser";

			String sResponse = httpGet(sUrl, getStandardHeaders());
			List<Map<String, Object>> aoJSONMap = s_oMapper.readValue(sResponse, new TypeReference<List<Map<String,Object>>>(){});

			return aoJSONMap;			
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return null;
		}	    
	}

	/**
	 * Get Id of a Workspace from the name
	 * Return the WorkspaceId as a String, "" if there is any error
	 * @param sWorkspaceName Workspace Name
	 * @return Workspace Id if found, "" if there is any error
	 */
	public String getWorkspaceIdByName(String sWorkspaceName) {
		try {
			String sUrl = m_sBaseUrl + "/ws/byuser";

			// Get all the Workspaces
			String sResponse = httpGet(sUrl, getStandardHeaders());
			List<Map<String, Object>> aoJSONMap = s_oMapper.readValue(sResponse, new TypeReference<List<Map<String,Object>>>(){});

			// Search the one by name
			for (Map<String, Object> oWorkspace : aoJSONMap) {
				if (oWorkspace.get("workspaceName").toString().equals(sWorkspaceName)) {
					// Found
					return (String) oWorkspace.get("workspaceId").toString();
				}
			}

			return "";
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}
	}

	/**
	 * Get User Id of the owner of a Workspace from the name
	 * Return the userId as a String, "" if there is any error
	 * @param sWorkspaceName Workspace Name
	 * @return User Id if found, "" if there is any error
	 */
	public String getWorkspaceOwnerByName(String sWorkspaceName) {
		log("WasdiLib.getWorkspaceOwnerByName( " + sWorkspaceName + " )");
		try {
			String sUrl = m_sBaseUrl + "/ws/byuser";

			// Get all the Workspaces
			String sResponse = httpGet(sUrl, getStandardHeaders());
			List<Map<String, Object>> aoJSONMap = s_oMapper.readValue(sResponse, new TypeReference<List<Map<String,Object>>>(){});

			// Search the one by name
			for (Map<String, Object> oWorkspace : aoJSONMap) {
				if (oWorkspace.get("workspaceName").toString().equals(sWorkspaceName)) {
					// Found
					return (String) oWorkspace.get("ownerUserId").toString();
				}
			}

			return "";
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}
	}

	/**
	 * Get userId of the owner of a Workspace from the workspace Id
	 * Return the userId as a String, "" if there is any error
	 * @param WorkspaceId Workspace Id
	 * @return userId if found, "" if there is any error
	 */
	public String getWorkspaceOwnerByWSId(String sWorkspaceId) {
		log("WasdiLib.getWorkspaceOwnerByWSId( " + sWorkspaceId + " )");
		try {
			String sUrl = m_sBaseUrl + "/ws/byuser";

			// Get all the Workspaces
			String sResponse = httpGet(sUrl, getStandardHeaders());
			List<Map<String, Object>> aoJSONMap = s_oMapper.readValue(sResponse, new TypeReference<List<Map<String,Object>>>(){});

			// Search the one by name
			for (Map<String, Object> oWorkspace : aoJSONMap) {
				if (oWorkspace.get("workspaceId").toString().equals(sWorkspaceId)) {
					// Found
					return (String) oWorkspace.get("ownerUserId").toString();
				}
			}

			return "";
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}
	}

	/**
	 * Get the URL of a workspace from the workspace Id
	 * @param sWorkspaceId Id Of the Workspace
	 * @return Url of the distribuited node of the Workspace
	 */
	public String getWorkspaceUrlByWsId(String sWorkspaceId) {
		log("WasdiLib.getWorkspaceUrlByWsId( " + sWorkspaceId + " )");
		try {
			String sUrl = m_sBaseUrl + "/ws?sWorkspaceId=" + sWorkspaceId;

			// Get all the Workspaces
			String sResponse = httpGet(sUrl, getStandardHeaders());
			Map<String, Object> oJSONMap = s_oMapper.readValue(sResponse, new TypeReference<Map<String,Object>>(){});

			return (String) oJSONMap.get("apiUrl");
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}		
	}

	
	/**
	 * Opens a workspace given its ID
	 * @param sWorkspaceId the ID of the workspace
	 * @return the workspace ID if opened successfully, empty string otherwise
	 */
	public String openWorkspaceById(String sWorkspaceId) {
		log("WasdiLib.openWorkspaceById( " + sWorkspaceId + " )");
		if(null==sWorkspaceId || sWorkspaceId.isEmpty()) {
			log("WasdiLib.openWorkspaceById: invalid workspace ID, aborting");
			return "";
		}
		setActiveWorkspace(sWorkspaceId);
		
		m_sWorkspaceOwner = getWorkspaceOwnerByWSId(sWorkspaceId);
		setWorkspaceBaseUrl(getWorkspaceUrlByWsId(m_sActiveWorkspace));

		if (m_sWorkspaceBaseUrl == null) {
			setWorkspaceBaseUrl("");
		}

		if (m_sWorkspaceBaseUrl.isEmpty()) {
			m_sWorkspaceBaseUrl = m_sBaseUrl;
		}

		return m_sActiveWorkspace;

	}

	/**
	 * Open a workspace
	 * @param sWorkspaceName Workspace name to open
	 * @return WorkspaceId as a String, empty string if there is any error
	 */
	public String openWorkspace(String sWorkspaceName) {

		log("Open Workspace " + sWorkspaceName);
		return openWorkspaceById(getWorkspaceIdByName(sWorkspaceName));
	}

	/**
	 * Get a List of the products in a Workspace
	 * @param sWorkspaceName Workspace Name
	 * @return List of Strings representing the product names
	 */
	public List <String> getProductsByWorkspace(String sWorkspaceName) {
		return getProductsByWorkspaceId(getWorkspaceIdByName(sWorkspaceName));
	}

	/**
	 * Get a List of the products in a Workspace
	 * @param sWorkspaceId Workspace ID
	 * @return List of Strings representing the product names
	 */
	public List <String> getProductsByWorkspaceId(String sWorkspaceId) {
		log("WasdiLib.getProductsByWorkspaceId( " + sWorkspaceId + " )");
		List<String> asProducts = new ArrayList<String>();
		try {

			String sUrl = m_sBaseUrl + "/product/byws?sWorkspaceId=" + sWorkspaceId;

			String sResponse = httpGet(sUrl, getStandardHeaders());
			List<Map<String, Object>> aoJSONMap = s_oMapper.readValue(sResponse, new TypeReference<List<Map<String,Object>>>(){});

			for (Map<String, Object> oProduct : aoJSONMap) {
				asProducts.add(oProduct.get("fileName").toString());
			}

			return asProducts;
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return asProducts;
		}
	} 

	/**
	 * Get a List of the products in the active Workspace
	 * @return List of Strings representing the product names
	 */
	public List <String> getProductsByActiveWorkspace() {

		List<String> asProducts = new ArrayList<String>();
		try {

			String sUrl = m_sBaseUrl + "/product/byws?sWorkspaceId=" + m_sActiveWorkspace;

			String sResponse = httpGet(sUrl, getStandardHeaders());
			List<Map<String, Object>> aoJSONMap = s_oMapper.readValue(sResponse, new TypeReference<List<Map<String,Object>>>(){});

			for (Map<String, Object> oProduct : aoJSONMap) {
				asProducts.add(oProduct.get("fileName").toString());
			}

			return asProducts;
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return asProducts;
		}
	}

	/**
	 * Get the local path of a file
	 * @param sProductName Name of the file
	 * @return Full local path
	 */
	public String getPath(String sProductName) {
		if (fileExistsOnWasdi(sProductName)) {
			return getFullProductPath(sProductName);
		}
		else {
			return getSavePath() + sProductName;
		}
	}

	/**
	 * Get the full local path of a product given the product name. Use the output of this API to open the file
	 * @param sProductName Product Name
	 * @return Product Full Path as a String ready to open file
	 */
	public String getFullProductPath(String sProductName) {
		log("WasdiLib.getFullProductPath( " + sProductName + " )");
		try {
			String sFullPath = m_sBasePath;

			if (! (sFullPath.endsWith("\\") || sFullPath.endsWith("/") ) ){
				sFullPath +=File.separator;
			}

			sFullPath = sFullPath +m_sWorkspaceOwner + File.separator + m_sActiveWorkspace + File.separator + sProductName;
			File oFile = new File(sFullPath);
			Boolean bFileExists = oFile.exists();

			if (m_bIsOnServer==false) {
				if (m_bDownloadActive && !bFileExists) {
					System.out.println("Local file Missing. Start WASDI download. Please wait");
					downloadFile(sProductName);
					System.out.println("File Downloaded on Local PC, keep on working!");
				}
			}
			else {
				if (!bFileExists) {
					if (fileExistsOnWasdi(sProductName)) {
						System.out.println("Local file Missing. Start WASDI download. Please wait");
						downloadFile(sProductName);
						System.out.println("File Downloaded on Local Node, keep on working!");						
					}
				}

			}

			return sFullPath;
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}
	}

	private boolean fileExistsOnWasdi(String sFileName) {
		log("WasdiLib.fileExistsOnWasdi( " + sFileName + " )");
		if(null==sFileName) {
			throw new NullPointerException("WasdiLib.fileExistssOnWasdi: passed a null file name");
		}
		int iResult = 200;
		try{
			String sUrl = getWorkspaceBaseUrl() + "/catalog/checkdownloadavaialibitybyname?token=";
			sUrl += m_sSessionId;
			sUrl += "&filename=";
			sUrl += sFileName;
			sUrl += "&workspace=";
			sUrl += m_sActiveWorkspace;

			URL oURL;
			oURL = new URL(sUrl);
			HttpURLConnection oConnection;
			oConnection = (HttpURLConnection) oURL.openConnection();
			oConnection.setRequestMethod("GET");
			oConnection.connect();
			iResult =  oConnection.getResponseCode();

			InputStream oInputStream = null;
			if(200 <= iResult && 299 >= iResult) {
				oInputStream = oConnection.getInputStream();
			} else {
				oInputStream = oConnection.getErrorStream();
			}
			ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
			if(null!=oInputStream) {
				Util.copyStream(oInputStream, oBytearrayOutputStream);
				String sMessage = oBytearrayOutputStream.toString();
				System.out.println(sMessage);
			}

			if(200 == iResult) {
				return true;
			}

		} catch(MalformedURLException e) {
			e.printStackTrace();
		} catch (ProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch(Exception e) {
			e.printStackTrace();
		}
		//false, because in general no upload is desirable 
		return false; 

	}

	/**
	 * Check if a file exists on a node
	 * NOTE: TO BE TESTED YET
	 * @param sFileName
	 * @return
	 */
	private boolean fileExistsOnNode(String sFileName) {
		if(null==sFileName) {
			throw new NullPointerException("WasdiLib.fileExistsOnNode: passed a null file name");
		}
		int iResult = 200;
		try{
			String sMessage = "";
			String sUrl = getWorkspaceBaseUrl() + "/catalog/fileOnNode?token=";
			sUrl += m_sSessionId;
			sUrl += "&filename=";
			sUrl += sFileName;
			sUrl += "&workspace=";
			sUrl += m_sActiveWorkspace;

			URL oURL;
			oURL = new URL(sUrl);
			HttpURLConnection oConnection;
			oConnection = (HttpURLConnection) oURL.openConnection();
			oConnection.setRequestMethod("GET");
			oConnection.connect();
			iResult =  oConnection.getResponseCode();

			InputStream oInputStream = null;
			if(200 <= iResult && 299 >= iResult) {
				oInputStream = oConnection.getInputStream();
			} else {
				oInputStream = oConnection.getErrorStream();
			}
			ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
			if(null!=oInputStream) {
				Util.copyStream(oInputStream, oBytearrayOutputStream);
				sMessage = oBytearrayOutputStream.toString();
				System.out.println(sMessage);				
			}

			if(200 == iResult) {

				Map<String, Object> oJSONMap = s_oMapper.readValue(sMessage, new TypeReference<Map<String,Object>>(){});

				if (oJSONMap != null) {
					if (oJSONMap.containsKey("boolValue")) {
						return (boolean) oJSONMap.get("boolValue");
					}
				}

				return false;
			}

		} catch(MalformedURLException e) {
			e.printStackTrace();
		} catch (ProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch(Exception e) {
			e.printStackTrace();
		}
		//false, because in general no upload is desirable 
		return false; 

	}

	/**
	 * Get the local Save Path to use to save custom generated files
	 * @return Local Path to use to save a custom generated file
	 */
	public String getSavePath() {
		try {
			String sFullPath = m_sBasePath;

			if (! (sFullPath.endsWith("\\") || sFullPath.endsWith("/") || !sFullPath.endsWith(File.separator)) ){
				sFullPath += File.separator;
			}

			sFullPath = sFullPath +m_sWorkspaceOwner + File.separator + m_sActiveWorkspace + File.separator;

			return sFullPath;
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}
	}



	/**
	 *   Get the list of Workflows for the user
	 *   Return None if there is any error
	 *   Return an array of WASI Workspace JSON Objects if everything is ok:
	 *   
	 *   {
	 *       "description":STRING,
	 *       "name": STRING,
	 *       "workflowId": STRING
	 *   }        
	 *   
	 * @return
	 */
	public List<Map<String, Object>> getWorkflows() {

		try {
			String sUrl = m_sBaseUrl + "/processing/getgraphsbyusr";

			String sResponse = httpGet(sUrl, getStandardHeaders());
			List<Map<String, Object>> aoJSONMap = s_oMapper.readValue(sResponse, new TypeReference<List<Map<String,Object>>>(){});

			return aoJSONMap;			
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return null;
		}	    
	}


	/**
	 * Internal execute workflow
	 * @param asInputFileNames
	 * @param asOutputFileNames
	 * @param sWorkflowName
	 * @param bAsynch true if asynch, false for synch
	 * @return if Asynch, the process Id else the ouput status of the workflow process
	 */
	protected String internalExecuteWorkflow(String [] asInputFileNames, String []  asOutputFileNames, String sWorkflowName, Boolean bAsynch) {
		try {

			String sProcessId = "";
			String sWorkflowId = "";
			String sUrl = m_sBaseUrl + "/processing/graph_id?workspace=" + m_sActiveWorkspace;

			List<Map<String,Object>> aoWorkflows = getWorkflows();

			for (Map<String, Object> oWorkflow : aoWorkflows) {
				if (oWorkflow.get("name").equals(sWorkflowName)) {
					sWorkflowId = oWorkflow.get("workflowId").toString();
					break;
				}
			}

			if (sWorkflowId.equals("")) return "";

			String sPayload = "{\"name\":\""+ sWorkflowName+"\", \"description\":\"\",\"workflowId\":\"" + sWorkflowId+"\"";
			sPayload += ", \"inputNodeNames\": []";
			sPayload += ", \"inputFileNames\": [";
			for (int i=0; i<asInputFileNames.length; i++) {
				if (i>0) sPayload += ",";
				sPayload += "\""+asInputFileNames[i]+"\"";
			}
			sPayload += "]";
			sPayload += ", \"outputNodeNames\":[]";
			sPayload += ", \"outputFileNames\":[";
			for (int i=0; i<asOutputFileNames.length; i++) {
				if (i>0) sPayload += ",";
				sPayload += "\""+asOutputFileNames[i]+"\"";
			}
			sPayload += "]";
			sPayload += "}";

			String sResponse = httpPost(sUrl,sPayload, getStandardHeaders());
			Map<String, Object> aoJSONMap = s_oMapper.readValue(sResponse, new TypeReference<Map<String,Object>>(){});

			sProcessId = aoJSONMap.get("stringValue").toString(); 

			if (bAsynch) return sProcessId;
			else return waitProcess(sProcessId);		
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}	 		
	}

	/**
	 * Executes a WASDI SNAP Workflow in a asynch mode
	 * @param sInputFileName
	 * @param sOutputFileName
	 * @param sWorkflowName
	 * @return Workflow Process Id if every thing is ok, '' if there was any problem
	 */
	public String asynchExecuteWorkflow(String [] asInputFileName, String []  asOutputFileName, String sWorkflowName) {
		return internalExecuteWorkflow(asInputFileName, asOutputFileName, sWorkflowName, true);
	}

	/**
	 * Executes a WASDI SNAP Workflow waiting for the process to finish
	 * @param sInputFileName 
	 * @param sOutputFileName
	 * @param sWorkflowName
	 * @return output status of the Workflow Process
	 */
	public String executeWorkflow(String []  asInputFileName, String [] asOutputFileName, String sWorkflowName) {
		return internalExecuteWorkflow(asInputFileName, asOutputFileName, sWorkflowName, false);
	}

	/**
	 * Get WASDI Process Status 
	 * @param sProcessId Process Id
	 * @return  Process Status as a String: CREATED,  RUNNING,  STOPPED,  DONE,  ERROR, WAITING, READY
	 */
	public String getProcessStatus(String sProcessId) {
		log("WasdiLib.getProcessStatus( " + sProcessId + " )");
		if(null==sProcessId || sProcessId.isEmpty()) {
			log("WasdiLib.getProcessStatus: process id null or empty, aborting");
		}
		try {

			String sUrl = getWorkspaceBaseUrl() + "/process/byid?sProcessId="+sProcessId;

			String sResponse = httpGet(sUrl, getStandardHeaders());
			Map<String, Object> aoJSONMap = s_oMapper.readValue(sResponse, new TypeReference<Map<String,Object>>(){});

			String sStatus = aoJSONMap.get("status").toString();
			if(isThisAValidStatus(sStatus)) {
				return sStatus;
			}
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
		}	  
		return "";
	}

	/**
	 * Get the status of a List of WASDI processes 
	 * @param sProcessId Process Id
	 * @return  Process Status as a String: CREATED,  RUNNING,  STOPPED,  DONE,  ERROR, WAITING, READY
	 */
	public String getProcessesStatus(List<String> asIds) {
		try {

			String sIds = "[";
			for (String sId : asIds) {
				sIds += "\"" + sId + "\"" + ",";
			}
			//remove last comma
			sIds = sIds.substring(0, sIds.length()-1);
			sIds += "]";

			String sUrl = getWorkspaceBaseUrl() + "/process/statusbyid";		    
			String sResponse = httpPost(sUrl, sIds, getStandardHeaders());

			return sResponse;			
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}	  
	}

	/**
	 * Get the status of a List of WASDI processes 
	 * @param sProcessId Process Id
	 * @return  Process Status as a String: CREATED,  RUNNING,  STOPPED,  DONE,  ERROR, WAITING, READY
	 */
	public List<String> getProcessesStatusAsList(List<String> asIds) {
		//List<String> asStatus =
		String sStatus = getProcessesStatus(asIds);


		sStatus = sStatus
				//get rid of opening and trailing square brackets 
				.substring(1, sStatus.length()-1)
				//"DONE" -> DONE
				.replaceAll("\"", "");

		return new ArrayList<String>(Arrays.asList(sStatus.split(",")));
	}

	/**
	 *  Update the status of the current process
	 * @param sStatus Status to set
	 * @param iPerc Progress in %
	 * @return updated status as a String or '' if there was any problem
	 */
	public String updateStatus(String sStatus) {

		if(!isThisAValidStatus(sStatus)) {
			return "";
		}
		if (m_bIsOnServer == false) return sStatus;

		return updateStatus(sStatus,-1);
	}

	/**
	 *  Update the status of the current process
	 * @param sStatus Status to set
	 * @param iPerc Progress in %
	 * @return updated status as a String or '' if there was any problem
	 */
	public String updateStatus(String sStatus,int iPerc) {

		if(!isThisAValidStatus(sStatus)) {
			return "";
		}
		if (m_bIsOnServer == false) return sStatus;

		return updateProcessStatus(getMyProcId(),sStatus,iPerc);
	}

	/**
	 *  Update the status of a process
	 * @param sProcessId Process Id
	 * @param sStatus Status to set
	 * @param iPerc Progress in %
	 * @return updated status as a String or '' if there was any problem
	 */
	public String updateProcessStatus(String sProcessId,String sStatus,int iPerc) {
		try {
			// iPerc not controlled: can be -1 to "not update"

			if(!isThisAValidStatus(sStatus)) {
				log("WasdiLib.updateProcessStatus: " + sStatus + " is not a valid status. It must be one of  CREATED,  RUNNING,  STOPPED,  DONE,  ERROR, WAITING, READY");
				return "";
			}

			if (sProcessId == null) {
				System.out.println("sProcessId must not be null");
			}

			if (sProcessId.equals("")) {
				System.out.println("sProcessId must not be empty");
			}

			String sUrl = getWorkspaceBaseUrl() + "/process/updatebyid?sProcessId="+sProcessId+"&status="+sStatus+"&perc="+iPerc + "&sendrabbit=1";

			String sResponse = httpGet(sUrl, getStandardHeaders());
			Map<String, Object> aoJSONMap = s_oMapper.readValue(sResponse, new TypeReference<Map<String,Object>>(){});

			return aoJSONMap.get("status").toString();			
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}
	}

	/**
	 *  Update the status of a process
	 * @param iPerc Progress in %
	 * @return updated status as a String or '' if there was any problem
	 */
	public String updateProgressPerc(int iPerc) {

		if (m_bIsOnServer == false) return "RUNNING";

		try {

			if (iPerc<0 || iPerc>100) {
				System.out.println("Percentage must be between 0 and 100 included");
				return "";
			}

			if (m_sMyProcId == null) {
				System.out.println("Own process Id net available");
			}

			if (m_sMyProcId.equals("")) {
				System.out.println("Progress: " + iPerc);
			}

			String sStatus = "RUNNING";

			String sUrl = getWorkspaceBaseUrl() + "/process/updatebyid?sProcessId="+m_sMyProcId+"&status="+sStatus+"&perc="+iPerc + "&sendrabbit=1";

			String sResponse = httpGet(sUrl, getStandardHeaders());
			Map<String, Object> aoJSONMap = s_oMapper.readValue(sResponse, new TypeReference<Map<String,Object>>(){});

			return aoJSONMap.get("status").toString();			
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}
	}

	/**
	 * Wait for a process to finish
	 * @param sProcessId
	 * @return the process status
	 */
	public String waitProcess(String sProcessId) {
		if(null==sProcessId || sProcessId.isEmpty()) {
			log("WasdiLib.waitProcess: sProcessId is null or empty");
		}

		updateStatus("WAITING");

		String sStatus = "";
		while ( ! (sStatus.equals("DONE") || sStatus.equals("STOPPED") || sStatus.equals("ERROR"))) {
			sStatus = getProcessStatus(sProcessId);
			if(!isThisAValidStatus(sStatus)) {
				return "";
			}
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		waitForResume();

		return sStatus;
	}


	private boolean isThisAValidStatus(String sStatus) {
		return(null!=sStatus &&(
				sStatus.equals("CREATED") ||
				sStatus.equals("RUNNING") ||
				sStatus.equals("DONE") ||
				sStatus.equals("STOPPED") ||
				sStatus.equals("ERROR") ||
				sStatus.equals("WAITING") ||
				sStatus.equals("READY")
				));
	}

	/**
	 * Wait for a collection of processes to finish
	 * @param sProcessId
	 * @return
	 */
	public List<String> waitProcesses(List<String> asIds) {
		updateStatus("WAITING");
		
		boolean bDone = false;
		while(!bDone) {
			bDone = true;	

			List<String> asStatus = getProcessesStatusAsList(asIds);

			for (String sStatus : asStatus) {
				if( !(sStatus.equals("DONE") || sStatus.equals("STOPPED") || sStatus.equals("ERROR")) ) {
					bDone = false;
					//we should break now and check their new status
					break;
				}
			}
			if(!bDone) {
				//then at least one needs to be waited for
				try {
					log("waitProcesses: sleep");
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}		
		waitForResume();

		return getProcessesStatusAsList(asIds);
	}

	/**
	 * Wait for a process to finish
	 * @param sProcessId
	 * @return
	 */
	protected void waitForResume() {

		if (!m_bIsOnServer) return;

		String sStatus = "READY";
		updateStatus(sStatus);

		while ( ! (sStatus.equals("RUNNING"))) {
			sStatus = getProcessStatus(getMyProcId());
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	
	/**
	 * Sets the payload of current process 
	 * @param sData the payload as a String. JSON format recommended  
	 */
	public void setPayload(String sData) {
		if(null==sData || sData.isEmpty()) {
			log("setPayload: null or empty payload, aborting");
			return;
		}
		if(getIsOnServer()) {
			setProcessPayload(getMyProcId(), sData);
		} else {
			log("setPayload: " + sData);
		}
	}
	
	/**
	 * Adds output payload to a process
	 * @param sProcessId
	 * @param sData
	 * @return
	 */
	public String setProcessPayload(String sProcessId,String sData) {
		try {

			if (sProcessId == null) {
				System.out.println("sProcessId must not be null");
			}

			if (sProcessId.equals("")) {
				System.out.println("sProcessId must not be empty");
			}

			if (!m_bIsOnServer) return "RUNNING";

			String sUrl = getWorkspaceBaseUrl() + "/process/setpayload?sProcessId="+sProcessId+"&payload="+sData;

			String sResponse = httpGet(sUrl, getStandardHeaders());
			Map<String, Object> aoJSONMap = s_oMapper.readValue(sResponse, new TypeReference<Map<String,Object>>(){});

			return aoJSONMap.get("status").toString();			
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}
	}


	/**
	 * Refresh Parameters reading again the file
	 */
	public void refreshParameters() {
		m_oParametersReader.refresh();
	}
	
	
	/**
	 * Private version of the add file to wasdi function.
	 * Adds a generated file to current open workspace
	 * @param sFileName File Name to add to the open workspace
	 * @param bAsynch true if the process has to be asynch, false to wait for the result
	 * @return
	 */
	protected String internalAddFileToWASDI(String sFileName, Boolean bAsynch) {
		return internalAddFileToWASDI(sFileName, bAsynch, null);
	}

	/**
	 * Private version of the add file to wasdi function.
	 * Adds a generated file to current open workspace
	 * @param sFileName File Name to add to the open workspace
	 * @param bAsynch true if the process has to be asynch, false to wait for the result
	 * @param sStyle name of a valid WMS style
	 * @return
	 */
	protected String internalAddFileToWASDI(String sFileName, Boolean bAsynch, String sStyle) {
		try {

			if (sFileName == null) {
				System.out.println("sFileName must not be null");
			}

			if (sFileName.equals("")) {
				System.out.println("sFileName must not be empty");
			}

			String sFilePath = getSavePath() + sFileName;
			File oFile = new File(sFilePath);
			Boolean bFileExists = oFile.exists();			

			if (m_bIsOnServer == false) {
				if(getUploadActive()) {
					if(bFileExists) {
						if(!fileExistsOnWasdi(sFileName)) {
							System.out.println("Remote file Missing. Start WASDI upload. Please wait");
							uploadFile(sFileName);
							System.out.println("File Uploaded on WASDI cloud, keep on working!");
						}
					}
				}				
			}
			else {
				if (bFileExists) {
					if (fileExistsOnNode(sFileName)==false) {
						System.out.println("Remote file Missing. Start WASDI upload. Please wait");
						uploadFile(sFileName);
						System.out.println("File Uploaded on WASDI cloud, keep on working!");						
					}
				}
			}

			String sUrl = getWorkspaceBaseUrl() + "/catalog/upload/ingestinws?file="+sFileName+"&workspace="+m_sActiveWorkspace;
			if(null!=sStyle && !sStyle.isEmpty()) {
				sUrl += "&style=" + sStyle;
			}

			String sResponse = httpGet(sUrl, getStandardHeaders());
			Map<String, Object> aoJSONMap = s_oMapper.readValue(sResponse, new TypeReference<Map<String,Object>>(){});

			String sProcessId = aoJSONMap.get("stringValue").toString();

			if (bAsynch) return sProcessId;
			else return waitProcess(sProcessId);
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}		
	}

	/**
	 * Adds a generated file to current open workspace in a synchronous way
	 * @param sFileName File Name to add to the open workspace
	 * @param sStyle name of a valid WMS style
	 * @return
	 */
	public String addFileToWASDI(String sFileName, String sStyle) {
		return internalAddFileToWASDI(sFileName, false, sStyle);
	}
	
	/**
	 * Adds a generated file to current open workspace in asynchronous way
	 * @param sFileName File Name to add to the open workspace
	 * @param sStyle name of a valid WMS style
	 * @return
	 */
	public String asynchAddFileToWASDI(String sFileName, String sStyle) {
		return internalAddFileToWASDI(sFileName, true, sStyle);
	}
	
	/**
	 * Ingest a new file in the Active WASDI Workspace waiting for the result
	 * The method takes a file saved in the workspace root (see getSaveFilePath) not already added to the WS
	 * To work be sure that the file is on the server
	 * @param sFileName Name of the file to add
	 * @return Output state of the ingestion process
	 */
	public String addFileToWASDI(String sFileName) {
		return internalAddFileToWASDI(sFileName, false);
	}

	/**
	 * Ingest a new file in the Active WASDI Workspace in an asynch way
	 * The method takes a file saved in the workspace root (see getSaveFilePath) not already added to the WS
	 * To work be sure that the file is on the server 
	 * @param sFileName Name of the file to add
	 * @return Process Id of the ingestion process
	 */
	public String asynchAddFileToWASDI(String sFileName) {
		return internalAddFileToWASDI(sFileName, true);
	}


	/**
	 * Protected Mosaic with minimum parameters
	 * @param bAsynch True to return after the triggering, False to wait the process to finish
	 * @param asInputFiles List of input files to mosaic
	 * @param sOutputFile Name of the mosaic output file
	 * @return Process id or end status of the process
	 */
	protected String internalMosaic(boolean bAsynch, List<String> asInputFiles, String sOutputFile) {
		return internalMosaic(bAsynch, asInputFiles, sOutputFile, null, null);
	}

	/**
	 * Protected Mosaic with also nodata value parameters
	 * @param bAsynch True to return after the triggering, False to wait the process to finish
	 * @param asInputFiles List of input files to mosaic
	 * @param sOutputFile Name of the mosaic output file
	 * @param sNoDataValue Value to use in output as no data
	 * @param sInputIgnoreValue Value to use as input no data
	 * @return Process id or end status of the process
	 */
	protected String internalMosaic(boolean bAsynch, List<String> asInputFiles, String sOutputFile, String sNoDataValue, String sInputIgnoreValue) {
		return internalMosaic(bAsynch, asInputFiles, sOutputFile, sNoDataValue, sInputIgnoreValue, new ArrayList<String>());
	}	
	/**
	 * Protected Mosaic with also Bands Parameters
	 * @param bAsynch True to return after the triggering, False to wait the process to finish
	 * @param asInputFiles List of input files to mosaic
	 * @param sOutputFile Name of the mosaic output file
	 * @param asBands List of the bands to use for the mosaic
	 * @return Process id or end status of the process
	 */
	protected String internalMosaic(boolean bAsynch, List<String> asInputFiles, String sOutputFile, String sNoDataValue, String sInputIgnoreValue, List<String> asBands) {
		return internalMosaic(bAsynch, asInputFiles, sOutputFile, sNoDataValue, sInputIgnoreValue, asBands, 0.005, 0.005);
	}


	/**
	 * Protected Mosaic with also Pixel Size Parameters
	 * @param bAsynch True to return after the triggering, False to wait the process to finish
	 * @param asInputFiles List of input files to mosaic
	 * @param sOutputFile Name of the mosaic output file
	 * @param asBands List of the bands to use for the mosaic
	 * @param dPixelSizeX X Pixel Size
	 * @param dPixelSizeY Y Pixel Size
	 * @return Process id or end status of the process
	 */
	protected String internalMosaic(boolean bAsynch, List<String> asInputFiles, String sOutputFile, String sNoDataValue, String sInputIgnoreValue, List<String> asBands, double dPixelSizeX, double dPixelSizeY) {

		String sCrs = "GEOGCS[\"WGS84(DD)\"," + 
				" DATUM[\"WGS84\"," + 
				" SPHEROID[\"WGS84\", 6378137.0, 298.257223563]]," + 
				" PRIMEM[\"Greenwich\", 0.0]," + 
				" UNIT[\"degree\", 0.017453292519943295]," + 
				" AXIS[\"Geodetic longitude\", EAST]," + 
				" AXIS[\"Geodetic latitude\", NORTH]]";

		return internalMosaic(bAsynch, asInputFiles, sOutputFile, sNoDataValue, sInputIgnoreValue, asBands, dPixelSizeX, dPixelSizeY, sCrs);
	}

	/**
	 * Protected Mosaic with also CRS Input
	 * @param bAsynch True to return after the triggering, False to wait the process to finish
	 * @param asInputFiles List of input files to mosaic
	 * @param sOutputFile Name of the mosaic output file
	 * @param asBands List of the bands to use for the mosaic
	 * @param dPixelSizeX X Pixel Size
	 * @param dPixelSizeY Y Pixel Size
	 * @param sCrs WKT of the CRS to use
	 * @return Process id or end status of the process
	 */
	protected String internalMosaic(boolean bAsynch, List<String> asInputFiles, String sOutputFile,String sNoDataValue,String sInputIgnoreValue, List<String> asBands, double dPixelSizeX, double dPixelSizeY, String sCrs) {

		double dSouthBound = -1.0;
		double dEastBound = -1.0;
		double dWestBound = -1.0;
		double dNorthBound = -1.0;
		String sOverlappingMethod = "MOSAIC_TYPE_OVERLAY";
		Boolean bShowSourceProducts = false;
		String sElevationModelName = "ASTER 1sec GDEM";
		String sResamplingName = "Nearest";
		Boolean bUpdateMode = false;
		Boolean bNativeResolution = true;
		String sCombine = "OR";


		return internalMosaic(bAsynch, asInputFiles, sOutputFile, sNoDataValue, sInputIgnoreValue, asBands, dPixelSizeX, dPixelSizeY, sCrs,dSouthBound, dNorthBound, dEastBound, dWestBound, sOverlappingMethod, bShowSourceProducts, sElevationModelName, sResamplingName, bUpdateMode, bNativeResolution, sCombine);
	}

	/**
	 * Protected Mosaic with all the input parameters
	 * @param bAsynch True to return after the triggering, False to wait the process to finish
	 * @param asInputFiles List of input files to mosaic
	 * @param sOutputFile Name of the mosaic output file
	 * @param asBands List of the bands to use for the mosaic
	 * @param dPixelSizeX X Pixel Size
	 * @param dPixelSizeY Y Pixel Size
	 * @param sCrs WKT of the CRS to use
	 * @param dSouthBound South Bound
	 * @param dNorthBound North Bound
	 * @param dEastBound East Bound
	 * @param dWestBound West Bound
	 * @param sOverlappingMethod Overlapping Method
	 * @param bShowSourceProducts Show Source Products Flag 
	 * @param sElevationModelName DEM Model Name
	 * @param sResamplingName Resampling Method Name
	 * @param bUpdateMode Update Mode Flag
	 * @param bNativeResolution Native Resolution Flag
	 * @param sCombine Combine verb
	 * @return Process id or end status of the process
	 */
	protected String internalMosaic(boolean bAsynch, List<String> asInputFiles, String sOutputFile, String sNoDataValue, String sInputIgnoreValue, List<String> asBands, double dPixelSizeX, double dPixelSizeY, String sCrs, double dSouthBound, double dNorthBound, double dEastBound, double dWestBound, String sOverlappingMethod, boolean bShowSourceProducts, String sElevationModelName, String sResamplingName, boolean bUpdateMode, boolean bNativeResolution, String sCombine) {
		try {

			// Check minimun input values
			if (asInputFiles == null) {
				System.out.println("asInputFiles must not be null");
				return "";
			}

			if (asInputFiles.size() == 0) {
				System.out.println("asInputFiles must not be empty");
				return "";
			}

			if (sOutputFile == null) {
				System.out.println("sOutputFile must not be null");
				return "";
			}

			if (sOutputFile.equals("")) {
				System.out.println("sOutputFile must not empty string");
				return "";
			}

			// Build API URL
			String sUrl = m_sBaseUrl + "/processing/geometric/mosaic?sDestinationProductName="+sOutputFile+"&sWorkspaceId="+m_sActiveWorkspace;

			// Output Format: "GeoTIFF" and BEAM supported
			String sOutputFormat = "GeoTIFF";
			if (sOutputFile.endsWith(".dim")) {
				sOutputFormat = "BEAM-DIMAP";
			}

			// Fill the Setting Object
			MosaicSetting oMosaicSetting = new MosaicSetting();

			if (sNoDataValue != null) {
				try {
					Integer oNoDataValue = Integer.parseInt(sNoDataValue);
					oMosaicSetting.setNoDataValue(oNoDataValue);
				}
				catch (Exception e) {
					log("InternalMosaic: NoDataValue is not a valid integer, set null");
				}
			}

			if (sInputIgnoreValue != null) {
				try {
					Integer oInputIgnoreValue = Integer.parseInt(sInputIgnoreValue);
					oMosaicSetting.setNoDataValue(oInputIgnoreValue);
				}
				catch (Exception e) {
					log("InternalMosaic: InputIgnoreValue is not a valid integer, set null");
				}
			}

			oMosaicSetting.setCombine(sCombine);
			oMosaicSetting.setCrs(sCrs);
			oMosaicSetting.setEastBound(dEastBound);
			oMosaicSetting.setElevationModelName(sElevationModelName);
			oMosaicSetting.setNativeResolution(bNativeResolution);
			oMosaicSetting.setNorthBound(dNorthBound);
			oMosaicSetting.setOverlappingMethod(sOverlappingMethod);
			oMosaicSetting.setPixelSizeX(dPixelSizeX);
			oMosaicSetting.setPixelSizeY(dPixelSizeY);
			oMosaicSetting.setResamplingName(sResamplingName);
			oMosaicSetting.setShowSourceProducts(bShowSourceProducts);
			oMosaicSetting.setOutputFormat(sOutputFormat);


			oMosaicSetting.setSources((List<String>) asInputFiles);
			oMosaicSetting.setSouthBound(dSouthBound);
			oMosaicSetting.setUpdateMode(bUpdateMode);

			oMosaicSetting.setVariableExpressions(new ArrayList<String>());

			List<String> asVariableNames = new ArrayList<>();

			for (String sVariable : asBands) {
				asVariableNames.add(sVariable);
			}

			oMosaicSetting.setVariableNames(asVariableNames);
			oMosaicSetting.setWestBound(dWestBound);

			// Convert to JSON
			String sMosaicSetting = s_oMapper.writeValueAsString(oMosaicSetting);

			// Call the API
			String sResponse = httpPost(sUrl, sMosaicSetting, getStandardHeaders());

			// Read the result
			Map<String, Object> aoJSONMap = s_oMapper.readValue(sResponse, new TypeReference<Map<String,Object>>(){});

			// Extract Process Id
			String sProcessId = aoJSONMap.get("stringValue").toString();

			// Return or wait
			if (bAsynch) return sProcessId;
			else return waitProcess(sProcessId);
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}		
	}

	/**
	 * Mosaic with minimum parameters: input and output files
	 * @param asInputFiles List of input files to mosaic
	 * @param sOutputFile Name of the mosaic output file
	 * @return End status of the process
	 */
	public String mosaic(List<String> asInputFiles, String sOutputFile) {
		return internalMosaic(false, asInputFiles, sOutputFile);
	}

	/**
	 * Mosaic with also NoData Parameters
	 * @param asInputFiles List of input files to mosaic
	 * @param sOutputFile Name of the mosaic output file
	 * @param sNoDataValue Value to use in output as no data
	 * @param sInputIgnoreValue Value to use as input no data
	 * @return End status of the process
	 */
	public String mosaic(List<String> asInputFiles, String sOutputFile, String sNoDataValue, String sInputIgnoreValue) {
		return internalMosaic(false, asInputFiles, sOutputFile, sNoDataValue, sInputIgnoreValue);
	}
	/**
	 * Mosaic with also Bands Parameters
	 * @param asInputFiles List of input files to mosaic
	 * @param sOutputFile Name of the mosaic output file
	 * @param asBands List of the bands to use for the mosaic
	 * @return End status of the process
	 */
	public String mosaic(List<String> asInputFiles, String sOutputFile, String sNoDataValue, String sInputIgnoreValue, List<String> asBands) {
		return internalMosaic(false, asInputFiles, sOutputFile, sNoDataValue, sInputIgnoreValue, asBands);
	}


	/**
	 *  Mosaic with also Pixel Size Parameters
	 * @param asInputFiles List of input files to mosaic
	 * @param sOutputFile Name of the mosaic output file
	 * @param asBands List of the bands to use for the mosaic
	 * @param dPixelSizeX X Pixel Size
	 * @param dPixelSizeY Y Pixel Size
	 * @return End status of the process
	 */
	public String mosaic( List<String> asInputFiles, String sOutputFile, String sNoDataValue, String sInputIgnoreValue, List<String> asBands, double dPixelSizeX, double dPixelSizeY) {		
		return internalMosaic(false, asInputFiles, sOutputFile, sNoDataValue, sInputIgnoreValue, asBands, dPixelSizeX, dPixelSizeY);
	}


	/**
	 *  Mosaic with also CRS Input
	 * @param asInputFiles List of input files to mosaic
	 * @param sOutputFile Name of the mosaic output file
	 * @param asBands List of the bands to use for the mosaic
	 * @param dPixelSizeX X Pixel Size
	 * @param dPixelSizeY Y Pixel Size
	 * @param sCrs WKT of the CRS to use
	 * @return End status of the process
	 */
	public String mosaic(List<String> asInputFiles, String sOutputFile, String sNoDataValue, String sInputIgnoreValue, List<String> asBands, double dPixelSizeX, double dPixelSizeY, String sCrs) {

		return internalMosaic(false, asInputFiles, sOutputFile, sNoDataValue, sInputIgnoreValue, asBands, dPixelSizeX, dPixelSizeY, sCrs);
	}


	/**
	 * Mosaic with all the input parameters
	 * @param asInputFiles List of input files to mosaic
	 * @param sOutputFile Name of the mosaic output file
	 * @param asBands List of the bands to use for the mosaic
	 * @param dPixelSizeX X Pixel Size
	 * @param dPixelSizeY Y Pixel Size
	 * @param sCrs WKT of the CRS to use
	 * @param dSouthBound South Bound
	 * @param dNorthBound North Bound
	 * @param dEastBound East Bound
	 * @param dWestBound West Bound
	 * @param sOverlappingMethod Overlapping Method
	 * @param bShowSourceProducts Show Source Products Flag 
	 * @param sElevationModelName DEM Model Name
	 * @param sResamplingName Resampling Method Name
	 * @param bUpdateMode Update Mode Flag
	 * @param bNativeResolution Native Resolution Flag
	 * @param sCombine Combine verb
	 * @return End status of the process
	 */
	public String mosaic(List<String> asInputFiles, String sOutputFile, String sNoDataValue, String sInputIgnoreValue, List<String> asBands, double dPixelSizeX, double dPixelSizeY, String sCrs, double dSouthBound, double dNorthBound, double dEastBound, double dWestBound, String sOverlappingMethod, boolean bShowSourceProducts, String sElevationModelName, String sResamplingName, boolean bUpdateMode, boolean bNativeResolution, String sCombine) {
		return internalMosaic(false, asInputFiles, sOutputFile, sNoDataValue, sInputIgnoreValue, asBands, dPixelSizeX, dPixelSizeY, sCrs, dSouthBound, dNorthBound, dEastBound, dWestBound, sOverlappingMethod, bShowSourceProducts, sElevationModelName, sResamplingName, bUpdateMode, bNativeResolution, sCombine);
	}

	/**
	 * Asynch Mosaic with minimum parameters
	 * @param asInputFiles List of input files to mosaic
	 * @param sOutputFile Name of the mosaic output file
	 * @return Process id 
	 */
	public String asynchMosaic(List<String> asInputFiles, String sOutputFile) {
		return internalMosaic(true, asInputFiles, sOutputFile);
	}


	/**
	 * Asynch Mosaic with also Bands Parameters
	 * @param asInputFiles List of input files to mosaic
	 * @param sOutputFile Name of the mosaic output file
	 * @return Process id 
	 */
	public String asynchMosaic(List<String> asInputFiles, String sOutputFile, String sNoDataValue, String sInputIgnoreValue) {
		return internalMosaic(true, asInputFiles, sOutputFile, sNoDataValue, sInputIgnoreValue);
	}

	/**
	 * Asynch Mosaic with also Bands Parameters
	 * @param asInputFiles List of input files to mosaic
	 * @param sOutputFile Name of the mosaic output file
	 * @param asBands List of the bands to use for the mosaic
	 * @return Process id 
	 */
	public String asynchMosaic(List<String> asInputFiles, String sOutputFile, String sNoDataValue, String sInputIgnoreValue, List<String> asBands) {
		return internalMosaic(true, asInputFiles, sOutputFile, sNoDataValue, sInputIgnoreValue, asBands);
	}


	/**
	 * Asynch Mosaic with also Pixel Size Parameters
	 * @param asInputFiles List of input files to mosaic
	 * @param sOutputFile Name of the mosaic output file
	 * @param asBands List of the bands to use for the mosaic
	 * @param dPixelSizeX X Pixel Size
	 * @param dPixelSizeY Y Pixel Size
	 * @return Process id 
	 */
	public String asynchMosaic( List<String> asInputFiles, String sOutputFile, String sNoDataValue, String sInputIgnoreValue, List<String> asBands, double dPixelSizeX, double dPixelSizeY) {		
		return internalMosaic(true, asInputFiles, sOutputFile, sNoDataValue, sInputIgnoreValue, asBands, dPixelSizeX, dPixelSizeY);
	}

	/**
	 * Asynch Mosaic with also CRS Input
	 * @param asInputFiles List of input files to mosaic
	 * @param sOutputFile Name of the mosaic output file
	 * @param asBands List of the bands to use for the mosaic
	 * @param dPixelSizeX X Pixel Size
	 * @param dPixelSizeY Y Pixel Size
	 * @param sCrs WKT of the CRS to use
	 * @return Process id
	 */
	public String asynchMosaic(List<String> asInputFiles, String sOutputFile, String sNoDataValue, String sInputIgnoreValue, List<String> asBands, double dPixelSizeX, double dPixelSizeY, String sCrs) {

		return internalMosaic(true, asInputFiles, sOutputFile, sNoDataValue, sInputIgnoreValue, asBands, dPixelSizeX, dPixelSizeY, sCrs);
	}

	/**
	 * Asynch Mosaic with all the input parameters
	 * @param asInputFiles List of input files to mosaic
	 * @param sOutputFile Name of the mosaic output file
	 * @param asBands List of the bands to use for the mosaic
	 * @param dPixelSizeX X Pixel Size
	 * @param dPixelSizeY Y Pixel Size
	 * @param sCrs WKT of the CRS to use
	 * @param dSouthBound South Bound
	 * @param dNorthBound North Bound
	 * @param dEastBound East Bound
	 * @param dWestBound West Bound
	 * @param sOverlappingMethod Overlapping Method
	 * @param bShowSourceProducts Show Source Products Flag 
	 * @param sElevationModelName DEM Model Name
	 * @param sResamplingName Resampling Method Name
	 * @param bUpdateMode Update Mode Flag
	 * @param bNativeResolution Native Resolution Flag
	 * @param sCombine Combine verb
	 * @return Process id
	 */
	public String asynchMosaic(List<String> asInputFiles, String sOutputFile, String sNoDataValue, String sInputIgnoreValue, List<String> asBands, double dPixelSizeX, double dPixelSizeY, String sCrs, double dSouthBound, double dNorthBound, double dEastBound, double dWestBound, String sOverlappingMethod, boolean bShowSourceProducts, String sElevationModelName, String sResamplingName, boolean bUpdateMode, boolean bNativeResolution, String sCombine) {
		return internalMosaic(true, asInputFiles, sOutputFile, sNoDataValue, sInputIgnoreValue, asBands, dPixelSizeX, dPixelSizeY, sCrs, dSouthBound, dNorthBound, dEastBound, dWestBound, sOverlappingMethod, bShowSourceProducts, sElevationModelName, sResamplingName, bUpdateMode, bNativeResolution, sCombine);
	}


	/**
	 * Search EO-Images 
	 * @param sPlatform Satellite Platform. Accepts "S1","S2"
	 * @param sDateFrom Starting date in format "YYYY-MM-DD"
	 * @param sDateTo End date in format "YYYY-MM-DD"
	 * @param dULLat Upper Left Lat Coordinate. Can be null.
	 * @param dULLon Upper Left Lon Coordinate. Can be null.
	 * @param dLRLat Lower Right Lat Coordinate. Can be null.
	 * @param dLRLon Lower Right Lon Coordinate. Can be null.
	 * @param sProductType Product Type. If Platform = "S1" -> Accepts "SLC","GRD", "OCN". If Platform = "S2" -> Accepts "S2MSI1C","S2MSI2Ap","S2MSI2A". Can be null.
	 * @param iOrbitNumber Sentinel Orbit Number. Can be null.
	 * @param sSensorOperationalMode Sensor Operational Mode. ONLY for S1. Accepts -> "SM", "IW", "EW", "WV". Can be null. Ignored for Platform "S2"
	 * @param sCloudCoverage Cloud Coverage. Sample syntax: [0 TO 9.4] 
	 * @return List of the available products as a LIST of Dictionary representing JSON Object:
	 * {
	 * 		footprint = <image footprint in WKT>
	 * 		id = <unique id of the product for the provider>
	 * 		link = <direct link for download>
	 * 		provider = <WASDI provider used for search>
	 * 		Size = <Product Size>
	 * 		title = <Name of the Product>
	 * 		properties = < Another JSON Object containing other product-specific info >
	 * }
	 */
	public List<Map<String, Object>> searchEOImages(String sPlatform, String sDateFrom, String sDateTo, Double dULLat, Double dULLon, Double dLRLat, Double dLRLon,
			String sProductType, Integer iOrbitNumber, String sSensorOperationalMode, String sCloudCoverage ) {

		log("WasdiLib.searchEOImages( " + sPlatform + ", [ " + sDateFrom + ", " + sDateTo + " ], " +
				"[ " + dULLat + ", " + dULLon + ", " + dLRLat + ", " + dLRLon + " ], " + 
				"product type: " + sProductType + ", " + 
				"orbit: " + iOrbitNumber + ", " +
				"sensor mode: " + sSensorOperationalMode + ", " +
				"cloud coverage: " + sCloudCoverage
		);
		
		List<Map<String, Object>> aoReturnList = (List<Map<String, Object>>) new ArrayList<Map<String, Object>>() ;

		if (sPlatform == null) {
			log("searchEOImages: platform cannot be null");
			return aoReturnList;
		}

		if (!(sPlatform.equals("S1")|| sPlatform.equals("S2"))) {
			log("searchEOImages: platform must be S1 or S2. Received [" + sPlatform + "]");
			return aoReturnList;
		}

		if (sPlatform.equals("S1")) {
			if (sProductType != null) {
				if (!(sProductType.equals("SLC") ||sProductType.equals("GRD") || sProductType.equals("OCN") )) {
					log("searchEOImages: Available Product Types for S1; SLC, GRD, OCN. Received [" + sProductType + "]");
				}
			}
		}

		if (sPlatform.equals("S2")) {
			if (sProductType != null) {
				if (!(sProductType.equals("S2MSI1C") ||sProductType.equals("S2MSI2Ap") || sProductType.equals("S2MSI2A") )) {
					log("searchEOImages: Available Product Types for S2; S2MSI1C, S2MSI2Ap, S2MSI2A. Received [" + sProductType + "]");
				}
			}
		}

		if (sDateFrom == null) {
			log("searchEOImages: sDateFrom cannot be null");
			return aoReturnList;			
		}

		if (sDateFrom.length()<10) {
			log("searchEOImages: sDateFrom must be in format YYYY-MM-DD");
			return aoReturnList;			
		}		

		if (sDateTo == null) {
			log("searchEOImages: sDateTo cannot be null");
			return aoReturnList;			
		}

		if (sDateTo.length()<10) {
			log("searchEOImages: sDateTo must be in format YYYY-MM-DD");
			return aoReturnList;			
		}

		// Create Query String:

		// Platform name for sure
		String sQuery = "( platformname:";
		if (sPlatform.equals("S2")) sQuery += "Sentinel-2 ";
		else sQuery += "Sentinel-1";

		// If available add product type
		if (sProductType != null) {
			sQuery += " AND producttype:" + sProductType;
		}

		// If available Sensor Operational Mode
		if (sSensorOperationalMode != null && sPlatform.equals("S1")) {
			sQuery += " AND sensoroperationalmode:" + sSensorOperationalMode;
		}

		// If available cloud coverage
		if (sCloudCoverage != null && sCloudCoverage.equals("S2")) {
			sQuery += " AND cloudcoverpercentage:" + sCloudCoverage;
		}

		// If available add orbit number
		if (iOrbitNumber != null) {
			sQuery += " AND relativeorbitnumber:" + iOrbitNumber;
		}

		// Close the first block
		sQuery += ") ";

		// Date Block
		sQuery += "AND ( beginPosition:[" + sDateFrom + "T00:00:00.000Z TO " + sDateTo + "T23:59:59.999Z]";
		sQuery += "AND ( endPosition:[" + sDateFrom + "T00:00:00.000Z TO " + sDateTo + "T23:59:59.999Z]";

		// Close the second block
		sQuery += ") ";

		if (dULLat != null && dULLon != null && dLRLat != null && dLRLon != null ) {
			String sFootPrint = "( footprint:\"intersects(POLYGON(( " + dULLon + " " +dLRLat + "," + dULLon + " " + dULLat + "," + dLRLon + " " + dULLat + "," + dLRLon + " " + dLRLat + "," + dULLon + " " +dLRLat + ")))\") AND ";
			sQuery = sFootPrint + sQuery;
		}

		String sQueryBody = "[\"" + sQuery.replace("\"", "\\\"") + "\"]"; 
		sQuery = "sQuery=" + URLEncoder.encode(sQuery) + "&offset=0&limit=10&providers=" + getDefaultProvider();


		try {
			String sUrl = m_sBaseUrl + "/search/querylist?" + sQuery;

			String sResponse = httpPost(sUrl, sQueryBody, getStandardHeaders());
			List<Map<String, Object>> aoJSONMap = s_oMapper.readValue(sResponse, new TypeReference<List<Map<String,Object>>>(){});

			log("" + aoJSONMap);

			return aoJSONMap;		
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return aoReturnList;
	}

	/**
	 * Get the name of a Product found by searchEOImage
	 * @param oProduct JSON Dictionary Product as returned by searchEOImage
	 * @return Name of the product
	 */
	public String getFoundProductName(Map<String, Object> oProduct) {
		if (oProduct == null) return "";
		if (!oProduct.containsKey("title")) return "";

		return oProduct.get("title").toString();
	}

	/**
	 * Get the direct download link of a Product found by searchEOImage
	 * @param oProduct JSON Dictionary Product as returned by searchEOImage
	 * @return Name of the product
	 */
	public String getFoundProductLink(Map<String, Object> oProduct) {
		if (oProduct == null) return "";
		if (!oProduct.containsKey("link")) return "";

		return oProduct.get("link").toString();
	}


	public String asynchImportProduct(Map<String, Object> oProduct) {
		log("WasdiLib.asynchImportProduct (with map)");
		return asynchImportProduct(oProduct, null);
	}

	/**
	 * Import a Product from a Provider in WASDI asynchronously.
	 * 
	 * @param oProduct Product Map JSON representation as returned by searchEOImage
	 * @param sProvider the provider of choice. If null, the default provider will be used
	 * @return status of the Import process
	 */
	public String asynchImportProduct(Map<String, Object> oProduct, String sProvider) {
		log("WasdiLib.asynchImportProduct( oProduct, " + sProvider + " )");
		String sReturn = "ERROR";

		try {
			// Get URL And Bounding Box from the JSON representation
			String sFileUrl = getFoundProductLink(oProduct);
			String sBoundingBox = "";

			if (oProduct.containsKey("footprint")) {
				sBoundingBox = oProduct.get("footprint").toString();
			}

			return asynchImportProduct(sFileUrl, sBoundingBox, sProvider);
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return sReturn;
	}


	/**
	 * Import a Product from a Provider in WASDI.
	 * 
	 * @param oProduct Product Map JSON representation as returned by searchEOImage
	 * @return status of the Import process
	 */
	public String importProduct(Map<String, Object> oProduct) {
		String sReturn = "ERROR";

		try {
			// Get URL And Bounding Box from the JSON representation
			String sFileUrl = getFoundProductLink(oProduct);
			String sBoundingBox = "";

			if (oProduct.containsKey("footprint")) {
				sBoundingBox = oProduct.get("footprint").toString();
			}

			return importProduct(sFileUrl, sBoundingBox);
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return sReturn;
	}

	/**
	 * Import a Product from a Provider in WASDI.
	 * @param sFileUrl Direct link of the product
	 * @return status of the Import process
	 */
	public String importProduct(String sFileUrl) {
		return importProduct(sFileUrl, "");
	}

	/**
	 * Import a Product from a Provider in WASDI asynchronously.
	 * @param sFileUrl Direct link of the product
	 * @return status of the Import process
	 */
	public String asynchImportProduct(String sFileUrl) {
		log("WasdiLib.asynchImportProduct( " + sFileUrl + " )");
		return asynchImportProduct(sFileUrl, "");
	}


	/**
	 * Import a Product from a Provider in WASDI asynchronously.
	 * @param sFileUrl Direct link of the product
	 * @param sBoundingBox bounding box
	 * @return
	 */
	public String asynchImportProduct(String sFileUrl, String sBoundingBox) {
		log("WasdiLib.asynchImportProduct( " + sFileUrl + ", " + sBoundingBox + " )");
		return asynchImportProduct(sFileUrl, sBoundingBox, null);
	}

	/**
	 * Import a Product from a Provider in WASDI.
	 * @param sFileUrl Direct link of the product
	 * @param sBoundingBox Bounding Box of the product
	 * @return status of the Import process
	 */
	public String asynchImportProduct(String sFileUrl, String sBoundingBox, String sProvider) {
		log("WasdiLib.asynchImportProduct( " + sFileUrl + ", " + sBoundingBox + ", " + sProvider + " )");
		String sReturn = "ERROR";

		try {
			if(sProvider== null || sProvider.isEmpty()) {
				sProvider = getDefaultProvider();
			}
			// Encode link and bb
			String sEncodedFileLink = URLEncoder.encode(sFileUrl);
			String sEncodedBoundingBox = URLEncoder.encode(sBoundingBox);

			String sUrl = m_sBaseUrl + "/filebuffer/download?sFileUrl=" + sEncodedFileLink+"&sProvider="+
					sProvider +"&sWorkspaceId="+m_sActiveWorkspace+"&sBoundingBox="+sEncodedBoundingBox;

			// Call the server
			String sResponse = httpGet(sUrl, getStandardHeaders());

			// Read the Primitive Result response
			Map<String, Object> aoJSONMap = s_oMapper.readValue(sResponse, new TypeReference<Map<String,Object>>(){});

			// Check if the process was ok
			if ( ((Boolean)aoJSONMap.get("boolValue")) == true) {
				// get the process id
				sReturn = (String) aoJSONMap.get("stringValue"); 
			}
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return sReturn;
	}

	public String importProduct(String sFileUrl, String sBoundingBox) {
		return importProduct(sFileUrl, sBoundingBox, null);
	}

	/**
	 * Import a Product from a Provider in WASDI.
	 * @param sFileUrl Direct link of the product
	 * @param sBoundingBox Bounding Box of the product
	 * @return status of the Import process
	 */
	public String importProduct(String sFileUrl, String sBoundingBox, String sProvider) {
		String sReturn = "ERROR";

		try {
			String sProcessId = asynchImportProduct(sFileUrl, sBoundingBox, sProvider);
			sReturn = waitProcess(sProcessId);

			// Return the status of the import WASDI process
			return sReturn;
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
		}

		return sReturn;
	}

	/**
	 * Imports a list of product asynchronously
	 * @param aoProductsToImport
	 * @return a list of String containing the WASDI process ids of all the imports 
	 */
	public List<String> asynchImportProductListWithMaps(List<Map<String, Object>> aoProductsToImport){
		log("WasdiLib.asynchImportProductList ( with list of maps )");
		if(null==aoProductsToImport) {
			return null;
		}
		List<String> asIds = new ArrayList<String>(aoProductsToImport.size());
		for (Map<String, Object> oProduct : aoProductsToImport) {
			asIds.add(asynchImportProduct(oProduct));
		}
		return asIds;
	}

	/**
	 * Imports a list of product asynchronously
	 * @param aoProductsToImport
	 * @return a list of String containing the WASDI process ids of all the imports 
	 */
	public List<String> asynchImportProductList(List<String> asProductsToImport){
		log("WasdiLib.asynchImportProductList ( with list )");
		if(null==asProductsToImport) {
			return null;
		}
		List<String> asIds = new ArrayList<String>(asProductsToImport.size());
		for (String sProductUrl: asProductsToImport) {
			asIds.add(asynchImportProduct(sProductUrl));
		}
		return asIds;
	}


	/**
	 * Imports a list of product asynchronously
	 * @param aoProductsToImport
	 * @return a list of String containing the WASDI process ids of all the imports 
	 */
	public List<String> importProductListWithMaps(List<Map<String, Object>> aoProductsToImport){
		return waitProcesses(asynchImportProductListWithMaps(aoProductsToImport));
	}

	/**
	 * Imports a list of product asynchronously
	 * @param aoProductsToImport
	 * @return a list of String containing the WASDI process ids of all the imports 
	 */
	public List<String> importProductList(List<String> asProductsToImport){
		return waitProcesses(asynchImportProductList(asProductsToImport));
	}

	/***
	 * Make a Subset (tile) of an input image in a specified Lat Lon Rectangle
	 * @param sInputFile Name of the input file
	 * @param sOutputFile Name of the output file
	 * @param dLatN Lat North Coordinate
	 * @param dLonW Lon West Coordinate
	 * @param dLatS Lat South Coordinate
	 * @param dLonE Lon East Coordinate 
	 * @return Status of the operation
	 */
	public String subset(String sInputFile, String sOutputFile, double dLatN, double dLonW, double dLatS, double dLonE) {
		try {

			// Check minimun input values
			if (sInputFile == null) {
				System.out.println("input file must not be null");
				return "";
			}

			if (sInputFile.equals("")) {
				System.out.println("input file must not be empty");
				return "";
			}

			if (sOutputFile == null) {
				System.out.println("sOutputFile must not be null");
				return "";
			}

			if (sOutputFile.equals("")) {
				System.out.println("sOutputFile must not empty string");
				return "";
			}

			// Build API URL
			String sUrl = m_sBaseUrl + "/processing/geometric/subset?sSourceProductName="+sInputFile+"&sDestinationProductName="+sOutputFile+"&sWorkspaceId="+m_sActiveWorkspace;

			// Fill the Setting Object
			String sSubsetSetting = "{ \"latN\":"+dLatN+", \"lonW\":" + dLonW + ", \"latS\":"+dLatS + ", \"lonE\":"+ dLonE + " }";

			// Call the API
			String sResponse = httpPost(sUrl, sSubsetSetting, getStandardHeaders());

			// Read the result
			Map<String, Object> aoJSONMap = s_oMapper.readValue(sResponse, new TypeReference<Map<String,Object>>(){});

			// Extract Process Id
			String sProcessId = aoJSONMap.get("stringValue").toString();

			// Return process output status
			return waitProcess(sProcessId);
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "ERROR";
		}		
	}


	/**
	 * Executes a synchronous process, i.e., runs the process and waits for it to complete
	 * @param sProcessorName the name of the processor
	 * @param aoParams a Map of parameters
	 * @return the WASDI processor ID
	 */
	public String executeProcessor(String sProcessorName, Map<String, Object> aoParams) {
		return waitProcess(asynchExecuteProcessor(sProcessorName, aoParams));
	}

	/**
	 * Execute a WASDI processor in Asynch way
	 * @param sProcessorName Processor Name
	 * @param aoParams Dictionary of Params
	 * @return ProcessWorkspace Id
	 */
	public String asynchExecuteProcessor(String sProcessorName, Map<String, Object> aoParams) {
		log("WasdiLib.asynchExecuteProcessor( " + sProcessorName + ", Map<String, Object> aoParams )");
		try {
			// Initialize
			String sParamsJson = "";

			// Convert dictionary in a JSON
			if (aoParams!= null) {
				sParamsJson = s_oMapper.writeValueAsString(aoParams);
			}

			// Encode the params
			sParamsJson = URLEncoder.encode(sParamsJson);

			// Use the string version
			return asynchExecuteProcessor(sProcessorName, sParamsJson);
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}

	}

	/**
	 * Executes a synchronous process, i.e., runs the process and waits for it to complete
	 * @param sProcessorName the name of the processor
	 * @param sEncodedParams a JSON formatted string of parameters for the processor
	 * @return the WASDI processor ID
	 */
	public String executeProcessor(String sProcessorName, String sEncodedParams) {
		return waitProcess(asynchExecuteProcessor(sProcessorName, sEncodedParams));
	}

	/**
	 * Execute a WASDI processor in Asynch way
	 * @param sProcessorName Processor Name
	 * @param sEncodedParams Already JSON Encoded Params
	 * @return ProcessWorkspace Id
	 */
	public String asynchExecuteProcessor(String sProcessorName, String sEncodedParams) {

		log("WasdiLib.asynchExecuteProcessor( " + sProcessorName + ", " +  sEncodedParams + " )"); 
		//Domain check
		if (sProcessorName == null) {
			log("ProcessorName is null, return");
			return "";
		}

		if (sProcessorName.isEmpty()) {
			log("ProcessorName is empty, return");
			return "";
		}

		if (sEncodedParams == null) {
			log("EncodedParams is null, return");
			return "";			
		}

		try {

			// Build API URL
			String sUrl = m_sBaseUrl + "/processors/run?workspace="+m_sActiveWorkspace+"&name="+sProcessorName+"&encodedJson="+sEncodedParams;
			log("WasdiLib.asynchExecuteProcessor: GET: " + sUrl );
			// Call API
			String sResponse = httpGet(sUrl, getStandardHeaders());
			log("WasdiLib.asynchExecuteProcessor: response: " + sResponse );
			// Read the result
			Map<String, Object> aoJSONMap = s_oMapper.readValue(sResponse, new TypeReference<Map<String,Object>>(){});

			// Extract Process Id
			String sProcessId = aoJSONMap.get("processingIdentifier").toString();

			return sProcessId;

		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}
	}


	/**
	 * Delete a Product in the active Workspace
	 * @param sProduct
	 */
	public String deleteProduct(String sProduct) {
		log("WasdiLib.deleteProduct( " + sProduct + " )");
		try {

			// Build API URL
			String sUrl = getWorkspaceBaseUrl() + "/product/delete?sProductName="+sProduct+"&bDeleteFile=true&sWorkspaceId="+m_sActiveWorkspace+"&bDeleteLayer=true";

			// Call API
			String sResponse = httpGet(sUrl, getStandardHeaders());

			return sResponse;
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "ERROR";
		}		
	}


	/**
	 * Write one row of Log
	 * @param sLogRow text to log
	 */
	public void wasdiLog(String sLogRow) {

		if (m_bIsOnServer) {
			try {

				// Check minimun input values
				if (sLogRow == null) {
					log("Log line null");
					return;
				}

				if (sLogRow.equals("")) {
					log("Log line empty");
					return;
				}

				String sWorkspaceUrl = getWorkspaceBaseUrl();

				// Build API URL
				String sUrl = sWorkspaceUrl + "/processors/logs/add?processworkspace="+getMyProcId();

				// Call the API
				httpPost(sUrl, sLogRow, getStandardHeaders());
			}
			catch (Exception oEx) {
				oEx.printStackTrace();
			}					
		}
		else {
			log(sLogRow);
		}
	}	

	/**
	 * Get the processor Path
	 * @return
	 */
	public String getProcessorPath() {
		log("WasdiLib.getProcessorPath");
		try {
			StackTraceElement[] aoStackTrace = new Throwable().getStackTrace();
			File oFile = new File(getClass().getClassLoader().getResource(aoStackTrace[1].getClassName().replace('.', '/') + ".class").toURI());

			return oFile.getParent()+ File.separator;
		}
		catch (Exception oEx) {
			System.out.println("Exception in get Processor Path " + oEx.toString());
			return "";
		}
	}

	/**
	 * Http get Method Helper
	 * @param sUrl Url to call
	 * @param asHeaders Headers Dictionary
	 * @return Server response
	 */
	public String httpGet(String sUrl, Map<String, String> asHeaders) {

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

			//System.out.println("\nSending 'GET' request to URL : " + sUrl);

			int iResponseCode =  oConnection.getResponseCode();
			//System.out.println("Response Code : " + responseCode);

			if(200 == iResponseCode) {
				BufferedReader oInputBuffer = new BufferedReader(new InputStreamReader(oConnection.getInputStream()));
				String sInputLine;
				StringBuffer sResponse = new StringBuffer();

				while ((sInputLine = oInputBuffer.readLine()) != null) {
					sResponse.append(sInputLine);
				}
				oInputBuffer.close();


				//print result
				//System.out.println("Count Done: Response " + sResponse.toString());

				return sResponse.toString();
			} else {
				String sMessage = oConnection.getResponseMessage();
				log(sMessage);
				return "";
			}			
		}
		catch (Exception oEx) {
			oEx.printStackTrace();
			return "";
		}
	}

	/**
	 * Standard http post uility function
	 * @param sUrl url to call
	 * @param sPayload payload of the post 
	 * @param asHeaders headers dictionary
	 * @return server response
	 */
	public String httpPost(String sUrl, String sPayload, Map<String, String> asHeaders) {

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

	/*
	public String httpPost(String sUrl, File oUploadFile, Map<String, String> asHeaders) {
//		URLConnection urlconnection = null;
//		try {
//			URL url = new URL(sUrl);
//			urlconnection = url.openConnection();
//			urlconnection.setDoOutput(true);
//			urlconnection.setDoInput(true);
//
//			if (urlconnection instanceof HttpURLConnection) {
//				((HttpURLConnection) urlconnection).setRequestMethod("POST");
//				((HttpURLConnection) urlconnection).setRequestProperty("Content-type", "multipart/form-data");
//				((HttpURLConnection) urlconnection).connect();
//			}
//
//			BufferedOutputStream bos = new BufferedOutputStream(urlconnection.getOutputStream());
//			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(oUploadFile));
//			int i;
//			// read byte by byte until end of stream
//			while ((i = bis.read()) > 0) {
//				bos.write(i);
//			}
//			bis.close();
//			bos.close();
//			System.out.println(((HttpURLConnection) urlconnection).getResponseMessage());
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		try {
//
//			InputStream inputStream;
//			int responseCode = ((HttpURLConnection) urlconnection).getResponseCode();
//			if ((responseCode >= 200) && (responseCode <= 202)) {
//				inputStream = ((HttpURLConnection) urlconnection).getInputStream();
//				int j;
//				while ((j = inputStream.read()) > 0) {
//					System.out.println(j);
//				}
//
//			} else {
//				inputStream = ((HttpURLConnection) urlconnection).getErrorStream();
//			}
//			((HttpURLConnection) urlconnection).disconnect();
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		return "";

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

			//TODO MY CODE (BY STACKOVERFLOW) 
//			BufferedOutputStream oBos = new BufferedOutputStream(oConnection.getOutputStream());
			OutputStream oBos = oConnection.getOutputStream();

//			BufferedInputStream oBis = new BufferedInputStream(new FileInputStream(oUploadFile));
			InputStream oBis = new FileInputStream(oUploadFile);


			int i;
			// read byte by byte until end of stream
			while ((i = oBis.read()) > 0) {
				oBos.write(i);
			}
			oBis.close();
			oBos.close();
			//TODO MY CODE END 

			oConnection.connect();//MOVE ABOVE ? 

			InputStream oInputStream= oConnection.getInputStream();
			BufferedReader oInputBuffer = new BufferedReader(new InputStreamReader(oInputStream));
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
	 */

	public String httpDelete(String sUrl, Map<String, String> asHeaders) {
		if(null==sUrl || sUrl.isEmpty()) {
			log("httpDelete: invalid URL, aborting");
			return null;
		}
		try {
			URL oUrl = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oUrl.openConnection();

			// optional default is GET
			oConnection.setRequestMethod("DELETE");

			if (asHeaders != null) {
				for (String sKey : asHeaders.keySet()) {
					oConnection.setRequestProperty(sKey,asHeaders.get(sKey));
				}
			}
			int iResponseCode =  oConnection.getResponseCode();

			if(200 == iResponseCode) {
				BufferedReader oInputBuffer = new BufferedReader(new InputStreamReader(oConnection.getInputStream()));
				String sInputLine;
				StringBuilder oResponse = new StringBuilder();

				while ((sInputLine = oInputBuffer.readLine()) != null) {
					oResponse.append(sInputLine);
				}
				oInputBuffer.close();

				return oResponse.toString();
			} else {
				String sMessage = oConnection.getResponseMessage();
				log("httpDelete: connection failed, message follows");
				log(sMessage);
				return null;
			}			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}


	/**
	 * Download a file on the local PC
	 * @param sFileName File Name
	 * @return Full Path
	 */
	protected String downloadFile(String sFileName) {
		try {

			if (sFileName == null) {
				System.out.println("sFileName must not be null");
			}

			if (sFileName.equals("")) {
				System.out.println("sFileName must not be empty");
			}

			String sUrl = getWorkspaceBaseUrl() + "/catalog/downloadbyname?filename="+sFileName+"&workspace="+m_sActiveWorkspace;

			String sOutputFilePath = "";

			Map<String, String> asHeaders = getStandardHeaders();

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

					String sSavePath = getSavePath();
					if(sAttachmentName!=null) {
						sOutputFilePath = sSavePath + sAttachmentName;
					} else {
						sOutputFilePath = sSavePath + sFileName;
					}

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

					if(null!=sAttachmentName && !sFileName.equals(sAttachmentName) && sAttachmentName.toLowerCase().endsWith(".zip")) {
						unzip(sAttachmentName, sSavePath);
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

	private void unzip(String sAttachmentName, String sPath) {
		ZipFile oZipFile = null;
		try {
			if(!sPath.endsWith("/") && !sPath.endsWith("\\") && !sPath.endsWith(File.separator)) {
				sPath+=File.separator;
			}
			String sZipFilePath = sPath+sAttachmentName;
			//create directories first, otherwise there's no place to write the files
			oZipFile = new ZipFile(sZipFilePath);
			Enumeration<? extends ZipEntry> aoEntries = oZipFile.entries();
			while(aoEntries.hasMoreElements()) {
				ZipEntry oZipeEntry = aoEntries.nextElement();
				if(oZipeEntry.isDirectory()) {
					String sDirName = sPath+oZipeEntry.getName();
					File oDir = new File(sDirName);
					if(!oDir.exists()) {
						boolean bCreated = oDir.mkdirs();
						if(!bCreated) {
							throw new IOException("WasdiLib.unzip: cannot create directory " + oDir);
						}
					}
				}
			}

			//now unzip files
			aoEntries = oZipFile.entries();
			while(aoEntries.hasMoreElements()) {
				ZipEntry oZipeEntry = aoEntries.nextElement();
				if(!oZipeEntry.isDirectory()) {
					InputStream oInputStream = oZipFile.getInputStream(oZipeEntry);
					BufferedInputStream oBufferedInputStream = new BufferedInputStream(oInputStream);
					String sFileName = sPath+oZipeEntry.getName();
					File oFile = new File(sFileName);
					//oFile.createNewFile();
					FileOutputStream oFileOutputStream = new FileOutputStream(oFile);
					BufferedOutputStream oBufferedOutputStream = new BufferedOutputStream(oFileOutputStream);
					Util.copyStream(oBufferedInputStream, oBufferedOutputStream);
					oBufferedOutputStream.close();
					oBufferedInputStream.close();
				}
			}


		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			if (oZipFile != null) {
				try {
					oZipFile.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Copy Input Stream in Output Stream
	 * @param oInputStream
	 * @param oOutputStream
	 * @throws IOException
	 */
	protected void copyStreamAndClose(InputStream oInputStream, OutputStream oOutputStream) throws IOException {
		copyStream(oInputStream, oOutputStream);

		oOutputStream.close();
		oInputStream.close();

	}

	protected void copyStream(InputStream oInputStream, OutputStream oOutputStream) throws IOException {
		int BUFFER_SIZE = 8192;//1024*1024;//4096;

		int iBytesRead = -1;
		byte[] abBuffer = new byte[BUFFER_SIZE];
		Long lTotal = 0L;

		//TODO maybe print transfer stats every minute or so: speed, time elapsed
		while ((iBytesRead = oInputStream.read(abBuffer)) != -1) {
			oOutputStream.write(abBuffer, 0, iBytesRead);
			lTotal += iBytesRead;
		}
	}

	/**
	 * Uploads and ingest a file in WASDI
	 * @param sFileName the name of the file to upload
	 */
	public void uploadFile(String sFileName) 
	{
		if(sFileName==null || sFileName.isEmpty()){
			throw new NullPointerException("WasdiLib.uploadFile: file name is null");
		}
		try {
			//local file
			String sFullPath = getSavePath() + sFileName;
			File oFile = new File(sFullPath);
			if(!oFile.exists()) {
				throw new IOException("WasdiLib.uploadFile: file not found");
			}
			InputStream oInputStream = new FileInputStream(oFile);

			//request
			String sUrl = getWorkspaceBaseUrl() + "/product/uploadfile?workspace=" + m_sActiveWorkspace + "&name=" + sFileName;
			URL oURL = new URL(sUrl);
			HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();
			oConnection.setDoOutput(true);
			oConnection.setDoInput(true);
			oConnection.setUseCaches(false);
			int iBufferSize = 8192;//8*1024*1024;
			oConnection.setChunkedStreamingMode(iBufferSize);
			Long lLen = oFile.length();
			System.out.println("WasdiLib.uploadFile: file length is: "+Long.toString(lLen));
			oConnection.setRequestProperty("x-session-token", m_sSessionId);

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
			System.out.println("WasdiLib.uploadFile: server returned " + iResponse);
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

	public String getWorkspaceBaseUrl() {
		return m_sWorkspaceBaseUrl;
	}

	public void setWorkspaceBaseUrl(String m_sWorkspaceBaseUrl) {
		this.m_sWorkspaceBaseUrl = m_sWorkspaceBaseUrl;
	}

	public String hello() {
		try {
			return httpGet(getBaseUrl() + "/wasdi/hello",getStandardHeaders());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	String getProductName(Map<String, Object> oProduct) {
		String sResult = null;
		try {
			sResult = (String)oProduct.get("title");
			if(sResult.startsWith("S1")|| sResult.startsWith("S2")) {
				if(!sResult.toLowerCase().endsWith(".zip")) {
					sResult += ".zip";
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sResult;

	}

	public void importAndPreprocess(List<Map<String, Object>> aoProductsToImport, String sWorkflow, String sPreProcSuffix) {
		log("WasdiLib.importAndPreprocess( aoProductsToImport, " + sWorkflow + ", " + sPreProcSuffix + " )");
		importAndPreprocess(aoProductsToImport, sWorkflow, sPreProcSuffix, null);
	}

	public void importAndPreprocess(List<Map<String, Object>> aoProductsToImport, String sWorkflow, String sPreProcSuffix, String sProvider) {
		log("WasdiLib.importAndPreprocess( aoProductsToImport, " + sWorkflow + ", " + sPreProcSuffix + ", " + sProvider + " )");
		if(null==aoProductsToImport) {
			wasdiLog("The list of products to be imported is null, aborting");
			return;
		}
		if(sProvider==null || sProvider.isEmpty()) {
			sProvider = getDefaultProvider();
		}

		//start downloads
		List<String> asDownloadIds = asynchImportProductListWithMaps(aoProductsToImport);

		//prepare for preprocessing
		List<String> asDownloadStatuses = null;

		final String sNotStartedYet = "NOT STARTED YET";

		//DOWNLOAD
		List<String> asWorkflowIds = new ArrayList<String>(asDownloadIds.size());
		//WORKFLOW
		List<String> asWorkflowStatuses = new ArrayList<String>(asDownloadIds.size());

		//init lists by marking process not started
		for(int i = 0; i < aoProductsToImport.size(); ++i) {
			asWorkflowIds.add(sNotStartedYet);
			asWorkflowStatuses.add(sNotStartedYet);
		}

		boolean bDownloading = true;
		boolean bRunningWorkflow = true;
		boolean bAllWorkflowsStarted = false;
		while(bRunningWorkflow || bDownloading) {

			//DOWNLOADS
			if(bDownloading) {
				bDownloading = false;
				//update status of downloads
				asDownloadStatuses = getProcessesStatusAsList(asDownloadIds);
				for (String sStatus : asDownloadStatuses) {
					if(!(sStatus.equals("DONE") || sStatus.equals("ERROR") || sStatus.equals("STOPPED"))) {
						bDownloading = true;
					}
				}
				if(!bDownloading) {
					wasdiLog("importAndPreprocess: completed all downloads");
				} else {
					log("importAndPreprocess: downloads not completed yet");
				}
			}

			//WORKFLOWS
			bRunningWorkflow = false;
			bAllWorkflowsStarted = true;
			for(int i = 0; i < asDownloadIds.size(); ++i) {
				//check the download status and start as many workflows as possible
				if(asDownloadStatuses.get(i).equals("DONE")) {
					//download complete
					if(asWorkflowIds.get(i).equals(sNotStartedYet)) {
						//then workflow must be started
						String sInputName = getProductName(aoProductsToImport.get(i));
						if(null==sInputName || sInputName.equals("")) {
							wasdiLog("importAndPreprocess: WARNING: input name for " + i + "th product could not be retrieved, skipping");
							asWorkflowIds.set(i, "ERROR "+i);
							asWorkflowStatuses.set(i, "ERROR");
						} else {
							String[] asInputFileName = new String[] {sInputName};
							String sOutputName = sInputName + sPreProcSuffix;
							String[] asOutputFileName = new String[] {sOutputName};
							String sWorkflowId = asynchExecuteWorkflow(asInputFileName, asOutputFileName, sWorkflow);
							wasdiLog("importAndPreprocess: started " + i + "th workflow with id " + sWorkflowId + " -> " + sOutputName);
							asWorkflowIds.set(i, sWorkflowId);
							bRunningWorkflow = true;
						}
					} else {
						//check the status of the workflow
						String sStatus = getProcessStatus(asWorkflowIds.get(i));
						if(!(sStatus.equals("DONE") || sStatus.equals("ERROR") || sStatus.equals("STOPPED"))) {
							bRunningWorkflow = true;
						}
						asWorkflowStatuses.set(i, sStatus);
					}
				} else if(asDownloadStatuses.get(i).equals("STOPPED")) {
					asWorkflowIds.set(i, "STOPPED");
				} else if(asDownloadStatuses.get(i).equals("ERROR")) {
					asWorkflowIds.set(i, "ERROR");
				}
				if(asWorkflowIds.get(i).equals(sNotStartedYet)) {
					bAllWorkflowsStarted = false;
				}
			}
			if(bAllWorkflowsStarted) {
				break;
			}
			try {
				log("importAndPreprocess: sleep");
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		waitProcesses(asWorkflowIds);
		wasdiLog("importAndPreprocess: complete :-)");
	}

	public String createWorkspace(String sWorkspaceName, String sNodeCode) {
		log("WasdiLib.createWorkspace( " + sWorkspaceName + ", " + sNodeCode + " )");
		String sReturn = null;
		try {
			if(null == sWorkspaceName) {
				sWorkspaceName = "";
			} 
			if(sWorkspaceName.isEmpty()) {
				log("createWorkspace: WARNING workspace name null or empty, it will be defaulted");
			}
			StringBuilder oUrl = new StringBuilder();
			oUrl.append(getBaseUrl()).append("/ws/create?");
			if(!sWorkspaceName.isEmpty()) {
				oUrl=oUrl.append("name=").append(sWorkspaceName);
			}
			if(null!=sNodeCode && !sNodeCode.isEmpty()) {
				oUrl=oUrl.append("&node=").append(sNodeCode);
			}
			String sResponse = httpGet(oUrl.toString(), getStandardHeaders());
			if(null==sResponse || sResponse.isEmpty()) {
				log("WasdiLib.createWorkspace: response is null or empty, aborting");
				return null;
			}
			Map<String, Object> aoJSONMap = s_oMapper.readValue(sResponse, new TypeReference<Map<String,Object>>(){});
			// get the workspace id
			sReturn = (String) aoJSONMap.get("stringValue"); 
			if(null!=sReturn) {
				openWorkspace(sReturn);
			} else {
				log("createWorkspace: creation failed.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sReturn;
	}

	/**
	 * Deletes the workspace given its ID
	 * @param sWorkspaceId the unique ID of the workspace 
	 * @return the ID of the workspace as a String if succesful, empty string otherwise
	 */
	public String deleteWorkspace(String sWorkspaceId) {
		log("WasdiLib.deleteWorkspace( " + sWorkspaceId + " )");
		String sResult = null;
		if(null==sWorkspaceId || sWorkspaceId.isEmpty()) {
			log("deleteWorkspace: none passed, aborting");
			return sResult;
		}
		try {
			StringBuilder oUrlBuilder = new StringBuilder()
					.append(getBaseUrl()).append("/ws/delete?sWorkspaceId=").append(sWorkspaceId)
					.append("&bDeleteLayer=").append(true).append("&bDeleteFile=").append(true);

			sResult = httpDelete(oUrlBuilder.toString(), getStandardHeaders());
			if(null==sResult) {
				log("WasdiLib.deleteWorkspace: could not delete workspace (please check the return value, it's going to be null)");
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
		return sResult;
	}


	/**
	 * Get a paginated list of processes in the active workspace 
	 * @param iStartIndex start index of the process (0 by default is the last one)
	 * @param iEndIndex end index of the process (optional)
	 * @param sStatus status filter, null by default. Can be CREATED,  RUNNING,  STOPPED,  DONE,  ERROR, WAITING, READY
	 * @param sOperationType Operation Type Filter, null by default. Can be RUNPROCESSOR, RUNIDL, RUNMATLAB, INGEST, DOWNLOAD, GRAPH, DEPLOYPROCESSOR
	 * @param sNamePattern Name filter. The name meaning depends by the operation type, null by default. For RUNPROCESSOR, RUNIDL and RUNMATLAB is the name of the application
	 * @return a list of process IDs
	 */
	public List<Map<String, String>> getProcessesByWorkspace(int iStartIndex, Integer iEndIndex, String sStatus, String sOperationType, String sNamePattern){

		try {
			iStartIndex = Math.max(0, iStartIndex);
			StringBuilder oUrl = new StringBuilder()
					.append(getWorkspaceBaseUrl())
					.append("/process/byws?")
					.append("sWorkspaceId=").append(getActiveWorkspace())
					.append("&startindex=").append(iStartIndex);
			if(null!=iEndIndex && iEndIndex > iStartIndex) {
				oUrl = oUrl.append("&endIndex=").append(iEndIndex);
			}
			if(null!=sStatus && !sStatus.isEmpty()) {
				oUrl = oUrl.append("&status=").append(sStatus);
			}
			if(null!=sOperationType && !sOperationType.isEmpty()) {
				oUrl = oUrl.append("&operationType").append(sOperationType);
			}
			if(null!=sNamePattern && !sNamePattern.isEmpty()) {
				oUrl = oUrl.append("namePattern").append(sNamePattern);
			}

			String sResponse = httpGet(oUrl.toString(), getStandardHeaders());
			List<Map<String, String>> aoProcesses = s_oMapper.readValue(sResponse, new TypeReference<List<Map<String,String>>>(){});			
			return aoProcesses;


		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}


	/**
	 * Gets the processor payload as a map
	 * @param sProcessObjId the ID of the processor
	 * @return
	 */
	public Map<String, Object> getProcessorPayload(String sProcessObjId){
		log("WasdiLib.getProcessorPayload( "+ sProcessObjId + " )");
		if(null==sProcessObjId || sProcessObjId.isEmpty()) {
			log("internalGetProcessorPayload: the processor ID is null or empty");
		}
		try {
			return s_oMapper.readValue(getProcessorPayloadAsJSON(sProcessObjId), new TypeReference<Map<String,Object>>(){});
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}


	/**
	 * retrieves the payload of a processor formatted as a JSON string
	 * @param sProcessObjId the ID of the processor 
	 * @return the payload as a JSON string, or null if error occurred
	 */
	public String getProcessorPayloadAsJSON(String sProcessObjId) {
		log("WasdiLib.getProcessorPayloadAsJSON( "+ sProcessObjId + " )");
		if(null==sProcessObjId || sProcessObjId.isEmpty()) {
			log("internalGetProcessorPayload: the processor ID is null or empty");
		}

		try {
			StringBuilder oUrl = new StringBuilder()
					.append(getWorkspaceBaseUrl())
					.append("/process/payload")
					.append("?processObjId=").append(sProcessObjId);
			return httpGet(oUrl.toString(), getStandardHeaders());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public String getProductBbox(String sFileName) {
		log("WasdiLib.getProductBBOX( " + sFileName + " )");
		if(null==sFileName || sFileName.isEmpty()) {
			log("WasdiLib.getProductBBOX: file name is null or empty, aborting");
			return null;
		}
		String sResponse = null;
		try {
			StringBuilder oUrl = new StringBuilder()
					.append(getBaseUrl())
					.append("/product/byname?sProductName=").append(sFileName)
					.append("&workspace=").append(getActiveWorkspace());
			sResponse = httpGet(oUrl.toString(), getStandardHeaders());
		}catch (Exception e) {
			e.printStackTrace();
		}
		Map<String,Object> aoResponse = null;
		try {
			aoResponse = s_oMapper.readValue(sResponse, new TypeReference<Map<String,Object>>(){});
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			return (String)aoResponse.get("bbox");
		}catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Copy a file from a workspace to the WASDI user's SFTP Folder in a synchronous way
	 * @param sFileName the filename to move to the SFTP folder
	 * @return Process ID is asynchronous execution, end status otherwise. An empty string is returned in case of failure
	 */
	public String copyFileToSftp(String sFileName) {
		log("WasdiLib.copyFileToSftp( " + sFileName + " )");
		return copyFileToSftp(sFileName, null);
	}
	
	/**
	 * Copy a file from a workspace to the WASDI user's SFTP Folder in a synchronous way
	 * @param sFileName the filename to move to the SFTP folder
	 * @param sRelativePath relative path in the SFTP root
	 * @return Process ID is asynchronous execution, end status otherwise. An empty string is returned in case of failure
	 */
	public String copyFileToSftp(String sFileName, String sRelativePath) {
		log("WasdiLib.copyFileToSftp( " + sFileName + ", " + sRelativePath + " )");
		return waitProcess(asynchCopyFileToSftp(sFileName, sRelativePath));
	}

	
	public String asynchCopyFileToSftp(String sFileName) {
		return asynchCopyFileToSftp(sFileName, null);
	}

	/**
	 * Copy a file from a workspace to the WASDI user's SFTP Folder in asynchronous way
	 * @param sFileName the filename to move to the SFTP folder
	 * @return Process ID is asynchronous execution, end status otherwise. An empty string is returned in case of failure
	 */
	public String asynchCopyFileToSftp(String sFileName, String sRelativePath) {
		log("WasdiLib.asynchCopyFileToSftp( " + sFileName + ", " + sRelativePath + " )");
		if(null==sFileName || sFileName.isEmpty()) {
			log("asynchCopyFileToSftp: invalid file name, aborting");
			return null;
		}
		
		//upload file if it is not on WASDI yet
		try {
			if(getUploadActive()) {
				//String sPath = getSavePath() + File.separator + sFileName;
				if(!fileExistsOnWasdi(sFileName)) {
					log("asynchCopyFileToSftp: file " + sFileName + " is not on WASDI yet, uploading...");
					uploadFile(sFileName);
				}
			}
		} catch (Exception oE) {
			log("asynchCopyFileToSftp: upload failed due to: " + oE + ", aborting");
			return null;
		}
		
		String sResponse = null;
		try {
			StringBuilder oUrl = new StringBuilder()
					.append(getWorkspaceBaseUrl())
					.append("/catalog/copytosfpt?file=").append(sFileName)
					.append("&workspace=").append(getActiveWorkspace());
	
			if(getIsOnServer()) {
				oUrl = oUrl.append("&parent=").append(getMyProcId());
			}
			
			if (sRelativePath != null) {
				if (!sRelativePath.isEmpty()) {
					oUrl = oUrl.append("&path=").append(sRelativePath);
				}
			}
			
			sResponse = httpGet(oUrl.toString(), getStandardHeaders());
		} catch (Exception oE) {
			log("asynchCopyFileToSftp: could not HTTP GET to /catalog/copytosfpt due to: " + oE + ", aborting");
			return null;
		}
		
		try {
			Map<String, Object> aoJSONMap = s_oMapper.readValue(sResponse, new TypeReference<Map<String,Object>>(){});
			String sProcessId = aoJSONMap.get("stringValue").toString();
			return sProcessId;
		} catch (Exception oE) {
			log("asynchCopyFileToSftp: could not parse response due to: " + oE + ", aborting");
		}
		
		return null;
	}
	
	/**
	 * Asynchronous multisubset: creates a Many Subsets from an image. MAX 10 TILES PER CALL. Assumes big tiff format by default
	 * @param sInputFile Input file
	 * @param asOutputFiles Array of Output File Names
	 * @param adLatN Array of Latitude north of the subset
	 * @param adLonW Array of Longitude west of the subset
	 * @param adLatS Array of Latitude South of the subset
	 * @param adLonE Array of Longitude Est of the subset
	 * @return
	 */
	public String multiSubset(String sInputFile, List<String> asOutputFiles, List<Double> adLatN, List<Double> adLonW, List<Double> adLatS, List<Double> adLonE) {
		log("WasdiLib.multiSubset( " + sInputFile + ", asOutputFiles, adLatN, adLonW, adLatS, adLonE )");
		return waitProcess(asynchMultiSubset(sInputFile, asOutputFiles, adLatN, adLonW, adLatS, adLonE));
	}

	/**
	 * Asynchronous multisubset: creates a Many Subsets from an image. MAX 10 TILES PER CALL
	 * @param sInputFile Input file
	 * @param asOutputFiles Array of Output File Names
	 * @param adLatN Array of Latitude north of the subset
	 * @param adLonW Array of Longitude west of the subset
	 * @param adLatS Array of Latitude South of the subset
	 * @param adLonE Array of Longitude Est of the subset
	 * @param bBigTiff whether to use the bigtiff format, for files bigger than 4 GB
	 * @return
	 */
	public String multiSubset(String sInputFile, List<String> asOutputFiles, List<Double> adLatN, List<Double> adLonW, List<Double> adLatS, List<Double> adLonE, boolean bBigTiff) {
		log("WasdiLib.multiSubset( " + sInputFile + ", asOutputFiles, adLatN, adLonW, adLatS, adLonE, " + bBigTiff + " )");
		return waitProcess(asynchMultiSubset(sInputFile, asOutputFiles, adLatN, adLonW, adLatS, adLonE, bBigTiff));
	}
	
	/**
	 * Asynchronous multisubset: creates a Many Subsets from an image. MAX 10 TILES PER CALL. Assumes big tiff format by default
	 * @param sInputFile Input file
	 * @param asOutputFiles Array of Output File Names
	 * @param adLatN Array of Latitude north of the subset
	 * @param adLonW Array of Longitude west of the subset
	 * @param adLatS Array of Latitude South of the subset
	 * @param adLonE Array of Longitude Est of the subset
	 * @return
	 */
	public String asynchMultiSubset(String sInputFile, List<String> asOutputFiles, List<Double> adLatN, List<Double> adLonW, List<Double> adLatS, List<Double> adLonE) {
		log("WasdiLib.asynchMultiSubset( " + sInputFile + ", asOutputFiles, adLatN, adLonW, adLatS, adLonE )");
		return asynchMultiSubset(sInputFile, asOutputFiles, adLatN, adLonW, adLatS, adLonE, true); 
	}
	
	/**
	 * Asynchronous multisubset: creates a Many Subsets from an image. MAX 10 TILES PER CALL
	 * @param sInputFile Input file
	 * @param asOutputFiles Array of Output File Names
	 * @param adLatN Array of Latitude north of the subset
	 * @param adLonW Array of Longitude west of the subset
	 * @param adLatS Array of Latitude South of the subset
	 * @param adLonE Array of Longitude Est of the subset
	 * @param bBigTiff whether to use the bigtiff format, for files bigger than 4 GB
	 * @return
	 */
	public String asynchMultiSubset(String sInputFile, List<String> asOutputFiles, List<Double> adLatN, List<Double> adLonW, List<Double> adLatS, List<Double> adLonE, boolean bBigTiff) {
		log("WasdiLib.asynchMultiSubset( " + sInputFile + ", asOutputFiles, adLatN, adLonW, adLatS, adLonE, " + bBigTiff + " )");
		if(null==sInputFile || sInputFile.isEmpty()) {
			log("multisubset: input file null or empty, aborting");
			return null;
		}
		if(null==asOutputFiles || asOutputFiles.size() <= 0) {
			log("multisubset: output files null or empty, aborting");
			return null;
		}
		if(null==adLatN || adLatN.size() <= 0) {
			log("multisubset: adLatN null or empty, aborting");
			return null;
		}
		if(null==adLonW|| adLonW.size() <= 0) {
			log("multisubset: adLonW null or empty, aborting");
			return null;
		}
		if(null==adLatS || adLatS.size() <= 0) {
			log("multisubset: adLatS null or empty, aborting");
			return null;
		}
		if(null==adLonE|| adLonE.size() <= 0) {
			log("multisubset: adLonE null or empty, aborting");
			return null;
		}
		
		StringBuilder oUrl = null;
		try {
			oUrl = new StringBuilder()
					.append(getBaseUrl())
					.append("/processing/geometric/multisubset?sSourceProductName=").append(sInputFile)
					.append("&sDestinationProductName=").append(sInputFile)
					.append("&sWorkspaceId=").append(getActiveWorkspace());
			
			if(getIsOnServer()) {
				oUrl = oUrl.append("&parent=").append(getMyProcId());
			}
		} catch (Exception oE) {
			log("multisubset: could not prepare URL due to " + oE + ", aborting");
			return null;
		}
		
		Map<String, Object> aoPayload = null;
		try {
			aoPayload = new HashMap<>();
			aoPayload.put("outputNames", asOutputFiles);
			aoPayload.put("latNList", adLatN);
			aoPayload.put("lonWList", adLonW);
			aoPayload.put("latSList", adLatS);
			aoPayload.put("lonEList", adLonE);
			
			if(bBigTiff) {
				aoPayload.put("bigTiff", true);
			}
		} catch (Exception oE) {
			log("multisubset: could not populate payload due to " + oE + ", aborting");
			return null;
		}
		
		String sPayload = null;
		try {
			sPayload = s_oMapper.writeValueAsString(aoPayload);
		} catch (Exception oE) {
			log("multisubset: could not serialize payload due to " + oE + ", aborting");
			return null;
		}
		
		String sResponse = null;
		try {
			sResponse = httpPost(oUrl.toString(), sPayload, getStandardHeaders());
		} catch (Exception oE) {
			log("multisubset: post did not succeed due to " + oE + ", aborting");
			return null;
		}
		
		try {
			Map<String, Object> aoJSONMap = s_oMapper.readValue(sResponse, new TypeReference<Map<String,Object>>(){});
			return (String)aoJSONMap.get("stringValue");
		} catch (Exception oE) {
			log("multisubset: response parsing failed due to " + oE + ", aborting");
		}
		
		return null;		
	}

	
	/**
	 * Sets the sub pid
	 * @param sProcessId the process ID
	 * @param iSubPid the subPid of the process
	 * @return the updated status of the processs
	 */
	public String setSubPid(String sProcessId, int iSubPid) {
		if(null==sProcessId || sProcessId.isEmpty()) {
			log("setSubPid: process ID null or empty, aborting");
			return "";
		}

		String sResponse = null;
		try {
			StringBuilder oUrl = new StringBuilder()
				.append(getWorkspaceBaseUrl())
				.append("/process/setsubpid?")
				.append("sProcessId=").append(sProcessId)
				.append("subpid").append(iSubPid);
		
			sResponse = httpGet(oUrl.toString(), getStandardHeaders());
		} catch (Exception oE) {
			log("setSubPid: could not HTTP GET due to " + oE + ", aborting");
			return "";
		}
		
		Map<String, Object> aoJSONMap = null;
		try {
			aoJSONMap = s_oMapper.readValue(sResponse, new TypeReference<Map<String,Object>>(){});
			return (String)aoJSONMap.get("status");
		} catch (Exception oE) {
			log("setSubPid: could not parse server response due to: " + oE + ", aborting");
		}
		return "";
	}
	
	public void printStatus() {
		log("wasdi: user: " + getUser());
		log("wasdi: password: ***********************");
		log("wasdi: session id: " + getSessionId());
		log("wasdi: active workspace id: " + getActiveWorkspace());
		log("wasdi: workspace owner: " + m_sWorkspaceOwner);
		log("wasdi: base path: " + getBasePath() );
		log("wasdi: is on server: " + getIsOnServer() );
		log("wasdi: download active: " + getDownloadActive() );
		log("wasdi: upload active: " + getUploadActive() );
		log("wasdi: verbose: " + getVerbose() );
		log("wasdi: parameters file path: " + getParametersFilePath());
		log("wasdi: params: " + getParams() );
		log("wasdi: proc id: " + getMyProcId() );
		log("wasdi: base url: " + getBaseUrl() );
		log("wasdi: workspace base URL: " + getWorkspaceBaseUrl() );
	}
}
