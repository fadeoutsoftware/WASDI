package it.fadeout.rest.resources;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.FileUtils;

import it.fadeout.Wasdi;
import it.fadeout.services.ProcessWorkspaceService;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.CloudProvider;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.JupyterNotebook;
import wasdi.shared.business.Node;
import wasdi.shared.business.ProductWorkspace;
import wasdi.shared.business.PublishedBand;
import wasdi.shared.business.Workspace;
import wasdi.shared.business.processors.ProcessorTypes;
import wasdi.shared.business.users.ResourceTypes;
import wasdi.shared.business.users.User;
import wasdi.shared.business.users.UserAccessRights;
import wasdi.shared.business.users.UserApplicationRole;
import wasdi.shared.business.users.UserResourcePermission;
import wasdi.shared.config.PathsConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.CloudProviderRepository;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.JupyterNotebookRepository;
import wasdi.shared.data.NodeRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProductWorkspaceRepository;
import wasdi.shared.data.PublishedBandsRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.data.UserResourcePermissionRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.geoserver.GeoServerManager;
import wasdi.shared.parameters.ProcessorParameter;
import wasdi.shared.utils.MailUtils;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.wasdiAPI.WorkspaceAPIClient;
import wasdi.shared.viewmodels.ClientMessageCodes;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.processworkspace.NodeScoreByProcessWorkspaceViewModel;
import wasdi.shared.viewmodels.workspaces.WorkspaceEditorViewModel;
import wasdi.shared.viewmodels.workspaces.WorkspaceListInfoViewModel;
import wasdi.shared.viewmodels.workspaces.WorkspaceSharingViewModel;

/**
 * Workspace Resource.
 *
 * Hosts API for:
 * 	.create, edit and delete workspaces
 * 	.get workspace list
 * 	.share workspaces
 *
 * @author p.campanella
 *
 */
@Path("/ws")
public class WorkspaceResource {

	@Context
	ServletConfig m_oServletConfig;

	/**
	 * Get a list of workspaces of a user
	 * @param sSessionId User Session Id
	 * @return List of Workspace List Info View Model
	 */
	@GET
	@Path("/byuser")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public List<WorkspaceListInfoViewModel> getListByUser(@HeaderParam("x-session-token") String sSessionId) {

		WasdiLog.debugLog("WorkspaceResource.getListByUser");

		User oUser = Wasdi.getUserFromSession(sSessionId);
		
		List<WorkspaceListInfoViewModel> aoUserWorkspacesList = new ArrayList<>();
		
		// Domain Check
		if (oUser == null) {
			WasdiLog.warnLog("WorkspaceResource.getListByUser: invalid session");
			return aoUserWorkspacesList;
		}

		try {

			WasdiLog.debugLog("WorkspaceResource.getListByUser: workspaces for " + oUser.getUserId());
			
			// Get the node list
			NodeRepository oNodeRepository = new NodeRepository();
			List<Node> aoNodeList = oNodeRepository.getNodesList();
			
			// And create a map based on the node code
			Map<String, Node> aoNodeMap = new HashMap<>();
			for (Node oNode : aoNodeList) {
				if (aoNodeMap.putIfAbsent(oNode.getNodeCode(), oNode) != null) {
					WasdiLog.warnLog("WorkspaceResource.getListByUser: duplicated key for node " + oNode.getNodeCode());
				}
			}
			

			// Create repo
			WorkspaceRepository oWSRepository = new WorkspaceRepository();
            UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();

			// Get Workspace List
			List<Workspace> aoWorkspaces = oWSRepository.getWorkspaceByUser(oUser.getUserId());

			// For each
			for (int iWorkspaces = 0; iWorkspaces < aoWorkspaces.size(); iWorkspaces++) {
				// Create View Model
				WorkspaceListInfoViewModel oWSViewModel = new WorkspaceListInfoViewModel();
				Workspace oWorkspace = aoWorkspaces.get(iWorkspaces);

				oWSViewModel.setOwnerUserId(oUser.getUserId());
				oWSViewModel.setWorkspaceId(oWorkspace.getWorkspaceId());
				oWSViewModel.setWorkspaceName(oWorkspace.getName());
				oWSViewModel.setNodeCode(oWorkspace.getNodeCode());
				oWSViewModel.setCreationDate(Utils.getDate(oWorkspace.getCreationDate()));
				oWSViewModel.setPublic(oWorkspace.isPublic());

				if (!Utils.isNullOrEmpty(oWorkspace.getNodeCode())) {
					if (oWorkspace.getNodeCode().equals("wasdi")) {
						oWSViewModel.setActiveNode(true);
					} else {
						Node oNode = aoNodeMap.get(oWorkspace.getNodeCode());

						if (oNode != null) {
							oWSViewModel.setActiveNode(oNode.getActive());
						}
					}
				}

				// Get Sharings
                List<UserResourcePermission> aoPermissions = oUserResourcePermissionRepository.getWorkspaceSharingsByWorkspaceId(oWorkspace.getWorkspaceId());

				// Add Sharings to View Model
				if (aoPermissions != null) {
					for (int i = 0; i < aoPermissions.size(); i++) {
						if (oWSViewModel.getSharedUsers() == null) {
							oWSViewModel.setSharedUsers(new ArrayList<String>());
						}

						oWSViewModel.getSharedUsers().add(aoPermissions.get(i).getUserId());
					}
				}

				aoUserWorkspacesList.add(oWSViewModel);
			}

			// Get the list of workspace shared with this user
            List<UserResourcePermission> aoSharedWorkspaces = oUserResourcePermissionRepository.getWorkspaceSharingsByUserId(oUser.getUserId());

			if (aoSharedWorkspaces.size() > 0) {
				// For each
				for (int iWorkspaces = 0; iWorkspaces < aoSharedWorkspaces.size(); iWorkspaces++) {

					// Create View Model
					WorkspaceListInfoViewModel oWSViewModel = new WorkspaceListInfoViewModel();
					Workspace oWorkspace = oWSRepository.getWorkspace(aoSharedWorkspaces.get(iWorkspaces).getResourceId());

					if (oWorkspace == null) {
						WasdiLog.debugLog("WorkspaceResult.getListByUser: WS Shared not available " + aoSharedWorkspaces.get(iWorkspaces).getResourceId());
						continue;
					}

					oWSViewModel.setOwnerUserId(oWorkspace.getUserId());
					oWSViewModel.setWorkspaceId(oWorkspace.getWorkspaceId());
					oWSViewModel.setWorkspaceName(oWorkspace.getName());
					oWSViewModel.setNodeCode(oWorkspace.getNodeCode());
					oWSViewModel.setCreationDate(Utils.getDate(oWorkspace.getCreationDate()));
					oWSViewModel.setPublic(oWorkspace.isPublic());
					oWSViewModel.setReadOnly(!aoSharedWorkspaces.get(iWorkspaces).canWrite());

					if (!Utils.isNullOrEmpty(oWorkspace.getNodeCode())) {
						if (oWorkspace.getNodeCode().equals("wasdi")) {
							oWSViewModel.setActiveNode(true);
						} else {
							Node oNode = aoNodeMap.get(oWorkspace.getNodeCode());

							if (oNode != null) {
								oWSViewModel.setActiveNode(oNode.getActive());
							}
						}
					}

					// Get Sharings
					List<UserResourcePermission> aoSharings = oUserResourcePermissionRepository.getWorkspaceSharingsByWorkspaceId(oWorkspace.getWorkspaceId());

					// Add Sharings to View Model
					if (aoSharings != null) {
						for (int iSharings = 0; iSharings < aoSharings.size(); iSharings++) {
							if (oWSViewModel.getSharedUsers() == null) {
								oWSViewModel.setSharedUsers(new ArrayList<String>());
							}

							oWSViewModel.getSharedUsers().add(aoSharings.get(iSharings).getUserId());
						}
					}

					aoUserWorkspacesList.add(oWSViewModel);
				}
			}

		} catch (Exception oEx) {
			WasdiLog.errorLog("WorkspaceResource.getListByUser error: " + oEx);
		}

		return aoUserWorkspacesList;
	}

	/**
	 * Returns the View Model of the workspace.
	 * The view model contains the baseic parameters of the ws, plus the base URL
	 * for the api calls following api calls, accordingly to the node url from DB.
	 * To change the workspace node Id checks the "update" call on this resource
	 * @param sSessionId the current sesssion, that should be validated
	 * @param sWorkspaceId Unique identifier of the workspace
	 * @return Workspace View Model with the updated values
	 */
	@GET
	@Path("getws")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public WorkspaceEditorViewModel getWorkspaceEditorViewModel(@HeaderParam("x-session-token") String sSessionId, @QueryParam("workspace") String sWorkspaceId) {

		WasdiLog.debugLog("WorkspaceResource.GetWorkspaceEditorViewModel( WS: " + sWorkspaceId + ")");

		WorkspaceEditorViewModel oWorkspaceEditorViewModel = new WorkspaceEditorViewModel();

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.warnLog("WorkspaceResource.getWorkspaceEditorViewModel: invalid session");
			return null;
		}
		
		try {
			// Domain Check
			if (Utils.isNullOrEmpty(sWorkspaceId)) {
				WasdiLog.warnLog("WorkspaceResource.getWorkspaceEditorViewModel: invalid workspace id");
				return oWorkspaceEditorViewModel;
			}
			
			// The user can access the workspace?
			if(!PermissionsUtils.canUserAccessWorkspace(oUser.getUserId(), sWorkspaceId)) {
				WasdiLog.warnLog("WorkspaceResource.getWorkspaceEditorViewModel: user cannot access workspace info, aborting");
				return oWorkspaceEditorViewModel;
			}

			// Create repo
			WorkspaceRepository oWSRepository = new WorkspaceRepository();
            UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			
			// Get requested workspace
			Workspace oWorkspace = oWSRepository.getWorkspace(sWorkspaceId);
			oWorkspaceEditorViewModel.setUserId(oWorkspace.getUserId());
			oWorkspaceEditorViewModel.setWorkspaceId(oWorkspace.getWorkspaceId());
			oWorkspaceEditorViewModel.setName(oWorkspace.getName());
			oWorkspaceEditorViewModel.setCreationDate(Utils.getDate(oWorkspace.getCreationDate()));
			oWorkspaceEditorViewModel.setLastEditDate(Utils.getDate(oWorkspace.getLastEditDate()));
			oWorkspaceEditorViewModel.setNodeCode(oWorkspace.getNodeCode());
			oWorkspaceEditorViewModel.setPublic(oWorkspace.isPublic());
			oWorkspaceEditorViewModel.setReadOnly(!PermissionsUtils.canUserWriteWorkspace(oUser.getUserId(), oWorkspace.getWorkspaceId()));
			if (oWorkspace.getStorageSize() != null) {
				oWorkspaceEditorViewModel.setStorageSize(oWorkspace.getStorageSize() > 0 ? FileUtils.byteCountToDisplaySize(oWorkspace.getStorageSize()) : "0B");
			}
			
			
			ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
			oWorkspaceEditorViewModel.setProcessesCount(oProcessWorkspaceRepository.countByWorkspace(sWorkspaceId)); 
			// If the workspace is on another node, copy the url to the view model
			if (oWorkspace.getNodeCode().equals("wasdi") == false) {
				// Get the Node
				NodeRepository oNodeRepository = new NodeRepository();
				String sNodeCode = oWorkspace.getNodeCode(); 
				Node oWorkspaceNode = oNodeRepository.getNodeByCode(sNodeCode);
				if (oWorkspaceNode != null) {
					if (oWorkspaceNode.getActive()) {
						oWorkspaceEditorViewModel.setApiUrl(oWorkspaceNode.getNodeBaseAddress());
					} else {
						oWorkspaceEditorViewModel.setApiUrl(WasdiConfig.Current.baseUrl);
					}

					oWorkspaceEditorViewModel.setActiveNode(oWorkspaceNode.getActive());
										
					if (!Utils.isNullOrEmpty(oWorkspaceNode.getCloudProvider())) {
						oWorkspaceEditorViewModel.setCloudProvider(oWorkspaceNode.getCloudProvider());
						
						CloudProviderRepository oCloudProviderRepository = new CloudProviderRepository();
						CloudProvider oCloudProvider = oCloudProviderRepository.getCloudProviderByCode(oWorkspaceNode.getCloudProvider());
						
						if (oCloudProvider != null) {
							oWorkspaceEditorViewModel.setSlaLink(oCloudProvider.getSlaLink());
						}
						
					}
					else {
						oWorkspaceEditorViewModel.setCloudProvider(oWorkspaceNode.getNodeCode());
					}
				}
			
			}
			else {
				oWorkspaceEditorViewModel.setCloudProvider("wasdi");
				oWorkspaceEditorViewModel.setActiveNode(true);
			}

			// Get Sharings
			List<UserResourcePermission> aoSharings = oUserResourcePermissionRepository.getWorkspaceSharingsByWorkspaceId(oWorkspace.getWorkspaceId());
			
			// Add Sharings to View Model
			if (aoSharings != null) {
				if (oWorkspaceEditorViewModel.getSharedUsers() == null) {
					oWorkspaceEditorViewModel.setSharedUsers(new ArrayList<String>());
				}

				for (UserResourcePermission oSharing : aoSharings) {
					oWorkspaceEditorViewModel.getSharedUsers().add(oSharing.getUserId());
				}
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog( "WorkspaceResource.getWorkspaceEditorViewModel error: " + oEx);
		}

		return oWorkspaceEditorViewModel;
	}

	/**
	 * Create a new workspace
	 * @param sSessionId User Session Id
	 * @param sName Workspace Name
	 * @param sNodeCode Code of the Node where the workspace must be created
	 * @return PrimitiveResult with stringValue = workspaceId of the new workspace
	 */
	@GET
	@Path("create")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult createWorkspace(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("name") String sName, @QueryParam("node") String sNodeCode) {

		WasdiLog.debugLog("WorkspaceResource.createWorkspace(" + sName + ", " + sNodeCode + " )");

		// sName and sNodeCode can be null, and will be defaulted

		// Validate Session
		User oUser = Wasdi.getUserFromSession(sSessionId);
		
		if (oUser == null) {
			WasdiLog.warnLog("WorkspaceResource.createWorkspace: invalid session");
			return null;
		}

		// Create New Workspace
		Workspace oWorkspace = new Workspace();

		if (Utils.isNullOrEmpty(sName)) {
			WasdiLog.warnLog("WorkspaceResource.createWorkspace: name is null or empty, defaulting");
			sName = "Untitled Workspace";
		}

		WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();

		while (oWorkspaceRepository.getByUserIdAndWorkspaceName(oUser.getUserId(), sName) != null) {
			sName = Utils.cloneName(sName);
			WasdiLog.debugLog("WorkspaceResource.createWorkspace: a workspace with the same name already exists. Changing the name to " + sName);
		}

		// Default values
		oWorkspace.setCreationDate(Utils.nowInMillis());
		oWorkspace.setLastEditDate(Utils.nowInMillis());
		oWorkspace.setName(sName);
		oWorkspace.setUserId(oUser.getUserId());
		oWorkspace.setWorkspaceId(Utils.getRandomName());
		oWorkspace.setPublic(false);
		
		
		if (Utils.isNullOrEmpty(sNodeCode)) {
			
			WasdiLog.debugLog("WorkspaceResource.createWorkspace: search the best node");
			
			List<NodeScoreByProcessWorkspaceViewModel> aoCandidates = Wasdi.getNodesSortedByScore(sSessionId, null);
			
			if (aoCandidates!=null) {
				if (aoCandidates.size()>0) {
					sNodeCode = aoCandidates.get(0).getNodeCode();
				}
			}
		}
		
		if (Utils.isNullOrEmpty(sNodeCode)) {
			WasdiLog.debugLog("WorkspaceResource.createWorkspace: it was impossible to associate a Node: try user default");
			//get user's default nodeCode
			sNodeCode = oUser.getDefaultNode();
		}
		
		if (Utils.isNullOrEmpty(sNodeCode)) {
			WasdiLog.debugLog("WorkspaceResource.createWorkspace: it was impossible to associate a Node: use system default");
			// default node code
			sNodeCode = "wasdi";
		}
		
		WasdiLog.debugLog("WorkspaceResource.createWorkspace: selected node " + sNodeCode);
		oWorkspace.setNodeCode(sNodeCode);

		if (oWorkspaceRepository.insertWorkspace(oWorkspace)) {
			PrimitiveResult oResult = new PrimitiveResult();
			oResult.setBoolValue(true);
			oResult.setStringValue(oWorkspace.getWorkspaceId());

			return oResult;
		} 
		else {
			WasdiLog.debugLog("WorkspaceResource.createWorkspace: insertion FAILED");
			return null;
		}

	}

	/**
	 * Updates details of a Workspace
	 *
	 * @param sSessionId User Session Id
	 * @param oWorkspaceEditorViewModel Workspace Editor View Model
	 * @return
	 */
	@POST
	@Path("update")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public WorkspaceEditorViewModel updateWorkspace(@HeaderParam("x-session-token") String sSessionId, WorkspaceEditorViewModel oWorkspaceEditorViewModel) {

		WasdiLog.debugLog("WorkspaceResource.updateWorkspace");

		// Validate Session
		User oUser = Wasdi.getUserFromSession(sSessionId);
		
		if (oUser == null) {
			WasdiLog.warnLog("WorkspaceResource.updateWorkspace: invalid session");
			return null;
		}
		
		// Validate input view model
		if (oWorkspaceEditorViewModel==null) {
			WasdiLog.warnLog("WorkspaceResource.updateWorkspace: invalid Workspace View Model");
			return null;			
		}
		
		// Check if the user can access
		if (!PermissionsUtils.canUserWriteWorkspace(oUser.getUserId(), oWorkspaceEditorViewModel.getWorkspaceId())) {
			WasdiLog.warnLog("WorkspaceResource.updateWorkspace: user cannot access the workspace");
			return null;			
		}

		try {
			// Create New Workspace
			Workspace oWorkspace = new Workspace();
			// Initialize repository
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();

			String sName = oWorkspaceEditorViewModel.getName();

			Workspace oExistingWorkspace = oWorkspaceRepository.getByUserIdAndWorkspaceName(oUser.getUserId(), sName);

			while (oExistingWorkspace != null && !oExistingWorkspace.getWorkspaceId().equals(oWorkspaceEditorViewModel.getWorkspaceId())) {
				sName = Utils.cloneName(sName);
				WasdiLog.debugLog("WorkspaceResource.updateWorkspace: a workspace with the same name already exists. Changing the name to " + sName);

				oExistingWorkspace = oWorkspaceRepository.getByUserIdAndWorkspaceName(oUser.getUserId(), sName);
			}

			// Default values
			oWorkspace.setCreationDate((double) oWorkspaceEditorViewModel.getCreationDate().getTime());
			oWorkspace.setLastEditDate((double) oWorkspaceEditorViewModel.getLastEditDate().getTime());
			oWorkspace.setName(sName);
			oWorkspace.setUserId(oWorkspaceEditorViewModel.getUserId());
			oWorkspace.setWorkspaceId(oWorkspaceEditorViewModel.getWorkspaceId());
			
			if (oExistingWorkspace != null) {
				oWorkspace.setStorageSize(oExistingWorkspace.getStorageSize());
			}
			
			if (oWorkspace.isPublic() != oWorkspaceEditorViewModel.isPublic()) {
				oWorkspace.setPublic(oWorkspaceEditorViewModel.isPublic());
				oWorkspaceRepository.updateWorkspacePublicFlag(oWorkspace);				
			}

			
			// if present and different from "wasdi", the node code must be updated
			if (oWorkspaceEditorViewModel.getNodeCode() != null && !(oWorkspaceEditorViewModel.getNodeCode().equals("wasdi"))) {
				NodeRepository oNodeRepository = new NodeRepository();
				String sNodeCode = oWorkspaceEditorViewModel.getNodeCode();
				Node oWorkspaceNode = oNodeRepository.getNodeByCode(sNodeCode);
				oWorkspace.setNodeCode(sNodeCode);
				// Set the base url on the returning view model
				oWorkspaceEditorViewModel.setApiUrl(oWorkspaceNode.getNodeBaseAddress());
				// update on Db
				oWorkspaceRepository.updateWorkspaceNodeCode(oWorkspace);
			}
			else if (oWorkspaceEditorViewModel.getNodeCode() != null && oWorkspaceEditorViewModel.getNodeCode().equals("wasdi")) {
				oWorkspaceEditorViewModel.setApiUrl(WasdiConfig.Current.baseUrl);
				oWorkspaceRepository.updateWorkspaceNodeCode(oWorkspace);
			}

			if (oWorkspaceRepository.updateWorkspaceName(oWorkspace)) {
				PrimitiveResult oResult = new PrimitiveResult();
				oResult.setStringValue(oWorkspace.getWorkspaceId());

				oWorkspaceEditorViewModel.setName(sName);

				return oWorkspaceEditorViewModel;
			} else {
				return null;
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("WorkspaceResource.updateWorkspace: Error update workspace: " + oEx);
			return null;
		}
	}

	/**
	 * Delete a workspace. This deletes also all the Products included, all
	 * files and folder in the node and published layers.
	 *
	 * @param sSessionId User Session Id
	 * @param sWorkspaceId Workspace Id
	 * @param bDeleteLayer Flag to confirm to delete WxS layer
	 * @param bDeleteFile Flag to confirm to delete files
	 * @return std http response
	 */
	@DELETE
	@Path("delete")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response deleteWorkspace(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("workspace") String sWorkspaceId, @QueryParam("deletelayer") Boolean bDeleteLayer,
			@QueryParam("deletefile") Boolean bDeleteFile) {

		WasdiLog.debugLog("WorkspaceResource.deleteWorkspace( WS: " + sWorkspaceId + ", DeleteLayer: " + bDeleteLayer + ", DeleteFile: " + bDeleteFile + " SessionId: " + sSessionId + ")");
		User oUser = null;
		
		//preliminary checks
		try {
			// Validate Session
			oUser = Wasdi.getUserFromSession(sSessionId);
			
			if (oUser == null) {
				WasdiLog.warnLog("WorkspaceResource.deleteWorkspace: invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			// before any operation check that this is not an injection attempt from the user
			if ( sWorkspaceId.contains("/") || sWorkspaceId.contains("\\") || sWorkspaceId.contains(File.separator)) {
				WasdiLog.warnLog("WorkspaceResource.deleteWorkspace: Injection attempt by user");
				return Response.status(Status.BAD_REQUEST).build();
			}

			//check user can access given workspace
			if(!PermissionsUtils.canUserAccessWorkspace(oUser.getUserId(), sWorkspaceId)) {
				WasdiLog.warnLog("WorkspaceResource.deleteWorkspace: user cannot access workspace aborting");
				return Response.status(Status.FORBIDDEN).build();
			}
			
		} catch (Exception oE) {
			WasdiLog.errorLog("WorkspaceResource.deleteWorkspace error: " + oE);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}

		try {
			// workspace repository
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();

			//check that the workspace really exists
			Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspaceId);
			if(null==oWorkspace) {
				WasdiLog.warnLog("WorkspaceResource.deleteWorkspace: not a valid workspace, aborting");
				return Response.status(Status.BAD_REQUEST).build();
			}

			//delete sharing if the user is not the owner
			String sWorkspaceOwner = Wasdi.getWorkspaceOwner(sWorkspaceId);
			if (!sWorkspaceOwner.equals(oUser.getUserId())) {
				// This is not the owner of the workspace
				WasdiLog.infoLog("WorkspaceResource.deleteWorkspace: User " + oUser.getUserId() + " is not the owner [" + sWorkspaceOwner + "]: delete the sharing, not the ws");
                UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
                oUserResourcePermissionRepository.deletePermissionsByUserIdAndWorkspaceId(oUser.getUserId(), sWorkspaceId);
				return Response.ok().build();
			}
			
			if (!WasdiConfig.Current.nodeCode.equals(oWorkspace.getNodeCode())) {
				WasdiLog.infoLog("WorkspaceResource.deleteWorkspace: redirecting delete command on target node");
				
				try {
					NodeRepository oNodeRepo = new NodeRepository();
					Node oTargetNode = oNodeRepo.getNodeByCode(oWorkspace.getNodeCode());
					
					HttpCallResponse oHttpResponse = WorkspaceAPIClient.deleteWorkspace(oTargetNode, sSessionId, sWorkspaceId);
					
					return Response.status(oHttpResponse.getResponseCode()).build();					
				}
				catch (Exception oEx) {
					WasdiLog.errorLog("WorkspaceResource.deleteWorkspace: exception calling delete API on target node", oEx);
					return Response.serverError().build();
				}
			}
			
			
			// Terminate and clean the console/Jupyter Lab instance if present
			terminateConsole(oWorkspace, sSessionId);
			
			//kill active processes
			ProcessWorkspaceService oProcessWorkspaceService = new ProcessWorkspaceService();
			if(!oProcessWorkspaceService.killProcessesInWorkspace(sWorkspaceId, sSessionId, true)) {
				WasdiLog.warnLog("WorkspaceResource.deleteWorkspace: WARNING: could not schedule kill processes in workspace");
			}
			
			// get workspace path
			String sWorkspacePath = PathsConfig.getWorkspacePath(sWorkspaceOwner, sWorkspaceId);

			WasdiLog.debugLog("WorkspaceResource.deleteWorkspace: deleting Workspace " + sWorkspaceId + " of user " + sWorkspaceOwner);

			// Delete Workspace Db Entry
			if (oWorkspaceRepository.deleteWorkspace(sWorkspaceId)) {
				// Get all Products in workspace
				ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
				List<ProductWorkspace> aoProductsWorkspaces = oProductWorkspaceRepository.getProductsByWorkspace(sWorkspaceId);


				DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
				// Do we need to delete layers?
				if (bDeleteLayer) {
					try {
						WasdiLog.debugLog("ProductResource.deleteWorkspace: Deleting workspace layers");

						// GeoServer Manager Object
						GeoServerManager oGeoServerManager = new GeoServerManager();

						// For each product in the workspace, if is unique, delete published bands and metadata file ref
						for (ProductWorkspace oProductWorkspace : aoProductsWorkspaces) {

							// Get the downloaded file
							DownloadedFile oDownloadedFile = oDownloadedFilesRepository.getDownloadedFileByPath(oProductWorkspace.getProductName());

							// Is the product used also in other workspaces?
							List<DownloadedFile> aoDownloadedFileList = oDownloadedFilesRepository.getDownloadedFileListByName(oDownloadedFile.getFileName());

							if (aoDownloadedFileList.size() > 1) {
								// Yes, it is in other Ws, jump
								WasdiLog.debugLog("WorkspaceResource.deleteWorkspace: The file is also in other workspaces, leave the bands as they are");
								continue;
							}

							// We need the View Model product name: start from file name
							String sProductName = oDownloadedFile.getFileName();

							// If view model is available (should be), get the name from the view model
							if (oDownloadedFile.getProductViewModel() != null) {
								sProductName = oDownloadedFile.getProductViewModel().getName();
							}

							// Get the list of published bands by product name
							PublishedBandsRepository oPublishRepository = new PublishedBandsRepository();
							
							
							List<PublishedBand> aoPublishedBands = oPublishRepository.getPublishedBandsByProductName(PathsConfig.getWorkspacePath(sWorkspaceOwner, sWorkspaceId) + sProductName);

							// For each published band
							for (PublishedBand oPublishedBand : aoPublishedBands) {

								try {
									// Remove Geoserver layer (and file)
									if (!oGeoServerManager.removeLayer(oPublishedBand.getLayerId())) {
										WasdiLog.infoLog("WorkspaceResource.deleteWorkspace: error deleting layer " + oPublishedBand.getLayerId() + " from geoserver");
									}

									try {
										// delete published band on database
										oPublishRepository.deleteByProductNameLayerId(sProductName, oPublishedBand.getLayerId());
									} catch (Exception oEx) {
										WasdiLog.errorLog("WorkspaceResource.deleteWorkspace: error deleting published band on data base " + oEx.toString());
									}
								} catch (Exception oEx) {
									WasdiLog.errorLog("WorkspaceResource.deleteWorkspace: error deleting layer id " + oEx.toString());
								}
							}
							
							// If view model is available (should be), get the metadata file reference
							if (oDownloadedFile.getProductViewModel() != null) {
								
								String sMetadataFileRef = oDownloadedFile.getProductViewModel().getMetadataFileReference();
								
								if (!Utils.isNullOrEmpty(sMetadataFileRef)) {
									WasdiLog.debugLog("WorkspaceResource.deleteWorkspace: deleting metadata file " + sMetadataFileRef);
									FileUtils.deleteQuietly(new File(sMetadataFileRef));
								}
							}
							
						}
					} catch (Exception oE) {
						WasdiLog.errorLog("WorkspaceResource.deleteWorkspace: error while trying to delete layers: " + oE);
					}

				}

				if (bDeleteFile) {
					try {

						WasdiLog.debugLog("WorkspaceResource.deleteWorkspace: Delete workspace folder " + sWorkspacePath);

						// delete directory
						try {
							File oDir = new File(sWorkspacePath);
							if (!oDir.exists()) {
								WasdiLog.warnLog("WorkspaceResource.deleteWorkspace: trying to delete non existing directory " + sWorkspacePath);
							}
							// try anyway for two reasons:
							// 1. non existing directories are handled anyway
							// 2. try avoiding race conditions
							FileUtils.deleteDirectory(oDir);
						} catch (Exception oE) {
							WasdiLog.errorLog("WorkspaceResource.deleteWorkspace: error while trying to delete directory " + sWorkspacePath + ": " + oE);
						}

						// delete download file on database
						for (ProductWorkspace oProductWorkspace : aoProductsWorkspaces) {
							try {

								WasdiLog.debugLog("WorkspaceResource.deleteWorkspace: Deleting file " + oProductWorkspace.getProductName());
								oDownloadedFilesRepository.deleteByFilePath(oProductWorkspace.getProductName());

							} catch (Exception oEx) {
								WasdiLog.errorLog("WorkspaceResource.deleteWorkspace: Error deleting download on data base: " + oEx);
							}
						}

					} catch (Exception oEx) {
						WasdiLog.errorLog("WorkspaceResource.deleteWorkspace: Error deleting workspace directory: " + oEx);
					}
				}
				else {
					WasdiLog.infoLog("WorkspaceResource.deleteWorkspace: delete files flag is False, we are not cleaning the Disk!");
				}

				// Delete Product Workspace entry
				oProductWorkspaceRepository.deleteByWorkspaceId(sWorkspaceId);

				// Delete also the sharings, it is deleted by the owner..
                UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
                oUserResourcePermissionRepository.deletePermissionsByWorkspaceId(sWorkspaceId);
                
                // Delete Process Workspaces
                
                // TODO: here is not correct... we should clean 
                //  -All the processor logs
                //	-All the parameters
                // Then we can clean the process workspaces
                
                ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
                oProcessWorkspaceRepository.deleteProcessWorkspaceByWorkspaceId(oWorkspace.getWorkspaceId());


				return Response.ok().build();
			} 
			else {
				WasdiLog.errorLog("WorkspaceResource.deleteWorkspace: Error deleting workspace on data base");
			}
				

		} catch (Exception oEx) {
			WasdiLog.errorLog("WorkspaceResource.deleteWorkspace: " + oEx);
		}

		return Response.serverError().build();
	}
	
	/**
	 * Share a workspace with another user.
	 *
	 * @param sSessionId User Session Id
	 * @param sWorkspaceId Workspace Id
	 * @param sDestinationUserId User id that will receive the workspace in sharing.
	 * @return Primitive Result with boolValue = true and stringValue = Done if ok. False and error description otherwise
	 */
	@PUT
	@Path("share/add")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult shareWorkspace(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("workspace") String sWorkspaceId, @QueryParam("userId") String sDestinationUserId, @QueryParam("rights") String sRights) {

		WasdiLog.debugLog("WorkspaceResource.ShareWorkspace( WS: " + sWorkspaceId + ", User: " + sDestinationUserId + " )");

		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);


		// Validate Session
		User oRequesterUser = Wasdi.getUserFromSession(sSessionId);
		
		if (oRequesterUser == null) {
			WasdiLog.warnLog("WorkspaceResource.shareWorkspace: invalid session");
			
			oResult.setIntValue(Status.UNAUTHORIZED.getStatusCode());
			oResult.setStringValue(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name());
			return oResult;
		}
		
		// Use Read By default
		if (!UserAccessRights.isValidAccessRight(sRights)) {
			sRights = UserAccessRights.READ.getAccessRight();
		}
		
		// Check if the workspace exists
		WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
		Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspaceId);
		
		if (oWorkspace == null) {
			WasdiLog.warnLog("WorkspaceResource.shareWorkspace: invalid workspace");
			
			oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
			oResult.setStringValue(ClientMessageCodes.MSG_ERROR_INVALID_WORKSPACE.name());
			return oResult;
		}

		// Can the user access this resource?
		if (!PermissionsUtils.canUserWriteWorkspace(oRequesterUser.getUserId(), sWorkspaceId)
				&& !UserApplicationRole.isAdmin(oRequesterUser)) {
			WasdiLog.warnLog("WorkspaceResource.shareWorkspace: " + sWorkspaceId + " cannot be accessed by " + oRequesterUser.getUserId() + ", aborting");

			oResult.setIntValue(Status.FORBIDDEN.getStatusCode());
			oResult.setStringValue(ClientMessageCodes.MSG_ERROR_NO_ACCESS_RIGHTS_OBJECT_WORKSPACE.name());
			return oResult;
		}

		// Cannot Autoshare
		if (oRequesterUser.getUserId().equals(sDestinationUserId) && !UserApplicationRole.isAdmin(oRequesterUser)) {
			WasdiLog.warnLog("WorkspaceResource.shareWorkspace: auto sharing not so smart");

			oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
			oResult.setStringValue(ClientMessageCodes.MSG_ERROR_SHARING_WITH_ONESELF.name());
			return oResult;
		}

		// Cannot share with the owner
		if (sDestinationUserId.equals(oWorkspace.getUserId())) {
			WasdiLog.warnLog("WorkspaceResource.shareWorkspace: sharing with the owner not so smart");

			oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
			oResult.setStringValue(ClientMessageCodes.MSG_ERROR_SHARING_WITH_OWNER.name());

			return oResult;
		}

		UserRepository oUserRepository = new UserRepository();
		User oDestinationUser = oUserRepository.getUser(sDestinationUserId);

		if (oDestinationUser == null) {
			//No. So it is neither the owner or a shared one
			WasdiLog.warnLog("WorkspaceResource.shareWorkspace: Destination user does not exists");

			oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
			oResult.setStringValue(ClientMessageCodes.MSG_ERROR_SHARING_WITH_NON_EXISTENT_USER.name());

			return oResult;
		}

		try {
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();

			if (!oUserResourcePermissionRepository.isWorkspaceSharedWithUser(sDestinationUserId, sWorkspaceId)) {
				UserResourcePermission oWorkspaceSharing =
						new UserResourcePermission(ResourceTypes.WORKSPACE.getResourceType(), sWorkspaceId, sDestinationUserId, oWorkspace.getUserId(), oRequesterUser.getUserId(), sRights);

				oUserResourcePermissionRepository.insertPermission(oWorkspaceSharing);				
			} else {
				WasdiLog.debugLog("WorkspaceResource.shareWorkspace: already shared!");
				oResult.setStringValue("Already Shared.");
				oResult.setBoolValue(true);
				oResult.setIntValue(Status.OK.getStatusCode());

				return oResult;
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("WorkspaceResource.shareWorkspace error: " + oEx);

			oResult.setIntValue(Status.INTERNAL_SERVER_ERROR.getStatusCode());
			oResult.setStringValue(ClientMessageCodes.MSG_ERROR_IN_INSERT_PROCESS.name());

			return oResult;
		}

		try {
			String sTitle = "Workspace " + oWorkspace.getName() + " Shared";
			String sMessage = "The user " + oRequesterUser.getUserId() +  " shared with you the workspace: " + oWorkspace.getName();
			MailUtils.sendEmail(WasdiConfig.Current.notifications.sftpManagementMailSender, sDestinationUserId, sTitle, sMessage);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("WorkspaceResource.shareWorkspace: notification exception " + oEx.toString());
		}
		
		oResult.setStringValue("Done");
		oResult.setBoolValue(true);

		return oResult;
	}

	/**
	 * Get the list of users that has a Workspace in sharing.
	 *
	 * @param sSessionId User Session
	 * @param sWorkspaceId Workspace Id
	 * @return list of Workspace Sharing View Models
	 */
	@GET
	@Path("share/byworkspace")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public List<WorkspaceSharingViewModel> getEnabledUsersSharedWorksace(@HeaderParam("x-session-token") String sSessionId, @QueryParam("workspace") String sWorkspaceId) {

		WasdiLog.debugLog("WorkspaceResource.getEnabledUsersSharedWorksace( WS: " + sWorkspaceId + " )");
	
		List<UserResourcePermission> aoWorkspaceSharing = null;
		List<WorkspaceSharingViewModel> aoWorkspaceSharingViewModels = new ArrayList<WorkspaceSharingViewModel>();

		User oOwnerUser = Wasdi.getUserFromSession(sSessionId);
		if (oOwnerUser == null) {
			WasdiLog.warnLog("WorkspaceResource.getEnabledUsersSharedWorksace: invalid session");
			return aoWorkspaceSharingViewModels;
		}
		
		if (!PermissionsUtils.canUserAccessWorkspace(oOwnerUser.getUserId(), sWorkspaceId)) {
			WasdiLog.warnLog("WorkspaceResource.getEnabledUsersSharedWorksace: user cannot access the workspace");
			return aoWorkspaceSharingViewModels;			
		}

		try {
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			aoWorkspaceSharing = oUserResourcePermissionRepository.getWorkspaceSharingsByWorkspaceId(sWorkspaceId);

			if (aoWorkspaceSharing != null) {
				for (UserResourcePermission oWorkspaceSharing : aoWorkspaceSharing) {
					WorkspaceSharingViewModel oWorkspaceSharingViewModel = new WorkspaceSharingViewModel();
					oWorkspaceSharingViewModel.setOwnerId(oWorkspaceSharing.getUserId());
					oWorkspaceSharingViewModel.setUserId(oWorkspaceSharing.getUserId());
					oWorkspaceSharingViewModel.setWorkspaceId(oWorkspaceSharing.getResourceId());
					oWorkspaceSharingViewModel.setPermissions(oWorkspaceSharing.getPermissions());

					aoWorkspaceSharingViewModels.add(oWorkspaceSharingViewModel);
				}

			}

		} catch (Exception oEx) {
			WasdiLog.errorLog("WorkspaceResource.getEnableUsersSharedWorksace error: " + oEx);
			return aoWorkspaceSharingViewModels;
		}

		return aoWorkspaceSharingViewModels;

	}

	@DELETE
	@Path("share/delete")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult deleteUserSharedWorkspace(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("workspace") String sWorkspaceId, @QueryParam("userId") String sUserId) {

		WasdiLog.debugLog("WorkspaceResource.deleteUserSharedWorkspace( WS: " + sWorkspaceId + ", User:" + sUserId + " )");
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);
		
		// Validate Session
		User oRequestingUser = Wasdi.getUserFromSession(sSessionId);

		if (oRequestingUser == null) {
			WasdiLog.warnLog("WorkspaceResource.deleteUserSharedWorkspace: invalid session");
			oResult.setIntValue(Status.UNAUTHORIZED.getStatusCode());
			oResult.setStringValue(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name());
			return oResult;
		}
		
		
		if (!PermissionsUtils.canUserAccessWorkspace(oRequestingUser.getUserId(), sWorkspaceId)
				&& !UserApplicationRole.isAdmin(oRequestingUser)) {
			WasdiLog.warnLog("WorkspaceResource.deleteUserSharedWorkspace: user cannot access workspace");
			oResult.setIntValue(Status.FORBIDDEN.getStatusCode());
			oResult.setStringValue(ClientMessageCodes.MSG_ERROR_NO_ACCESS_RIGHTS_APPLICATION_RESOURCE_WORKSPACE.name());
			return oResult;
		}
		

		try {

			UserRepository oUserRepository = new UserRepository();
			User oDestinationUser = oUserRepository.getUser(sUserId);

			if (oDestinationUser == null) {
				WasdiLog.warnLog("WorkspaceResource.deleteUserSharedWorkspace: invalid destination user");

				oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
				oResult.setStringValue(ClientMessageCodes.MSG_ERROR_INVALID_DESTINATION_USER.name());

				return oResult;
			}
			
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspaceId);
			
			if (oWorkspace == null) {
				WasdiLog.warnLog("WorkspaceResource.deleteUserSharedWorkspace: invalid workspace");

				oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
				oResult.setStringValue(ClientMessageCodes.MSG_ERROR_INVALID_WORKSPACE.name());

				return oResult;				
			}
			
			
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			
			UserResourcePermission oSharing = oUserResourcePermissionRepository.getWorkspaceSharingByUserIdAndWorkspaceId(sUserId, sWorkspaceId);
			
			if (oSharing == null) {
				WasdiLog.warnLog("WorkspaceResource.deleteUserSharedWorkspace: sharing not existing");
				oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
				oResult.setStringValue(ClientMessageCodes.MSG_ERROR_INVALID_DESTINATION_USER.name());
				return oResult;								
			}
			
			if (oSharing.getUserId().equals(sUserId) 
					|| PermissionsUtils.canUserWriteWorkspace(oRequestingUser.getUserId(), sWorkspaceId)
					|| UserApplicationRole.isAdmin(oRequestingUser)) {
				oUserResourcePermissionRepository.deletePermissionsByUserIdAndWorkspaceId(sUserId, sWorkspaceId);
				
				oResult.setStringValue("Done");
				oResult.setBoolValue(true);
				oResult.setIntValue(Status.OK.getStatusCode());

				return oResult;							
			}
			else {
				WasdiLog.warnLog("WorkspaceResource.deleteUserSharedWorkspace: user cannot access workspace");
				oResult.setIntValue(Status.FORBIDDEN.getStatusCode());
				oResult.setStringValue(ClientMessageCodes.MSG_ERROR_NO_ACCESS_RIGHTS_APPLICATION_RESOURCE_WORKSPACE.name());
				return oResult;				
			}
			
		} 
		catch (Exception oEx) {
			WasdiLog.errorLog("WorkspaceResource.deleteUserSharedWorkspace: error " + oEx);

			oResult.setIntValue(Status.INTERNAL_SERVER_ERROR.getStatusCode());
			oResult.setStringValue(ClientMessageCodes.MSG_ERROR_IN_DELETE_PROCESS.name());

			return oResult;
		}
	}
	
	/**
	 * Get a Workspace Name from Workspace Id
	 * @param sSessionId User Session
	 * @param sWorkspaceId Workspace Id
	 * @return http std response: if it is 200, a string with the name of the workspace
	 */
	@GET
	@Path("wsnamebyid")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response getWorkspaceNameById(@HeaderParam("x-session-token") String sSessionId, @QueryParam("workspace") String sWorkspaceId ) {

		if (Utils.isNullOrEmpty(sWorkspaceId)) {
			WasdiLog.warnLog("WorkspaceResource.getWorkspaceNameById: workspace is null or empty, aborting");
			return Response.status(Status.BAD_REQUEST).entity("workspaceId is null or empty").build();
		}

		User oUser = Wasdi.getUserFromSession(sSessionId);
		if (null == oUser) {
			WasdiLog.warnLog("WorkspaceResource.getWorkspaceNameById: session is invalid, aborting");
			return Response.status(Status.UNAUTHORIZED).entity("session invalid").build();
		}
		
		//check the user can access the workspace
		if(!PermissionsUtils.canUserAccessWorkspace(oUser.getUserId(), sWorkspaceId)) {
			WasdiLog.warnLog("WorkspaceResource.getWorkspaceNameById: user cannot access workspace info, aborting");
			return Response.status(Status.FORBIDDEN).build();
		}
		
		try {
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();

			Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspaceId);
			if (oWorkspace != null) {
				String sName = oWorkspace.getName();

				if (!Utils.isNullOrEmpty(sName)) {
					return Response.ok(sName, MediaType.TEXT_PLAIN).build();
				}
			}

			return Response.status(Status.BAD_REQUEST).build();
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("WorkspaceResource.getWorkspaceNameById error: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	/**
	 * Terminates the notebook that may be present in the workspace
	 * @param oWorkspace
	 * @return
	 */
	protected boolean terminateConsole(Workspace oWorkspace, String sSessionId) {
		try {
			String sJupyterNotebookCode = Utils.generateJupyterNotebookCode(oWorkspace.getUserId(), oWorkspace.getWorkspaceId());

			JupyterNotebookRepository oJupyterNotebookRepository = new JupyterNotebookRepository();
			JupyterNotebook oJupyterNotebook = oJupyterNotebookRepository.getJupyterNotebookByCode(sJupyterNotebookCode);

			if (oJupyterNotebook != null) {
				WasdiLog.debugLog("WorkspaceResource.terminateConsole: found JupyterNotebook record: " + sJupyterNotebookCode + ". Trying to delete it!");
				oJupyterNotebookRepository.deleteJupyterNotebook(sJupyterNotebookCode);
			} 
			else {
				WasdiLog.debugLog("WorkspaceResource.terminateConsole: no notebook in the workspace");
				return true;
			}


			// Schedule the process to run the processor
			String sProcessObjId = Utils.getRandomName();

			WasdiLog.debugLog("WorkspaceResource.terminateConsole: create local operation " + sProcessObjId);

			ProcessorParameter oProcessorParameter = new ProcessorParameter();
			oProcessorParameter.setName("jupyter-notebook");
			oProcessorParameter.setProcessorType(ProcessorTypes.JUPYTER_NOTEBOOK);
			oProcessorParameter.setWorkspace(oWorkspace.getWorkspaceId());
			oProcessorParameter.setUserId(oWorkspace.getUserId());
			oProcessorParameter.setExchange(oWorkspace.getWorkspaceId());
			oProcessorParameter.setProcessObjId(sProcessObjId);
			oProcessorParameter.setSessionID(sSessionId);
			oProcessorParameter.setWorkspaceOwnerId(Wasdi.getWorkspaceOwner(oWorkspace.getWorkspaceId()));

			PrimitiveResult oRes = Wasdi.runProcess(oWorkspace.getUserId(), sSessionId, LauncherOperations.TERMINATEJUPYTERNOTEBOOK.name(), sJupyterNotebookCode, oProcessorParameter);

			if (oRes.getBoolValue()) {
				return true;
			} 
			else {
				return false;
			}
			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("WorkspaceResource.terminateConsole: Exception " + oEx.toString());
		}
		
		return false;
	}
	
}
