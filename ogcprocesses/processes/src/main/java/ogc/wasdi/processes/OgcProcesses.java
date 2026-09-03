package ogc.wasdi.processes;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.json.JSONObject;

import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiContextLocator;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import ogc.wasdi.processes.providers.JerseyMapperProvider;
import ogc.wasdi.processes.providers.OgcProcessesViewModelBodyWriter;
import ogc.wasdi.processes.rest.resources.OgcProcessesOpenApiResource;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.Node;
import wasdi.shared.business.users.*;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.mongo.MongoRepository;
import wasdi.shared.data.SessionRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.auth.AuthTokenUtil;
import wasdi.shared.utils.auth.KeycloakUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.runtime.RunTimeUtils;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.ogcprocesses.Link;
import wasdi.shared.viewmodels.ogcprocesses.Results;
import wasdi.shared.viewmodels.processworkspace.ProcessWorkspaceViewModel;

public class OgcProcesses extends ResourceConfig {
	
	
	/**
	 * Servlet Config to access web.xml file
	 */
	@Context
	ServletConfig m_oServletConfig;	
	
	public static String s_sBaseAddress = "";
	
	public OgcProcesses() {
		packages(true, "ogc.wasdi.processes.rest.resources");
		register(JacksonFeature.class);
		register(org.glassfish.jersey.media.multipart.MultiPartFeature.class);
		register(JerseyMapperProvider.class);
		register(OgcProcessesViewModelBodyWriter.class);
		register(OgcProcessesOpenApiResource.class);
	}

	private void updateOpenApiServerUrl() {
		try {
			String sBaseUrl = WasdiConfig.Current.ogcProcessesApi.baseAddress;

			if (sBaseUrl.endsWith("/")) sBaseUrl = sBaseUrl.substring(0, sBaseUrl.length() - 1);

			Info oInfo = new Info()
					.title("WASDI OGC API - Processes")
					.version("1.0.0")
					.description(WasdiConfig.Current.ogcProcessesApi.landingDescription);

			Server oServer = new Server();
			oServer.setUrl(sBaseUrl);
			oServer.setDescription("WASDI OGC Processes Server Base URL");

			Set<String> aoScannedPackages = new HashSet<>();
			aoScannedPackages.add("ogc.wasdi.processes.rest.resources");

			SwaggerConfiguration oSwaggerConfig = new SwaggerConfiguration()
					.openAPI(new OpenAPI().info(oInfo).servers(java.util.Collections.singletonList(oServer)))
					.prettyPrint(true)
					.resourcePackages(aoScannedPackages);

			JaxrsOpenApiContextBuilder<?> oContextBuilder = new JaxrsOpenApiContextBuilder<>()
					.openApiConfiguration(oSwaggerConfig);
			OpenApiContext oContext = oContextBuilder.buildContext(true);
			OpenApiContextLocator.getInstance().putOpenApiContext("openapi.context.id.default", oContext);

			WasdiLog.debugLog("OgcProcesses.updateOpenApiServerUrl: OpenAPI base URL set to " + sBaseUrl);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("OgcProcesses.updateOpenApiServerUrl: " + oEx.getMessage());
		}
	}

	/**
	 * Web Server intialization: it loads the main web-server configuration
	 * @throws URISyntaxException 
	 */
	@PostConstruct
	public void initOgcProcesses() throws URISyntaxException {
		
		WasdiLog.debugLog("WASDI OGC-Processes Server start");
		
		String sConfigFilePath = "/etc/wasdi/wasdiConfig.json";
		
		if (m_oServletConfig!=null) {
			if (Utils.isNullOrEmpty(m_oServletConfig.getInitParameter("ConfigFilePath")) == false){
				sConfigFilePath = m_oServletConfig.getInitParameter("ConfigFilePath");
			}
		}
		
		String sEnvConfigFile = RunTimeUtils.getSystemEnvironmentVariable("WASDI_CONFIG_FILE");
		
		if (!Utils.isNullOrEmpty(sEnvConfigFile)) {
			sConfigFilePath = sEnvConfigFile;
			WasdiLog.infoLog("Wasdi.initWasdi: found WASDI_CONFIG_FILE env variable, using it");				
		}		

		if (!WasdiConfig.readConfig(sConfigFilePath)) {
			WasdiLog.errorLog("ERROR IMPOSSIBLE TO READ CONFIG FILE IN " + sConfigFilePath);
		}

		WasdiLog.initLogger(WasdiConfig.Current.logLevelServer);
				
		OgcProcesses.s_sBaseAddress = WasdiConfig.Current.ogcProcessesApi.baseAddress;
		
		if (!OgcProcesses.s_sBaseAddress.endsWith("/")) OgcProcesses.s_sBaseAddress += "/";

		updateOpenApiServerUrl();
		
		// Read MongoDb Configuration
		try {

            MongoRepository.readConfig();

			WasdiLog.debugLog("-------Mongo db User " + MongoRepository.DB_USER);

		} catch (Throwable oE) {
			WasdiLog.errorLog("OgcProcesses.initOgcProcesses: error during the initialization of the ogc process" + oE.getMessage());
		}		
	}
	
	/**
	 * Get the User object from the session Id or JWT token.
	 * Supports multiple authentication methods with priority order:
	 * 1. x-session-token header (WASDI legacy sessionId)
	 * 2. Authorization Bearer JWT token (APEx M2M / OAuth2 client credentials)
	 * 3. Authorization Bearer wasdi-<sessionId> (legacy wrapped sessionId)
	 * 4. Basic HTTP auth (user:sessionId)
	 * 5. Validation mode override (for testing)
	 * 
	 * @param sSessionId WASDI session token from x-session-token header
	 * @param sAuthorization HTTP Authorization header (Bearer or Basic)
	 * @return Authenticated User object or null if authentication fails
	 */
	public static User getUserFromSession(String sSessionId, String sAuthorization) {
		
		User oUser = null;
		
		try {
			
    		// Priority 1: Check if we have x-session-token header
    		if (!Utils.isNullOrEmpty(sSessionId)) {
    			return validateAndGetUserFromSessionId(sSessionId);
    		}
    		
    		// Priority 2: Check if we have Authorization header
    		if (!Utils.isNullOrEmpty(sAuthorization)) {
    			String sToken = AuthTokenUtil.extractTokenFromAuthHeader(sAuthorization);
    			
    			if (!Utils.isNullOrEmpty(sToken)) {
    				// Check if it's a JWT Bearer token (APEx M2M scenario)
    				if (AuthTokenUtil.appearsToBeJWT(sToken) && !AuthTokenUtil.isLegacyWasdiToken(sToken)) {
    					WasdiLog.debugLog("OgcProcesses.getUserFromSession: JWT Bearer token detected");
    					oUser = handleJWTAuthentication(sToken);
    					if (oUser != null) {
    						return oUser;
    					}
    				}
    				
    				// Check if it's legacy wrapped sessionId ("Bearer wasdi-<sessionId>")
    				if (AuthTokenUtil.isLegacyWasdiToken(sToken)) {
    					WasdiLog.debugLog("OgcProcesses.getUserFromSession: legacy wrapped sessionId detected");
    					String sUnwrappedSessionId = AuthTokenUtil.extractLegacySessionId(sToken);
    					return validateAndGetUserFromSessionId(sUnwrappedSessionId);
    				}
    				
    				// Fallback: treat token as raw sessionId
    				WasdiLog.debugLog("OgcProcesses.getUserFromSession: treating Authorization token as sessionId");
    				return validateAndGetUserFromSessionId(sToken);
    			}
    			
    			// Priority 3: Try Basic HTTP auth (user:sessionId format)
    			WasdiLog.debugLog("OgcProcesses.getUserFromSession: attempting Basic HTTP auth");
    			sSessionId = getSessionIdFromBasicAuthentication(sAuthorization);
    			if (!Utils.isNullOrEmpty(sSessionId)) {
    				return validateAndGetUserFromSessionId(sSessionId);
    			}
    		}
    		
    		// Priority 4: Validation mode override (for testing)
    		if (WasdiConfig.Current.ogcProcessesApi.validationModeOn) {
    			if (!Utils.isNullOrEmpty(WasdiConfig.Current.ogcProcessesApi.validationUserId)) {
    				if (!Utils.isNullOrEmpty(WasdiConfig.Current.ogcProcessesApi.validationSessionId)) {
    					WasdiLog.warnLog("OgcProcesses.getUserFromSession: VALIDATION MODE ON - AUTO LOGIN");
    					return getValidationModeUser();
    				}
    			}
    		}
    		
    		// No authentication found
    		WasdiLog.debugLog("OgcProcesses.getUserFromSession: no valid authentication found");
    		return null;
    		
		} catch (Exception oE) {
			WasdiLog.errorLog("OgcProcesses.getUserFromSession: exception during authentication: " + oE.getMessage());
		}

		return oUser;
	}
	
	/**
	 * Validate sessionId and return authenticated User.
	 * Queries SessionRepository and then UserRepository.
	 * 
	 * @param sSessionId WASDI session ID from database
	 * @return User object or null if session is invalid/expired
	 */
	private static User validateAndGetUserFromSessionId(String sSessionId) {
		try {
			if (Utils.isNullOrEmpty(sSessionId)) {
				return null;
			}
			
			SessionRepository oSessionRepository = new SessionRepository();
			UserSession oUserSession = oSessionRepository.getSession(sSessionId);
			
			if (oUserSession == null) {
				WasdiLog.debugLog("OgcProcesses.validateAndGetUserFromSessionId: session not found: " + sSessionId);
				return null;
			}
			
			String sUserId = oUserSession.getUserId();
			if (Utils.isNullOrEmpty(sUserId)) {
				return null;
			}
			
			UserRepository oUserRepository = new UserRepository();
			User oUser = oUserRepository.getUser(sUserId);
			
			if (oUser == null) {
				WasdiLog.warnLog("OgcProcesses.validateAndGetUserFromSessionId: user not found for session: " + sSessionId);
			}
			
			return oUser;
		} catch (Exception oE) {
			WasdiLog.errorLog("OgcProcesses.validateAndGetUserFromSessionId: " + oE.getMessage());
		}
		
		return null;
	}
	
	/**
	 * Handle JWT Bearer token authentication (APEx M2M scenario).
	 * Validates JWT, extracts userId, creates/updates M2M session, returns User.
	 * 
	 * @param sJwtToken Keycloak JWT access token
	 * @return Authenticated User object or null if JWT validation fails
	 */
	private static User handleJWTAuthentication(String sJwtToken) {
		try {
			// Validate JWT and extract userId from token claims
			String sUserId = KeycloakUtils.validateJwtAndGetUserId(sJwtToken);
			String sClientId = KeycloakUtils.validateJwtAndGetClientId(sJwtToken);
			if (isAcceptedExternalClient(sClientId)) {
				sUserId = sClientId.toLowerCase();
				ensureServiceUser(sUserId);
			}
			
			if (Utils.isNullOrEmpty(sUserId)) {
				WasdiLog.warnLog("OgcProcesses.handleJWTAuthentication: JWT validation failed or no userId extracted");
				return null;
			}
			
			WasdiLog.debugLog("OgcProcesses.handleJWTAuthentication: JWT valid for user: " + sUserId);
			
			// Get or create M2M session for this JWT user
			UserSession oSession = getOrCreateSessionFromJWT(sUserId);
			if (oSession == null) {
				WasdiLog.errorLog("OgcProcesses.handleJWTAuthentication: failed to create/get M2M session for user: " + sUserId);
				return null;
			}
			
			// Get user object from repository
			UserRepository oUserRepo = new UserRepository();
			User oUser = oUserRepo.getUser(sUserId);
			
			if (oUser == null) {
				WasdiLog.warnLog("OgcProcesses.handleJWTAuthentication: user not found in DB for JWT userId: " + sUserId);
				return null;
			}
			
			WasdiLog.debugLog("OgcProcesses.handleJWTAuthentication: successfully authenticated JWT user: " + sUserId);
			return oUser;
		} catch (Exception oE) {
			WasdiLog.errorLog("OgcProcesses.handleJWTAuthentication: " + oE.getMessage());
		}
		
		return null;
	}

	private static boolean isAcceptedExternalClient(String sClientId) {
		if (Utils.isNullOrEmpty(sClientId) || WasdiConfig.Current.keycloack == null
				|| WasdiConfig.Current.keycloack.acceptedClientIds == null) {
			return false;
		}

		for (String sAcceptedClientId : WasdiConfig.Current.keycloack.acceptedClientIds) {
			if (sClientId.equals(sAcceptedClientId)) return true;
		}
		return false;
	}

	private static User ensureServiceUser(String sUserId) {
		UserRepository oUserRepository = new UserRepository();
		User oUser = oUserRepository.getUser(sUserId);
		if (oUser != null) return oUser;

		User oServiceUser = new User();
		oServiceUser.setUserId(sUserId);
		oServiceUser.setName(sUserId);
		oServiceUser.setSurname("");
		oServiceUser.setPassword(null);
		oServiceUser.setValidAfterFirstAccess(true);
		oServiceUser.setAuthServiceProvider("service");
		oServiceUser.setDefaultNode(WasdiConfig.Current.usersDefaultNode);
		oServiceUser.setSkin(WasdiConfig.Current.defaultSkin);
		if (oUserRepository.insertUser(oServiceUser)) return oServiceUser;
		return oUserRepository.getUser(sUserId);
	}
	
	/**
	 * Get or create a lightweight M2M session for JWT-authenticated user.
	 * Sessions created from JWT are persistent (no short expiry) since they
	 * represent M2M client credentials flow rather than user login.
	 * 
	 * @param sUserId User ID from JWT claims
	 * @return M2M UserSession or null on error
	 */
	private static UserSession getOrCreateSessionFromJWT(String sUserId) {
		try {
			if (Utils.isNullOrEmpty(sUserId)) {
				return null;
			}
			
			SessionRepository oSessionRepository = new SessionRepository();
			
			// Try to get existing active M2M session for this user
			// (might be from previous M2M calls within token lifetime)
			List<UserSession> aoSessions = oSessionRepository.getAllActiveSessions(sUserId);
			UserSession oSession = null;
			
			// Find most recent active M2M session
			if (aoSessions != null && !aoSessions.isEmpty()) {
				for (UserSession oS : aoSessions) {
					// M2M sessions are identifiable; reuse if valid
					// For now, just take the first one (most recent)
					oSession = oS;
					break;
				}
			}
			
			// If no existing session, create new M2M session
			if (oSession == null) {
				oSession = new UserSession();
				// Generate unique M2M session ID
				oSession.setSessionId(java.util.UUID.randomUUID().toString());
				oSession.setUserId(sUserId);
				oSession.setLoginDate(Utils.nowInMillis());
				oSession.setLastTouch(Utils.nowInMillis());
				// Note: M2M sessions don't have a short expiry; they're valid as long as JWT is valid
				
				WasdiLog.debugLog("OgcProcesses.getOrCreateSessionFromJWT: creating new M2M session for user: " + sUserId);
				oSessionRepository.insertSession(oSession);
			} else {
				// Refresh existing M2M session
				oSession.setLastTouch(Utils.nowInMillis());
				WasdiLog.debugLog("OgcProcesses.getOrCreateSessionFromJWT: refreshing existing M2M session for user: " + sUserId);
				oSessionRepository.touchSession(oSession);
			}
			
			return oSession;
		} catch (Exception oE) {
			WasdiLog.errorLog("OgcProcesses.getOrCreateSessionFromJWT: " + oE.getMessage());
		}
		
		return null;
	}
	
	/**
	 * Get user in validation mode (for testing/debugging).
	 * Creates or validates a test session.
	 * 
	 * @return Test User object or null if validation mode not properly configured
	 */
	private static User getValidationModeUser() {
		try {
			UserRepository oUserRepo = new UserRepository();
			User oUser = oUserRepo.getUser(WasdiConfig.Current.ogcProcessesApi.validationUserId);
			
			if (oUser == null) {
				WasdiLog.errorLog("OgcProcesses.getValidationModeUser: configured validation user not found");
				return null;
			}
			
			SessionRepository oSessionRepository = new SessionRepository();
			UserSession oSession = oSessionRepository.getSession(WasdiConfig.Current.ogcProcessesApi.validationSessionId);
			
			if (oSession == null) {
				oSession = new UserSession();
				oSession.setLoginDate(Utils.nowInMillis());
				oSession.setLastTouch(Utils.nowInMillis());
				oSession.setSessionId(WasdiConfig.Current.ogcProcessesApi.validationSessionId);
				oSession.setUserId(WasdiConfig.Current.ogcProcessesApi.validationUserId);
				oSessionRepository.insertSession(oSession);
			}
			
			return oUser;
		} catch (Exception oE) {
			WasdiLog.errorLog("OgcProcesses.getValidationModeUser: " + oE.getMessage());
		}
		
		return null;
	}
	
	/**
	 * Get the wasdi session id from the basic authentication http header in the form: user:sessionId
	 * @param sAuthorization
	 * @return
	 */
	public static String getSessionIdFromBasicAuthentication(String sAuthorization) {
		try {
			if (Utils.isNullOrEmpty(sAuthorization)) return "";
			sAuthorization = sAuthorization.replace("Basic ", "");
			byte[] ayDecodedBytes = Base64.getDecoder().decode(sAuthorization);
			
			if (ayDecodedBytes==null) return "";
			
			String sDecodedString = new String(ayDecodedBytes);
			String [] asParts = sDecodedString.split(":");
			return asParts[1];
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("OgcProcesses.getSessionIdFromBasicAuthentication: something bad happened: " + oEx);
		}
		
		return "";
	}
	
	/**
	 * Updates the session Id taking it from Authorization if needed
	 * considering also the validationMode option
	 * @param sSessionId Session id as received from x-session-token
	 * @param sAuthorization Basic http authorization token
	 * @return Actual value of the session Id
	 */
	public static String updateSessionId(String sSessionId, String sAuthorization) {
		try {
			
			if (!Utils.isNullOrEmpty(sSessionId)) return sSessionId;
			
			if (Utils.isNullOrEmpty(sAuthorization)) {
				if (WasdiConfig.Current.ogcProcessesApi.validationModeOn) {
					return WasdiConfig.Current.ogcProcessesApi.validationSessionId;
				}
			}
			
			String sToken = AuthTokenUtil.extractTokenFromAuthHeader(sAuthorization);
			if (AuthTokenUtil.appearsToBeJWT(sToken)) {
				String sClientId = KeycloakUtils.validateJwtAndGetClientId(sToken);
				if (isAcceptedExternalClient(sClientId)) {
					String sUserId = sClientId.toLowerCase();
					User oUser = ensureServiceUser(sUserId);
					if (oUser != null) {
						UserSession oSession = getOrCreateSessionFromJWT(sUserId);
						return oSession == null ? "" : oSession.getSessionId();
					}
				}
			}

			return getSessionIdFromBasicAuthentication(sAuthorization);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("OgcProcesses.getSessionIdFromBasicAuthentication: something bad happened: " + oEx);
		}
		
		return "";		
	}
	
	/**
	 * Adds Link Headers to http response
	 * @param oResponse Response Builder
	 * @param aoLinks List of links to add
	 */
	public static ResponseBuilder addLinkHeaders(ResponseBuilder oResponse, List<Link> aoLinks) {
		
		try {
			// Safe check
			if (oResponse == null) return oResponse;
			if (aoLinks == null) return oResponse;
			if (aoLinks.size()==0) return oResponse;
			
			String sLinkHeaderContent = "";
			
			// For all the links
			for (Link oLink : aoLinks) {
				// Get the url				
				String sUri = oLink.getHref();
				
				// Check if there is something
				if (Utils.isNullOrEmpty(sUri)) continue;
				
				// The href must stay a valid, untouched URI: only rel/type/etc are quoted attributes
				String sFinalLink = "<" + sUri + ">";
				
				if (!Utils.isNullOrEmpty(oLink.getRel())) {
					String sRel = oLink.getRel();
					// RFC 8288 allows a single relation-type unquoted: keep simple tokens (self/alternate/monitor/up)
					// unquoted for interop with clients doing a plain "rel=xxx" substring match; quote URI-style rel values
					if (sRel.matches("[a-zA-Z0-9_-]+")) {
						sFinalLink += "; rel=" + sRel;
					}
					else {
						sFinalLink += "; rel=\"" + sRel + "\"";
					}
				}
				
				if (!Utils.isNullOrEmpty(oLink.getType())) {
					sFinalLink += "; type=\"" + oLink.getType() + "\"";
				}
				
				if (!Utils.isNullOrEmpty(oLink.getHreflang())) {
					sFinalLink += "; hreflang=\"" + oLink.getHreflang() + "\"";
				}
				
				if (!Utils.isNullOrEmpty(oLink.getTitle())) {
					sFinalLink += "; title=\"" + oLink.getTitle() + "\"";
				}
				
				sLinkHeaderContent += sFinalLink + ", ";
			}
			
			if (!Utils.isNullOrEmpty(sLinkHeaderContent)) {
				sLinkHeaderContent=sLinkHeaderContent.substring(0, sLinkHeaderContent.length()-2);
				oResponse = oResponse.header("Link", sLinkHeaderContent);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("OgcProcesses.addLinkHeaders: something bad happened: " + oEx);
		}
		
		return oResponse;
	}
	
	/**
	 * Reads a single Process Workspace from a Node using WASDI API
	 * @param sProcessWorkspaceId Id of the process Workspace
	 * @param oNode Node Entity
	 * @param sSessionId Actual Session Id
	 * @return The Process Workspace View Model if available or null
	 */
	public static ProcessWorkspaceViewModel readProcessWorkspaceFromNode(String sProcessWorkspaceId, Node oNode, String sSessionId) {
		try {
			if (oNode.getActive()==false) return null;
			
			String sUrl = oNode.getNodeBaseAddress();
			
			if (!sUrl.endsWith("/")) sUrl += "/";
			
			sUrl += "process/byid?procws="+sProcessWorkspaceId;
			
			WasdiLog.debugLog("JobsResource.readProcessWorkspaceFromNode: calling url: " + sUrl);
			
			HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(sUrl, HttpUtils.getStandardHeaders(sSessionId)); 
			String sResponse = oHttpCallResponse.getResponseBody();
			
			if (Utils.isNullOrEmpty(sResponse)==false) {
				ProcessWorkspaceViewModel oProcWs = MongoRepository.s_oMapper.readValue(sResponse, ProcessWorkspaceViewModel.class);
				return oProcWs;
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("OgcProcesses.readProcessWorkspaceFromNode: exception contacting computing node: " + oEx.toString());
		}		
		
		return null;
	}
	
	public static Results getResultsFromProcessWorkspace(ProcessWorkspaceViewModel oProcWsViewModel, Node oNode, String sSessionId) {
		Results oResults = new Results();
		
		try {
			oResults.put("workspaceId", oProcWsViewModel.getWorkspaceId());
			oResults.put("payload", oProcWsViewModel.getPayload());
			
			DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
			List<DownloadedFile> aoFiles = oDownloadedFilesRepository.getByWorkspace(oProcWsViewModel.getWorkspaceId());
			
			String sBaseUrl = WasdiConfig.Current.baseUrl;
			
			if (oNode!=null) sBaseUrl = oNode.getNodeBaseAddress();
			
			String[] asFiles = new String[aoFiles.size()];
			
			for (int iFiles = 0; iFiles<aoFiles.size(); iFiles++) {
				String sLink = sBaseUrl + "/catalog/downloadbyname?token=" + sSessionId + "&filename=" + aoFiles.get(iFiles).getFileName() + "&workspace=" + oProcWsViewModel.getWorkspaceId();
				asFiles[iFiles] = sLink;
			}
			
			oResults.put("files", asFiles);			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("OgcProcesses.getResultsFromProcessWorkspace: exception contacting computing node: " + oEx.toString());
		}	

		
		return oResults;
	}
}
