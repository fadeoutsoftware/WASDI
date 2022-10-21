package it.fadeout.rest.resources;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import it.fadeout.Wasdi;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.JupyterNotebook;
import wasdi.shared.business.Node;
import wasdi.shared.business.ProcessorTypes;
import wasdi.shared.business.User;
import wasdi.shared.business.Workspace;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.JupyterNotebookRepository;
import wasdi.shared.data.NodeRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.jinja.JinjaTemplateRenderer;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.PrimitiveResult;

@Path("/console")
public class ConsoleResource {

	private static final String FILE_SEPARATOR = System.getProperty("file.separator");

	@POST
	@Path("/create")
	public PrimitiveResult create(@Context HttpServletRequest oRequest, @HeaderParam("x-session-token") String sSessionId, @QueryParam("workspaceId") String sWorkspaceId) {
		Utils.debugLog("ConsoleResource.create( WS: " + sWorkspaceId + " )");

		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);

		try {
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser == null) {
				Utils.debugLog("ConsoleResource.create( Session: " + sSessionId + ", WS: " + sWorkspaceId + " ): invalid session");
				oResult.setIntValue(Status.UNAUTHORIZED.getStatusCode());
				return oResult;
			}

			if (Utils.isNullOrEmpty(sWorkspaceId)) {
				oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
				return oResult;
			}

			String sUserId = oUser.getUserId();

			//check the user can access the workspace
			if (!PermissionsUtils.canUserAccessWorkspace(sUserId, sWorkspaceId)) {
				Utils.debugLog("ConsoleResource.create: user cannot access workspace info, aborting");
				oResult.setIntValue(Status.FORBIDDEN.getStatusCode());

				return oResult;
			}

			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspaceId);

			if (oWorkspace == null) {
				Utils.debugLog("ConsoleResource.create: " + sWorkspaceId + " is not a valid workspace, aborting");
				oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());

				return oResult;
			}


			if (!Wasdi.s_sMyNodeCode.equals(oWorkspace.getNodeCode())) {

				oResult.setStringValue("WORKSPACE NOT IN THIS NODE");
				oResult.setBoolValue(false);

				return oResult;
			}
			
			String sClientIp = resolveClientIp(oRequest);
			Utils.debugLog("ConsoleResource.create: client IP: " + sClientIp);

			String sJupyterNotebookCode = Utils.generateJupyterNotebookCode(oWorkspace.getUserId(), sWorkspaceId);

			JupyterNotebookRepository oJupyterNotebookRepository = new JupyterNotebookRepository();
			JupyterNotebook oJupyterNotebook = oJupyterNotebookRepository.getJupyterNotebookByCode(sJupyterNotebookCode);

			if (oJupyterNotebook != null) {
				
				Utils.debugLog("ConsoleResource.create: this is an existing notebook");
				
				// We want to be safe
				if (Utils.isNullOrEmpty(oJupyterNotebook.getAllowedIpAddresses())) {
					// If there is nothing, we just re-start
					oJupyterNotebook.setAllowedIpAddresses("");
				}
				
				// Check if the user is using the same IP
				boolean bIsAllowed = isClientIpAllowedForThisUser(oJupyterNotebook.getAllowedIpAddresses(), sUserId, sClientIp);
				
				if (bIsAllowed) {
					Utils.debugLog("ConsoleResource.create: client ip allowed for the notebook");
				}
				else {
					Utils.debugLog("ConsoleResource.create: client ip NOT allowed for the notebook: add it");
					
					// Take the paths of the template and the output file
					String sVolumeFolder = WasdiConfig.Current.paths.traefikMountedVolume;
					if (!sVolumeFolder.endsWith("/")) sVolumeFolder += "/";
					
					String sTemplateFile = sVolumeFolder + "template/conf.d_notebook.yml.j2";
					String sOutputFile = sVolumeFolder + "conf.d/nb_" + sJupyterNotebookCode + ".yml";
					
					// We need to update the list of allowed users: start removing the actual one
					oJupyterNotebook.removeUserFromAllowedIp(sUserId);
					// Get the original list
					String sOldList = oJupyterNotebook.getAllowedIpAddresses();
					
					if (Utils.isNullOrEmpty(sOldList)) sOldList = "";
					else sOldList += ";";
					
					// Append this user with the new IP
					String sNewList =  sOldList + sUserId+":"+sClientIp;
					
					// Update the list on the db
					oJupyterNotebook.setAllowedIpAddresses(sNewList);
					oJupyterNotebookRepository.updateJupyterNotebook(oJupyterNotebook);
					
					// Now extract the list of IP to enable in the firewall
					ArrayList<String> asAllowedIps = oJupyterNotebook.extractListOfWhiteIp();
					
					// Add the white list from config
					if (WasdiConfig.Current.traefik.firewallWhiteList!=null) {
						asAllowedIps.addAll(WasdiConfig.Current.traefik.firewallWhiteList);
					}
					// Create the parameters Map
					Map<String, Object> aoTraefikTemplateParams = new HashMap<>();
					
					aoTraefikTemplateParams.put("wasdiNotebookId", sJupyterNotebookCode);
					aoTraefikTemplateParams.put("sourceRangeList", asAllowedIps);
					
					// Render the template in the new config file
					JinjaTemplateRenderer oJinjaTemplateRenderer = new JinjaTemplateRenderer();
					oJinjaTemplateRenderer.translate(sTemplateFile, sOutputFile, aoTraefikTemplateParams);
				}
				
				
				Utils.debugLog("ConsoleResource.create: notebook already exists, check if it is up and running");
				
				// Here we know it is a db: so if it is null is for sure not active
				JupyterNotebook oNotebook = internalIsJupyterActive(oWorkspace.getUserId(), sWorkspaceId); 

				boolean bIsActive = false;
				
				if (oNotebook!=null) bIsActive = true;
				
				Utils.debugLog("ConsoleResource.create | bIsActive: " + bIsActive);
				
				boolean bIsUpToDate = internalNotebookUpToDate();
				Utils.debugLog("ConsoleResource.create | bIsUpToDate: " + bIsUpToDate);

				if (bIsActive && bIsUpToDate) {
					Utils.debugLog("ConsoleResource.create: JupyterNotebook started");

					String sUrl = oNotebook.getUrl();

					oResult.setStringValue(sUrl);
					oResult.setBoolValue(true);

					return oResult;
				} else {
					Utils.debugLog("ConsoleResource.create: JupyterNotebook is not started or is out-of-date");

					// restart JN instance
					// update JN instance
					oJupyterNotebookRepository.deleteJupyterNotebook(sJupyterNotebookCode);
					return create(oRequest, sSessionId, sWorkspaceId);
				}

			} 
			else {
				
				Utils.debugLog("ConsoleResource.create: this is an NEW notebook");
				
				// This is a new notebook!
				oJupyterNotebook = new JupyterNotebook();
				oJupyterNotebook.setUserId(oWorkspace.getUserId());
				oJupyterNotebook.setWorkspaceId(sWorkspaceId);
				oJupyterNotebook.setCode(sJupyterNotebookCode);
				oJupyterNotebook.setAllowedIpAddresses(sUserId+":"+sClientIp);

				String sUrl = getNodeJupyterNotebookBasePath(getNodeBaseAddress(oWorkspace));
				sUrl += "notebook/";
				sUrl += sJupyterNotebookCode;

				oJupyterNotebook.setUrl(sUrl);

				oJupyterNotebookRepository.insertJupyterNotebook(oJupyterNotebook);
				
				// Schedule the process to run the processor
				String sProcessObjId = Utils.getRandomName();

				Utils.debugLog("ConsoleResource.create: create local operation");

				ProcessorParameter oProcessorParameter = new ProcessorParameter();
				oProcessorParameter.setName(sJupyterNotebookCode);
				oProcessorParameter.setProcessorType(ProcessorTypes.JUPYTER_NOTEBOOK);
				oProcessorParameter.setWorkspace(oWorkspace.getWorkspaceId());
				oProcessorParameter.setUserId(sUserId);
				oProcessorParameter.setExchange(sWorkspaceId);
				oProcessorParameter.setProcessObjId(sProcessObjId);
				oProcessorParameter.setSessionID(sSessionId);
				oProcessorParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspaceId));

				String sPath = WasdiConfig.Current.paths.serializationPath;

				PrimitiveResult oRes = Wasdi.runProcess(sUserId, sSessionId, LauncherOperations.LAUNCHJUPYTERNOTEBOOK.name(), "jupyter-notebook", sPath, oProcessorParameter);

				if (oRes.getBoolValue()) {
					oResult.setStringValue("PATIENCE IS THE VIRTUE OF THE STRONG");
					oResult.setBoolValue(true);

					return oResult;
				} else {
					oResult.setStringValue("ERROR CREATING THE NOTEBOOK");
					oResult.setBoolValue(false);

					return oResult;
				}				
			}

		} catch (Exception oEx) {
			Utils.debugLog("ConsoleResource.create: " + oEx);

			oResult.setStringValue("Error in starting proccess");
			oResult.setBoolValue(false);

			return oResult;
		}
	}

	@POST
	@Path("/terminate")
	public PrimitiveResult terminate(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("workspaceId") String sWorkspaceId) {
		Utils.debugLog("ConsoleResource.terminate( WS: " + sWorkspaceId + " )");

		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);

		try {
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser == null) {
				Utils.debugLog("ConsoleResource.terminate( Session: " + sSessionId + ", WS: " + sWorkspaceId + " ): invalid session");
				oResult.setIntValue(Status.UNAUTHORIZED.getStatusCode());

				return oResult;
			}

			if (Utils.isNullOrEmpty(sWorkspaceId)) {
				oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());

				return oResult;
			}

			String sUserId = oUser.getUserId();


			//check the user can access the workspace
			if (!PermissionsUtils.canUserAccessWorkspace(sUserId, sWorkspaceId)) {
				Utils.debugLog("ConsoleResource.terminate: user cannot access workspace info, aborting");
				oResult.setIntValue(Status.FORBIDDEN.getStatusCode());

				return oResult;
			}

			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspaceId);

			if (oWorkspace == null) {
				Utils.debugLog("ConsoleResource.terminate: " + sWorkspaceId + " is not a valid workspace, aborting");
				oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());

				return oResult;
			}

			if (!Wasdi.s_sMyNodeCode.equals(oWorkspace.getNodeCode())) {

				oResult.setStringValue("Delegating the work to the appropiate node");
				oResult.setBoolValue(true);

				return oResult;
			}
			
			if (oWorkspace.getUserId().equals(sUserId) == false) {
				Utils.debugLog("ConsoleResource.terminate: " + sWorkspaceId + " is not owned by " + sUserId);
				oResult.setIntValue(Status.FORBIDDEN.getStatusCode());

				return oResult;
			}

			String sJupyterNotebookCode = Utils.generateJupyterNotebookCode(oWorkspace.getUserId(), sWorkspaceId);

			JupyterNotebookRepository oJupyterNotebookRepository = new JupyterNotebookRepository();
			JupyterNotebook oJupyterNotebook = oJupyterNotebookRepository.getJupyterNotebookByCode(sJupyterNotebookCode);

			if (oJupyterNotebook != null) {
				Utils.debugLog("ConsoleResource.terminate: found JupyterNotebook record: " + sJupyterNotebookCode + ". Trying to delete it!");
				oJupyterNotebookRepository.deleteJupyterNotebook(sJupyterNotebookCode);
			} 
			else {
				Utils.debugLog("ConsoleResource.terminate: did not find JupyterNotebook record: " + sJupyterNotebookCode + "!!!");
				
				oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
				return oResult;				
			}


			// Schedule the process to run the processor
			String sProcessObjId = Utils.getRandomName();

			Utils.debugLog("ConsoleResource.terminate: create local operation " + sProcessObjId);

			ProcessorParameter oProcessorParameter = new ProcessorParameter();
			oProcessorParameter.setName("jupyter-notebook");
			oProcessorParameter.setProcessorType(ProcessorTypes.JUPYTER_NOTEBOOK);
			oProcessorParameter.setWorkspace(oWorkspace.getWorkspaceId());
			oProcessorParameter.setUserId(sUserId);
			oProcessorParameter.setExchange(sWorkspaceId);
			oProcessorParameter.setProcessObjId(sProcessObjId);
			oProcessorParameter.setSessionID(sSessionId);
			oProcessorParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspaceId));

			String sPath = WasdiConfig.Current.paths.serializationPath;

			PrimitiveResult oRes = Wasdi.runProcess(sUserId, sSessionId, LauncherOperations.TERMINATEJUPYTERNOTEBOOK.name(), sJupyterNotebookCode, sPath, oProcessorParameter);

			if (oRes.getBoolValue()) {
				oResult.setStringValue("Jupyter Notebook is terminated");
				oResult.setBoolValue(true);

				return oResult;
			} else {
				oResult.setStringValue("Jupyter Notebook could not been terminated");
				oResult.setBoolValue(false);

				return oResult;
			}

		} catch (Exception oEx) {
			Utils.debugLog("ConsoleResource.terminate: " + oEx);

			oResult.setStringValue("Error in termination proccess");
			oResult.setBoolValue(false);

			return oResult;
		}

	}

	@GET
	@Path("/active")
	public PrimitiveResult isJupyterNotebookActive(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("workspaceId") String sWorkspaceId) {
		Utils.debugLog("ConsoleResource.isJupyterNotebookActive( WS: " + sWorkspaceId + " )");

		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			Utils.debugLog("ConsoleResource.isJupyterNotebookActive( Session: " + sSessionId + ", WS: " + sWorkspaceId + " ): invalid session");
			oResult.setIntValue(Status.UNAUTHORIZED.getStatusCode());

			return oResult;
		}

		if (Utils.isNullOrEmpty(sWorkspaceId)) {
			oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());

			return oResult;
		}

		String sUserId = oUser.getUserId();


		//check the user can access the workspace
		if (!PermissionsUtils.canUserAccessWorkspace(sUserId, sWorkspaceId)) {
			Utils.debugLog("ConsoleResource.isJupyterNotebookActive: user cannot access workspace info, aborting");
			oResult.setIntValue(Status.FORBIDDEN.getStatusCode());

			return oResult;
		}

		WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
		Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspaceId);

		if (oWorkspace == null) {
			Utils.debugLog("ConsoleResource.isJupyterNotebookActive: " + sWorkspaceId + " is not a valid workspace, aborting");
			oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());

			return oResult;
		}
				
		JupyterNotebook oJupyterNotebook = internalIsJupyterActive(sUserId, oWorkspace.getWorkspaceId());
		
		if (oJupyterNotebook != null) {
			oResult.setBoolValue(true);			
			oResult.setStringValue(oJupyterNotebook.getUrl());
		} else {
			oResult.setBoolValue(false);
			oResult.setStringValue("The Jupyter Notebook instance is down.");
		}

		return oResult;
	}

	/**
	 * Checks if a notebook is up and running. We have 2 conditions for this: the docker with the notebook should answer
	 * and the configuration files should be equal to the ones in the reference folder.
	 * If both conditions are met, the answer is a PrimitiveResult with true and the url. 
	 * Otherwise is false.
	 * 
	 * @param sSessionId User Session
	 * @param sWorkspaceId Workspace where the notebook is requested
	 * @return Primitive Result with true and the url if ready, with false if not
	 */
	@GET
	@Path("/ready")
	public Response isNotebookReady(@HeaderParam("x-session-token") String sSessionId, @QueryParam("workspaceId") String sWorkspaceId) {
		
		try {
			
			// Verify the user
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser == null) {
				Utils.debugLog("ConsoleResource.ready( Session: " + sSessionId + ", WS: " + sWorkspaceId + " ): invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}

			if (Utils.isNullOrEmpty(sWorkspaceId)) {
				Utils.debugLog("ConsoleResource.ready( Session: " + sSessionId + ", WS: " + sWorkspaceId + " ): invalid workspace id");
				return Response.status(Status.BAD_REQUEST).build();
			}

			String sUserId = oUser.getUserId();

			//check the user can access the workspace
			if (!PermissionsUtils.canUserAccessWorkspace(sUserId, sWorkspaceId)) {
				Utils.debugLog("ConsoleResource.ready: user cannot access workspace info, aborting");
				return Response.status(Status.FORBIDDEN).build();
			}
			
			// And check the workspace it self
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspaceId);

			if (oWorkspace == null) {
				Utils.debugLog("ConsoleResource.ready: " + sWorkspaceId + " is not a valid workspace, aborting");
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			// Ok all in the right direction, lets check if the docker answers
			JupyterNotebook oNotebook = internalIsJupyterActive(sUserId, sWorkspaceId);
			
			if (oNotebook == null) {
				
				// No: so for sure is not ready
				Utils.debugLog("ConsoleResource.ready: notebook not active, return false");
				
				PrimitiveResult oResult = new PrimitiveResult();
				oResult.setBoolValue(false);
				return Response.ok(oResult).build();
			}
			
			// Is it updated?
			boolean bIsUpToDate = internalNotebookUpToDate();

			PrimitiveResult oResult = new PrimitiveResult();
			
			if (bIsUpToDate) {
				// Ok we can return the url
				Utils.debugLog("ConsoleResource.ready: JupyterNotebook Up and Running");
				
				oResult.setStringValue(oNotebook.getUrl());
				oResult.setBoolValue(true);
			} else {
				// No way
				Utils.debugLog("ConsoleResource.ready: JupyterNotebook changed, not ready");
				oResult.setBoolValue(false);				
			}	
			
			return Response.ok(oResult).build();
		}
		catch (Exception oEx) {
			Utils.debugLog("ConsoleResource.create: JupyterNotebook started");
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		

	}
	
	/**
	 * Checks if a Notebook is up and running or not
	 * @param sUserId
	 * @param sWorkspaceId
	 * @return The Notebook entity if it is up and running, Null if not
	 */
	private JupyterNotebook internalIsJupyterActive(String sUserId, String sWorkspaceId) {
		
		// Min domain control
		if (Utils.isNullOrEmpty(sUserId)) return null;
		if (Utils.isNullOrEmpty(sWorkspaceId)) return null;
		
		try {
			
			// Generate the code
			String sJupyterNotebookCode = Utils.generateJupyterNotebookCode(sUserId, sWorkspaceId);

			// Try to get the notebook from the db
			JupyterNotebookRepository oJupyterNotebookRepository = new JupyterNotebookRepository();
			JupyterNotebook oJupyterNotebook = oJupyterNotebookRepository.getJupyterNotebookByCode(sJupyterNotebookCode);
			
			// Notebook not present
			if (oJupyterNotebook == null) {
				return null;
			}
			
			// Lets see if it answer
			HttpCallResponse oHttpCallResponse = HttpUtils.newStandardHttpGETQuery(oJupyterNotebook.getUrl(), Collections.emptyMap());

			int iResponseCode = oHttpCallResponse.getResponseCode().intValue();

			if (iResponseCode == 200) {
				// ok good
				return oJupyterNotebook;
			} else {
				// not active
				return null;
			}			
		}
		catch (Exception oEx) {
			// Something went wrong
			Utils.debugLog("ConsoleResource.internalIsActive exception: " + oEx.toString());
		}
		
		return null;
	}
	
	/**
	 * Check if the notebook is up to date or not
	 * @return true of the conf files of the notebook are the same of the reference folder
	 */
	private boolean internalNotebookUpToDate() {

		String sProcessorName = "jupyter-notebook";

		String sProcessorTemplateGeneralCommonEnvFilePath = getProcessorTemplateGeneralCommonEnvFilePath(sProcessorName);
		Utils.debugLog("ConsoleResource.internalNotebookUpToDate | sProcessorTemplateGeneralCommonEnvFilePath: " + sProcessorTemplateGeneralCommonEnvFilePath);

		String sProcessorGeneralCommonEnvFilePath = getProcessorGeneralCommonEnvFilePath(sProcessorName);
		Utils.debugLog("ConsoleResource.internalNotebookUpToDate | sProcessorGeneralCommonEnvFilePath: " + sProcessorGeneralCommonEnvFilePath);

		boolean bFilesAreTheSame = WasdiFileUtils.filesAreTheSame(sProcessorTemplateGeneralCommonEnvFilePath, sProcessorGeneralCommonEnvFilePath);
		Utils.debugLog("ConsoleResource.internalNotebookUpToDate | bFilesAreTheSame: " + bFilesAreTheSame);	
		
		return bFilesAreTheSame;
	}
	
	private Node getWorkspaceNode(Workspace oWorkspace) {
		NodeRepository oNodeRepo = new NodeRepository();

		String sNodeCode = oWorkspace.getNodeCode();
		Node oNode = oNodeRepo.getNodeByCode(sNodeCode);

		return oNode;
	}

	private String getNodeBaseAddress(Workspace oWorkspace) {
		Node oNode = getWorkspaceNode(oWorkspace); 

		if (oNode == null) {
			return WasdiConfig.Current.baseUrl + "/wasdiwebserver/rest";
		} else {
			return oNode.getNodeBaseAddress();
		}
	}

	/**
	 * Extract the JupyterNotebook base path out of the node base address
	 * from: https://test2.wasdi.net/wasdiwebserver/rest
	 * to:   https://test2.wasdi.net/
	 * @param sNodeBaseAddress the node's base address
	 * @return the jupyter notebook base path on that node
	 */
	private String getNodeJupyterNotebookBasePath(String sNodeBaseAddress) {
		if (Utils.isNullOrEmpty(sNodeBaseAddress)) {
			return null;
		}

		String sNodeDomain = sNodeBaseAddress.replace("wasdiwebserver/rest", "");
		String sNodeNormalizedDomain = URI.create(sNodeDomain).normalize().toString();

		return sNodeNormalizedDomain;
	}

	private String getProcessorGeneralCommonEnvFilePath(String sProcessorName) {
		// Set the processor path
		String sDownloadRootPath = WasdiConfig.Current.paths.downloadRootPath;

		if (!sDownloadRootPath.endsWith(File.separator)) sDownloadRootPath = sDownloadRootPath + File.separator;

		String sProcessorFolder = sDownloadRootPath + "processors" + File.separator + sProcessorName + File.separator;

		return sProcessorFolder + "var" + FILE_SEPARATOR + "general_common.env";
	}

	private String getProcessorTemplateGeneralCommonEnvFilePath(String sProcessorName) {
		// Set the processor template path
		String sDockerTemplatePath = WasdiConfig.Current.paths.dockerTemplatePath;

		if (!sDockerTemplatePath.endsWith(File.separator)) sDockerTemplatePath = sDockerTemplatePath + File.separator;

		String sProcessorTemplateFolder = sDockerTemplatePath + sProcessorName + File.separator;

		return sProcessorTemplateFolder + "var" + FILE_SEPARATOR + "general_common.env";
	}

	/**
	 * Checks if the actual client Ip fo the user is enabled for the notebook
	 * 
	 * @param sAllowedIpAddresses List of user:ip strings separated by ";"
	 * @param sUserId Actual User
	 * @param sClientIp Actual Ip
	 * @return true if allowed, false otherwise
	 */
	private boolean isClientIpAllowedForThisUser(String sAllowedIpAddresses, String sUserId, String sClientIp) {
		
		boolean bIsAllowed = false;
		
		try {
			// Take the allowed user:ip list
			String [] asAllowedIp = sAllowedIpAddresses.split(";");
			
			// Must be not null
			if (asAllowedIp!=null) {
				// Cycle on all the elements we have
				for (String sAllowedUserIp : asAllowedIp) {
					// Split user and ip
					String [] asUserIp = sAllowedUserIp.split(":");
					// Again, safe programming
					if (asUserIp!=null) {
						// We need 2 values
						if (asUserIp.length>1) {
							// Ok this is the user
							String sNotebookUser = asUserIp[0];
							// And this is the ip
							String sAllowedIp = asUserIp[1];
							
							// Is the user requesting the access ?
							if (sNotebookUser.equals(sUserId)) {
								// Do we have the IP ?
								if (sClientIp.equals(sAllowedIp)) {
									// Ok, allowed!!
									bIsAllowed = true;
									break;
								}																			
							}
						}
					}
				}
			}					
		}
		catch (Exception oEx) {
			Utils.debugLog("ConsoleResource.isClientIpAllowedForThisUser exception: " + oEx.toString());
		}

		return bIsAllowed;
	}
	
	protected String resolveClientIp(HttpServletRequest oRequest) {
		
		String sXRealIp = oRequest.getHeader("X-Real-IP");
		String sXForwardedFor = oRequest.getHeader("X-Forwarded-For");
		String sRemoteAddr = oRequest.getRemoteAddr();
		
		if (sXRealIp != null)
			return sXRealIp;
		
		if (!Utils.isNullOrEmpty(sXForwardedFor)) return sXForwardedFor;
		else return sRemoteAddr;
	}

}
