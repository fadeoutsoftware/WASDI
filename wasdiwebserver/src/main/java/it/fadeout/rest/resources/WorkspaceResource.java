package it.fadeout.rest.resources;

import static wasdi.shared.business.UserApplicationPermission.ADMIN_DASHBOARD;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import it.fadeout.mercurius.business.Message;
import it.fadeout.mercurius.client.MercuriusAPI;
import it.fadeout.services.ProcessWorkspaceService;
import wasdi.shared.LauncherOperations;
import wasdi.shared.business.CloudProvider;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.JupyterNotebook;
import wasdi.shared.business.Node;
import wasdi.shared.business.ProcessorTypes;
import wasdi.shared.business.ProductWorkspace;
import wasdi.shared.business.PublishedBand;
import wasdi.shared.business.User;
import wasdi.shared.business.UserApplicationRole;
import wasdi.shared.business.UserResourcePermission;
import wasdi.shared.business.Workspace;
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
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
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

	private static final String MSG_ERROR_INVALID_SESSION = "MSG_ERROR_INVALID_SESSION";

	private static final String MSG_ERROR_NO_ACCESS_RIGHTS_APPLICATION_RESOURCE_WORKSPACE = "MSG_ERROR_NO_ACCESS_RIGHTS_APPLICATION_RESOURCE_WORKSPACE";
	private static final String MSG_ERROR_NO_ACCESS_RIGHTS_OBJECT_WORKSPACE = "MSG_ERROR_NO_ACCESS_RIGHTS_OBJECT_WORKSPACE";
	private static final String MSG_ERROR_ACTIVE_PROJECT_REQUIRED = "MSG_ERROR_ACTIVE_PROJECT_REQUIRED";

	private static final String MSG_ERROR_SHARING_WITH_OWNER = "MSG_ERROR_SHARING_WITH_OWNER";
	private static final String MSG_ERROR_SHARING_WITH_ONESELF = "MSG_ERROR_SHARING_WITH_ONESELF";
	private static final String MSG_ERROR_SHARING_WITH_NON_EXISTENT_USER = "MSG_ERROR_SHARING_WITH_NON_EXISTENT_USER";

	private static final String MSG_ERROR_INVALID_WORKSPACE = "MSG_ERROR_INVALID_WORKSPACE";
	private static final String MSG_ERROR_INVALID_DESTINATION_USER = "MSG_ERROR_INVALID_DESTINATION_USER";
	private static final String MSG_ERROR_IN_DELETE_PROCESS = "MSG_ERROR_IN_DELETE_PROCESS";
	private static final String MSG_ERROR_IN_INSERT_PROCESS = "MSG_ERROR_IN_INSERT_PROCESS";


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
		
		List<WorkspaceListInfoViewModel> aoWSList = new ArrayList<>();
		
		// Domain Check
		if (oUser == null) {
			WasdiLog.debugLog("WorkspaceResource.getListByUser: invalid session");
			return aoWSList;
		}

		try {

			WasdiLog.debugLog("WorkspaceResource.getListByUser: workspaces for " + oUser.getUserId());

			NodeRepository oNodeRepository = new NodeRepository();
			List<Node> aoNodeList = oNodeRepository.getNodesList();

			Map<String, Node> aoNodeMap = aoNodeList.stream()
					.collect(Collectors.toMap(Node::getNodeCode, Function.identity()));

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
                List<UserResourcePermission> aoPermissions = oUserResourcePermissionRepository
                		.getWorkspaceSharingsByWorkspaceId(oWorkspace.getWorkspaceId());

				// Add Sharings to View Model
				if (aoPermissions != null) {
					for (int i = 0; i < aoPermissions.size(); i++) {
						if (oWSViewModel.getSharedUsers() == null) {
							oWSViewModel.setSharedUsers(new ArrayList<String>());
						}

						oWSViewModel.getSharedUsers().add(aoPermissions.get(i).getUserId());
					}
				}

				aoWSList.add(oWSViewModel);

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

					aoWSList.add(oWSViewModel);

				}
			}

		} catch (Exception oEx) {
			WasdiLog.errorLog("WorkspaceResource.getListByUser error: " + oEx);
		}

		return aoWSList;
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
	public WorkspaceEditorViewModel getWorkspaceEditorViewModel(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("workspace") String sWorkspaceId) {

		WasdiLog.debugLog("WorkspaceResource.GetWorkspaceEditorViewModel( WS: " + sWorkspaceId + ")");

		WorkspaceEditorViewModel oVM = new WorkspaceEditorViewModel();

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.debugLog("WorkspaceResource.getWorkspaceEditorViewModel: invalid session");
			return null;
		}
		
		try {
			// Domain Check
			if (Utils.isNullOrEmpty(sWorkspaceId)) {
				WasdiLog.debugLog("WorkspaceResource.getWorkspaceEditorViewModel: invalid workspace id");
				return oVM;
			}

			if(!PermissionsUtils.canUserAccessWorkspace(oUser.getUserId(), sWorkspaceId)) {
				WasdiLog.debugLog("WorkspaceResource.getWorkspaceEditorViewModel: user cannot access workspace info, aborting");
				return oVM;
			}

			// Create repo
			WorkspaceRepository oWSRepository = new WorkspaceRepository();
			
            UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			
			// Get requested workspace
			Workspace oWorkspace = oWSRepository.getWorkspace(sWorkspaceId);
			oVM.setUserId(oWorkspace.getUserId());
			oVM.setWorkspaceId(oWorkspace.getWorkspaceId());
			oVM.setName(oWorkspace.getName());
			oVM.setCreationDate(Utils.getDate(oWorkspace.getCreationDate()));
			oVM.setLastEditDate(Utils.getDate(oWorkspace.getLastEditDate()));
			oVM.setNodeCode(oWorkspace.getNodeCode());
			
			ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
			oVM.setProcessesCount(oProcessWorkspaceRepository.countByWorkspace(sWorkspaceId)); 
			// If the workspace is on another node, copy the url to the view model
			if (oWorkspace.getNodeCode().equals("wasdi") == false) {
				// Get the Node
				NodeRepository oNodeRepository = new NodeRepository();
				String sNodeCode = oWorkspace.getNodeCode(); 
				Node oWorkspaceNode = oNodeRepository.getNodeByCode(sNodeCode);
				if (oWorkspaceNode != null) {
					if (oWorkspaceNode.getActive()) {
						oVM.setApiUrl(oWorkspaceNode.getNodeBaseAddress());
					} else {
						oVM.setApiUrl(WasdiConfig.Current.baseUrl);
					}

					oVM.setActiveNode(oWorkspaceNode.getActive());
										
					if (!Utils.isNullOrEmpty(oWorkspaceNode.getCloudProvider())) {
						oVM.setCloudProvider(oWorkspaceNode.getCloudProvider());
						
						CloudProviderRepository oCloudProviderRepository = new CloudProviderRepository();
						CloudProvider oCloudProvider = oCloudProviderRepository.getCloudProviderByCode(oWorkspaceNode.getCloudProvider());
						
						if (oCloudProvider != null) {
							oVM.setSlaLink(oCloudProvider.getSlaLink());
						}
						
					}
					else {
						oVM.setCloudProvider(oWorkspaceNode.getNodeCode());
					}
				}
			
			}
			else {
				oVM.setCloudProvider("wasdi");

				oVM.setActiveNode(true);
			}

			// Get Sharings
			List<UserResourcePermission> aoSharings = oUserResourcePermissionRepository
					.getWorkspaceSharingsByWorkspaceId(oWorkspace.getWorkspaceId());
			// Add Sharings to View Model
			if (aoSharings != null) {
				if (oVM.getSharedUsers() == null) {
					oVM.setSharedUsers(new ArrayList<String>());
				}

				for (UserResourcePermission oSharing : aoSharings) {
					oVM.getSharedUsers().add(oSharing.getUserId());
				}
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog( "WorkspaceResource.getWorkspaceEditorViewModel error: " + oEx);
		}

		return oVM;
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
			WasdiLog.debugLog("WorkspaceResource.createWorkspace: invalid session");
			return null;
		}

		// Create New Workspace
		Workspace oWorkspace = new Workspace();

		if (Utils.isNullOrEmpty(sName)) {
			WasdiLog.debugLog("WorkspaceResource.createWorkspace: name is null or empty, defaulting");
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
	public WorkspaceEditorViewModel updateWorkspace(@HeaderParam("x-session-token") String sSessionId,
			WorkspaceEditorViewModel oWorkspaceEditorViewModel) {

		WasdiLog.debugLog("WorkspaceResource.updateWorkspace");

		// Validate Session
		User oUser = Wasdi.getUserFromSession(sSessionId);
		
		if (oUser == null) {
			WasdiLog.debugLog("WorkspaceResource.updateWorkspace: invalid session");
			return null;
		}
		
		// Validate input view model
		if (oWorkspaceEditorViewModel==null) {
			WasdiLog.debugLog("WorkspaceResource.updateWorkspace: invalid Workspace View Model");
			return null;			
		}
		
		// Check if the user can access
		if (!PermissionsUtils.canUserAccessWorkspace(oUser.getUserId(), oWorkspaceEditorViewModel.getWorkspaceId())) {
			WasdiLog.debugLog("WorkspaceResource.updateWorkspace: user cannot access the workspace");
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

			
			// if present and different from "wasdi", the node code must be updated
			if (oWorkspaceEditorViewModel.getNodeCode() != null &&
					!(oWorkspaceEditorViewModel.getNodeCode().equals("wasdi"))) {
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
				WasdiLog.debugLog("WorkspaceResource.deleteWorkspace: invalid session");
				return Response.status(Status.UNAUTHORIZED).build();
			}
			
			// before any operation check that this is not an injection attempt from the user
			if ( sWorkspaceId.contains("/") || sWorkspaceId.contains("\\") || sWorkspaceId.contains(File.separator)) {
				WasdiLog.debugLog("WorkspaceResource.deleteWorkspace: Injection attempt by user");
				return Response.status(Status.BAD_REQUEST).build();
			}

			//check user can access given workspace
			if(!PermissionsUtils.canUserAccessWorkspace(oUser.getUserId(), sWorkspaceId)) {
				WasdiLog.debugLog("WorkspaceResource.deleteWorkspace: user cannot access workspace aborting");
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
				WasdiLog.debugLog("WorkspaceResource.deleteWorkspace: not a valid workspace, aborting");
				return Response.status(Status.BAD_REQUEST).build();
			}

			//delete sharing if the user is not the owner
			String sWorkspaceOwner = Wasdi.getWorkspaceOwner(sWorkspaceId);
			if (!sWorkspaceOwner.equals(oUser.getUserId())) {
				// This is not the owner of the workspace
				WasdiLog.debugLog("WorkspaceResource.deleteWorkspace: User " + oUser.getUserId() + " is not the owner [" + sWorkspaceOwner + "]: delete the sharing, not the ws");
                UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
                oUserResourcePermissionRepository.deletePermissionsByUserIdAndWorkspaceId(oUser.getUserId(), sWorkspaceId);
				return Response.ok().build();
			}
			
			// Terminate and clean the console/Jupyter Lab instance if present
			terminateConsole(oWorkspace, sSessionId);
			
			//kill active processes
			ProcessWorkspaceService oProcessWorkspaceService = new ProcessWorkspaceService();
			if(oProcessWorkspaceService.killProcessesInWorkspace(sWorkspaceId, sSessionId, true)) {
				WasdiLog.debugLog("WorkspaceResource.deleteWorkspace: WARNING: could not schedule kill processes in workspace");
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
										WasdiLog.debugLog("WorkspaceResource.deleteWorkspace: error deleting layer " + oPublishedBand.getLayerId() + " from geoserver");
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
								WasdiLog.debugLog("WorkspaceResource.deleteWorkspace: trying to delete non existing directory " + sWorkspacePath);
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

				// Delete Product Workspace entry
				oProductWorkspaceRepository.deleteByWorkspaceId(sWorkspaceId);

				// Delete also the sharings, it is deleted by the owner..
                UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
                oUserResourcePermissionRepository.deletePermissionsByWorkspaceId(sWorkspaceId);


				return Response.ok().build();
			} 
			else {
				WasdiLog.debugLog("WorkspaceResource.deleteWorkspace: Error deleting workspace on data base");
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
			@QueryParam("workspace") String sWorkspaceId, @QueryParam("userId") String sDestinationUserId) {

		WasdiLog.debugLog("WorkspaceResource.ShareWorkspace( WS: " + sWorkspaceId + ", User: " + sDestinationUserId + " )");

		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);


		// Validate Session
		User oRequesterUser = Wasdi.getUserFromSession(sSessionId);
		
		if (oRequesterUser == null) {
			WasdiLog.debugLog("WorkspaceResource.shareWorkspace: invalid session");
			
			oResult.setIntValue(Status.UNAUTHORIZED.getStatusCode());
			oResult.setStringValue(MSG_ERROR_INVALID_SESSION);
			return oResult;
		}
		
		// Check if the workspace exists
		WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
		Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspaceId);
		
		if (oWorkspace == null) {
			WasdiLog.debugLog("WorkspaceResource.shareWorkspace: invalid workspace");
			
			oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
			oResult.setStringValue(MSG_ERROR_INVALID_WORKSPACE);
			return oResult;
		}

		// Can the user access this resource?
		if (!PermissionsUtils.canUserAccessWorkspace(oRequesterUser.getUserId(), sWorkspaceId)
				&& !UserApplicationRole.userHasRightsToAccessApplicationResource(oRequesterUser.getRole(), ADMIN_DASHBOARD)) {
			WasdiLog.debugLog("WorkspaceResource.shareWorkspace: " + sWorkspaceId + " cannot be accessed by " + oRequesterUser.getUserId() + ", aborting");

			oResult.setIntValue(Status.FORBIDDEN.getStatusCode());
			oResult.setStringValue(MSG_ERROR_NO_ACCESS_RIGHTS_OBJECT_WORKSPACE);
			return oResult;
		}

		// Cannot Autoshare
		if (oRequesterUser.getUserId().equals(sDestinationUserId)) {
			WasdiLog.debugLog("WorkspaceResource.shareWorkspace: auto sharing not so smart");

			oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
			oResult.setStringValue(MSG_ERROR_SHARING_WITH_ONESELF);
			return oResult;
		}

		// Cannot share with the owner
		if (sDestinationUserId.equals(oWorkspace.getUserId())) {
			WasdiLog.debugLog("WorkspaceResource.shareWorkspace: sharing with the owner not so smart");

			oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
			oResult.setStringValue(MSG_ERROR_SHARING_WITH_OWNER);

			return oResult;
		}

		UserRepository oUserRepository = new UserRepository();
		User oDestinationUser = oUserRepository.getUser(sDestinationUserId);

		if (oDestinationUser == null) {
			//No. So it is neither the owner or a shared one
			WasdiLog.debugLog("WorkspaceResource.shareWorkspace: Destination user does not exists");

			oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
			oResult.setStringValue(MSG_ERROR_SHARING_WITH_NON_EXISTENT_USER);

			return oResult;
		}

		try {
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();

			if (!oUserResourcePermissionRepository.isWorkspaceSharedWithUser(sDestinationUserId, sWorkspaceId)) {
				UserResourcePermission oWorkspaceSharing =
						new UserResourcePermission("workspace", sWorkspaceId, sDestinationUserId, oWorkspace.getUserId(), oRequesterUser.getUserId(), "write");

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
			oResult.setStringValue(MSG_ERROR_IN_INSERT_PROCESS);

			return oResult;
		}

		sendNotificationEmail(oRequesterUser.getUserId(), sDestinationUserId, oWorkspace.getName());

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
			WasdiLog.debugLog("WorkspaceResource.getEnabledUsersSharedWorksace: invalid session");
			return aoWorkspaceSharingViewModels;
		}
		
		if (!PermissionsUtils.canUserAccessWorkspace(oOwnerUser.getUserId(), sWorkspaceId)) {
			WasdiLog.debugLog("WorkspaceResource.getEnabledUsersSharedWorksace: user cannot access the workspace");
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
			WasdiLog.debugLog("WorkspaceResource.deleteUserSharedWorkspace: invalid session");
			oResult.setIntValue(Status.UNAUTHORIZED.getStatusCode());
			oResult.setStringValue(MSG_ERROR_INVALID_SESSION);
			return oResult;
		}
		
		
		if (!PermissionsUtils.canUserAccessWorkspace(oRequestingUser.getUserId(), sWorkspaceId)
				&& !UserApplicationRole.userHasRightsToAccessApplicationResource(oRequestingUser.getRole(), ADMIN_DASHBOARD)) {
			WasdiLog.debugLog("WorkspaceResource.deleteUserSharedWorkspace: user cannot access workspace");
			oResult.setIntValue(Status.FORBIDDEN.getStatusCode());
			oResult.setStringValue(MSG_ERROR_NO_ACCESS_RIGHTS_APPLICATION_RESOURCE_WORKSPACE);
			return oResult;
		}
		

		try {

			UserRepository oUserRepository = new UserRepository();
			User oDestinationUser = oUserRepository.getUser(sUserId);

			if (oDestinationUser == null) {
				WasdiLog.debugLog("WorkspaceResource.deleteUserSharedWorkspace: invalid destination user");

				oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
				oResult.setStringValue(MSG_ERROR_INVALID_DESTINATION_USER);

				return oResult;
			}

			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			oUserResourcePermissionRepository.deletePermissionsByUserIdAndWorkspaceId(sUserId, sWorkspaceId);
			
			oResult.setStringValue("Done");
			oResult.setBoolValue(true);
			oResult.setIntValue(Status.OK.getStatusCode());

			return oResult;			
		} 
		catch (Exception oEx) {
			WasdiLog.errorLog("WorkspaceResource.deleteUserSharedWorkspace: error " + oEx);

			oResult.setIntValue(Status.INTERNAL_SERVER_ERROR.getStatusCode());
			oResult.setStringValue(MSG_ERROR_IN_DELETE_PROCESS);

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
			WasdiLog.debugLog("WorkspaceResource.getWorkspaceNameById: workspace is null or empty, aborting");
			return Response.status(Status.BAD_REQUEST).entity("workspaceId is null or empty").build();
		}

		User oUser = Wasdi.getUserFromSession(sSessionId);
		if (null == oUser) {
			WasdiLog.debugLog("WorkspaceResource.getWorkspaceNameById: session is invalid, aborting");
			return Response.status(Status.UNAUTHORIZED).entity("session invalid").build();
		}
		
		//check the user can access the workspace
		if(!PermissionsUtils.canUserAccessWorkspace(oUser.getUserId(), sWorkspaceId)) {
			WasdiLog.debugLog("WorkspaceResource.getWorkspaceNameById: user cannot access workspace info, aborting");
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

			String sPath = WasdiConfig.Current.paths.serializationPath;

			PrimitiveResult oRes = Wasdi.runProcess(oWorkspace.getUserId(), sSessionId, LauncherOperations.TERMINATEJUPYTERNOTEBOOK.name(), sJupyterNotebookCode, sPath, oProcessorParameter);

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
	
	private static void sendNotificationEmail(String sRequesterUserId, String sDestinationUserId, String sWorkspaceName) {
		try {
			String sMercuriusAPIAddress = WasdiConfig.Current.notifications.mercuriusAPIAddress;

			if(Utils.isNullOrEmpty(sMercuriusAPIAddress)) {
				WasdiLog.debugLog("WorkspaceResource.sendNotificationEmail: sMercuriusAPIAddress is null");
			}
			else {

				WasdiLog.debugLog("WorkspaceResource.sendNotificationEmail: send notification");

				MercuriusAPI oAPI = new MercuriusAPI(sMercuriusAPIAddress);			
				Message oMessage = new Message();

				String sTitle = "Workspace " + sWorkspaceName + " Shared";

				oMessage.setTilte(sTitle);

				String sSender = WasdiConfig.Current.notifications.sftpManagementMailSender;
				if (sSender==null) {
					sSender = "wasdi@wasdi.net";
				}

				oMessage.setSender(sSender);

				String sMessage = "The user " + sRequesterUserId +  " shared with you the workspace: " + sWorkspaceName;

				oMessage.setMessage(sMessage);

				Integer iPositiveSucceded = 0;

				iPositiveSucceded = oAPI.sendMailDirect(sDestinationUserId, oMessage);
				
				WasdiLog.debugLog("WorkspaceResource.sendNotificationEmail: notification sent with result " + iPositiveSucceded);
			}

		}
		catch (Exception oEx) {
			WasdiLog.errorLog("WorkspaceResource.sendNotificationEmail: notification exception " + oEx.toString());
		}
	}
}
