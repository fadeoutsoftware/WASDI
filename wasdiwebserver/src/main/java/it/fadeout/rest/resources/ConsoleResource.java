package it.fadeout.rest.resources;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response.Status;

import it.fadeout.Wasdi;
import it.fadeout.threads.LaunchJupyterNotebookWorker;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.JupyterNotebook;
import wasdi.shared.business.Node;
import wasdi.shared.business.Processor;
import wasdi.shared.business.User;
import wasdi.shared.business.Workspace;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.JupyterNotebookRepository;
import wasdi.shared.data.NodeRepository;
import wasdi.shared.data.ProcessorRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.PrimitiveResult;

@Path("/console")
public class ConsoleResource {

	@POST
	@Path("/create")
	public PrimitiveResult createConsole(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("workspaceId") String sWorkspaceId) {
		Utils.debugLog("ConsoleResource.createConsole( Session: " + sSessionId + ", WS: " + sWorkspaceId + " )");

		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);

		try {
			User oUser = Wasdi.getUserFromSession(sSessionId);

			if (oUser == null) {
				Utils.debugLog("ConsoleResource.createConsole( Session: " + sSessionId + ", WS: " + sWorkspaceId + " ): invalid session");
				oResult.setIntValue(Status.UNAUTHORIZED.getStatusCode());

				return oResult;
			}

			if (Utils.isNullOrEmpty(sWorkspaceId)) {
				oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());

				return oResult;
			}

			String sUserId = oUser.getUserId();

			Utils.debugLog("ConsoleResource.createConsole: get Processor");

			ProcessorRepository oProcessorRepository = new ProcessorRepository();

			String sJNProcessorName = "jupyter-notebook";
			Processor oJNProcessor = oProcessorRepository.getProcessorByName(sJNProcessorName);

			if (oJNProcessor == null) {
				Utils.debugLog("ConsoleResource.createConsole: unable to find processor " + sJNProcessorName);
				return oResult;
			}


			//check the user can access the workspace
			if (!PermissionsUtils.canUserAccessWorkspace(sUserId, sWorkspaceId)) {
				Utils.debugLog("WorkspaceResource.getWorkspaceNameById: user cannot access workspace info, aborting");
				oResult.setIntValue(Status.FORBIDDEN.getStatusCode());

				return oResult;
			}

			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspaceId);

			if (oWorkspace == null) {
				Utils.debugLog("ConsoleResource.createConsole: " + sWorkspaceId + " is not a valid workspace, aborting");
				oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());

				return oResult;
			}



			if (Wasdi.s_sMyNodeCode.equals("wasdi")) {

				// In the main node: start a thread to engage the relevant computing node

				try {
					Utils.debugLog("ConsoleResource.createConsole: this is the main node, starting Worker to engage the relevant computing node");

					//This is the main node: forward the request to other nodes
					LaunchJupyterNotebookWorker oWorker = new LaunchJupyterNotebookWorker();

					NodeRepository oNodeRepo = new NodeRepository();

					Node oNode = oNodeRepo.getNodeByCode(oWorkspace.getNodeCode());

					if (oNode != null) {
						oWorker.init(oNode, sSessionId, sWorkspaceId);
						oWorker.start();

						Utils.debugLog("ConsoleResource.createConsole: Worker started");
					}
				} catch (Exception oEx) {
					Utils.debugLog("ConsoleResource.createConsole: error starting UpdateProcessorEnvironmentWorker " + oEx.toString());
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
				String sUrl = oJupyterNotebook.getUrl();

				boolean bIsActive = isJupyterNotebookActive(sUrl);

				if (bIsActive) {
					Utils.debugLog("ConsoleResource.createConsole: JupyterNotebook started");

//					oResult.setStringValue("https://test2.wasdi.net/notebook/67a5d87e4553a44345fe233ba6240e1a5912c4640e77d8020e6e6534/tree?");
					oResult.setStringValue(sUrl);
					oResult.setBoolValue(true);

					return oResult;
				} else {
					// the JupyterNotebook is already registered on MongoDB
					Utils.debugLog("ConsoleResource.createConsole: JupyterNotebook exists but it is not started");
				}

			} else {
				oJupyterNotebook = new JupyterNotebook();
				oJupyterNotebook.setUserId(sUserId);
				oJupyterNotebook.setWorkspaceId(sWorkspaceId);
				oJupyterNotebook.setCode(sJupyterNotebookCode);

				// https://test2.wasdi.net/notebook/67a5d87e4553a44345fe233ba6240e1a5912c4640e77d8020e6e6534/tree?
				String sUrl = getNodeJupyterNotebookBasePath(getNodeBaseAddress(oWorkspace.getNodeCode()));
				sUrl += "notebook/";
				sUrl += sJupyterNotebookCode;
//				sUrl += "/tree?";

				oJupyterNotebook.setUrl(sUrl);

				oJupyterNotebookRepository.insertJupyterNotebook(oJupyterNotebook);
			}


			// Schedule the process to run the processor
			String sProcessObjId = Utils.getRandomName();

			Utils.debugLog("ConsoleResource.createConsole: create local operation");

			ProcessorParameter oProcessorParameter = new ProcessorParameter();
			oProcessorParameter.setName(oJNProcessor.getName());
			oProcessorParameter.setProcessorID(oJNProcessor.getProcessorId());
			oProcessorParameter.setProcessorType(oJNProcessor.getType());
			oProcessorParameter.setWorkspace(oWorkspace.getWorkspaceId());
			oProcessorParameter.setUserId(sUserId);
			oProcessorParameter.setExchange(sWorkspaceId);
			oProcessorParameter.setProcessObjId(sProcessObjId);
			oProcessorParameter.setSessionID(sSessionId);
			oProcessorParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(sWorkspaceId));

			String sPath = WasdiConfig.Current.paths.serializationPath;

			PrimitiveResult oRes = Wasdi.runProcess(sUserId, sSessionId, LauncherOperations.LAUNCHJUPYTERNOTEBOOK.name(), oJNProcessor.getName(), sPath, oProcessorParameter);

			if (oRes.getBoolValue()) {
//				oResult.setStringValue("https://test2.wasdi.net/notebook/67a5d87e4553a44345fe233ba6240e1a5912c4640e77d8020e6e6534/tree?");
				System.out.println("oJupyterNotebook.getUrl(): " + oJupyterNotebook.getUrl());
//				oResult.setStringValue(oJupyterNotebook.getUrl());
				oResult.setStringValue("Jupyter Notebook is starting");
				oResult.setBoolValue(true);

				return oResult;
			} else {
				oResult.setStringValue("Jupyter Notebook could not been started");
				oResult.setBoolValue(false);

				return oResult;
			}

		} catch (Exception oEx) {
			Utils.debugLog("ConsoleResource.createConsole: " + oEx);

			oResult.setStringValue("Error in save proccess");
			oResult.setBoolValue(false);

			return oResult;
		}
	}

	private boolean isJupyterNotebookActive(String sUrl) {
		Map<String, String> asHeaders = Collections.emptyMap();

		HttpCallResponse oHttpCallResponse = HttpUtils.newStandardHttpGETQuery(sUrl, asHeaders);

		return oHttpCallResponse.getResponseCode().intValue() == 200;
	}

	private String getNodeBaseAddress(String sNodeCode) {
		NodeRepository oNodeRepository = new NodeRepository();
		Node oNode = oNodeRepository.getNodeByCode(sNodeCode);

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

}
