package it.fadeout.rest.resources;

import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response.Status;

import it.fadeout.Wasdi;
import it.fadeout.threads.LaunchJupyterNotebookWorker;
import it.fadeout.threads.TerminateJupyterNotebookWorker;
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
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.PrimitiveResult;

@Path("/console")
public class ConsoleResource {

	private static final String FILE_SEPARATOR = System.getProperty("file.separator");

	@POST
	@Path("/create")
	public PrimitiveResult create(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("workspaceId") String sWorkspaceId) {
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



			if (Wasdi.s_sMyNodeCode.equals("wasdi")) {

				// In the main node: start a thread to engage the relevant computing node

				try {
					Utils.debugLog("ConsoleResource.create: this is the main node, starting Worker to engage the relevant computing node");

					//This is the main node: forward the request to other nodes
					LaunchJupyterNotebookWorker oWorker = new LaunchJupyterNotebookWorker();

					Node oNode = getWorkspaceNode(oWorkspace);

					if (oNode != null) {
						oWorker.init(oNode, sSessionId, sWorkspaceId);
						oWorker.start();

						Utils.debugLog("ConsoleResource.create: Worker started");
					}
				} catch (Exception oEx) {
					Utils.debugLog("ConsoleResource.create: error starting LaunchJupyterNotebookWorker " + oEx.toString());
				}
			}



			if (!Wasdi.s_sMyNodeCode.equals(oWorkspace.getNodeCode())) {

				oResult.setStringValue("ROUTING YOUR REQUEST TO DESTINATION NODE");
				oResult.setBoolValue(true);

				return oResult;
			}


			String sJupyterNotebookCode = Utils.generateJupyterNotebookCode(sUserId, sWorkspaceId);

			JupyterNotebookRepository oJupyterNotebookRepository = new JupyterNotebookRepository();
			JupyterNotebook oJupyterNotebook = oJupyterNotebookRepository.getJupyterNotebookByCode(sJupyterNotebookCode);

			if (oJupyterNotebook != null) {

				PrimitiveResult oIsActiveResult = isJupyterNotebookActive(sSessionId, sWorkspaceId);
				boolean bIsActive = oIsActiveResult.getBoolValue();
				Utils.debugLog("ConsoleResource.create | bIsActive: " + bIsActive);

				PrimitiveResult oIsUpToDateResult = isJupyterNotebookUpToDate(sSessionId, sWorkspaceId);
				boolean bIsUpToDate = oIsUpToDateResult.getBoolValue();
				Utils.debugLog("ConsoleResource.create | bIsUpToDate: " + bIsUpToDate);

				if (bIsActive && bIsUpToDate) {
					Utils.debugLog("ConsoleResource.create: JupyterNotebook started");

					String sUrl = oIsActiveResult.getStringValue();

					oResult.setStringValue(sUrl);
					oResult.setBoolValue(true);

					return oResult;
				} else {
					Utils.debugLog("ConsoleResource.create: JupyterNotebook exists but it is not started or is out-of-date");
					Utils.debugLog("ConsoleResource.create: " + oIsActiveResult.getStringValue());

					// restart JN instance
					// update JN instance
					oJupyterNotebookRepository.deleteJupyterNotebook(sJupyterNotebookCode);
					return create(sSessionId, sWorkspaceId);
				}

			} else {
				oJupyterNotebook = new JupyterNotebook();
				oJupyterNotebook.setUserId(sUserId);
				oJupyterNotebook.setWorkspaceId(sWorkspaceId);
				oJupyterNotebook.setCode(sJupyterNotebookCode);

				String sUrl = getNodeJupyterNotebookBasePath(getNodeBaseAddress(oWorkspace));
				sUrl += "notebook/";
				sUrl += sJupyterNotebookCode;

				oJupyterNotebook.setUrl(sUrl);

				oJupyterNotebookRepository.insertJupyterNotebook(oJupyterNotebook);
			}


			// Schedule the process to run the processor
			String sProcessObjId = Utils.getRandomName();

			Utils.debugLog("ConsoleResource.create: create local operation");

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

			PrimitiveResult oRes = Wasdi.runProcess(sUserId, sSessionId, LauncherOperations.LAUNCHJUPYTERNOTEBOOK.name(), sJupyterNotebookCode, sPath, oProcessorParameter);

			if (oRes.getBoolValue()) {
				oResult.setStringValue("PATIENCE IS THE VIRTUE OF THE STRONG");
				oResult.setBoolValue(true);

				return oResult;
			} else {
				oResult.setStringValue("ERROR CREATING NOTEBOOK");
				oResult.setBoolValue(false);

				return oResult;
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



			if (Wasdi.s_sMyNodeCode.equals("wasdi")) {

				// In the main node: start a thread to engage the relevant computing node

				try {
					Utils.debugLog("ConsoleResource.terminate: this is the main node, starting Worker to engage the relevant computing node");

					//This is the main node: forward the request to other nodes
					TerminateJupyterNotebookWorker oWorker = new TerminateJupyterNotebookWorker();

					Node oNode = getWorkspaceNode(oWorkspace);

					if (oNode != null) {
						oWorker.init(oNode, sSessionId, sWorkspaceId);
						oWorker.start();

						Utils.debugLog("ConsoleResource.terminate: Worker started");
					}
				} catch (Exception oEx) {
					Utils.debugLog("ConsoleResource.terminate: error starting TerminateJupyterNotebookWorker " + oEx.toString());
				}
			}



			if (!Wasdi.s_sMyNodeCode.equals(oWorkspace.getNodeCode())) {

				oResult.setStringValue("Delegating the work to the appropiate node");
				oResult.setBoolValue(true);

				return oResult;
			}


			String sJupyterNotebookCode = Utils.generateJupyterNotebookCode(sUserId, sWorkspaceId);

			JupyterNotebookRepository oJupyterNotebookRepository = new JupyterNotebookRepository();
			JupyterNotebook oJupyterNotebook = oJupyterNotebookRepository.getJupyterNotebookByCode(sJupyterNotebookCode);

			if (oJupyterNotebook != null) {
				Utils.debugLog("ConsoleResource.terminate: found JupyterNotebook record: " + sJupyterNotebookCode + ". Trying to delete it!");

				oJupyterNotebookRepository.deleteJupyterNotebook(sJupyterNotebookCode);
			} else {
				Utils.debugLog("ConsoleResource.terminate: did not find JupyterNotebook record: " + sJupyterNotebookCode + "!!!");
			}


			// Schedule the process to run the processor
			String sProcessObjId = Utils.getRandomName();

			Utils.debugLog("ConsoleResource.terminate: create local operation");

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
	@Path("/isJupyterNotebookUpToDate")
	public PrimitiveResult isJupyterNotebookUpToDate(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("workspaceId") String sWorkspaceId) {
		Utils.debugLog("ConsoleResource.isJupyterNotebookUpToDate( WS: " + sWorkspaceId + " )");

		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);
		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			Utils.debugLog("ConsoleResource.isJupyterNotebookUpToDate( Session: " + sSessionId + ", WS: " + sWorkspaceId + " ): invalid session");
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
			Utils.debugLog("ConsoleResource.isJupyterNotebookUpToDate: user cannot access workspace info, aborting");
			oResult.setIntValue(Status.FORBIDDEN.getStatusCode());

			return oResult;
		}

		WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
		Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspaceId);

		if (oWorkspace == null) {
			Utils.debugLog("ConsoleResource.isJupyterNotebookUpToDate: " + sWorkspaceId + " is not a valid workspace, aborting");
			oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());

			return oResult;
		}


		String sProcessorName = "jupyter-notebook";

		String sProcessorTemplateGeneralCommonEnvFilePath = getProcessorTemplateGeneralCommonEnvFilePath(sProcessorName);
		Utils.debugLog("ConsoleResource.isJupyterNotebookUpToDate | sProcessorTemplateGeneralCommonEnvFilePath: " + sProcessorTemplateGeneralCommonEnvFilePath);

		String sProcessorGeneralCommonEnvFilePath = getProcessorGeneralCommonEnvFilePath(sProcessorName);
		Utils.debugLog("ConsoleResource.isJupyterNotebookUpToDate | sProcessorGeneralCommonEnvFilePath: " + sProcessorGeneralCommonEnvFilePath);

		boolean bFilesAreTheSame = WasdiFileUtils.filesAreTheSame(sProcessorTemplateGeneralCommonEnvFilePath, sProcessorGeneralCommonEnvFilePath);
		Utils.debugLog("ConsoleResource.isJupyterNotebookUpToDate | bFilesAreTheSame: " + bFilesAreTheSame);

		oResult.setBoolValue(bFilesAreTheSame);

		return oResult;
	}

	@GET
	@Path("/isJupyterNotebookActive")
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


		Map<String, String> asHeaders = Collections.emptyMap();

		String sJupyterNotebookCode = Utils.generateJupyterNotebookCode(sUserId, sWorkspaceId);

		JupyterNotebookRepository oJupyterNotebookRepository = new JupyterNotebookRepository();
		JupyterNotebook oJupyterNotebook = oJupyterNotebookRepository.getJupyterNotebookByCode(sJupyterNotebookCode);
		String sUrl = oJupyterNotebook.getUrl();

		HttpCallResponse oHttpCallResponse = HttpUtils.newStandardHttpGETQuery(sUrl, asHeaders);

		int iResponseCode = oHttpCallResponse.getResponseCode().intValue();

		if (iResponseCode == 200) {
			oResult.setBoolValue(true);
			oResult.setStringValue(sUrl);
		} else {
			oResult.setBoolValue(false);
			oResult.setStringValue("The Jupyter Notebook instance is down.");
		}

		oResult.setIntValue(iResponseCode);

		return oResult;
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

}