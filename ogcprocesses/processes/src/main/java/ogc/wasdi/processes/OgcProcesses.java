package ogc.wasdi.processes;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.json.JSONObject;

import ogc.wasdi.processes.providers.JerseyMapperProvider;
import ogc.wasdi.processes.providers.OgcProcessesViewModelBodyWriter;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.Node;
import wasdi.shared.business.users.*;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.data.SessionRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.StringUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
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
		register(JerseyMapperProvider.class);
		register(OgcProcessesViewModelBodyWriter.class);
	}

	/**
	 * Web Server intialization: it loads the main web-server configuration
	 * @throws URISyntaxException 
	 */
	@PostConstruct
	public void initOgcProcesses() throws URISyntaxException {
		
		String sPath = new File(WasdiLog.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
		System.out.println(sPath);
		WasdiLog.debugLog("WASDI OGC-Processes Server start");
		
		String sConfigFilePath = "/etc/wasdi/wasdiConfig.json";
		
		if (Utils.isNullOrEmpty(m_oServletConfig.getInitParameter("ConfigFilePath")) == false){
			sConfigFilePath = m_oServletConfig.getInitParameter("ConfigFilePath");
		}
		
		if (!WasdiConfig.readConfig(sConfigFilePath)) {
			WasdiLog.errorLog("ERROR IMPOSSIBLE TO READ CONFIG FILE IN " + sConfigFilePath);
		}
				
		OgcProcesses.s_sBaseAddress = WasdiConfig.Current.ogcProcessesApi.baseAddress;
		
		if (!OgcProcesses.s_sBaseAddress.endsWith("/")) OgcProcesses.s_sBaseAddress += "/";
		
		// Read MongoDb Configuration
		try {

            MongoRepository.readConfig();

			WasdiLog.debugLog("-------Mongo db User " + MongoRepository.DB_USER);

		} catch (Throwable oE) {
			WasdiLog.errorLog("OgcProcesses.initOgcProcesses: error during the initialization of the ogc process" + oE.getMessage());
		}		
	}
	
	/**
	 * Get the User object from the session Id
	 * It checks first in Key Cloak and later on the local session mechanism.
	 * @param sSessionId
	 * @return
	 */
	public static User getUserFromSession(String sSessionId, String sAuthorization) {
		
		User oUser = null;
		
		try {
			
    		// Check if we have our session header
    		if (Utils.isNullOrEmpty(sSessionId)) {
    			// Try to get it from the basic http auth
    			sSessionId = OgcProcesses.getSessionIdFromBasicAuthentication(sAuthorization);
    		}
    		
    		if (Utils.isNullOrEmpty(sSessionId)) {
    			
    			if (WasdiConfig.Current.ogcProcessesApi.validationModeOn) {
    				if (!Utils.isNullOrEmpty(WasdiConfig.Current.ogcProcessesApi.validationUserId)) {
    					if (!Utils.isNullOrEmpty(WasdiConfig.Current.ogcProcessesApi.validationSessionId)) {
    						
    						WasdiLog.warnLog("OgcProcesses.getUserFromSession: VALIDATION MODE ON - AUTO LOGIN");
    						
    						UserRepository oUserRepo = new UserRepository();
    						oUser = oUserRepo.getUser(WasdiConfig.Current.ogcProcessesApi.validationUserId);
    						
    						if (oUser == null) {
    							WasdiLog.errorLog("OgcProcesses.getUserFromSession: VALIDATION MODE Invalid validation user");
    							return null;
    						}
    						
    						sSessionId=WasdiConfig.Current.ogcProcessesApi.validationSessionId;
    						
    						SessionRepository oSessionRepository = new SessionRepository();
    						UserSession oSession = oSessionRepository.getSession(sSessionId);
    						
    						if (oSession == null) {
    							oSession = new UserSession();
    							oSession.setLoginDate(Utils.nowInMillis());
    							oSession.setLastTouch(Utils.nowInMillis());
    							oSession.setSessionId(sSessionId);
    							oSession.setUserId(WasdiConfig.Current.ogcProcessesApi.validationUserId);
    							
    							oSessionRepository.insertSession(oSession);
    						}
    						
    						return oUser;
    					}
    					
    				}
    			}
    			
    			return null;
    		}
    		
			// Check The Session with Keycloak
			String sUserId = null;
			
			try  {
				//introspect
				String sPayload = "token=" + sSessionId;
				Map<String,String> asHeaders = new HashMap<>();
				asHeaders.put("Content-Type", "application/x-www-form-urlencoded");
				
				HttpCallResponse oHttpCallResponse = HttpUtils.httpPost(WasdiConfig.Current.keycloack.introspectAddress, sPayload, asHeaders, WasdiConfig.Current.keycloack.client + ":" + WasdiConfig.Current.keycloack.clientSecret); 
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
				
				String sFinalLink = "";
				
				// Check if there is something
				if (!Utils.isNullOrEmpty(sUri)) {
					
					if (sUri.contains("?")) {
						// Get the address
						String [] asUriParts = sUri.split("\\?");
						
						if (asUriParts != null) {
							if (asUriParts.length>0) {
								
								// Set the encoded address
								sFinalLink = "<" + StringUtils.encodeUrl(asUriParts[0]) + ">";
								
								// Let see if there are also Query parameters
								String sParams = "";
								
								for (int iParts = 1; iParts<asUriParts.length; iParts++) {
									sParams += asUriParts[iParts];
								}
								
								// Split parameters
								String [] asParams = sParams.split("&");
								
								if (asParams != null) {
									if (asParams.length>0) {
										// Add the parameter
										sFinalLink += "; ";
										for (String sParam : asParams) {
											sFinalLink += sParam + ";";
										}
										
										// Drop Last char
										sFinalLink = sFinalLink.substring(0, sFinalLink.length()-1);
									}
								}
								
							}
						}						
					}
					else  {
						sFinalLink = "<" + StringUtils.encodeUrl(sUri) + ">";
					}
					
					if (!Utils.isNullOrEmpty(sFinalLink)) {
						sLinkHeaderContent += sFinalLink + ", ";
					}						
					
				}
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
