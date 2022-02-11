using System.Security.AccessControl;
using System.Web;

using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using WasdiLib.Configuration;
using WasdiLib.Helpers;
using WasdiLib.Models;
using WasdiLib.Services;

namespace WasdiLib
{
	public class WasdiLib
	{

		private readonly ILogger<WasdiLib> _logger;
		private IConfiguration _configuration;


		private IProcessWorkspaceService _processWorkspaceService;
		private IProductService _productService;
		private IWasdiService _wasdiService;
		private IWorkflowService _workflowService;
		private IWorkspaceService _workspaceService;


		private string m_sUser = "";
		private string? m_sPassword = "";
		private string? m_sActiveWorkspace = "";
		private string m_sWorkspaceBaseUrl = "";
		private string m_sWorkspaceOwner = "";
		private string m_sSessionId = "";
		private string m_sBaseUrl = "https://www.wasdi.net/wasdiwebserver/rest/";
		private bool m_bIsOnServer = false;
		private bool m_bDownloadActive = true;
		private bool m_bUploadActive = true;
		private string m_sBasePath = "";
		private string m_sMyProcId = "";
		private bool m_bVerbose = true;
		private Dictionary<string, string> m_aoParams = new Dictionary<string, string>();
		private string m_sParametersFilePath = "";
		private string m_sDefaultProvider = "AUTO";


		public WasdiLib()
		{


			Startup.RegisterServices();
			Startup.SetupErrorLogger();

			_logger = Startup.ServiceProvider.GetService<ILogger<WasdiLib>>();
			_logger.LogDebug("WasdiLib()");

			_processWorkspaceService = Startup.ServiceProvider.GetService<IProcessWorkspaceService>();
			_productService = Startup.ServiceProvider.GetService<IProductService>();
			_wasdiService = Startup.ServiceProvider.GetService<IWasdiService>();
			_workflowService = Startup.ServiceProvider.GetService<IWorkflowService>();
			_workspaceService = Startup.ServiceProvider.GetService<IWorkspaceService>();

		}

		/// <summary>
		/// Init the WASDI Library starting from a configuration file.
		/// If the path is not provided, the application will attempt to use the conventional appsettings.json file.
		/// </summary>
		/// <param name="sConfigFilePath">full path of the configuration file</param>
		/// <returns>True if the system is initialized, False if there is any error</returns>
		public bool Init(string? sConfigFilePath = null)
		{
			_logger.LogDebug($"Init({sConfigFilePath})");

			bool loadConfiguration = LoadConfigurationFile(sConfigFilePath);

			if (!loadConfiguration)
				return false;

			LoadConfigurationValues();

			LoadParametersFile();


			if (InternalInit(GetUser(), GetPassword(), GetSessionId()))
			{
				if (String.IsNullOrEmpty(m_sActiveWorkspace))
				{
					string sWorkspaceName = _configuration.GetValue<string>("WORKSPACE", "");

					_logger.LogInformation($"Workspace to open: {sWorkspaceName}");

					if (sWorkspaceName != "")
						OpenWorkspace(sWorkspaceName);
				}
				else
				{
					OpenWorkspaceById(m_sActiveWorkspace);
					_logger.LogInformation($"Active workspace set {m_sActiveWorkspace}");
				}

				return true;
			}
			else
			{
				return false;
			}

		}

		/// <summary>
		/// Load the configuration file.
		/// If the path is not provided, the application will attempt to load the conventional appsettings.json file.
		/// </summary>
		/// <param name="sConfigFilePath">full path of the configuration file</param>
		/// <returns>True if the configuration file is loaded, False otherwise</returns>
		private bool LoadConfigurationFile(string? sConfigFilePath = null)
		{
			if (String.IsNullOrEmpty(sConfigFilePath))
			{
				_logger.LogInformation($"Init: \"{sConfigFilePath}\" is not a valid path, trying with the default file");
				sConfigFilePath = Path.GetFullPath("appsettings.json");
			}

			if (!File.Exists(sConfigFilePath))
			{
				_logger.LogError($"Init: \"{sConfigFilePath}\" is not a valid path for a config file, aborting");
				return false;
			}

			Startup.LoadConfiguration(sConfigFilePath);
			_configuration = Startup.ConfigurationRoot;

			if (_configuration == null)
			{
				_logger.LogError($"Init: \"{sConfigFilePath}\" is not a valid path for a config file, aborting");
				return false;
			}

			return true;
		}

		/// <summary>
		/// Initialize the member variables with the values read from the configuration file.
		/// </summary>
		private void LoadConfigurationValues()
		{

			m_sUser = _configuration.GetValue<string>("USER", "");
			m_sPassword = _configuration.GetValue<string>("PASSWORD", "");
			m_sBasePath = _configuration.GetValue<string>("BASEPATH", "");
			m_sBaseUrl = _configuration.GetValue<string>("BASEURL", "https://www.wasdi.net/wasdiwebserver/rest");

			if (!m_sBaseUrl.EndsWith("/"))
				m_sBaseUrl += "/";

			m_sSessionId = _configuration.GetValue<string>("SESSIONID", "");
			m_sActiveWorkspace = _configuration.GetValue<string>("WORKSPACEID", "");
			m_sMyProcId = _configuration.GetValue<string>("MYPROCID", "");
			m_sParametersFilePath = _configuration.GetValue<string>("PARAMETERSFILEPATH", "./parameters.txt");

			string sVerbose = _configuration.GetValue<string>("VERBOSE", "");
			if (sVerbose == "1" || sVerbose.ToUpper() == "TRUE")
				m_bVerbose = true;

			_logger.LogInformation("SessionId from config " + m_sSessionId);

			string sDownloadActive = _configuration.GetValue<string>("DOWNLOADACTIVE", "1");
			if (sDownloadActive == "0" || sDownloadActive.ToUpper() == "FALSE")
				m_bDownloadActive = false;

			string sUploadactive = _configuration.GetValue<string>("UPLOADACTIVE", "1");
			if (sUploadactive == "0" || sUploadactive.ToUpper() == "FALSE")
				SetUploadActive(false);

			string sIsOnServer = _configuration.GetValue<string>("ISONSERVER", "0");
			if (sIsOnServer == "1" || sUploadactive.ToUpper() == "TRUE")
			{
				m_bIsOnServer = true;
				// On Server Force Download to false
				m_bDownloadActive = false;
				m_bVerbose = true;
			}
			else
			{
				m_bIsOnServer = false;
			}

			if (m_sBasePath == "")
			{
				if (!m_bIsOnServer)
				{
					string sUserHome = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
					string sWasdiHome = sUserHome + Path.DirectorySeparatorChar + ".wasdi" + Path.DirectorySeparatorChar;

					//create the directory
					if (!Directory.Exists(sWasdiHome))
					{
						Directory.CreateDirectory(sWasdiHome);
					}

					m_sBasePath = sWasdiHome;
				}
				else
				{
					m_sBasePath = "/data/wasdi/";
				}
			}
		}

		/// <summary>
		/// Load the parameters file.
		/// The configuration file should contain a reference to a parameters file.
		/// If the path is not provided, the application will attempt to load the parameters.json file from the same folder.
		/// </summary>
		private void LoadParametersFile()
		{
			if (String.IsNullOrEmpty(m_sParametersFilePath))
			{
				_logger.LogWarning("Init: the parameters-file path is not set, trying to use the parameters.json file");
				m_sParametersFilePath = Path.GetFullPath("parameters.json");
			}

			if (File.Exists(m_sParametersFilePath))
			{
				var sParametersJson = File.ReadAllText(m_sParametersFilePath);
				try
				{
					m_aoParams = SerializationHelper.FromJson<Dictionary<string, string>>(sParametersJson);
				}
				catch (Exception ex)
				{
					_logger.LogError($"LoadParametersFile: the content of the {m_sParametersFilePath} file could not be read as a JSON file.");
				}

				if (m_aoParams == null)
					m_aoParams = new Dictionary<string, string>();
			}
		}


		/// <summary>
		/// Call this after base parameters settings to init the system.
		/// Needed at least: Base Path, User, Password or SessionId.
		/// </summary>
		/// <returns></returns>
		public bool InternalInit()
		{
			_logger.LogDebug("InternalInit");

			return InternalInit(GetUser(), GetPassword(), GetSessionId());
		}


		/// <summary>
		/// Call this after base parameters settings to init the system.
		/// Needed at least: Base Path, User, Password or SessionId.
		/// </summary>
		/// <param name="sUser">the username</param>
		/// <param name="sPassword">the password</param>
		/// <param name="sSessionId">the session Id</param>
		/// <returns>True if a session is successfully established, False otherwise</returns>
		private bool InternalInit(string? sUser, string? sPassword, string? sSessionId)
		{
			_logger.LogDebug("InternalInit");
			_logger.LogDebug("C# WASDILib Init");

			// User Name Needed
			if (sUser == null)
				return false;

			_logger.LogDebug($"User not null {sUser}");

			// Is there a password?
			if (!String.IsNullOrEmpty(sPassword))
			{
				_logger.LogDebug("Password not null. Try to login");

				// Try to log in
				sSessionId = CreateSession(sUser, sPassword);

				if (sSessionId != null)
				{
					SetSessionId(sSessionId);
					_logger.LogInformation($"User logged: session ID {sSessionId}");

					return true;
				}
			}
			else if (sSessionId != null)
			{
				_logger.LogInformation($"Check Session: session ID {sSessionId}");

				// Check User supplied Session
				if (CheckSession(sSessionId, sUser))
				{
					_logger.LogInformation("Check Session: session ID OK");
					return true;
				}
				else
				{
					_logger.LogError("Check Session: session ID not valid");
					return false;
				}
			}

			return false;
		}


		public string GetDefaultProvider()
		{
			return m_sDefaultProvider;
		}

		public void SetDefaultProvider(string? sProvider)
		{
			_logger.LogDebug($"SetDefaultProvider({sProvider})");

			if (String.IsNullOrEmpty(sProvider))
			{
				_logger.LogError("SetDefaultProvider: the provider cannot be null or empty, aborting");
				return;
			}

			this.m_sDefaultProvider = sProvider;
		}

		public string GetUser()
		{
			return m_sUser;
		}

		public void SetUser(string? sUser)
		{
			_logger.LogDebug($"SetUser({sUser})");

			if (String.IsNullOrEmpty(sUser))
			{
				_logger.LogError("SetUser: user null or empty, aborting");
				return;
			}

			this.m_sUser = sUser;
		}

		public string? GetPassword()
		{
			return m_sPassword;
		}

		public void SetPassword(string? sPassword)
		{
			_logger.LogDebug("SetPassword( ********** )");

			this.m_sPassword = sPassword;
		}

		public string? GetActiveWorkspace()
		{
			return m_sActiveWorkspace;
		}

		/// <summary>
		/// If the new active workspace is not null, sets also the workspace owner.
		/// </summary>
		/// <param name="sNewActiveWorkspaceId">the new Id of the activeWorkspace</param>
		public void SetActiveWorkspace(string? sNewActiveWorkspaceId)
		{
			_logger.LogDebug($"SetActiveWorkspace({sNewActiveWorkspaceId})");

			this.m_sActiveWorkspace = sNewActiveWorkspaceId;

			if (!String.IsNullOrEmpty(m_sActiveWorkspace))
			{
				m_sWorkspaceOwner = GetWorkspaceOwnerByWSId(sNewActiveWorkspaceId);
			}
		}

		public string GetSessionId()
		{
			return m_sSessionId;
		}

		/// <summary>
		/// Sets the sessionId only if the input is not null.
		/// </summary>
		/// <param name="sSessionId"></param>
		public void SetSessionId(string? sSessionId)
		{
			_logger.LogDebug($"SetSessionId({sSessionId})");

			if (String.IsNullOrEmpty(sSessionId))
			{
				_logger.LogError("SetSessionId: session null or empty, aborting");
				return;
			}

			this.m_sSessionId = sSessionId;
		}

		public string GetBaseUrl()
		{
			return m_sBaseUrl;
		}

		/// <summary>
		/// Sets the baseUrl only if the input is not null and if it represents a valid URI.
		/// </summary>
		/// <param name="sBaseUrl">the new baseUrl</param>
		public void SetBaseUrl(string? sBaseUrl)
		{
			_logger.LogDebug($"SetBaseUrl({sBaseUrl})");

			if (String.IsNullOrEmpty(sBaseUrl))
			{
				_logger.LogError("SetBaseUrl: baseUrl null or empty, aborting");
				return;
			}

			try
			{
				Uri? oURI = new Uri(sBaseUrl);

				if (oURI == null || oURI.AbsolutePath == null)
				{
					_logger.LogError($"SetBaseUrl: \"{sBaseUrl}\" is not a valid URL: cannot obtain URI from URL, aborting");
					return;
				}

				this.m_sBaseUrl = sBaseUrl;
			}
			catch (UriFormatException ex)
			{
				_logger.LogError(ex.StackTrace);
			}
		}

		public bool GetIsOnServer()
		{
			return m_bIsOnServer;
		}

		public void SetIsOnServer(bool bIsOnServer)
		{
			_logger.LogDebug($"SetIsOnServer({bIsOnServer})");

			this.m_bIsOnServer = bIsOnServer;
		}

		public Boolean GetDownloadActive()
		{
			return m_bDownloadActive;
		}

		public void SetDownloadActive(bool bDownloadActive)
		{
			_logger.LogDebug($"SetDownloadActive({bDownloadActive})");

			this.m_bDownloadActive = bDownloadActive;
		}

		public bool GetUploadActive()
		{
			return m_bUploadActive;
		}

		public void SetUploadActive(bool bUploadActive)
		{
			_logger.LogDebug($"SetUploadActive({bUploadActive})");

			this.m_bUploadActive = bUploadActive;
		}

		public string GetBasePath()
		{
			return m_sBasePath;
		}

		/// <summary>
		/// Sets the basePath only if the input is not null and if it represents a valid path and the user has the permissions to read and write.
		/// </summary>
		/// <param name="sBasePath">the new basePath</param>
		public void SetBasePath(string? sBasePath)
		{
			_logger.LogDebug($"SetBasePath({sBasePath})");

			if (String.IsNullOrEmpty(sBasePath) || sBasePath.Contains(".."))
			{
				_logger.LogError($"SetBasePath: \"{sBasePath}\" is not a valid path, aborting");
				return;
			}

			if (!Directory.Exists(sBasePath))
			{
				_logger.LogInformation($"SetBasePath: \"{sBasePath}\" does not exist, attempting to create it...");
				DirectoryInfo directoryInfo = Directory.CreateDirectory(sBasePath);

				if (directoryInfo == null || !directoryInfo.Exists)
				{
					_logger.LogError($"SetBasePath: directory \"{sBasePath}\" could not be created, aborting");
					return;
				}
				else
				{
					_logger.LogInformation($"SetBasePath: directory \"{sBasePath}\" successfully created");
				}

				//check accessibility
				if (!directoryInfo.Exists || !HasWritePermissionOnDir(sBasePath))
				{
					_logger.LogError($"SetBasePath: \"{sBasePath}\" canot be read properly, aborting");
					return;
				}
			}

			this.m_sBasePath = sBasePath;
		}

		/// <summary>
		/// Check if the user has the permission to read and write.
		/// </summary>
		/// <param name="sPath">the path to be verified</param>
		/// <returns>True if the user has permissions, False otherwise</returns>
		public static bool HasWritePermissionOnDir(string sPath)
		{
			var writeAllow = false;
			var writeDeny = false;

			DirectoryInfo directoryInfo = new DirectoryInfo(sPath);
			var accessControlList = directoryInfo.GetAccessControl();

			if (accessControlList == null)
				return false;

			var accessRules = accessControlList.GetAccessRules(true, true,                        
				typeof(System.Security.Principal.SecurityIdentifier));

			if (accessRules == null)
				return false;

			foreach (FileSystemAccessRule rule in accessRules)
			{
				if ((FileSystemRights.Write & rule.FileSystemRights) != FileSystemRights.Write)
					continue;

				if (rule.AccessControlType == AccessControlType.Allow)
					writeAllow = true;
				else if (rule.AccessControlType == AccessControlType.Deny)
					writeDeny = true;
			}

			return writeAllow && !writeDeny;
		}

		public string GetMyProcId()
		{
			return m_sMyProcId;
		}

		/// <summary>
		/// Set the myProcessId only if the input is not null or empty.
		/// </summary>
		/// <param name="sMyProcId"></param>
		public void SetMyProcId(string? sMyProcId)
		{
			_logger.LogDebug($"SetMyProcId({sMyProcId})");

			if (String.IsNullOrEmpty(sMyProcId))
			{
				_logger.LogError("SetSessionId: processor ID is null or empty, aborting");
				return;
			}

			this.m_sMyProcId = sMyProcId;
		}

		public bool GetVerbose()
		{
			return m_bVerbose;
		}

		public void SetVerbose(bool bVerbose)
		{
			_logger.LogDebug($"SetVerbose({bVerbose})");

			this.m_bVerbose = bVerbose;
		}

		/// <summary>
		/// Get the parameters (except for the user, sessionId and workspaceid).
		/// </summary>
		/// <returns>the Params dictionary</returns>
		public Dictionary<string, string> GetParams()
		{
			Dictionary<string, string> aoParamsCopy = new Dictionary<string, string>(m_aoParams);

			if (aoParamsCopy.ContainsKey("user"))
				aoParamsCopy.Remove("user");

			if (aoParamsCopy.ContainsKey("sessionid"))
				aoParamsCopy.Remove("sessionid");

			if (aoParamsCopy.ContainsKey("workspaceid"))
				aoParamsCopy.Remove("workspaceid");

			return aoParamsCopy;
		}

		/// <summary>
		/// Get the parameters in Json format.
		/// </summary>
		/// <returns></returns>
		public string GetParamsAsJsonString()
		{
			if (GetParams() == null)
			{
				_logger.LogError("getParamsAsJsonString: no params, returning empty JSON");
				return "{}";
			}

			return SerializationHelper.ToJson(GetParams());
		}

		/// <summary>
		/// Add a parameter to the parameters dictionary.
		/// </summary>
		/// <param name="sKey">the new key</param>
		/// <param name="sParam">the new value</param>
		public void AddParam(string sKey, string sParam)
		{
			_logger.LogDebug($"SetMyProcId({sKey}, {sParam})");

			m_aoParams.Add(sKey, sParam);
		}

		/// <summary>
		/// Get a specific parameter from the parameters dictionary.
		/// If the key is not contained by the dictionary, an empty string is returned.
		/// </summary>
		/// <param name="sKey">the key</param>
		/// <returns>the value corresponding to the key or an empty string</returns>
		public string GetParam(string sKey)
		{
			return m_aoParams.GetValueOrDefault(sKey, "");
		}

		public string GetParametersFilePath()
		{
			return m_sParametersFilePath;
		}

		/// <summary>
		/// Sets the parametersFilePath only if the input is not null and if it represents a valid path.
		/// </summary>
		/// <param name="sParametersFilePath"></param>
		public void SetParametersFilePath(string? sParametersFilePath)
		{
			_logger.LogDebug($"SetParametersFilePath({sParametersFilePath})");

			if (String.IsNullOrEmpty(sParametersFilePath) || sParametersFilePath.Contains(".."))
			{
				_logger.LogError($"SetParametersFilePath: \"{sParametersFilePath}\" is not a valid path, aborting");
				return;
			}

			if (!File.Exists(sParametersFilePath))
			{
				_logger.LogError($"SetParametersFilePath: \"{sParametersFilePath}\" does not exist, aborting");
				return;
			}

			this.m_sParametersFilePath = sParametersFilePath;
		}

		/// <summary>
		/// Create a new session and return its Id.
		/// </summary>
		/// <param name="sUser">the username</param>
		/// <param name="sPassword">the password</param>
		/// <returns>the newly created sessionId</returns>
		public string CreateSession(string sUser, string sPassword)
		{

			try
			{
				LoginResponse loginResponse = _wasdiService.Authenticate(m_sBaseUrl, sUser, sPassword);

				if (loginResponse != null)
					return loginResponse.SessionId;
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
			}

			return null;
		}

		/// <summary>
		/// Check the session.
		/// </summary>
		/// <param name="sSessionId">the actual session Id</param>
		/// <param name="sUser">the username of the expected user</param>
		/// <returns>True if the actual user is the same as the expected user, false otherwise</returns>
		public bool CheckSession(string sSessionId, string sUser)
		{

			try
			{
				LoginResponse loginResponse = _wasdiService.CheckSession(m_sBaseUrl, sSessionId);

				if (loginResponse != null)
					return loginResponse.UserId == sUser;
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
			}

			return false;
		}


		public string GetWorkspaceBaseUrl()
		{
			return m_sWorkspaceBaseUrl;
		}

		/// <summary>
		/// Sets the workspace's baseUrl only if the input is not null and if it represents a valid URI.
		/// </summary>
		/// <param name="sWorkspaceBaseUrl">the new baseUrl of the workspace</param>
		public void SetWorkspaceBaseUrl(string sWorkspaceBaseUrl)
		{
			_logger.LogDebug($"SetWorkspaceBaseUrl({sWorkspaceBaseUrl})");

			Uri uriResult;
			bool result = Uri.TryCreate(sWorkspaceBaseUrl, UriKind.Absolute, out uriResult)
				&& (uriResult.Scheme == Uri.UriSchemeHttp || uriResult.Scheme == Uri.UriSchemeHttps);

			if (!result || uriResult == null)
			{
				_logger.LogError($"SetWorkspaceBaseUrl: \"{sWorkspaceBaseUrl}\" is not a valid URL: cannot obtain URI from URL, aborting");
				return;
			}

			m_sWorkspaceBaseUrl = sWorkspaceBaseUrl;
		}


		/// <summary>
		/// Call the hello endpoint and return thre response.
		/// </summary>
		/// <returns>the response of the server or null in case of any error</returns>
		public string? Hello()
		{
			_logger.LogDebug("Hello()");

			try
			{
				PrimitiveResult? primitiveResult = _wasdiService.Hello(m_sBaseUrl);

				if (primitiveResult != null)
					return primitiveResult.StringValue;
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
			}

			return null;
		}

		/// <summary>
		/// Get the list of workspaces of the logged user.
		/// </summary>
		/// <returns>a list of workspaces or null in case of any error</returns>
		public List<Workspace>? GetWorkspaces()
		{
			_logger.LogDebug("GetWorkspaces()");

			try
			{
				List<Workspace>? workspaces = _workspaceService.GetWorkspaces(m_sBaseUrl, m_sSessionId);

				return workspaces;
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
			}

			return null;
		}

		/// <summary>
		/// Get the list of workspaces' names of the logged user.
		/// </summary>
		/// <returns>a list of workspaces' names or an empty list in case of any error</returns>
		public List<string> GetWorkspacesNames()
		{
			_logger.LogDebug("GetWorkspacesNames()");

			List<string> workspacesNames = new List<string>();

			try
			{
				List<Workspace>? workspaces = _workspaceService.GetWorkspaces(m_sBaseUrl, m_sSessionId);

				if (workspaces != null)
				{
					workspacesNames = workspaces.Select(w => w.WorkspaceName).ToList();
				}
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
			}

			return workspacesNames;
		}

		/// <summary>
		/// Get the Id of a workspace identified by name.
		/// </summary>
		/// <param name="sWorkspaceName">the name of the workspace</param>
		/// <returns>the Id of the workspace or an empty string in case of an error or if there is no workspace with the name indicated</returns>
		public string GetWorkspaceIdByName(string? sWorkspaceName)
		{
			_logger.LogDebug($"GetWorkspaceIdByName({sWorkspaceName})");

			if (String.IsNullOrEmpty(sWorkspaceName))
			{
				_logger.LogError("GetWorkspaceIdByName: invalid workspace name, aborting");
				return "";
			}

			string workspacesId = "";

			try
			{
				List<Workspace>? workspaces = _workspaceService.GetWorkspaces(m_sBaseUrl, m_sSessionId);

				if (workspaces != null && workspaces.Count > 0)
				{
					var firstWorkspace = workspaces.FirstOrDefault(w => w.WorkspaceName == sWorkspaceName);

					if (firstWorkspace != null)
						workspacesId = firstWorkspace.WorkspaceId;
				}
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
			}

			return workspacesId;
		}


		/// <summary>
		/// Get the name of a workspace identified by Id.
		/// </summary>
		/// <param name="sWorkspacesId">the Id of the workspace</param>
		/// <returns>the name of the workspace or an empty string in case of an error or if there is no workspace with the Id indicated</returns>
		public string GetWorkspaceNameById(string? sWorkspacesId)
		{
			_logger.LogDebug($"GetWorkspaceNameById({sWorkspacesId})");

			if (String.IsNullOrEmpty(sWorkspacesId))
			{
				_logger.LogError("GetWorkspaceNameById: invalid workspace Id, aborting");
				return "";
			}

			string workspaceName = "";

			try
			{
				List<Workspace>? workspaces = _workspaceService.GetWorkspaces(m_sBaseUrl, m_sSessionId);

				if (workspaces != null && workspaces.Count > 0)
				{
					var firstWorkspace = workspaces.FirstOrDefault(w => w.WorkspaceId == sWorkspacesId);

					if (firstWorkspace != null)
						workspaceName = firstWorkspace.WorkspaceName;
				}
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
			}

			return workspaceName;
		}

		/// <summary>
		/// Get the userId of the owner of a workspace identified by name.
		/// </summary>
		/// <param name="sWorkspaceName">the name of the workspace</param>
		/// <returns>the user Id of the workspace's owner or an empty string in case of an error or if there is no workspace with the name indicated</returns>
		public string GetWorkspaceOwnerByName(string? sWorkspaceName)
		{
			_logger.LogDebug($"GetWorkspaceOwnerdByName({sWorkspaceName})");

			if (String.IsNullOrEmpty(sWorkspaceName))
			{
				_logger.LogError("GetWorkspaceOwnerByName: invalid workspace name, aborting");
				return "";
			}

			string workspaceOwner = "";

			try
			{
				List<Workspace>? workspaces = _workspaceService.GetWorkspaces(m_sBaseUrl, m_sSessionId);

				if (workspaces != null && workspaces.Count > 0)
				{
					var firstWorkspace = workspaces.FirstOrDefault(w => w.WorkspaceName == sWorkspaceName);

					if (firstWorkspace != null)
						workspaceOwner = firstWorkspace.OwnerUserId;
				}
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
			}

			return workspaceOwner;
		}

		/// <summary>
		/// Get the userId of the owner of a workspace identified by Id.
		/// </summary>
		/// <param name="sWorkspaceId">the Id of the workspace</param>
		/// <returns>the user Id of the workspace's owner or an empty string in case of an error or if there is no workspace with the Id indicated</returns>
		public string GetWorkspaceOwnerByWSId(string? sWorkspaceId)
		{
			_logger.LogDebug($"GetWorkspaceOwnerdById({sWorkspaceId})");

			if (String.IsNullOrEmpty(sWorkspaceId))
			{
				_logger.LogError("GetWorkspaceOwnerByWSId: invalid workspace Id, aborting");
				return "";
			}

			string workspaceOwner = "";

			try
			{
				List<Workspace>? workspaces = _workspaceService.GetWorkspaces(m_sBaseUrl, m_sSessionId);

				if (workspaces != null && workspaces.Count > 0)
				{
					var firstWorkspace = workspaces.FirstOrDefault(w => w.WorkspaceId == sWorkspaceId);

					if (firstWorkspace != null)
						workspaceOwner = firstWorkspace.OwnerUserId;
				}
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
			}

			return workspaceOwner;
		}

		/// <summary>
		/// Get the workspace's URL of a workspace identified by Id.
		/// </summary>
		/// <param name="sWorkspaceId">the Id of the workspace</param>
		/// <returns>the workspace's URL or an empty string in case of an error or if there is no workspace with the Id indicated</returns>
		public string? GetWorkspaceUrlByWsId(string? sWorkspaceId)
		{
			_logger.LogDebug($"getWorkspaceUrlByWsId({sWorkspaceId})");

			if (String.IsNullOrEmpty(sWorkspaceId))
			{
				_logger.LogError("GetWorkspaceUrlByWsId: invalid workspace ID, aborting");
				return "";
			}

			try
			{
				WorkspaceEditorViewModel? workspace = _workspaceService.GetWorkspace(m_sBaseUrl, m_sSessionId, sWorkspaceId);

				if (workspace != null)
					return workspace.ApiUrl;
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
			}

			return "";
		}

		/// <summary>
		/// Open a workspace given its Id.
		/// </summary>
		/// <param name="sWorkspaceId">the Id of the workspace</param>
		/// <returns>the workspace Id if opened successfully, empty string otherwise</returns>
		public string OpenWorkspaceById(string? sWorkspaceId)
		{
			_logger.LogDebug($"OpenWorkspaceById({sWorkspaceId})");

			if (String.IsNullOrEmpty(sWorkspaceId))
			{
				_logger.LogError("OpenWorkspaceById: invalid workspace ID, aborting");
				return "";
			}

			SetActiveWorkspace(sWorkspaceId);

			m_sWorkspaceOwner = GetWorkspaceOwnerByWSId(sWorkspaceId);
			SetWorkspaceBaseUrl(GetWorkspaceUrlByWsId(m_sActiveWorkspace));

			if (m_sWorkspaceBaseUrl == null)
				SetWorkspaceBaseUrl("");

			if (m_sWorkspaceBaseUrl == "")
				m_sWorkspaceBaseUrl = m_sBaseUrl;

			return m_sActiveWorkspace;
		}

		/// <summary>
		/// Open a workspace.
		/// </summary>
		/// <param name="sWorkspaceName">Workspace name to open</param>
		/// <returns>the workspace Id if opened successfully, empty string otherwise</returns>
		public string OpenWorkspace(string? sWorkspaceName)
		{
			_logger.LogDebug($"OpenWorkspace({sWorkspaceName})");

			if (String.IsNullOrEmpty(sWorkspaceName))
			{
				_logger.LogError("OpenWorkspace: invalid workspace name, aborting");
				return "";
			}

			return OpenWorkspaceById(GetWorkspaceIdByName(sWorkspaceName));
		}

		/// <summary>
		/// Get a List of the products in a workspace.
		/// </summary>
		/// <param name="sWorkspaceName">the name of the workspace</param>
		/// <returns>List of Strings representing the product names</returns>
		public List<string> GetProductsByWorkspace(string sWorkspaceName)
		{
			return GetProductsByWorkspaceId(GetWorkspaceIdByName(sWorkspaceName));
		}

		/// <summary>
		/// Get a List of the products in a Workspace
		/// </summary>
		/// <param name="sWorkspaceId">the Id of the workspace</param>
		/// <returns>List of Strings representing the product names</returns>
		public List<string> GetProductsByWorkspaceId(string? sWorkspaceId)
		{
			_logger.LogDebug($"GetProductsByWorkspaceId({sWorkspaceId})");

			List<string> asProducts = new List<string>();

			if (String.IsNullOrEmpty(sWorkspaceId))
			{
				_logger.LogError("GetProductsByWorkspaceId: invalid workspace ID, aborting");
				return asProducts;
			}

			try
			{
				List<Product>? productList = _productService.GetProductsByWorkspaceId(m_sBaseUrl, m_sSessionId, sWorkspaceId);

				if (productList != null)
					asProducts = productList.Select(p => p.FileName).ToList();
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
			}

			return asProducts;
		}

		/// <summary>
		/// Get a List of the products in the active workspace
		/// </summary>
		/// <returns>List of Strings representing the product names</returns>
		public List<string> GetProductsByActiveWorkspace()
		{
			return GetProductsByWorkspaceId(m_sActiveWorkspace);
		}

		/// <summary>
		/// Get the name of the product provided.
		/// For the names starting with S1 or S2, add the .zip extension in case it is missing.
		/// </summary>
		/// <param name="oProduct">the product</param>
		/// <returns>the name of the product or null in case of any error</returns>
		string? GetProductName(Dictionary<string, object> oProduct)
		{
			string? sResult = null;

			try
			{
				sResult = (string)oProduct["title"];

				if (sResult.StartsWith("S1") || sResult.StartsWith("S2"))
					if (!sResult.ToLower().EndsWith(".zip"))
						sResult += ".zip";
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
			}

			return sResult;
		}

		/// <summary>
		/// Import and pre-process with links.
		/// </summary>
		/// <param name="asProductsLink">the list of product links</param>
		/// <param name="asProductsNames">the list of product names</param>
		/// <param name="sWorkflow">the workflow</param>
		/// <param name="sPreProcSuffix">the pre-process suffix</param>
		public void ImportAndPreprocessWithLinks(List<string> asProductsLink, List<string> asProductsNames, string sWorkflow, string sPreProcSuffix)
		{
			_logger.LogDebug($"ImportAndPreprocess( aoProductsToImport, {sWorkflow}, {sPreProcSuffix} )");

			ImportAndPreprocessWithLinks(asProductsLink, asProductsNames, sWorkflow, sPreProcSuffix, null);
		}

		/// <summary>
		/// Import and pre-process with links.
		/// </summary>
		/// <param name="asProductsLink">the list of product links</param>
		/// <param name="asProductsNames">the list of product names</param>
		/// <param name="sWorkflow">the workflow</param>
		/// <param name="sPreProcSuffix">the pre-process suffix</param>
		/// <param name="sProvider">the provider</param>
		public void ImportAndPreprocessWithLinks(List<string>? asProductsLinks, List<string>? asProductsNames, string sWorkflow, string sPreProcSuffix, string? sProvider)
		{
			_logger.LogDebug($"ImportAndPreprocess( aoProductsToImport, {sWorkflow}, {sPreProcSuffix}, {sProvider} )");

			if (asProductsLinks == null)
			{
				WasdiLog("The list of products links to be imported is null, aborting");

				return;
			}

			if (asProductsNames == null)
			{
				WasdiLog("The list of products names to be imported is null, aborting");
				
				return;
			}

			if (asProductsLinks.Count != asProductsNames.Count)
			{
				WasdiLog("The list of products names has a different size from the list of product links, aborting");

				return;
			}

			if (String.IsNullOrEmpty(sProvider))
				sProvider = GetDefaultProvider();

			//start downloads
			List<string>? asDownloadIds = AsynchImportProductList(asProductsLinks, asProductsNames);

			List<string>? asWorkflowIds = AsynchPreprocessProductsOnceDownloadedWithNames(asProductsNames, sWorkflow, sPreProcSuffix, asDownloadIds);

			WaitProcesses(asWorkflowIds);
			WasdiLog("ImportAndPreprocess: complete :-)");
		}

		/// <summary>
		/// Import and pre-process.
		/// </summary>
		/// <param name="aoProductsToImport">the list of products</param>
		/// <param name="sWorkflow">the workflow</param>
		/// <param name="sPreProcSuffix">the pre-process suffix</param>
		public void ImportAndPreprocess(List<Dictionary<string, object>>? aoProductsToImport, string sWorkflow, string sPreProcSuffix)
		{
			_logger.LogDebug("ImportAndPreprocess( aoProductsToImport, " + sWorkflow + ", " + sPreProcSuffix + " )");

			ImportAndPreprocess(aoProductsToImport, sWorkflow, sPreProcSuffix, null);
		}

		/// <summary>
		/// Import and pre-process.
		/// </summary>
		/// <param name="aoProductsToImport">the list of products</param>
		/// <param name="sWorkflow">the workflow</param>
		/// <param name="sPreProcSuffix">the pre-process suffix</param>
		/// <param name="sProvider">the provider</param>
		public void ImportAndPreprocess(List<Dictionary<string, object>>? aoProductsToImport, string sWorkflow, string sPreProcSuffix, string? sProvider)
		{
			_logger.LogDebug("WasdiLib.importAndPreprocess( aoProductsToImport, " + sWorkflow + ", " + sPreProcSuffix + ", " + sProvider + " )");

			if (aoProductsToImport == null)
			{
				WasdiLog("The list of products to be imported is null, aborting");

				return;
			}

			if (String.IsNullOrEmpty(sProvider))
				sProvider = GetDefaultProvider();

			//start downloads
			List<string>? asDownloadIds = AsynchImportProductListWithMaps(aoProductsToImport);

			List<string>? asWorkflowIds = AsynchPreprocessProductsOnceDownloaded(aoProductsToImport, sWorkflow, sPreProcSuffix, asDownloadIds);

			WaitProcesses(asWorkflowIds);
			WasdiLog("ImportAndPreprocess: complete :-)");
		}

		/// <summary>
		/// Asynchronously pre-process products once ther are downloaded.
		/// </summary>
		/// <param name="aoProductsToImport">the list of products</param>
		/// <param name="sWorkflow">the workflow</param>
		/// <param name="sPreProcSuffix">the pre-process suffix</param>
		/// <param name="asDownloadIds">the list of downloads Ids</param>
		/// <returns>the list of workflow ids</returns>
		private List<string>? AsynchPreprocessProductsOnceDownloaded(List<Dictionary<string, object>>? aoProductsToImport, string sWorkflow, string sPreProcSuffix, List<string>? asDownloadIds)
		{

			if (aoProductsToImport == null)
			{
				WasdiLog("The list of products to be imported is null, aborting");

				return null;
			}

			try
			{
				List<string> asProductsNames = new List<string>(aoProductsToImport.Count);

				foreach (Dictionary<string, object> aoMap in aoProductsToImport)
				{
					string? sName = GetProductName(aoMap);
					if (sName != null)
						asProductsNames.Add(sName);
				}

				return AsynchPreprocessProductsOnceDownloadedWithNames(asProductsNames, sWorkflow, sPreProcSuffix, asDownloadIds);
			}
			catch (Exception ex)
			{
				_logger.LogError($"AsynchPreprocessProductsOnceDownloaded: {ex.Message}, aborting");

				return null;
			}
		}

		/// <summary>
		/// Asynchronously pre-process products once ther are downloaded and names are provided.
		/// </summary>
		/// <param name="asProductsNames">the list of product names</param>
		/// <param name="sWorkflow">the workflow</param>
		/// <param name="sPreProcSuffix">the pre-process suffix</param>
		/// <param name="asDownloadIds">the list of downloads Ids</param>
		/// <returns>the list of workflow ids</returns>
		private List<string>? AsynchPreprocessProductsOnceDownloadedWithNames(List<string> asProductsNames, string sWorkflow, string sPreProcSuffix, List<string>? asDownloadIds)
		{

			if (asDownloadIds == null)
			{
				WasdiLog("The list of downloaded product ids is null, aborting");

				return null;
			}

			try
			{
				//prepare for preprocessing
				List<string>? asDownloadStatuses = null;

				const string sNotStartedYet = "NOT STARTED YET";

				//DOWNLOAD
				List<string> asWorkflowIds = new List<string>(asDownloadIds.Count);

				//WORKFLOW
				List<string> asWorkflowStatuses = new List<string>(asDownloadIds.Count);

				//init lists by marking process not started
				for (int i = 0; i < asProductsNames.Count; ++i)
				{
					asWorkflowIds.Add(sNotStartedYet);
					asWorkflowStatuses.Add(sNotStartedYet);
				}

				bool bDownloading = true;
				bool bRunningWorkflow = true;
				bool bAllWorkflowsStarted = false;

				while (bRunningWorkflow || bDownloading)
				{

					//DOWNLOADS
					if (bDownloading)
					{
						bDownloading = false;

						//update status of downloads
						asDownloadStatuses = GetProcessesStatusAsList(asDownloadIds);

						foreach (string sStatus in asDownloadStatuses)
						{
							if (!(sStatus.Equals("DONE") || sStatus.Equals("ERROR") || sStatus.Equals("STOPPED")))
							{
								bDownloading = true;
							}
						}

						if (!bDownloading)
						{
							WasdiLog("ImportAndPreprocess: completed all downloads");
						}
						else
						{
							_logger.LogInformation("ImportAndPreprocess: downloads not completed yet");
						}
					}

					//WORKFLOWS
					bRunningWorkflow = false;
					bAllWorkflowsStarted = true;

					for (int i = 0; i < asDownloadIds.Count; ++i)
					{
						//check the download status and start as many workflows as possible
						if (asDownloadStatuses.ElementAt(i).Equals("DONE"))
						{
							//download complete
							if (asWorkflowIds.ElementAt(i).Equals(sNotStartedYet))
							{
								//then workflow must be started
								string sInputName = asProductsNames.ElementAt(i);

								if (String.IsNullOrEmpty(sInputName))
								{
									WasdiLog("ImportAndPreprocess: WARNING: input name for " + i + "th product could not be retrieved, skipping");
									asWorkflowIds.Insert(i, "ERROR " + i);
									asWorkflowStatuses.Insert(i, "ERROR");
								}
								else
								{
									List<string> asInputFileName = new List<string> { sInputName };
									string sOutputName = sInputName + sPreProcSuffix;

									List<string> asOutputFileName = new List<string> { sOutputName };
									string sWorkflowId = AsynchExecuteWorkflow(asInputFileName, asOutputFileName, sWorkflow);
									
									WasdiLog("ImportAndPreprocess: started " + i + "th workflow with id " + sWorkflowId + " -> " + sOutputName);
									asWorkflowIds.Insert(i, sWorkflowId);
									bRunningWorkflow = true;
								}
							}
							else
							{
								//check the status of the workflow
								string sStatus = GetProcessStatus(asWorkflowIds.ElementAt(i));
								if (!(sStatus.Equals("DONE") || sStatus.Equals("ERROR") || sStatus.Equals("STOPPED")))
								{
									bRunningWorkflow = true;
								}
								asWorkflowStatuses.Insert(i, sStatus);
							}
						}
						else if (asDownloadStatuses.ElementAt(i).Equals("STOPPED"))
						{
							asWorkflowIds.Insert(i, "STOPPED");
						}
						else if (asDownloadStatuses.ElementAt(i).Equals("ERROR"))
						{
							asWorkflowIds.Insert(i, "ERROR");
						}

						if (asWorkflowIds.ElementAt(i).Equals(sNotStartedYet))
						{
							bAllWorkflowsStarted = false;
						}
					}

					if (bAllWorkflowsStarted)
					{
						break;
					}

					_logger.LogInformation("ImportAndPreprocess: sleep");
					Thread.Sleep(2000);
				}
				return asWorkflowIds;
			}
			catch (Exception ex)
			{
				_logger.LogError("AsynchPreprocessProductsOnceDownloadedWithNames: " + ex.Message + ", aborting");
			}

			return null;
		}


		/// <summary>
		/// Get the local path of a file.
		/// </summary>
		/// <param name="sProductName">the name of the file</param>
		/// <returns>the full local path</returns>
		public string GetPath(string sProductName)
		{
			if (String.IsNullOrEmpty(sProductName))
			{
				_logger.LogWarning("GetPath: product name is empty or null, returning save path");
				return GetSavePath();
			}

			if (FileExistsOnWasdi(sProductName))
			{
				return InternalGetFullProductPath(sProductName);
			}
			else
			{
				return GetSavePath() + sProductName;
			}
		}

		/// <summary>
		/// Get the full local path of a product given the product name. Use the output of this API to open the file.
		/// </summary>
		/// <param name="sProductName">the product name</param>
		/// <returns>the product's full path as a String ready to open file</returns>
		public string InternalGetFullProductPath(string? sProductName)
		{
			_logger.LogDebug($"GetFullProductPath( {sProductName} )");

			if (String.IsNullOrEmpty(sProductName))
			{
				_logger.LogError("GetFullProductPath: product name is null or empty, aborting");
				return "";
			}

			try
			{
				string sFullPath = m_sBasePath;

				if (!(sFullPath.EndsWith("\\") || sFullPath.EndsWith("/")))
					sFullPath += Path.DirectorySeparatorChar;

				sFullPath = sFullPath + m_sWorkspaceOwner + Path.DirectorySeparatorChar + m_sActiveWorkspace + Path.DirectorySeparatorChar + sProductName;
				bool bFileExists = Directory.Exists(sFullPath);

				if (!m_bIsOnServer)
				{
					if (m_bDownloadActive && !bFileExists)
					{
						_logger.LogDebug("Local file Missing. Start WASDI download. Please wait");
						DownloadFile(sProductName);
						_logger.LogDebug("File Downloaded on Local PC, keep on working!");

					}
				}
				else
				{
					if (!bFileExists)
					{
						if (FileExistsOnWasdi(sProductName))
						{
							_logger.LogDebug("Local file Missing. Start WASDI download. Please wait");
							DownloadFile(sProductName);
							_logger.LogDebug("File Downloaded on Local Node, keep on working!");
						}
					}
				}

				return sFullPath;
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
				return "";
			}
		}

		/// <summary>
		/// Check if a file exists on Wasdi.
		/// </summary>
		/// <param name="sFileName">the name of the file</param>
		/// <returns>True if the file exists, False otherwise</returns>
		private bool FileExistsOnWasdi(string? sFileName)
		{
			_logger.LogDebug($"FileExistsOnWasdi({sFileName})");

			if (String.IsNullOrEmpty(sFileName))
			{
				_logger.LogError("FileExistssOnWasdi: passed a null or empty file name, aborting");
				return false;
			}

			try
			{
				return _wasdiService.FileExistsOnServer(GetWorkspaceBaseUrl(), m_sSessionId, m_sActiveWorkspace, true, sFileName);
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
			}

			//false, because in general no upload is desirable 
			return false;
		}

		/// <summary>
		/// Check if a file exists on a node.
		/// </summary>
		/// <param name="sFileName">the name of the file</param>
		/// <returns>True if the file exists, False otherwise</returns>
		private bool FileExistsOnNode(string? sFileName)
		{
			_logger.LogDebug($"FileExistsOnNode({sFileName})");

			if (String.IsNullOrEmpty(sFileName))
			{
				_logger.LogError("FileExistssOnNode: passed a null or empty file name, aborting");
				return false;
			}

			try
			{
				return _wasdiService.FileExistsOnServer(GetWorkspaceBaseUrl(), m_sSessionId, m_sActiveWorkspace, false, sFileName);
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
			}

			//false, because in general no upload is desirable 
			return false;
		}

		/// <summary>
		/// Get the local Save Path to use to save custom generated files.
		/// </summary>
		/// <returns>the local path to use to save a custom generated file</returns>
		public string GetSavePath()
		{
			string sFullPath = m_sBasePath;

			if (!(sFullPath.EndsWith("\\") || sFullPath.EndsWith("/") || !sFullPath.EndsWith(Path.DirectorySeparatorChar)))
				sFullPath += Path.DirectorySeparatorChar;

			sFullPath = sFullPath + m_sWorkspaceOwner + Path.DirectorySeparatorChar + m_sActiveWorkspace + Path.DirectorySeparatorChar;

			return sFullPath;
		}

		/// <summary>
		/// Get the list of workflows for the user.
		/// </summary>
		/// <returns>the list of the workflows or null in case of any error</returns>
		public List<Workflow> GetWorkflows()
		{
			_logger.LogDebug("GetWorkflows()");

			try
			{
				List<Workflow> workflows = _workflowService.GetWorkflows(m_sBaseUrl, m_sSessionId);

				return workflows;
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
			}

			return null;
		}

		/// <summary>
		/// Internal execute workflow.
		/// </summary>
		/// <param name="asInputFileNames">the list of input file names</param>
		/// <param name="asOutputFileNames">the list of output file names</param>
		/// <param name="sWorkflowName">the workflow's name</param>
		/// <param name="bAsynch">bAsynch true if asynch, false for synch</param>
		/// <returns>the Id of the process in asynch case, the ouput status of the workflow process otherwise</returns>
		protected string InternalExecuteWorkflow(List<string> asInputFileNames, List<string> asOutputFileNames, string sWorkflowName, Boolean bAsynch)
		{
			try
			{
				List<Workflow> aoWorkflows = _workflowService.GetWorkflows(m_sBaseUrl, m_sSessionId);
			

				string sProcessId = "";
				string sWorkflowId = "";

				foreach (Workflow oWorkflow in aoWorkflows)
				{
					if (oWorkflow.Name == sWorkflowName)
					{
						sWorkflowId = oWorkflow.WorkflowId;
						break;
					}
				}

				if (sWorkflowId == "")
					return "";

				Workflow workflow = new Workflow()
				{
					Name = sWorkflowName,
					Description = "",
					WorkflowId = sWorkflowId,
					InputNodeNames = new List<string>(),
					InputFileNames = asInputFileNames,
					OutputNodeNames = new List<string>(),
					OutputFileNames = asOutputFileNames
				};

				string? sParentId = null;

				if (m_bIsOnServer)
				{
					sParentId = m_sMyProcId;
				}

				PrimitiveResult primitiveResult = _workflowService.RunWorkflow(m_sBaseUrl, m_sSessionId, m_sActiveWorkspace, sParentId, workflow);
				if (primitiveResult != null)
					sProcessId = primitiveResult.StringValue;

				if (bAsynch)
					return sProcessId;
				else
					return WaitProcess(sProcessId);
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
				return "";
			}
		}

		/// <summary>
		/// Executes a WASDI SNAP Workflow in a asynch mode.
		/// </summary>
		/// <param name="asInputFileNames">the list of input file names</param>
		/// <param name="asOutputFileNames">the list of output file names</param>
		/// <param name="sWorkflowName">the workflow's name</param>
		/// <returns>the Id of the workflow process or empty string in case of any issue</returns>
		public string AsynchExecuteWorkflow(List<string> asInputFileName, List<string> asOutputFileName, string sWorkflowName)
		{
			return InternalExecuteWorkflow(asInputFileName, asOutputFileName, sWorkflowName, true);
		}

		/// <summary>
		/// Executes a WASDI SNAP Workflow waiting for the process to finish.
		/// </summary>
		/// <param name="asInputFileNames">the list of input file names</param>
		/// <param name="asOutputFileNames">the list of output file names</param>
		/// <param name="sWorkflowName">the workflow's name</param>
		/// <returns>output status of the Workflow Process</returns>
		public string ExecuteWorkflow(List<string> asInputFileName, List<string> asOutputFileName, string sWorkflowName)
		{
			return InternalExecuteWorkflow(asInputFileName, asOutputFileName, sWorkflowName, false);
		}

		/// <summary>
		/// Get WASDI Process Status.
		/// </summary>
		/// <param name="sProcessId">the process's Id</param>
		/// <returns>rocess Status as a String: CREATED,  RUNNING,  STOPPED,  DONE,  ERROR, WAITING, READY</returns>
		public string GetProcessStatus(string? sProcessId)
		{
			_logger.LogDebug($"GetProcessStatus({sProcessId})");

			if (String.IsNullOrEmpty(sProcessId))
			{
				_logger.LogError("GetProcessStatus: process id null or empty, aborting");
				return "";
			}

			try
			{
				ProcessWorkspace? processWorkspace = _processWorkspaceService.GetProcessWorkspaceByProcessId(GetWorkspaceBaseUrl(), m_sSessionId, sProcessId);

				if (processWorkspace != null)
				{
					string sStatus = processWorkspace.Status;

					if (IsThisAValidStatus(sStatus))
						return sStatus;
				}
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
			}

			return "";
		}

		/// <summary>
		/// Get the status of a List of WASDI processes.
		/// </summary>
		/// <param name="asIds">the list of processes Ids</param>
		/// <returns>Process Status as a String: CREATED,  RUNNING,  STOPPED,  DONE,  ERROR, WAITING, READY</returns>
		public string GetProcessesStatus(List<string> asIds)
		{
			try
			{
				string sResponse = _processWorkspaceService.GetProcessesStatus(GetWorkspaceBaseUrl(), m_sSessionId, asIds);

			return sResponse;
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
			}

			return "";
		}

		/// <summary>
		/// Get the status of a List of WASDI processes.
		/// </summary>
		/// <param name="asIds">the list of processes Ids</param>
		/// <returns>Process Status as a String: CREATED,  RUNNING,  STOPPED,  DONE,  ERROR, WAITING, READY</returns>
		public List<string> GetProcessesStatusAsList(List<string> asIds)
		{
			string sStatus = GetProcessesStatus(asIds);

			if (sStatus.Length < 2)
				return new List<string>();

			List<string> result = sStatus
				.Substring(1, sStatus.Length - 1)
				.Replace("\"", "")
				.Split(",").ToList();

			return result;
		}

		/// <summary>
		/// Update the status of the current process.
		/// </summary>
		/// <param name="sStatus">the Status to set</param>
		/// <returns>updated status as a String or empty string in case of any issue</returns>
		public string? UpdateStatus(string? sStatus)
		{
			_logger.LogDebug($"UpdateStatus({sStatus})");

			if (!IsThisAValidStatus(sStatus))
			{
				_logger.LogError($"UpdateStatus: {sStatus} is not a valid status, aborting");
				return "";
			}

			if (!m_bIsOnServer)
				return sStatus;

			return UpdateStatus(sStatus, -1);
		}

		/// Update the status of the current process.
		/// </summary>
		/// <param name="sStatus">the Status to set</param>
		/// <param name="iPerc">Progress in %</param>
		/// <returns>updated status as a String or empty string in case of any issue</returns>
		public string UpdateStatus(string? sStatus, int iPerc)
		{
			_logger.LogDebug($"UpdateStatus({sStatus}, {iPerc})");

			if (!IsThisAValidStatus(sStatus))
			{
				_logger.LogError($"UpdateStatus( {sStatus}, {iPerc} ): {sStatus} is not a valid status, aborting");
				return "";
			}

			if (!m_bIsOnServer)
				return sStatus;

			return UpdateProcessStatus(GetMyProcId(), sStatus, iPerc);
		}


		/// Update the status of the current process.
		/// </summary>
		/// <param name="sProcessId">the process id</param>
		/// <param name="sStatus">the Status to set</param>
		/// <param name="iPerc">Progress in %</param>
		/// <returns>updated status as a String or empty string in case of any issue</returns>
		public string UpdateProcessStatus(string? sProcessId, string? sStatus, int iPerc)
		{

			if (!IsThisAValidStatus(sStatus))
			{
				_logger.LogError($"UpdateStatus: {sStatus} is not a valid status. It must be one of  CREATED,  RUNNING,  STOPPED,  DONE,  ERROR, WAITING, READY");
				return "";
			}

			if (String.IsNullOrEmpty(sProcessId))
			{
				_logger.LogError("sProcessId must not be null or empty");
				return "";
			}

			try
			{
				ProcessWorkspace processWorkspace = _processWorkspaceService.UpdateProcessStatus(GetWorkspaceBaseUrl(), m_sSessionId, sProcessId, sStatus, iPerc);

				if (processWorkspace != null)
					return processWorkspace.Status;
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
			}

			return "";
		}

		/// <summary>
		/// Update the status of a process
		/// </summary>
		/// <param name="iPerc">Progress in %</param>
		/// <returns>updated status as a String or empty string in case of any issue</returns>
		public string UpdateProgressPerc(int iPerc)
		{
			if (!m_bIsOnServer)
				return "RUNNING";

			if (iPerc < 0 || iPerc > 100)
			{
				_logger.LogError("Percentage must be between 0 and 100 included");
				return "";
			}

			if (String.IsNullOrEmpty(m_sMyProcId))
			{
				_logger.LogError("Own process Id not available");
				return "";
			}

			string sStatus = "RUNNING";

			try
			{
				ProcessWorkspace? processWorkspace = _processWorkspaceService.UpdateProcessStatus(GetWorkspaceBaseUrl(), m_sSessionId, m_sMyProcId, sStatus, iPerc);

				if (processWorkspace != null)
					return processWorkspace.Status;
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
			}

			return "";
		}

		/// <summary>
		/// Wait for a process to finish.
		/// </summary>
		/// <param name="sProcessId">the process id</param>
		/// <returns>the process status</returns>
		public string WaitProcess(string? sProcessId)
		{
			if (String.IsNullOrEmpty(sProcessId))
			{
				_logger.LogError("WaitProcess: sProcessId is null or empty");
				return "";
			}

			UpdateStatus("WAITING");

			string sStatus = "";

			while (!(sStatus == "DONE" || sStatus == "STOPPED" || sStatus == "ERROR"))
			{
				sStatus = GetProcessStatus(sProcessId);

				if (!IsThisAValidStatus(sStatus))
				{
					_logger.LogError($"WaitProcess: the returned status \"{sStatus}\" is not valid, please check the process ID you passed. Aborting");
					return "";
				}

				Thread.Sleep(2000);
			}

			if (WaitForResume())
				return sStatus;
			else
				return "";
		}

		/// <summary>
		/// Check if the status is valid.
		/// </summary>
		/// <param name="sStatus">the status</param>
		/// <returns>True if the status is valid, False otherwise</returns>
		private static bool IsThisAValidStatus(string? sStatus)
		{
			return sStatus == "CREATED"
				|| sStatus == "RUNNING"
				|| sStatus == "DONE"
				|| sStatus == "STOPPED"
				|| sStatus == "ERROR"
				|| sStatus == "WAITING"
				|| sStatus == "READY";
		}

		/// <summary>
		/// Wait for a collection of processes to finish.
		/// </summary>
		/// <param name="asIds">the list of processes Ids</param>
		/// <returns></returns>
		public List<string>? WaitProcesses(List<string>? asIds)
		{
			_logger.LogDebug($"WaitProcesses({asIds})");

			if (asIds == null)
			{
				_logger.LogError("WaitProcesses: passed a null list, aborting");
				return null;
			}

			if (asIds.Count <= 0)
			{
				_logger.LogError("WaitProcesses: list is empty, returning immediately");
				return new List<string>();
			}

			UpdateStatus("WAITING");

			bool bDone = false;
			while (!bDone)
			{
				bDone = true;

				List<string> asStatus = GetProcessesStatusAsList(asIds);

				if (asStatus == null)
				{
					_logger.LogError("WaitProcesses: status list after getProcessesStatusAsList is null, aborting");
					WaitForResume();
					return null;
				}

				if (asStatus.Count != asIds.Count)
				{
					_logger.LogWarning($"WaitProcesses: warning: status list after getProcessesStatusAsList has size {asStatus.Count} instead of {asIds.Count}, please check the process IDs you passed");
				}

				if (asStatus.Count <= 0)
				{
					_logger.LogInformation("WaitProcesses: status list after getProcessesStatusAsList is empty, please check the IDs you passed. Aborting");

					if (WaitForResume())
						return new List<string>(asIds.Count);
					else
						return null;
				}

				// ok we're good, check the statuses
				foreach (string sStatus in asStatus)
				{
					if (!IsThisAValidStatus(sStatus))
					{
						_logger.LogError("WaitProcesses: got \"" + sStatus + "\" which is not a valid status, skipping it (please check the IDs you passed)");


						//canonot assign to 'sStatus' because it is a 'foreach iteration variable'
						/*
						//set it temporary to error
						sStatus = "ERROR";
						*/

					}

					if (!(sStatus == "DONE" || sStatus == "STOPPED" || sStatus == "ERROR"))
					{
						bDone = false;
						//break: there's at least one process for which we need to wait
						break;
					}
				}

				if (!bDone)
				{
					//then at least one needs to be waited for
					_logger.LogInformation("waitProcesses: sleep");
					Thread.Sleep(2000);
				}

			}

			if (WaitForResume())
				return GetProcessesStatusAsList(asIds);
			else
				return null;
		}

		/// <summary>
		/// Wait for a process to finish.
		/// </summary>
		/// <returns>True if "RUNNING", False otherwise</returns>
		protected bool WaitForResume()
		{
			if (!m_bIsOnServer)
				return true;

			string sStatus = "READY";
			UpdateStatus(sStatus);

			while (true)
			{
				sStatus = GetProcessStatus(GetMyProcId());

				switch (sStatus)
				{
					case "WAITING":
					case "CREATED":
						WasdiLog($"WaitForResume: found status {sStatus} and this should not be.");
						sStatus = "READY";
						UpdateStatus(sStatus);

						//Control cannot fall through from one case label to another
						Thread.Sleep(2000);
						break;

					case "READY":
						Thread.Sleep(2000);
						break;
					case "RUNNING":
						return true;
					case "DONE":
					case "ERROR":
					case "STOPPED":
						WasdiLog($"WaitForResume: found status {sStatus} and this should not be.");
						return false;
					default:
						WasdiLog($"waitForResume: found unknown status {sStatus}. Please report this issue to the WASDI admin.");
						return false;
				}

			}
		}

		/// <summary>
		/// Set the payload of the current process (only if the input is not null or empty).
		/// </summary>
		/// <param name="sData">the payload as a String. JSON format recommended</param>
		public void SetPayload(string? sData)
		{
			if (String.IsNullOrEmpty(sData))
			{
				_logger.LogError("SetPayload: null or empty payload, aborting");
				return;
			}

			if (GetIsOnServer())
				SetProcessPayload(GetMyProcId(), sData);
			else
				_logger.LogDebug("setPayload: " + sData);

		}

		/// <summary>
		/// Adds output payload to a process (only if the input is not null or empty).
		/// </summary>
		/// <param name="sProcessId">the process Id</param>
		/// <param name="sData">the payload as a String. JSON format recommended</param>
		/// <returns>the status of the process or empty string in case of any issues</returns>
		public string SetProcessPayload(string? sProcessId, string sData)
		{
			if (String.IsNullOrEmpty(sProcessId))
			{
				_logger.LogError("SetPayload: sProcessId must not be null or empty");
			}

			if (!m_bIsOnServer)
				return "RUNNING";


			try
			{
				ProcessWorkspace? processWorkspace = _processWorkspaceService.UpdateProcessPayload(GetWorkspaceBaseUrl(), GetSessionId(), sProcessId, sData);

				if (processWorkspace != null)
					return processWorkspace.Status;
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
			}

			return "";
		}

		/// <summary>
		/// Refresh Parameters reading again the file.
		/// </summary>
		public void RefreshParameters()
		{
			if (File.Exists(m_sParametersFilePath))
			{
				var sParametersJson = File.ReadAllText(m_sParametersFilePath);
				m_aoParams = SerializationHelper.FromJson<Dictionary<string, string>>(sParametersJson);
			}
		}

		/// <summary>
		/// Private version of the add file to wasdi function.
		/// Adds a generated file to current open workspace.
		/// </summary>
		/// <param name="sFileName">the name of the file to be added to the open workspace</param>
		/// <param name="bAsynch">True if the process has to be asynch, False to wait for the result</param>
		/// <returns>the process Id or empty string in case of any issues</returns>
		protected string InternalAddFileToWASDI(string? sFileName, Boolean bAsynch)
		{
			return InternalAddFileToWASDI(sFileName, bAsynch, null);
		}

		/// <summary>
		/// Private version of the add file to wasdi function.
		/// </summary>
		/// <param name="sFileName">the name of the file to be added to the open workspace</param>
		/// <param name="bAsynch">True if the process has to be asynch, False to wait for the result</param>
		/// <param name="sStyle">name of a valid WMS style</param>
		/// <returns>the process Id or empty string in case of any issues</returns>
		protected string InternalAddFileToWASDI(string? sFileName, Boolean bAsynch, string? sStyle)
		{
			if (String.IsNullOrEmpty(sFileName))
			{
				_logger.LogError("InternalAddFileToWASDI: sFileName must not be null or empty");
				return "";
			}

			try
			{

				string sFilePath = GetSavePath() + sFileName;

				Boolean bFileExists = File.Exists(sFilePath);

				if (!m_bIsOnServer)
				{
					if (GetUploadActive())
					{
						if (bFileExists)
						{
							if (!FileExistsOnWasdi(sFileName))
							{
								_logger.LogInformation("InternalAddFileToWASDI: Remote file Missing. Start WASDI upload. Please wait");
								UploadFile(sFileName);
								_logger.LogInformation("InternalAddFileToWASDI: File Uploaded on WASDI cloud, keep on working!");
							}
						}
					}
				}
				else
				{
					if (bFileExists)
					{
						if (!FileExistsOnNode(sFileName))
						{
							_logger.LogInformation("InternalAddFileToWASDI: Remote file Missing. Start WASDI upload. Please wait");
							UploadFile(sFileName);
							_logger.LogInformation("InternalAddFileToWASDI: File Uploaded on WASDI cloud, keep on working!");
						}
					}
				}

				PrimitiveResult? primitiveResult = _wasdiService.CatalogUploadIngest(GetWorkspaceBaseUrl(), m_sSessionId, m_sActiveWorkspace, sFileName, sStyle);

				if (primitiveResult == null)
					return "";

				string sProcessId = primitiveResult.StringValue;

				if (bAsynch)
					return sProcessId;
				else
					return WaitProcess(sProcessId);
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
				return "";
			}
		}

		/// <summary>
		/// Adds a generated file to current open workspace in a synchronous way.
		/// </summary>
		/// <param name="sFileName">the name of the file to be added to the open workspace</param>
		/// <param name="sStyle">name of a valid WMS style</param>
		/// <returns>the process Id or empty string in case of any issues</returns>
		public string AddFileToWASDI(string sFileName, string sStyle)
		{
			return InternalAddFileToWASDI(sFileName, false, sStyle);
		}

		/// <summary>
		/// Adds a generated file to current open workspace in asynchronous way.
		/// </summary>
		/// <param name="sFileName">the name of the file to be added to the open workspace</param>
		/// <param name="sStyle">name of a valid WMS style</param>
		/// <returns>the process Id or empty string in case of any issues</returns>
		public string AsynchAddFileToWASDI(string sFileName, string sStyle)
		{
			return InternalAddFileToWASDI(sFileName, true, sStyle);
		}

		/// <summary>
		/// Adds a generated file to current open workspace in a synchronous way.
		/// </summary>
		/// <param name="sFileName">the name of the file to be added to the open workspace</param>
		/// <returns>the process Id or empty string in case of any issues</returns>
		public string AddFileToWASDI(string sFileName)
		{
			return InternalAddFileToWASDI(sFileName, false);
		}

		/// <summary>
		/// Adds a generated file to current open workspace in asynchronous way.
		/// </summary>
		/// <param name="sFileName">the name of the file to be added to the open workspace</param>
		/// <returns>the process Id or empty string in case of any issues</returns>
		public string AsynchAddFileToWASDI(string sFileName)
		{
			return InternalAddFileToWASDI(sFileName, true);
		}

		/// <summary>
		/// Protected Mosaic with minimum parameters.
		/// </summary>
		/// <param name="bAsynch">True if the process has to be asynch, False to wait for the result</param>
		/// <param name="asInputFiles">the list of input files to mosaic</param>
		/// <param name="sOutputFile">the name of the mosaic output file</param>
		/// <returns>the process id or end status of the process</returns>
		protected string InternalMosaic(bool bAsynch, List<string> asInputFiles, string sOutputFile)
		{
			return InternalMosaic(bAsynch, asInputFiles, sOutputFile, null, null);
		}

		/// <summary>
		/// Protected Mosaic with also nodata value parameters.
		/// </summary>
		/// <param name="bAsynch">True if the process has to be asynch, False to wait for the result</param>
		/// <param name="asInputFiles">the list of input files to mosaic</param>
		/// <param name="sOutputFile">the name of the mosaic output file</param>
		/// <param name="sNoDataValue">the value to use in output as no data</param>
		/// <param name="sInputIgnoreValue">the value to use as input no data</param>
		/// <returns>the process id or end status of the process</returns>
		protected string InternalMosaic(bool bAsynch, List<string> asInputFiles, string sOutputFile, string? sNoDataValue, string? sInputIgnoreValue)
		{
			return InternalMosaic(bAsynch, asInputFiles, sOutputFile, sNoDataValue, sInputIgnoreValue, -1.0, -1.0);
		}

		/// <summary>
		/// Protected Mosaic with all the input parameters.
		/// </summary>
		/// <param name="bAsynch">True if the process has to be asynch, False to wait for the result</param>
		/// <param name="asInputFiles">the list of input files to mosaic</param>
		/// <param name="sOutputFile">the name of the mosaic output file</param>
		/// <param name="sNoDataValue">the value to use in output as no data</param>
		/// <param name="sInputIgnoreValue">the value to use as input no data</param>
		/// <param name="dPixelSizeX">the X Pixel Size</param>
		/// <param name="dPixelSizeY">the Y Pixel Size</param>
		/// <returns>the process id or end status of the process</returns>
		protected string InternalMosaic(bool bAsynch, List<string>? asInputFiles, string? sOutputFile, string? sNoDataValue, string? sInputIgnoreValue, double dPixelSizeX, double dPixelSizeY)
		{
			_logger.LogDebug("InternalMosaic()");

			if (asInputFiles == null)
			{
				_logger.LogError("InternalMosaic: asInputFiles must not be null");
				return "";
			}

			if (asInputFiles.Count == 0)
			{
				_logger.LogError("InternalMosaic: asInputFiles must not be empty");
				return "";
			}

			if (sOutputFile == null)
			{
				_logger.LogError("InternalMosaic: sOutputFile must not be null");
				return "";
			}

			if (sOutputFile == "")
			{
				_logger.LogError("InternalMosaic: sOutputFile must not empty string");
				return "";
			}

			// Build API URL
			string sUrl = $"{m_sBaseUrl}/processing/mosaic?name={sOutputFile}&workspace={m_sActiveWorkspace}";

			// Output Format: "GeoTIFF" and BEAM supported
			string sOutputFormat = "GeoTIFF";
			if (sOutputFile.EndsWith(".dim"))
				sOutputFormat = "BEAM-DIMAP";

			// Fill the Setting Object
			MosaicSetting oMosaicSetting = new MosaicSetting();

			int? oNoDataValue = ToInt(sNoDataValue);
			if (oNoDataValue.HasValue)
				oMosaicSetting.NoDataValue = oNoDataValue.Value;

			int? oInputIgnoreValue = ToInt(sInputIgnoreValue);
			if (oInputIgnoreValue.HasValue)
				oMosaicSetting.InputIgnoreValue = oInputIgnoreValue.Value;

			oMosaicSetting.PixelSizeX = dPixelSizeX;
			oMosaicSetting.PixelSizeY = dPixelSizeY;
			oMosaicSetting.OutputFormat = sOutputFormat;

			oMosaicSetting.Sources = asInputFiles;


			try
			{
				// Extract Process Id
				string sProcessId = String.Empty;

				PrimitiveResult? primitiveResult = _wasdiService.ProcessingMosaic(sUrl, m_sSessionId, oMosaicSetting);

				if (primitiveResult != null)
					sProcessId = primitiveResult.StringValue;

				// Return or wait
				if (bAsynch)
					return sProcessId;
				else
					return WaitProcess(sProcessId);
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
				return "";
			}
		}

		/// <summary>
		/// Convert a sting to an int.
		/// </summary>
		/// <param name="value">the string value</param>
		/// <returns>the converted int or null in case of any issues</returns>
		private int? ToInt(string? value)
		{
			try
			{
				return Convert.ToInt32(value);
			}
			catch (ArgumentNullException)
			{
				_logger.LogError("ToInt64: the string value is null, set null");
				return null;
			}
			catch (FormatException)
			{
				_logger.LogError("ToInt64: the string value is not a valid integer, set null");
				return null;
			}

		}

		/// <summary>
		/// Mosaic with minimum parameters: input and output files.
		/// </summary>
		/// <param name="asInputFiles">the list of input files to mosaic</param>
		/// <param name="sOutputFile">the name of the mosaic output file</param>
		/// <returns>the end status of the process</returns>
		public string Mosaic(List<string> asInputFiles, string sOutputFile)
		{
			return InternalMosaic(false, asInputFiles, sOutputFile);
		}

		/// <summary>
		/// Mosaic with also NoData Parameters.
		/// </summary>
		/// <param name="asInputFiles">the list of input files to mosaic</param>
		/// <param name="sOutputFile">the name of the mosaic output file</param>
		/// <param name="sNoDataValue">the value to use in output as no data</param>
		/// <param name="sInputIgnoreValue">the value to use as input no data</param>
		/// <returns>the end status of the process</returns>
		public string Mosaic(List<string> asInputFiles, string sOutputFile, string sNoDataValue, string sInputIgnoreValue)
		{
			return InternalMosaic(false, asInputFiles, sOutputFile, sNoDataValue, sInputIgnoreValue);
		}

		/// <summary>
		/// Mosaic with all the input parameters
		/// </summary>
		/// <param name="asInputFiles">the list of input files to mosaic</param>
		/// <param name="sOutputFile">the name of the mosaic output file</param>
		/// <param name="sNoDataValue">the value to use in output as no data</param>
		/// <param name="sInputIgnoreValue">the value to use as input no data</param>
		/// <param name="dPixelSizeX">the X Pixel Size</param>
		/// <param name="dPixelSizeY">the Y Pixel Size</param>
		/// <returns>the end status of the process</returns>
		public string Mosaic(List<string> asInputFiles, string sOutputFile, string sNoDataValue, string sInputIgnoreValue, double dPixelSizeX, double dPixelSizeY)
		{
			return InternalMosaic(false, asInputFiles, sOutputFile, sNoDataValue, sInputIgnoreValue, dPixelSizeX, dPixelSizeY);
		}

		/// <summary>
		/// Asynch Mosaic with minimum parameters.
		/// </summary>
		/// <param name="asInputFiles">the list of input files to mosaic</param>
		/// <param name="sOutputFile">the name of the mosaic output file</param>
		/// <returns>the process Id</returns>
		public string AsynchMosaic(List<string> asInputFiles, string sOutputFile)
		{
			return InternalMosaic(true, asInputFiles, sOutputFile);
		}

		/// <summary>
		/// Asynch Mosaic with also No Data Params.
		/// </summary>
		/// <param name="asInputFiles">the list of input files to mosaic</param>
		/// <param name="sOutputFile">the name of the mosaic output file</param>
		/// <param name="sNoDataValue">the value to use in output as no data</param>
		/// <param name="sInputIgnoreValue">the value to use as input no data</param>
		/// <returns>the process Id</returns>
		public string AsynchMosaic(List<string> asInputFiles, string sOutputFile, string sNoDataValue, string sInputIgnoreValue)
		{
			return InternalMosaic(true, asInputFiles, sOutputFile, sNoDataValue, sInputIgnoreValue);
		}

		/// <summary>
		/// Asynch Mosaic with all the input parameters.
		/// </summary>
		/// <param name="asInputFiles">the list of input files to mosaic</param>
		/// <param name="sOutputFile">the name of the mosaic output file</param>
		/// <param name="sNoDataValue">the value to use in output as no data</param>
		/// <param name="sInputIgnoreValue">the value to use as input no data</param>
		/// <param name="dPixelSizeX">the X Pixel Size</param>
		/// <param name="dPixelSizeY">the Y Pixel Size</param>
		/// <returns>the process Id</returns>
		public string AsynchMosaic(List<string> asInputFiles, string sOutputFile, string sNoDataValue, string sInputIgnoreValue, double dPixelSizeX, double dPixelSizeY)
		{
			return InternalMosaic(true, asInputFiles, sOutputFile, sNoDataValue, sInputIgnoreValue, dPixelSizeX, dPixelSizeY);
		}

		/// <summary>
		/// Search EO-Images
		/// </summary>
		/// <param name="sPlatform">the Satellite Platform. Accepts "S1","S2","S3","S5P","ENVI","L8","VIIRS","ERA5"</param>
		/// <param name="sDateFrom">the Starting date in format "YYYY-MM-DD"</param>
		/// <param name="sDateTo">the End date in format "YYYY-MM-DD"</param>
		/// <param name="dULLat">Upper Left Lat Coordinate. Can be null.</param>
		/// <param name="dULLon">Upper Left Lon Coordinate. Can be null.</param>
		/// <param name="dLRLat">Lower Right Lat Coordinate. Can be null..</param>
		/// <param name="dLRLon">Lower Right Lon Coordinate. Can be null.</param>
		/// <param name="sProductType">the Product Type. If Platform = "S1" -> Accepts "SLC","GRD", "OCN". If Platform = "S2" -> Accepts "S2MSI1C","S2MSI2Ap","S2MSI2A". Can be null.</param>
		/// <param name="iOrbitNumber">the Sentinel Orbit Number. Can be null.</param>
		/// <param name="sSensorOperationalMode">the Sensor Operational Mode. ONLY for S1. Accepts -> "SM", "IW", "EW", "WV". Can be null. Ignored for Platform "S2"</param>
		/// <param name="sCloudCoverage">the Cloud Coverage. Sample syntax: [0 TO 9.4]</param>
		/// <returns>the list of the available products</returns>
		public List<QueryResultViewModel> SearchEOImages(string? sPlatform, string? sDateFrom, string? sDateTo, Double? dULLat, Double? dULLon, Double? dLRLat, Double? dLRLon,
			string? sProductType, int? iOrbitNumber, string? sSensorOperationalMode, string? sCloudCoverage)
		{
			_logger.LogDebug($"SearchEOImages()( {sPlatform}, [ {sDateFrom}, {sDateTo} ], " +
				$"[ {dULLat}, {dULLon}, {dLRLat}, {dLRLon} ], " +
				$"product type: {sProductType}, " +
				$"orbit: {iOrbitNumber}, " +
				$"sensor mode: {sSensorOperationalMode}, " +
				$"cloud coverage: {sCloudCoverage}");

			List<QueryResultViewModel> aoReturnList = new List<QueryResultViewModel>();

			if (sPlatform == null)
			{
				_logger.LogError("SearchEOImages: platform cannot be null");
				return aoReturnList;
			}

			if (!(sPlatform.Equals("S1") || sPlatform.Equals("S2") || sPlatform.Equals("S3") || sPlatform.Equals("S5P") || sPlatform.Equals("ENVI") || sPlatform.Equals("VIIRS") || sPlatform.Equals("ERA5") || sPlatform.Equals("L8")))
				_logger.LogError("SearchEOImages: platform should be one of S1, S2, S3, S5P, ENVI, VIIRS, ERA5, L8. Received [" + sPlatform + "]");

			if (sPlatform.Equals("S1"))
				if (sProductType != null)
					if (!(sProductType.Equals("SLC") || sProductType.Equals("GRD") || sProductType.Equals("OCN")))
						_logger.LogError("SearchEOImages: Available Product Types for S1; SLC, GRD, OCN. Received [" + sProductType + "]");

			if (sPlatform.Equals("S2"))
				if (sProductType != null)
					if (!(sProductType.Equals("S2MSI1C") || sProductType.Equals("S2MSI2Ap") || sProductType.Equals("S2MSI2A")))
						_logger.LogError("SearchEOImages: Available Product Types for S2; S2MSI1C, S2MSI2Ap, S2MSI2A. Received [" + sProductType + "]");

			if (sPlatform.Equals("S3"))
				if (sProductType != null)
					if (!(sProductType.Equals("SR_1_SRA___") || sProductType.Equals("SR_1_SRA_A_") || sProductType.Equals("SR_1_SRA_BS") || sProductType.Equals("SR_2_LAN___")))
						_logger.LogError("SearchEOImages: Available Product Types for S3; SR_1_SRA___, SR_1_SRA_A_, SR_1_SRA_BS, SR_2_LAN___. Received [" + sProductType + "]");

			if (sDateFrom == null)
			{
				_logger.LogError("SearchEOImages: sDateFrom cannot be null");
				return aoReturnList;
			}

			if (sDateFrom.Length < 10)
			{
				_logger.LogError("SearchEOImages: sDateFrom must be in format YYYY-MM-DD");
				return aoReturnList;
			}

			if (sDateTo == null)
			{
				_logger.LogError("SearchEOImages: sDateTo cannot be null");
				return aoReturnList;
			}

			if (sDateTo.Length < 10)
			{
				_logger.LogError("SearchEOImages: sDateTo must be in format YYYY-MM-DD");
				return aoReturnList;
			}


			// Create Query String:

			// Platform name for sure
			string sQuery = "(platformname:";
			if (sPlatform.Equals("S1")) sQuery += "Sentinel-1";
			else if (sPlatform.Equals("S2")) sQuery += "Sentinel-2";
			else if (sPlatform.Equals("S3")) sQuery += "Sentinel-3";
			else if (sPlatform.Equals("S5P")) sQuery += "Sentinel-5P";
			else if (sPlatform.Equals("ENVI")) sQuery += "Envisat";
			else if (sPlatform.Equals("L8")) sQuery += "Landsat-*";
			else if (sPlatform.Equals("VIIRS")) sQuery += "VIIRS";
			else if (sPlatform.Equals("ERA5")) sQuery += "ERA5";
			else sQuery += sPlatform;

			// If available add product type
			if (sProductType != null)
				sQuery += " AND producttype:" + sProductType;
			else if (sPlatform.Equals("VIIRS"))
				sQuery += " AND producttype:VIIRS_1d_composite";

			// If available Sensor Operational Mode
			if (sSensorOperationalMode != null && sPlatform.Equals("S1"))
				sQuery += " AND sensoroperationalmode:" + sSensorOperationalMode;

			// If available cloud coverage
			if (sCloudCoverage != null && sCloudCoverage != "" && sPlatform.Equals("S2"))
				sQuery += " AND cloudcoverpercentage:" + sCloudCoverage;

			// If available add orbit number
			if (iOrbitNumber != null)
				sQuery += " AND relativeorbitnumber:" + iOrbitNumber;

			// Close the first block
			sQuery += ") ";

			// Date Block
			sQuery += "AND ( beginPosition:[" + sDateFrom + "T00:00:00.000Z TO " + sDateTo + "T23:59:59.999Z]";
			sQuery += " AND endPosition:[" + sDateFrom + "T00:00:00.000Z TO " + sDateTo + "T23:59:59.999Z]";

			// Close the second block
			sQuery += ") ";

			if (dULLat != null && dULLon != null && dLRLat != null && dLRLon != null)
			{
				string sFootPrint = "( footprint:\"intersects(POLYGON((" + dULLon + " " + dLRLat + "," + dULLon + " " + dULLat + "," + dLRLon + " " + dULLat + "," + dLRLon + " " + dLRLat + "," + dULLon + " " + dLRLat + ")))\") AND ";
				sQuery = sFootPrint + sQuery;
			}

			String sQueryBody = "[\"" + sQuery.Replace("\"", "\\\"") + "\"]";
			sQuery = "providers=" + GetDefaultProvider();

			String sUrl = $"{m_sBaseUrl}/search/querylist?{sQuery}";

			try
			{
				aoReturnList = _wasdiService.SearchQueryList(sUrl, m_sSessionId, sQueryBody);
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
			}

			return aoReturnList;
		}

		/// <summary>
		/// Get the name of a Product found by searchEOImage.
		/// </summary>
		/// <param name="oProduct">the Product as returned by searchEOImage</param>
		/// <returns>the name of the product</returns>
		public string GetFoundProductName(QueryResultViewModel? oProduct)
		{
			if (oProduct == null)
				return "";

			return oProduct.Title;
		}

		/// <summary>
		/// Get the name of a Product found by searchEOImage.
		/// </summary>
		/// <param name="oProduct">the JSON Dictionary Product as returned by searchEOImage</param>
		/// <returns>the name of the product</returns>
		public string GetFoundProductName(Dictionary<string, object>? oProduct)
		{
			if (oProduct == null)
				return "";

			if (!oProduct.ContainsKey("title"))
				return "";

			return (string)oProduct["title"];
		}

		/// <summary>
		/// Get the direct download link of a Product found by searchEOImage.
		/// </summary>
		/// <param name="oProduct">the Product as returned by searchEOImage</param>
		/// <returns>the link of the product</returns>
		public string GetFoundProductLink(QueryResultViewModel? oProduct)
		{
			if (oProduct == null)
				return "";

			return oProduct.Link;
		}

		/// <summary>
		/// Get the direct download link of a Product found by searchEOImage.
		/// </summary>
		/// <param name="oProduct">the JSON Dictionary Product as returned by searchEOImage</param>
		/// <returns>the link of the product</returns>
		public string GetFoundProductLink(Dictionary<string, object>? oProduct)
		{
			if (oProduct == null)
				return "";

			if (!oProduct.ContainsKey("link"))
				return "";

			return (string)oProduct["link"];
		}

		/// <summary>
		/// Get the footprint of a Product found by searchEOImage.
		/// </summary>
		/// <param name="oProduct">the Product as returned by searchEOImage</param>
		/// <returns>the footprint of the product</returns>
		public string GetFoundProductFootprint(QueryResultViewModel? oProduct)
		{
			if (oProduct == null)
				return "";

			return oProduct.Footprint;
		}


		/// <summary>
		/// Get the footprint of a Product found by searchEOImage.
		/// </summary>
		/// <param name="oProduct">the JSON Dictionary Product as returned by searchEOImage</param>
		/// <returns>the footprint of the product</returns>
		public string GetFoundProductFootprint(Dictionary<string, object>? oProduct)
		{
			if (oProduct == null)
				return "";

			if (!oProduct.ContainsKey("footprint"))
				return "";

			return (string)oProduct["footprint"];
		}

		/// <summary>
		/// Asynchronously import a product.
		/// </summary>
		/// <param name="oProduct">the product to be imported</param>
		/// <returns>the status of the Import process</returns>
		public string AsynchImportProduct(Dictionary<string, object> oProduct)
		{
			_logger.LogDebug("AsynchImportProduct (with map)");

			return AsynchImportProduct(oProduct, null);
		}

		/// <summary>
		/// Asynchronously import a product.
		/// </summary>
		/// <param name="oProduct">the product to be imported</param>
		/// <param name="sProvider">the provider of choice. If null, the default provider will be used</param>
		/// <returns>the status of the Import process</returns>
		public string AsynchImportProduct(Dictionary<string, object> oProduct, string sProvider)
		{
			_logger.LogDebug($"AsynchImportProduct ( oProduct, {sProvider} )");

			string sReturn = "ERROR";

			try
			{
				// Get URL And Bounding Box from the JSON representation
				string sFileUrl = GetFoundProductLink(oProduct);
				string sBoundingBox = GetFoundProductFootprint(oProduct);
				string sName = GetFoundProductName(oProduct);

				return AsynchImportProduct(sFileUrl, sName, sBoundingBox, sProvider);
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
			}

			return sReturn;
		}

		/// <summary>
		/// Import a Product from a Provider in WASDI.
		/// </summary>
		/// <param name="oProduct">the product to be imported</param>
		/// <returns>the status of the Import process</returns>
		public string ImportProduct(Dictionary<string, object> oProduct)
		{
			_logger.LogDebug("ImportProduct()");

			string sReturn = "ERROR";

			try
			{
				// Get URL And Bounding Box from the JSON representation
				string sFileUrl = GetFoundProductLink(oProduct);
				string sBoundingBox = GetFoundProductFootprint(oProduct);
				string sName = GetFoundProductName(oProduct);

				return ImportProduct(sFileUrl, sName, sBoundingBox);
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
			}

			return sReturn;
		}

		/// <summary>
		/// Import a Product from a Provider in WASDI.
		/// </summary>
		/// <param name="oProduct">the product to be imported</param>
		/// <returns>the status of the Import process</returns>
		public string ImportProduct(QueryResultViewModel oProduct)
		{
			_logger.LogDebug("ImportProduct()");

			string sReturn = "ERROR";

			try
			{
				// Get URL And Bounding Box from the JSON representation
				string sFileUrl = oProduct.Link;
				string sBoundingBox = oProduct.Footprint;
				string sName = oProduct.Title;

				return ImportProduct(sFileUrl, sName, sBoundingBox);
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
			}

			return sReturn;
		}

		/// <summary>
		/// Import a Product from a Provider in WASDI.
		/// </summary>
		/// <param name="sFileUrl">the Direct link of the product</param>
		/// <param name="sFileName">the name of the file</param>
		/// <returns>the status of the Import process</returns>
		public string ImportProduct(string sFileUrl, string sFileName)
		{
			return ImportProduct(sFileUrl, sFileName, "");
		}

		/// <summary>
		/// Import a Product from a Provider in WASDI asynchronously.
		/// </summary>
		/// <param name="sFileUrl">the Direct link of the product</param>
		/// <param name="sFileName">the name of the file</param>
		/// <returns>the status of the Import process</returns>
		public string AsynchImportProduct(string sFileUrl, string? sFileName)
		{
			return AsynchImportProduct(sFileUrl, sFileName, "");
		}

		/// <summary>
		/// Import a Product from a Provider in WASDI asynchronously.
		/// </summary>
		/// <param name="sFileUrl">the Direct link of the product</param>
		/// <param name="sFileName">the name of the file</param>
		/// <param name="sBoundingBox">the bounding box</param>
		/// <returns>the status of the Import process</returns>
		public string AsynchImportProduct(string sFileUrl, string? sFileName, string sBoundingBox)
		{
			return AsynchImportProduct(sFileUrl, sFileName, sBoundingBox, null);
		}

		/// <summary>
		/// Import a Product from a Provider in WASDI.
		/// </summary>
		/// <param name="sFileUrl">the Direct link of the product</param>
		/// <param name="sFileName">the name of the file</param>
		/// <param name="sBoundingBox">the bounding box</param>
		/// <param name="sProvider">the provider</param>
		/// <returns>the status of the Import process</returns>
		public string AsynchImportProduct(string sFileUrl, string? sFileName, string sBoundingBox, string? sProvider)
		{
			_logger.LogDebug("AsynchImportProduct()");

			String sReturn = "ERROR";

			try
			{
				if (String.IsNullOrEmpty(sProvider))
					sProvider = GetDefaultProvider();

				PrimitiveResult? primitiveResult =
					_wasdiService.FilebufferDownload(m_sBaseUrl, m_sSessionId, m_sActiveWorkspace, sProvider, sFileUrl, sFileName, sBoundingBox);

				if (primitiveResult != null)
					if (primitiveResult.BoolValue)
						sReturn = primitiveResult.StringValue;
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
			}

			return sReturn;
		}

		/// <summary>
		/// Import a Product from a Provider in WASDI.
		/// </summary>
		/// <param name="sFileUrl">the Direct link of the product</param>
		/// <param name="sFileName">the name of the file</param>
		/// <param name="sBoundingBox">the bounding box</param>
		/// <returns>the status of the Import process</returns>
		public string ImportProduct(string sFileUrl, string sFileName, string sBoundingBox)
		{
			_logger.LogDebug("AsynchImportProduct()");

			return ImportProduct(sFileUrl, sFileName, sBoundingBox, null);
		}

		/// <summary>
		/// Import a Product from a Provider in WASDI.
		/// </summary>
		/// <param name="sFileUrl">the Direct link of the product</param>
		/// <param name="sFileName">the name of the file</param>
		/// <param name="sBoundingBox">the bounding box</param>
		/// <param name="sProvider">the provider</param>
		/// <returns>the status of the Import process</returns>
		public string ImportProduct(string sFileUrl, string sFileName, string sBoundingBox, string? sProvider)
		{
			_logger.LogDebug("ImportProduct()");

			string sReturn = "ERROR";

			try
			{
				string sProcessId = AsynchImportProduct(sFileUrl, sFileName, sBoundingBox, sProvider);
				sReturn = WaitProcess(sProcessId);

				// Return the status of the import WASDI process
				return sReturn;
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
			}

			return sReturn;
		}

		/// <summary>
		/// Imports a list of product asynchronously.
		/// </summary>
		/// <param name="aoProductsToImport">the list of products to import</param>
		/// <returns>a list of String containing the WASDI process ids of all the imports</returns>
		public List<string>? AsynchImportProductListWithMaps(List<Dictionary<string, object>>? aoProductsToImport)
		{
			_logger.LogDebug("AsynchImportProductListWithMaps()");

			if (aoProductsToImport == null)
			{
				return null;
			}

			List<string> asIds = aoProductsToImport.Select(p => AsynchImportProduct(p)).ToList();

			return asIds;
		}

		/// <summary>
		/// Imports a list of product asynchronously.
		/// </summary>
		/// <param name="aoProductsToImport">the list of products to import</param>
		/// <param name="asNames">the list of names</param>
		/// <returns>a list of String containing the WASDI process ids of all the imports</returns>
		public List<string>? AsynchImportProductList(List<string>? asProductsToImport, List<string>? asNames)
		{
			_logger.LogDebug("AsynchImportProductList ( with list )");

			if (asProductsToImport == null)
			{
				_logger.LogError("AsynchImportProductList: list is null, aborting");
				return null;
			}

			if (asProductsToImport.Count == 0)
			{
				_logger.LogError("AsynchImportProductList: list has no elements, aborting");
				return null;
			}

			_logger.LogDebug($"AsynchImportProductList: list has {asProductsToImport.Count} elements");

			List<string> asIds = new List<string>(asProductsToImport.Count);

			for (int i = 0; i < asProductsToImport.Count; i++)
			{
				string sProductUrl = asProductsToImport.ElementAt(i);
				string? sName = null;

				if (asNames != null)
				{
					if (i < asNames.Count)
					{
						sName = asNames.ElementAt(i);
					}
				}

				asIds.Add(AsynchImportProduct(sProductUrl, sName));
			}

			return asIds;
		}

		/// <summary>
		/// Imports a list of products.
		/// </summary>
		/// <param name="aoProductsToImport">the list of products to import</param>
		/// <returns>a list of String containing the WASDI process ids of all the imports</returns>
		public List<string>? ImportProductListWithMaps(List<Dictionary<string, object>> aoProductsToImport)
		{
			return WaitProcesses(AsynchImportProductListWithMaps(aoProductsToImport));
		}

		/// <summary>
		/// Imports a list of products.
		/// </summary>
		/// <param name="aoProductsToImport">the list of products to import</param>
		/// <param name="asNames">the list of names</param>
		/// <returns>a list of String containing the WASDI process ids of all the imports</returns>
		public List<string> ImportProductList(List<string> asProductsToImport, List<string> asNames)
		{
			return WaitProcesses(AsynchImportProductList(asProductsToImport, asNames));
		}

		/// <summary>
		/// Make a Subset (tile) of an input image in a specified Lat Lon Rectangle.
		/// </summary>
		/// <param name="sInputFile">the name of the input file</param>
		/// <param name="sOutputFile">the name of the output file</param>
		/// <param name="dLatN">the Lat North Coordinate</param>
		/// <param name="dLonW">the Lon West Coordinate</param>
		/// <param name="dLatS">the Lat South Coordinate</param>
		/// <param name="dLonE">the Lon East Coordinate</param>
		/// <returns>the status of the operation</returns>
		public string Subset(string? sInputFile, string? sOutputFile, double dLatN, double dLonW, double dLatS, double dLonE)
		{
			_logger.LogDebug("Subset()");


			// Check minimun input values
			if (sInputFile == null)
			{
				_logger.LogError("input file must not be null");
				return "";
			}

			if (sInputFile == "")
			{
				_logger.LogError("input file must not be empty");
				return "";
			}

			if (sOutputFile == null)
			{
				_logger.LogError("sOutputFile must not be null");
				return "";
			}

			if (sOutputFile == "")
			{
				_logger.LogError("sOutputFile must not empty string");
				return "";
			}


			// Fill the Setting Object
			string sSubsetSetting = "{ \"latN\":" + dLatN + ", \"lonW\":" + dLonW + ", \"latS\":" + dLatS + ", \"lonE\":" + dLonE + " }";


			try
			{
				PrimitiveResult? primitiveResult = _wasdiService.ProcessingSubset(m_sBaseUrl, m_sSessionId, m_sActiveWorkspace, sInputFile, sOutputFile, sSubsetSetting);

				if (primitiveResult != null)
				{
					string? sProcessId = primitiveResult.StringValue;
					if (sProcessId != null)
					{
						// Return process output status
						return WaitProcess(sProcessId);
					}
				}

			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
			}

			return "ERROR";
		}

		/// <summary>
		/// Creates a Many Subsets from an image. MAX 10 TILES PER CALL. Assumes big tiff format by default.
		/// </summary>
		/// <param name="sInputFile">the name of the input file</param>
		/// <param name="asOutputFiles">the list of names of the output files</param>
		/// <param name="adLatN">the list of Lat North Coordinates</param>
		/// <param name="adLonW">the list of Lon West Coordinates</param>
		/// <param name="adLatS">the list of Lat South Coordinates</param>
		/// <param name="adLonE">the list of Lon East Coordinates</param>
		/// <returns>the status of the operation</returns>
		public string MultiSubset(string sInputFile, List<string> asOutputFiles, List<Double> adLatN, List<Double> adLonW, List<Double> adLatS, List<Double> adLonE)
		{
			_logger.LogDebug($"MultiSubset( {sInputFile}, asOutputFiles, adLatN, adLonW, adLatS, adLonE )");
			return WaitProcess(AsynchMultiSubset(sInputFile, asOutputFiles, adLatN, adLonW, adLatS, adLonE));
		}

		/// <summary>
		/// Creates a Many Subsets from an image. MAX 10 TILES PER CALL.
		/// </summary>
		/// <param name="sInputFile">the name of the input file</param>
		/// <param name="asOutputFiles">the list of names of the output files</param>
		/// <param name="adLatN">the list of Lat North Coordinates</param>
		/// <param name="adLonW">the list of Lon West Coordinates</param>
		/// <param name="adLatS">the list of Lat South Coordinates</param>
		/// <param name="adLonE">the list of Lon East Coordinates</param>
		/// <param name="bBigTiff">flag indicating whether to use the bigtiff format, for files bigger than 4 GB</param>
		/// <returns>the status of the operation</returns>
		public string MultiSubset(string sInputFile, List<string> asOutputFiles, List<Double> adLatN, List<Double> adLonW, List<Double> adLatS, List<Double> adLonE, bool bBigTiff)
		{
			_logger.LogDebug($"MultiSubset( {sInputFile}, asOutputFiles, adLatN, adLonW, adLatS, adLonE, {bBigTiff} )");
			return WaitProcess(AsynchMultiSubset(sInputFile, asOutputFiles, adLatN, adLonW, adLatS, adLonE, bBigTiff));
		}

		/// <summary>
		/// Asynchronous multisubset: creates a Many Subsets from an image. MAX 10 TILES PER CALL. Assumes big tiff format by default.
		/// </summary>
		/// <param name="sInputFile">the name of the input file</param>
		/// <param name="asOutputFiles">the list of names of the output files</param>
		/// <param name="adLatN">the list of Lat North Coordinates</param>
		/// <param name="adLonW">the list of Lon West Coordinates</param>
		/// <param name="adLatS">the list of Lat South Coordinates</param>
		/// <param name="adLonE">the list of Lon East Coordinates</param>
		/// <returns>the status of the operation</returns>
		public string AsynchMultiSubset(string sInputFile, List<string> asOutputFiles, List<Double> adLatN, List<Double> adLonW, List<Double> adLatS, List<Double> adLonE)
		{
			_logger.LogDebug($"AsynchMultiSubset( {sInputFile}, asOutputFiles, adLatN, adLonW, adLatS, adLonE )");
			return AsynchMultiSubset(sInputFile, asOutputFiles, adLatN, adLonW, adLatS, adLonE, true);
		}

		/// <summary>
		/// Asynchronous multisubset: creates a Many Subsets from an image. MAX 10 TILES PER CALL.
		/// </summary>
		/// <param name="sInputFile">the name of the input file</param>
		/// <param name="asOutputFiles">the list of names of the output files</param>
		/// <param name="adLatN">the list of Lat North Coordinates</param>
		/// <param name="adLonW">the list of Lon West Coordinates</param>
		/// <param name="adLatS">the list of Lat South Coordinates</param>
		/// <param name="adLonE">the list of Lon East Coordinates</param>
		/// <param name="bBigTiff">flag indicating whether to use the bigtiff format, for files bigger than 4 GB</param>
		/// <returns>the status of the operation</returns>
		public string? AsynchMultiSubset(string? sInputFile, List<string>? asOutputFiles, List<Double>? adLatN, List<Double>? adLonW, List<Double>? adLatS, List<Double>? adLonE, bool bBigTiff)
		{
			_logger.LogDebug($"AsynchMultiSubset( {sInputFile}, asOutputFiles, adLatN, adLonW, adLatS, adLonE, {bBigTiff} )");


			if (String.IsNullOrEmpty(sInputFile))
			{
				_logger.LogError("MultiSubset: input file null or empty, aborting");
				return null;
			}

			if (asOutputFiles == null || asOutputFiles.Count == 0)
			{
				_logger.LogError("Multisubset: output files null or empty, aborting");
				return null;
			}

			if (adLonW == null || adLonW.Count == 0)
			{
				_logger.LogError("Multisubset: adLonW null or empty, aborting");
				return null;
			}

			if (adLonW == null || adLonW.Count == 0)
			{
				_logger.LogError("Multisubset: adLonW null or empty, aborting");
				return null;
			}

			if (adLatS == null || adLatS.Count == 0)
			{
				_logger.LogError("Multisubset: adLatS null or empty, aborting");
				return null;
			}

			if (adLonE == null || adLonE.Count == 0)
			{
				_logger.LogError("Multisubset: adLonE null or empty, aborting");
				return null;
			}

			Dictionary<string, object> aoPayload = new Dictionary<string, object>();

			if (asOutputFiles != null)
				aoPayload.Add("outputNames", asOutputFiles);

			if (adLatN != null)
				aoPayload.Add("latNList", adLatN);

			if (adLonW != null)
				aoPayload.Add("lonWList", adLonW);

			if (adLatS != null)
				aoPayload.Add("latSList", adLatS);

			if (adLonE != null)
				aoPayload.Add("lonEList", adLonE);

			if (bBigTiff)
				aoPayload.Add("bigTiff", true);


			bool bIsOnServer = GetIsOnServer();
			string sProcessId = GetMyProcId();

			try
			{
				PrimitiveResult? primitiveResult = _wasdiService.ProcessingMultisubset(m_sBaseUrl, m_sSessionId, m_sActiveWorkspace, bIsOnServer, sInputFile, sProcessId, aoPayload);

				if (primitiveResult != null)
					return primitiveResult.StringValue;
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
			}

			return null;
		}

		/// <summary>
		/// Executes a synchronous process, i.e., runs the process and waits for it to complete.
		/// </summary>
		/// <param name="sProcessorName">the name of the processor</param>
		/// <param name="aoParams">the dictionary of params</param>
		/// <returns>the WASDI processor ID</returns>
		public string ExecuteProcessor(string sProcessorName, Dictionary<string, object> aoParams)
		{
			return WaitProcess(AsynchExecuteProcessor(sProcessorName, aoParams));
		}

		/// <summary>
		/// Execute a WASDI processor in Asynch way.
		/// </summary>
		/// <param name="sProcessorName">the name of the processor</param>
		/// <param name="aoParams">the dictionary of params</param>
		/// <returns>the WASDI processor ID</returns>
		public string AsynchExecuteProcessor(string sProcessorName, Dictionary<string, object>? aoParams)
		{
			_logger.LogDebug($"AsynchExecuteProcessor( {sProcessorName}, Map<string, object> aoParams )");

			try
			{
				// Initialize
				string sParamsJson = "";

				// Convert dictionary in a JSON
				if (aoParams != null)
					sParamsJson = SerializationHelper.ToJson(aoParams);

				// Encode the params
				sParamsJson = HttpUtility.UrlEncode(sParamsJson);

				// Use the string version
				return AsynchExecuteProcessor(sProcessorName, sParamsJson);
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
				return "";
			}
		}

		/// <summary>
		/// Executes a synchronous process, i.e., runs the process and waits for it to complete.
		/// </summary>
		/// <param name="sProcessorName">the name of the processor</param>
		/// <param name="sEncodedParams">a JSON formatted string of parameters for the processor</param>
		/// <returns>the WASDI processor ID</returns>
		public string ExecuteProcessor(string sProcessorName, string sEncodedParams)
		{
			return WaitProcess(AsynchExecuteProcessor(sProcessorName, sEncodedParams));
		}

		/// <summary>
		/// Execute a WASDI processor in Asynch way.
		/// </summary>
		/// <param name="sProcessorName">the name of the processor</param>
		/// <param name="sEncodedParams">a JSON formatted string of parameters for the processor</param>
		/// <returns>the WASDI processor ID</returns>
		public string AsynchExecuteProcessor(string? sProcessorName, string? sEncodedParams)
		{

			_logger.LogDebug($"AsynchExecuteProcessor( {sProcessorName}, {sEncodedParams} )");

			//Domain check
			if (sProcessorName == null)
			{
				_logger.LogError("ProcessorName is null, return");
				return "";
			}

			if (sProcessorName == "")
			{
				_logger.LogError("ProcessorName is empty, return");
				return "";
			}

			if (sEncodedParams == null)
			{
				_logger.LogError("EncodedParams is null, return");
				return "";
			}

			RunningProcessorViewModel? primitiveResult = _wasdiService.ProcessorsRun(m_sBaseUrl, m_sSessionId, m_sActiveWorkspace, sProcessorName, sEncodedParams);

			if (primitiveResult != null)
			{
				string sProcessId = primitiveResult.ProcessingIdentifier;
				return sProcessId;
			}

			return "";
		}

		/// <summary>
		/// Delete a Product in the active Workspace.
		/// </summary>
		/// <param name="sProduct">the product's name</param>
		/// <returns>the status of the operation</returns>
		public string DeleteProduct(string sProduct)
		{
			_logger.LogDebug($"DeleteProduct({sProduct})");

			try
			{
				PrimitiveResult? primitiveResult = _productService.DeleteProduct(m_sWorkspaceBaseUrl, m_sSessionId, m_sActiveWorkspace, sProduct);
			
				if (primitiveResult != null)
					return primitiveResult.StringValue;
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
			}

			return "ERROR";
		}

		/// <summary>
		/// Write one row of Log.
		/// </summary>
		/// <param name="sLogRow">the text to log</param>
		public void WasdiLog(string? sLogRow)
		{
			if (m_bIsOnServer)
			{
				// Check minimun input values
				if (sLogRow == null)
				{
					_logger.LogError("Log line null, aborting");
					return;
				}

				if (sLogRow == "")
				{
					_logger.LogError("Log line empty, aborting");
					return;
				}


				try
				{
					_wasdiService.AddProcessorsLog(GetWorkspaceBaseUrl(), m_sSessionId, GetMyProcId(), sLogRow);
				}
				catch (Exception ex)
				{
					_logger.LogError(ex.StackTrace);
				}
			}
			else
				_logger.LogInformation(sLogRow);
		}

		/// <summary>
		/// Get the processor Path.
		/// The value should resemble the following path: C:/dev/WasdiLib.Client/bin/Debug/net6.0
		/// </summary>
		/// <returns>the processor path</returns>
		public string? GetProcessorPath()
		{
			_logger.LogDebug("GetProcessorPath");

			try
			{
				string sExeFilePath = System.Reflection.Assembly.GetExecutingAssembly().Location;
				string? sWorkPath = Path.GetDirectoryName(sExeFilePath);

				return sWorkPath;
			}
			catch (Exception ex) {
				_logger.LogError($"Exception in get Processor Path {ex.Message}");
				return "";
			}
		}

		/// <summary>
		/// Create a workspace using the provided name.
		/// Once the workspace is created, it is also opened.
		/// </summary>
		/// <param name="sWorkspaceName">the name of the workspace</param>
		/// <returns>the Id of the newly created workspace or empty string in case of any issues</returns>
		public string CreateWorkspace(string sWorkspaceName)
		{
			_logger.LogDebug($"CreateWorkspace( {sWorkspaceName} )");

			return CreateWorkspace(sWorkspaceName, null);
		}

		/// <summary>
		/// Create a workspace using the provided name on the indicated node.
		/// Once the workspace is created, it is also opened.
		/// </summary>
		/// <param name="sWorkspaceName">the name of the workspace</param>
		/// <param name="nodeCode">the node on which to create the workspace</param>
		/// <returns>the Id of the newly created workspace or empty string in case of any issues</returns>
		public string? CreateWorkspace(string? workspaceName, string? nodeCode = null)
		{
			_logger.LogDebug($"CreateWorkspace({workspaceName}, {nodeCode})");

			if (workspaceName == null)
			{
				workspaceName = "";
			}

			if (workspaceName == "")
			{
				_logger.LogInformation("CreateWorkspace: WARNING workspace name null or empty, it will be defaulted");
			}

			string? sReturn = null;

			try
			{
				PrimitiveResult? primitiveResult = _workspaceService.CreateWorkspace(m_sBaseUrl, m_sSessionId, workspaceName, nodeCode);

				if (primitiveResult != null)
					sReturn = primitiveResult.StringValue;
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
			}


			if (sReturn != null)
				OpenWorkspaceById(sReturn);
			else
				_logger.LogError("CreateWorkspace: creation failed.");

			return sReturn;
		}

		/// <summary>
		/// Deletes the workspace given its Id.
		/// </summary>
		/// <param name="workspaceId">the Id of the workspace</param>
		/// <returns>the Id of the workspace as a String if succesful, empty string otherwise</returns>
		public string? DeleteWorkspace(string? workspaceId)
		{
			_logger.LogDebug($"DeleteWorkspace({workspaceId})");

			if (String.IsNullOrEmpty(workspaceId))
			{
				_logger.LogError("DeleteWorkspace: none passed, aborting");
				return null;
			}

			string? sResult = null;

			try
			{
				sResult = _workspaceService.DeleteWorkspace(m_sBaseUrl, m_sSessionId, workspaceId);

				if (sResult == null)
					_logger.LogError("DeleteWorkspace: could not delete workspace (please check the return value, it's going to be null)");
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
			}

			return sResult;
		}

		/// <summary>
		/// Get the process workspaces by workspace id
		/// </summary>
		/// <param name="workspaceId">the Id of the workspace</param>
		/// <returns>the list of process workspaces or an empty list in case of any issues</returns>
		public List<ProcessWorkspace> GetProcessWorkspacesByWorkspaceId(string workspaceId)
		{
			_logger.LogDebug($"GetProcessWorkspacesByWorkspaceId({workspaceId})");

			List<ProcessWorkspace> ProcessWorkspaceList = new List<ProcessWorkspace>();

			try
			{
				ProcessWorkspaceList = _processWorkspaceService.GetProcessWorkspacesByWorkspaceId(GetWorkspaceBaseUrl(), m_sSessionId, workspaceId);
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
			}

			return ProcessWorkspaceList;
		}

		/// <summary>
		/// Get a paginated list of processes in the active workspace, each element of which is a JSON string.
		/// </summary>
		/// <param name="iStartIndex">the start index of the process (0 by default is the last one)</param>
		/// <param name="iEndIndex">the end index of the process (optional)</param>
		/// <param name="sStatus">the status filter, null by default. Can be CREATED,  RUNNING,  STOPPED,  DONE,  ERROR, WAITING, READY</param>
		/// <param name="sOperationType">the Operation Type Filter, null by default. Can be RUNPROCESSOR, RUNIDL, RUNMATLAB, INGEST, DOWNLOAD, GRAPH, DEPLOYPROCESSOR</param>
		/// <param name="sNamePattern">the Name filter. The name meaning depends by the operation type, null by default. For RUNPROCESSOR, RUNIDL and RUNMATLAB is the name of the application</param>
		/// <returns>a list of process IDs</returns>
		public List<string> GetProcessesByWorkspaceAsListOfJson(int iStartIndex, Int32 iEndIndex, string sStatus, string sOperationType, string sNamePattern)
		{
			_logger.LogDebug($"GetProcessesByWorkspaceAsListOfJson({iStartIndex}, {iEndIndex}, {sStatus}, {sOperationType}, {sNamePattern})");

			List<ProcessWorkspace> ProcessWorkspaceList = GetProcessesByWorkspace(iStartIndex, iEndIndex, sStatus, sOperationType, sNamePattern);

			List<string> asJson = new List<string>(ProcessWorkspaceList.Count);

			foreach (ProcessWorkspace processWorkspace in ProcessWorkspaceList)
				asJson.Add(SerializationHelper.ToJson(processWorkspace));

			return asJson;
		}

		/// <summary>
		/// Get a paginated list of processes in the active workspace.
		/// </summary>
		/// <param name="iStartIndex">the start index of the process (0 by default is the last one)</param>
		/// <param name="iEndIndex">the end index of the process (optional)</param>
		/// <param name="sStatus">the status filter, null by default. Can be CREATED,  RUNNING,  STOPPED,  DONE,  ERROR, WAITING, READY</param>
		/// <param name="sOperationType">the Operation Type Filter, null by default. Can be RUNPROCESSOR, RUNIDL, RUNMATLAB, INGEST, DOWNLOAD, GRAPH, DEPLOYPROCESSOR</param>
		/// <param name="sNamePattern">the Name filter. The name meaning depends by the operation type, null by default. For RUNPROCESSOR, RUNIDL and RUNMATLAB is the name of the application</param>
		/// <returns>a list of process IDs</returns>
		public List<ProcessWorkspace> GetProcessesByWorkspace(int iStartIndex, Int32 iEndIndex, string sStatus, string sOperationType, string sNamePattern)
		{
			_logger.LogDebug($"GetProcessesByWorkspace({m_sActiveWorkspace}, {iStartIndex}, {iEndIndex}, {sStatus}, {sOperationType}, {sNamePattern})");

			List<ProcessWorkspace> processWorkspaceList = new List<ProcessWorkspace>();

			try
			{
				processWorkspaceList = _processWorkspaceService
					.GetProcessWorkspacesByWorkspace(m_sWorkspaceBaseUrl, m_sSessionId, m_sActiveWorkspace, iStartIndex, iEndIndex, sStatus, sOperationType, sNamePattern);
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
			}

			return processWorkspaceList;
		}

		/// <summary>
		/// Gets the processor payload as a dictionary.
		/// </summary>
		/// <param name="sProcessObjId">the ID of the processor</param>
		/// <returns>the processor payload as a dictionary</returns>
		public Dictionary<string, object>? GetProcessorPayload(string? sProcessObjId)
		{
			_logger.LogDebug($"GetProcessesByWorkspace({sProcessObjId}");

			if (String.IsNullOrEmpty(sProcessObjId))
			{
				_logger.LogError("GetProcessorPayload: the processor ID is null or empty");
				return null;
			}

			string? sJsonPayload = GetProcessorPayloadAsJSON(sProcessObjId);

			if (sJsonPayload == null)
				return null;

			return SerializationHelper.FromJson<Dictionary<string, object>>(sJsonPayload);
		}

		/// <summary>
		/// Retrieve the payload of a processor formatted as a JSON string.
		/// </summary>
		/// <param name="sProcessObjId">the ID of the processor</param>
		/// <returns>the payload as a JSON string, or null if error occurred</returns>
		public string? GetProcessorPayloadAsJSON(string? sProcessObjId)
		{
			_logger.LogDebug($"GetProcessorPayloadAsJSON({sProcessObjId}");

			if (String.IsNullOrEmpty(sProcessObjId))
			{
				_logger.LogError("GetProcessorPayloadAsJSON: the processor ID is null or empty");
				return null;
			}

			try
			{
				return _processWorkspaceService.GetProcessPayload(m_sWorkspaceBaseUrl, m_sSessionId, sProcessObjId);
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
				return null;
			}
		}

		/// <summary>
		/// Get the product bounding box.
		/// </summary>
		/// <param name="sFileName">the file name</param>
		/// <returns>the product bounding box</returns>
		public string? GetProductBbox(string? sFileName)
		{
			_logger.LogDebug($"GetProductBbox({sFileName}");

			if (String.IsNullOrEmpty(sFileName))
			{
				_logger.LogError("GetProductBBOX: file name is null or empty, aborting");
				return null;
			}

			try
			{
				Product? product = _productService.GetProductByName(m_sBaseUrl, m_sSessionId, m_sActiveWorkspace, sFileName);

				if (product != null)
					return product.Bbox;
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
			}

			return null;
		}

		/// <summary>
		/// Download a file on the local PC.
		/// </summary>
		/// <param name="sFileName">the name of the file</param>
		/// <returns>the full path of the file</returns>
		private string DownloadFile(string? sFileName)
		{
			_logger.LogDebug($"DownloadFile({sFileName})");

			if (String.IsNullOrEmpty(sFileName))
			{
				_logger.LogError("DownloadFile: fileName must not be null or empty");
				return "";
			}

			string sSavePath = GetSavePath();

			try
			{
				string sOutputFilePath = _wasdiService.CatalogDownload(m_sWorkspaceBaseUrl, m_sSessionId, m_sActiveWorkspace, sSavePath, sFileName);

				return sOutputFilePath;
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
			}

			return "";
		}

		/// <summary>
		/// Uploads and ingest a file in WASDI.
		/// </summary>
		/// <param name="sFileName">the name of the file to upload</param>
		/// <returns>True if the file was uploaded, False otherwise</returns>
		/// <exception cref="Exception">in case of any issues</exception>
		public bool UploadFile(string? sFileName)
		{
			_logger.LogDebug($"UploadFile({sFileName})");

			if (String.IsNullOrEmpty(sFileName))
				throw new Exception("WasdiLib.UploadFile: file name is null or empty");

			//local file
			string sSavePath = GetSavePath();
			string sFullPath = sSavePath + sFileName;

			if (!File.Exists(sFullPath))
				throw new Exception("WasdiLib.UploadFile: file not found");

			bool success = _productService.UploadFile(m_sWorkspaceBaseUrl, m_sSessionId, m_sActiveWorkspace, sSavePath, sFileName);
			_logger.LogDebug("UploadFile " + (success ? "succeeded" : "failed"));

			return success;
		}

		/// <summary>
		/// Copy a file from a workspace to the WASDI user's SFTP Folder in a synchronous way.
		/// </summary>
		/// <param name="sFileName">the filename to move to the SFTP folder</param>
		/// <returns>the Process ID is synchronous execution, end status otherwise. An empty string is returned in case of failure</returns>
		public string CopyFileToSftp(string sFileName)
		{
			_logger.LogDebug($"CopyFileToSftp( {sFileName} )");

			return CopyFileToSftp(sFileName, null);
		}

		/// <summary>
		/// Copy a file from a workspace to the WASDI user's SFTP Folder in a synchronous way.
		/// </summary>
		/// <param name="sFileName">the filename to move to the SFTP folder</param>
		/// <param name="sRelativePath">the relative path in the SFTP root</param>
		/// <returns>the Process ID is synchronous execution, end status otherwise. An empty string is returned in case of failure</returns>
		public string CopyFileToSftp(string sFileName, string? sRelativePath)
		{
			_logger.LogDebug($"CopyFileToSftp( {sFileName}, {sRelativePath} )");

			return WaitProcess(AsynchCopyFileToSftp(sFileName, sRelativePath));
		}

		/// <summary>
		/// Copy a file from a workspace to the WASDI user's SFTP Folder in a asynchronous way.
		/// </summary>
		/// <param name="sFileName">the filename to move to the SFTP folder</param>
		/// <returns>the Process ID</returns>
		public string? AsynchCopyFileToSftp(string sFileName)
		{
			return AsynchCopyFileToSftp(sFileName, null);
		}

		/// <summary>
		/// Copy a file from a workspace to the WASDI user's SFTP Folder in a asynchronous way.
		/// </summary>
		/// <param name="sFileName">the filename to move to the SFTP folder</param>
		/// <param name="sRelativePath">the relative path in the SFTP root</param>
		/// <returns>the Process ID is asynchronous execution, end status otherwise. An empty string is returned in case of failure</returns>
		public string? AsynchCopyFileToSftp(string? sFileName, string? sRelativePath)
		{
			_logger.LogDebug($"AsynchCopyFileToSftp({sFileName}, {sRelativePath})");

			if (String.IsNullOrEmpty(sFileName))
			{
				_logger.LogError("AsynchCopyFileToSftp: invalid file name, aborting");
				return null;
			}

			//upload file if it is not on WASDI yet
			try
			{
				if (GetUploadActive())
				{
					if (!FileExistsOnWasdi(sFileName))
					{
						_logger.LogDebug("AsynchCopyFileToSftp: file " + sFileName + " is not on WASDI yet, uploading...");
						UploadFile(sFileName);
					}
				}
			}
			catch (Exception ex)
			{
				_logger.LogError(ex.StackTrace);
				return null;
			}

			bool bIsOnServer = GetIsOnServer();
			string sProcessId = GetMyProcId();

			try
			{
				PrimitiveResult? primitiveResult = _wasdiService.AsynchCopyFileToSftp(m_sWorkspaceBaseUrl, m_sSessionId, m_sActiveWorkspace, bIsOnServer, sRelativePath, sFileName, sProcessId);

				if (primitiveResult != null)
					return primitiveResult.StringValue;
			}
			catch (Exception ex)
			{
				_logger.LogError($"AsynchCopyFileToSftp: could not HTTP GET to /catalog/copytosfpt due to: {ex.Message}, aborting");
				return null;
			}

			return null;
		}

		/// <summary>
		/// Sets the sub pid.
		/// </summary>
		/// <param name="sProcessId">the process ID</param>
		/// <param name="iSubPid">the subPid of the process</param>
		/// <returns>the updated status of the processs</returns>
		public string SetSubPid(string? sProcessId, int iSubPid)
		{
			_logger.LogDebug($"SetSubPid({sProcessId}, {iSubPid})");

			if (String.IsNullOrEmpty(sProcessId))
			{
				_logger.LogError("SetSubPid: process ID null or empty, aborting");
				return "";
			}

			try
			{
				ProcessWorkspace? processWorkspace = _processWorkspaceService.SetSubPid(m_sWorkspaceBaseUrl, m_sSessionId, sProcessId, iSubPid);

				if (processWorkspace != null)
					return processWorkspace.Status;
			}
			catch (Exception ex)
			{
				_logger.LogError($"SetSubPid: could not HTTP GET due to: {ex.Message}, aborting");
				return "";
			}

			return "";
		}

		/// <summary>
		/// Print the status information ot the Wasdi application.
		/// </summary>
		public void PrintStatus()
		{
			string info = $@"
				wasdi: user: {GetUser()}
				wasdi: password: ***********************
				wasdi: session id: {GetSessionId()}
				wasdi: active workspace id: {GetActiveWorkspace()}
				wasdi: workspace owner: {m_sWorkspaceOwner}
				wasdi: base path: {GetBasePath()}
				wasdi: is on server: {GetIsOnServer()}
				wasdi: download active: {GetDownloadActive()}
				wasdi: upload active: {GetDownloadActive()}
				wasdi: verbose: {GetVerbose()}
				wasdi: parameters file path: {GetParametersFilePath()}
				wasdi: params: {GetParamsAsJsonString()}
				wasdi: proc id: {GetMyProcId()}
				wasdi: base url: {GetBaseUrl()}
				wasdi: workspace base URL: {GetWorkspaceBaseUrl()}
				";

			_logger.LogInformation(info);
		}

	}
}
