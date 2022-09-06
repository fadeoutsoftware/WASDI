package it.fadeout.rest.resources;

import static wasdi.shared.business.UserApplicationPermission.*;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
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
import wasdi.shared.business.CloudProvider;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.Node;
import wasdi.shared.business.ProductWorkspace;
import wasdi.shared.business.PublishedBand;
import wasdi.shared.business.User;
import wasdi.shared.business.UserApplicationRole;
import wasdi.shared.business.UserResourcePermission;
import wasdi.shared.business.Workspace;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.CloudProviderRepository;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.NodeRepository;
import wasdi.shared.data.ProcessWorkspaceRepository;
import wasdi.shared.data.ProductWorkspaceRepository;
import wasdi.shared.data.PublishedBandsRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.data.UserResourcePermissionRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.geoserver.GeoServerManager;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.Utils;
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

	@GET
	@Path("/workspacelistbyproductname")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response getWorkspaceListByProductName(
			@HeaderParam("x-session-token") String sSessionId, @QueryParam("productname") String sProductName) {
		Utils.debugLog("WorkspaceResource.getWorkspaceListByProductName( Product: " + sProductName + " )");

		// input validation
		if (Utils.isNullOrEmpty(sProductName)) {
			Utils.debugLog("WorkspaceResource.getWorkspaceListByProductName: sProductName null or empty");
			return Response.status(Status.BAD_REQUEST).entity("sProductName null or empty").build();
		}
		
		User oUser = Wasdi.getUserFromSession(sSessionId);
		
		if (oUser == null) {
			Utils.debugLog("WorkspaceResource.getWorkspaceListByProductName( Product: " + sProductName + " ): invalid session");
			return Response.status(Status.UNAUTHORIZED).entity("session invalid").build();
		}
		
		// get list of workspaces ID by product name
		ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
		List<String> asWorkspaces = oProductWorkspaceRepository.getWorkspaces(sProductName);

		if (asWorkspaces == null) {
			Utils.debugLog("WorkspaceResource.getWorkspaceListByProductName: Workspaces list is null");
			return Response.status(Status.BAD_REQUEST).entity("Workspaces list is null").build();
		}

		WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
		List<WorkspaceListInfoViewModel> aoViewModel = new ArrayList<>();

		// get workspace info for each workspace ID
		for (String sWorkspaceID : asWorkspaces) {

			Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspaceID);

			if (null != oWorkspace) {
				WorkspaceListInfoViewModel oTemp = new WorkspaceListInfoViewModel();
				oTemp.setWorkspaceId(oWorkspace.getWorkspaceId());
				oTemp.setWorkspaceName(oWorkspace.getName());
				oTemp.setOwnerUserId(oWorkspace.getUserId());
				aoViewModel.add(oTemp);
			}
		}

		return Response.ok(aoViewModel).build();
	}

	/**
	 * Get a list of workspaces of a user
	 * @param sSessionId User Session Id
	 * @return List of Workspace List Info View Model
	 */
	@GET
	@Path("/byuser")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response getListByUser(@HeaderParam("x-session-token") String sSessionId) {

		Utils.debugLog("WorkspaceResource.GetListByUser()");

		User oUser = Wasdi.getUserFromSession(sSessionId);
		
		List<WorkspaceListInfoViewModel> aoWSList = new ArrayList<>();
		
		// Domain Check
		if (oUser == null) {
			Utils.debugLog("WorkspaceResource.GetListByUser: invalid session: " + sSessionId);
			return Response.status(Status.UNAUTHORIZED).entity("session invalid").build();
		}

		try {

			if (!UserApplicationRole.userHasRightsToAccessResource(oUser.getRole(), WORKSPACE_READ)) {
				return Response.status(Status.FORBIDDEN).entity("user cannot access workspace section").build();
			}


			Utils.debugLog("WorkspaceResource.GetListByUser: workspaces for " + oUser.getUserId());

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
						Utils.debugLog("WorkspaceResult.getListByUser: WS Shared not available " + aoSharedWorkspaces.get(iWorkspaces).getResourceId());
						continue;
					}

					oWSViewModel.setOwnerUserId(oWorkspace.getUserId());
					oWSViewModel.setWorkspaceId(oWorkspace.getWorkspaceId());
					oWSViewModel.setWorkspaceName(oWorkspace.getName());
					oWSViewModel.setNodeCode(oWorkspace.getNodeCode());

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
			oEx.toString();
		}

		return Response.ok(aoWSList).build();
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
	public Response getWorkspaceEditorViewModel(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("workspace") String sWorkspaceId) {

		Utils.debugLog("WorkspaceResource.GetWorkspaceEditorViewModel( WS: " + sWorkspaceId + ")");

		WorkspaceEditorViewModel oVM = new WorkspaceEditorViewModel();

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			Utils.debugLog("WorkspaceResource.GetWorkspaceEditorViewModel: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity("session invalid").build();
		}
		
		try {
			// Domain Check
			if (Utils.isNullOrEmpty(sWorkspaceId)) {
				return Response.status(Status.BAD_REQUEST).entity("Invalid workspace: " + sWorkspaceId).build();
			}

			if(!PermissionsUtils.canUserAccessWorkspace(oUser.getUserId(), sWorkspaceId)) {
				Utils.debugLog("WorkspaceResource.GetWorkspaceEditorViewModel: user cannot access workspace info, aborting");
				return Response.status(Status.FORBIDDEN).entity("user cannot access workspace info, aborting").build();
			}

			Utils.debugLog("WorkspaceResource.GetWorkspaceEditorViewModel: read workspaces " + sWorkspaceId);

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
			Utils.debugLog( "WorkspaceResource.getWorkspaceEditorViewModel: " + oEx);
		}
		return Response.ok(oVM).build();
	}

	/**
	 * Create a new workspace
	 * @param sSessionId User Session Id
	 * @param sName Workspace Name
	 * @param sNodeCode Code of the Node where the workspace must be created
	 * @return Response with stringValue = workspaceId of the new workspace
	 */
	@GET
	@Path("create")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response createWorkspace(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("name") String sName, @QueryParam("node") String sNodeCode) {

		Utils.debugLog("WorkspaceResource.CreateWorkspace(" + sName + ", " + sNodeCode + " )");

		// sName and sNodeCode can be null, and will be defaulted

		// Validate Session
		User oUser = Wasdi.getUserFromSession(sSessionId);
		if (oUser == null) {
			Utils.debugLog("WorkspaceResource.CreateWorkspace: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity("session invalid").build();
		}

		// Create New Workspace
		Workspace oWorkspace = new Workspace();

		if (Utils.isNullOrEmpty(sName)) {
			Utils.debugLog("WorkspaceResource.CreateWorkspace: name is null or empty, defaulting");
			sName = "Untitled Workspace";
		}

		WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();

		while (oWorkspaceRepository.getByUserIdAndWorkspaceName(oUser.getUserId(), sName) != null) {
			sName = Utils.cloneWorkspaceName(sName);
			Utils.debugLog("WorkspaceResource.CreateWorkspace: a workspace with the same name already exists. Changing the name to " + sName);
		}

		// Default values
		oWorkspace.setCreationDate((double) new Date().getTime());
		oWorkspace.setLastEditDate((double) new Date().getTime());
		oWorkspace.setName(sName);
		oWorkspace.setUserId(oUser.getUserId());
		oWorkspace.setWorkspaceId(Utils.getRandomName());
		
		if (Utils.isNullOrEmpty(sNodeCode)) {
			//get user's default nodeCode
			sNodeCode = oUser.getDefaultNode();
		}
		if (Utils.isNullOrEmpty(sNodeCode)) {
			// default node code
			sNodeCode = "wasdi";
		}
		oWorkspace.setNodeCode(sNodeCode);

		if (oWorkspaceRepository.insertWorkspace(oWorkspace)) {
			return Response.ok(oWorkspace.getWorkspaceId(), MediaType.TEXT_PLAIN).build();
		} else {
			Utils.debugLog("WorkspaceResource.CreateWorkspace( " + sName + ", " + sNodeCode + " ): insertion FAILED");
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
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
	public Response updateWorkspace(@HeaderParam("x-session-token") String sSessionId,
			WorkspaceEditorViewModel oWorkspaceEditorViewModel) {

		Utils.debugLog("WorkspaceResource.UpdateWorkspace");

		// Validate Session
		User oUser = Wasdi.getUserFromSession(sSessionId);
		if (oUser == null) {
			Utils.debugLog("WorkspaceResource.UpdateWorkspace: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity("session invalid").build();
		}

		try {
			// Create New Workspace
			Workspace oWorkspace = new Workspace();
			// Initialize repository
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();

			// Default values
			oWorkspace.setCreationDate((double) oWorkspaceEditorViewModel.getCreationDate().getTime());
			oWorkspace.setLastEditDate((double) oWorkspaceEditorViewModel.getLastEditDate().getTime());
			oWorkspace.setName(oWorkspaceEditorViewModel.getName());
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

			if (oWorkspaceRepository.updateWorkspaceName(oWorkspace)) {
				return Response.ok(oWorkspaceEditorViewModel).build();
			} else {
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
			}
		} catch (Exception oEx) {
			Utils.debugLog("WorkspaceResource.UpdateWorkspace: Error update workspace: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
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

		Utils.debugLog("WorkspaceResource.DeleteWorkspace( WS: " + sWorkspaceId + ", DeleteLayer: " + bDeleteLayer + ", DeleteFile: " + bDeleteFile + " )");
		User oUser = null;
		
		//preliminary checks
		try {
			// Validate Session
			oUser = Wasdi.getUserFromSession(sSessionId);
			if (oUser == null) {
				Utils.debugLog("WorkspaceResource.DeleteWorkspace: invalid session");
				return Response.status(Status.UNAUTHORIZED).entity("session invalid").build();
			}
			
			// before any operation check that this is not an injection attempt from the user
			if ( sWorkspaceId.contains("/") || sWorkspaceId.contains("\\") || sWorkspaceId.contains(File.separator)) {
				Utils.debugLog("WorkspaceResource.deleteWorkspace: Injection attempt by user: " + oUser.getUserId() + " on path: " + sWorkspaceId);
				return Response.status(Status.BAD_REQUEST).entity("Invalid workspace: " + sWorkspaceId).build();
			}

			//check user can access given workspace
			if(!PermissionsUtils.canUserAccessWorkspace(oUser.getUserId(), sWorkspaceId)) {
				Utils.debugLog("WorkspaceResource.DeleteWorkspace: " + sWorkspaceId + " cannot be accessed by " + oUser.getUserId() + ", aborting");
				return Response.status(Status.FORBIDDEN).entity(sWorkspaceId + " cannot be accessed by " + oUser.getUserId() + ", aborting").build();
			}
			
		} catch (Exception oE) {
			Utils.debugLog("WorkspaceResource.DeleteWorkspace( " + sSessionId + ", " + sWorkspaceId + " ): cannot complete checks due to " + oE);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}


		try {
			// workspace repository
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();

			//check that the workspace really exists
			Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspaceId);
			if(null==oWorkspace) {
				Utils.debugLog("WorkspaceResource.DeleteWorkspace: " + sWorkspaceId + " is not a valid workspace, aborting");
				return Response.status(Status.BAD_REQUEST).entity("Invalid workspace: " + sWorkspaceId).build();
			}


			ConsoleResource oConsoleResource = new ConsoleResource();
			oConsoleResource.terminate(sSessionId, sWorkspaceId);
			Thread.sleep(2000);


			//delete sharing if the user is not the owner
			String sWorkspaceOwner = Wasdi.getWorkspaceOwner(sWorkspaceId);
			if (!sWorkspaceOwner.equals(oUser.getUserId())) {
				// This is not the owner of the workspace
				Utils.debugLog("WorkspaceResource.DeleteWorkspace: User " + oUser.getUserId() + " is not the owner [" + sWorkspaceOwner + "]: delete the sharing, not the ws");
                UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
                oUserResourcePermissionRepository.deletePermissionsByUserIdAndWorkspaceId(oUser.getUserId(), sWorkspaceId);
				return Response.ok().build();
			}

			//kill active processes
			ProcessWorkspaceService oProcessWorkspaceService = new ProcessWorkspaceService();
			if(oProcessWorkspaceService.killProcessesInWorkspace(sWorkspaceId, sSessionId, true)) {
				Utils.debugLog("WorkspaceResource.DeleteWorkspace: WARNING: could not schedule kill processes in workspace");
			}
			
			// get workspace path
			String sWorkspacePath = Wasdi.getWorkspacePath(sWorkspaceOwner, sWorkspaceId);

			Utils.debugLog("WorkspaceResource.DeleteWorkspace: deleting Workspace " + sWorkspaceId + " of user " + sWorkspaceOwner);

			// Delete Workspace Db Entry
			if (oWorkspaceRepository.deleteWorkspace(sWorkspaceId)) {
				// Get all Products in workspace
				ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
				List<ProductWorkspace> aoProductsWorkspaces = oProductWorkspaceRepository.getProductsByWorkspace(sWorkspaceId);


				DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();
				// Do we need to delete layers?
				if (bDeleteLayer) {
					try {
						Utils.debugLog("ProductResource.DeleteProduct: Deleting workspace layers");

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
								Utils.debugLog("WorkspaceResource.DeleteWorkspace: The file is also in other workspaces, leave the bands as they are");
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
							
							
							List<PublishedBand> aoPublishedBands = oPublishRepository.getPublishedBandsByProductName(Wasdi.getWorkspacePath(sWorkspaceOwner, sWorkspaceId) + sProductName);

							// For each published band
							for (PublishedBand oPublishedBand : aoPublishedBands) {

								try {
									// Remove Geoserver layer (and file)
									if (!oGeoServerManager.removeLayer(oPublishedBand.getLayerId())) {
										Utils.debugLog("WorkspaceResource.DeleteWorkspace: error deleting layer " + oPublishedBand.getLayerId() + " from geoserver");
									}

									try {
										// delete published band on database
										oPublishRepository.deleteByProductNameLayerId(sProductName, oPublishedBand.getLayerId());
									} catch (Exception oEx) {
										Utils.debugLog("WorkspaceResource.DeleteWorkspace: error deleting published band on data base " + oEx.toString());
									}
								} catch (Exception oEx) {
									Utils.debugLog("WorkspaceResource.DeleteWorkspace: error deleting layer id " + oEx.toString());
								}
							}
							
							// If view model is available (should be), get the metadata file reference
							if (oDownloadedFile.getProductViewModel() != null) {
								
								String sMetadataFileRef = oDownloadedFile.getProductViewModel().getMetadataFileReference();
								
								if (!Utils.isNullOrEmpty(sMetadataFileRef)) {
									Utils.debugLog("WorkspaceResource.DeleteWorkspace: deleting metadata file " + sMetadataFileRef);
									FileUtils.deleteQuietly(new File(sMetadataFileRef));
								}
							}
							
						}
					} catch (Exception oE) {
						Utils.debugLog("WorkspaceResource.DeleteWorkspace: error while trying to delete layers: " + oE);
					}

				}

				if (bDeleteFile) {
					try {

						Utils.debugLog("WorkspaceResource.DeleteWorkspace: Delete workspace folder " + sWorkspacePath);

						// delete directory
						try {
							File oDir = new File(sWorkspacePath);
							if (!oDir.exists()) {
								Utils.debugLog("WorkspaceResource.DeleteWorkspace: trying to delete non existing directory " + sWorkspacePath);
							}
							// try anyway for two reasons:
							// 1. non existing directories are handled anyway
							// 2. try avoiding race conditions
							FileUtils.deleteDirectory(oDir);
						} catch (Exception oE) {
							Utils.debugLog("WorkspaceResource.DeleteWorkspace: error while trying to delete directory " + sWorkspacePath + ": " + oE);
						}

						// delete download file on database
						for (ProductWorkspace oProductWorkspace : aoProductsWorkspaces) {

							try {

								Utils.debugLog("WorkspaceResource.DeleteWorkspace: Deleting file " + oProductWorkspace.getProductName());
								oDownloadedFilesRepository.deleteByFilePath(oProductWorkspace.getProductName());

							} catch (Exception oEx) {
								Utils.debugLog("WorkspaceResource.DeleteWorkspace: Error deleting download on data base: " + oEx);
							}
						}

					} catch (Exception oEx) {
						Utils.debugLog("WorkspaceResource.DeleteWorkspace: Error deleting workspace directory: " + oEx);
					}
				}

				// Delete Product Workspace entry
				oProductWorkspaceRepository.deleteByWorkspaceId(sWorkspaceId);

				// Delete also the sharings, it is deleted by the owner..
                UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
                oUserResourcePermissionRepository.deletePermissionsByWorkspaceId(sWorkspaceId);


				return Response.ok().build();
			} else
				Utils.debugLog("WorkspaceResource.DeleteWorkspace: Error deleting workspace on data base");

		} catch (Exception oEx) {
			Utils.debugLog("WorkspaceResource.DeleteWorkspace: " + oEx);
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
	public Response shareWorkspace(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("workspace") String sWorkspaceId, @QueryParam("userId") String sDestinationUserId) {

		Utils.debugLog("WorkspaceResource.ShareWorkspace( WS: " + sWorkspaceId + ", User: " + sDestinationUserId + " )");
		

		// Validate Session
		User oRequesterUser = Wasdi.getUserFromSession(sSessionId);
		if (oRequesterUser == null) {
			Utils.debugLog("WorkspaceResource.shareWorkspace: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity("invalid session").build();
		}
		
		// Check if the workspace exists
		WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
		Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspaceId);
		
		if (oWorkspace == null) {
			Utils.debugLog("WorkspaceResource.ShareWorkspace: invalid workspace");
			return Response.status(Status.BAD_REQUEST).entity("Invalid workspace.").build();
		}

		// Can the user access this section?
		if (!UserApplicationRole.userHasRightsToAccessResource(oRequesterUser.getRole(), WORKSPACE_READ)) {
			return Response.status(Status.FORBIDDEN).entity("user cannot access workspace section").build();
		}
		
		// Can the user access this resource?
		if(!PermissionsUtils.canUserAccessWorkspace(oRequesterUser.getUserId(), sWorkspaceId)) {
			Utils.debugLog("WorkspaceResource.shareWorkspace: " + sWorkspaceId + " cannot be accessed by " + oRequesterUser.getUserId() + ", aborting");
			return Response.status(Status.FORBIDDEN).entity(sWorkspaceId + " cannot be accessed by " + oRequesterUser.getUserId() + ", aborting").build();
		}		
		
		// Cannot Autoshare
		if (oRequesterUser.getUserId().equals(sDestinationUserId)) {
			Utils.debugLog("WorkspaceResource.ShareWorkspace: auto sharing not so smart");
			return Response.status(Status.BAD_REQUEST).entity("Impossible to autoshare.").build();
		}
		
		// Cannot share with the owner
		if (oWorkspace.getUserId().equals(sDestinationUserId)) {
			Utils.debugLog("WorkspaceResource.ShareWorkspace: sharing with the owner not so smart");
			return Response.status(Status.BAD_REQUEST).entity("Cannot Share with owner.").build();
		}			

		UserRepository oUserRepository = new UserRepository();
		User oDestinationUser = oUserRepository.getUser(sDestinationUserId);

		if (oDestinationUser == null) {
			//No. So it is neither the owner or a shared one
			return Response.status(Status.BAD_REQUEST).entity("Destination user does not exists").build();
		}

		try {
			UserResourcePermission oWorkspaceSharing = new UserResourcePermission();
            UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			
			if (!oUserResourcePermissionRepository.isWorkspaceSharedWithUser(sDestinationUserId, sWorkspaceId)) {
				Timestamp oTimestamp = new Timestamp(System.currentTimeMillis());
				oWorkspaceSharing.setResourceType("workspace");
				oWorkspaceSharing.setOwnerId(oRequesterUser.getUserId());
				oWorkspaceSharing.setUserId(sDestinationUserId);
				oWorkspaceSharing.setResourceId(sWorkspaceId);
				oWorkspaceSharing.setCreatedBy(oRequesterUser.getUserId());
				oWorkspaceSharing.setCreatedDate((double) oTimestamp.getTime());
				oWorkspaceSharing.setPermissions("write");
				oUserResourcePermissionRepository.insertPermission(oWorkspaceSharing);				
			}
			else {
				Utils.debugLog("WorkspaceResource.ShareWorkspace: already shared!");
				return Response.ok().entity("Already Shared.").build();
			}
			
		} catch (Exception oEx) {
			Utils.debugLog("WorkspaceResource.ShareWorkspace: " + oEx);

			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}

		sendNotificationEmail(oRequesterUser.getUserId(), sDestinationUserId, oWorkspace.getName());

		return Response.ok().build();

	}

	private static void sendNotificationEmail(String sRequesterUserId, String sDestinationUserId, String sWorkspaceName) {
		try {
			String sMercuriusAPIAddress = WasdiConfig.Current.notifications.mercuriusAPIAddress;
			
			if(Utils.isNullOrEmpty(sMercuriusAPIAddress)) {
				Utils.debugLog("WorkspaceResource.ShareWorkspace: sMercuriusAPIAddress is null");
			}
			else {
				
				Utils.debugLog("WorkspaceResource.ShareWorkspace: send notification");
				
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
				
				Utils.debugLog("WorkspaceResource.ShareWorkspace: notification sent with result " + iPositiveSucceded);
			}
				
		}
		catch (Exception oEx) {
			Utils.debugLog("WorkspaceResource.ShareWorkspace: notification exception " + oEx.toString());
		}
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
	public Response getEnableUsersSharedWorksace(@HeaderParam("x-session-token") String sSessionId, @QueryParam("workspace") String sWorkspaceId) {

		Utils.debugLog("WorkspaceResource.getEnableUsersSharedWorksace( WS: " + sWorkspaceId + " )");

	
		List<UserResourcePermission> aoWorkspaceSharing = null;
		List<WorkspaceSharingViewModel> aoWorkspaceSharingViewModels = new ArrayList<WorkspaceSharingViewModel>();

		User oOwnerUser = Wasdi.getUserFromSession(sSessionId);
		if (oOwnerUser == null) {
			Utils.debugLog("WorkspaceResource.getEnableUsersSharedWorksace: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity("session invalid").build();
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
			Utils.debugLog("WorkspaceResource.getEnableUsersSharedWorksace: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}

		return Response.ok(aoWorkspaceSharingViewModels).build();

	}

	@DELETE
	@Path("share/delete")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response deleteUserSharedWorkspace(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("workspace") String sWorkspaceId, @QueryParam("userId") String sUserId) {

		Utils.debugLog("WorkspaceResource.deleteUserSharedWorkspace( WS: " + sWorkspaceId + ", User:" + sUserId + " )");

		// Validate Session
		User oRequestingUser = Wasdi.getUserFromSession(sSessionId);

		if (oRequestingUser == null) {
			Utils.debugLog("WorkspaceResource.deleteUserSharedWorkspace: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity("session invalid").build();
		}

		try {

			UserRepository oUserRepository = new UserRepository();
			User oDestinationUser = oUserRepository.getUser(sUserId);

			if (oDestinationUser == null) {
				Utils.debugLog("WorkspaceResource.deleteUserSharedWorkspace: invalid destination user");
				return Response.status(Status.BAD_REQUEST).entity("Invalid destination user").build();
			}

            UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
            oUserResourcePermissionRepository.deletePermissionsByUserIdAndWorkspaceId(sUserId, sWorkspaceId);
		} catch (Exception oEx) {
			Utils.debugLog("WorkspaceResource.deleteUserSharedWorkspace: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error in delete proccess").build();
		}

		return Response.ok().build();

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
			Utils.debugLog("WorkspaceResource.getWorkspaceNameById: workspace is null or empty, aborting");
			return Response.status(Status.BAD_REQUEST).entity("workspaceId is null or empty").build();
		}

		User oUser = Wasdi.getUserFromSession(sSessionId);
		if (null == oUser) {
			Utils.debugLog("WorkspaceResource.getWorkspaceNameById: session is invalid, aborting");
			return Response.status(Status.UNAUTHORIZED).entity("session invalid").build();
		}
		
		//check the user can access the workspace
		if(!PermissionsUtils.canUserAccessWorkspace(oUser.getUserId(), sWorkspaceId)) {
			Utils.debugLog("WorkspaceResource.getWorkspaceNameById: user cannot access workspace info, aborting");
			return Response.status(Status.FORBIDDEN).entity("user cannot access workspace info, aborting").build();
		}
		
		try {
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();

			Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspaceId);
			if (oWorkspace != null) {
				String sName = oWorkspace.getName();

				if (!Utils.isNullOrEmpty(sName)) {
					return Response.status(Status.OK).entity(sName).build();
				}
			}

			return Response.status(Status.BAD_REQUEST).entity("Invalid workspace: " + sWorkspaceId).build();
		}
		catch (Exception oEx) {
			Utils.debugLog("WorkspaceResource.getWorkspaceNameById: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	} 
}
