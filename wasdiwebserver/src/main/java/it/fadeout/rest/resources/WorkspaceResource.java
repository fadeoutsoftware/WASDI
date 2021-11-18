package it.fadeout.rest.resources;

import it.fadeout.Wasdi;
import it.fadeout.mercurius.business.Message;
import it.fadeout.mercurius.client.MercuriusAPI;
import it.fadeout.services.ProcessService;
import org.apache.commons.io.FileUtils;
import wasdi.shared.business.*;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.*;
import wasdi.shared.geoserver.GeoServerManager;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.workspaces.WorkspaceEditorViewModel;
import wasdi.shared.viewmodels.workspaces.WorkspaceListInfoViewModel;
import wasdi.shared.viewmodels.workspaces.WorkspaceSharingViewModel;

import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

	@Inject
	ProcessService m_oProcessWorkspaceService;

	private CredentialPolicy m_oCredentialPolicy = new CredentialPolicy();
	private WorkspacePolicy m_oWorkspacePolicy = new WorkspacePolicy();

	@Context
	ServletConfig m_oServletConfig;

	@GET
	@Path("/workspacelistbyproductname")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public ArrayList<WorkspaceListInfoViewModel> getWorkspaceListByProductName(
			@HeaderParam("x-session-token") String sSessionId, @QueryParam("productname") String sProductName) {
		Utils.debugLog("WorkspaceResource.getWorkspaceListByProductName( Product: " + sProductName + " )");

		// input validation
		if (!m_oWorkspacePolicy.validProductName(sProductName)) {
			Utils.debugLog("WorkspaceResource.getWorkspaceListByProductName: invalid sProductName");
			return null;
		}
		
		User oUser = Wasdi.getUserFromSession(sSessionId);
		
		if (oUser == null) {
			Utils.debugLog("WorkspaceResource.getWorkspaceListByProductName( Product: " + sProductName + " ): invalid session");
			return null;			
		}
		
		// get list of workspaces ID by product name
		ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
		List<String> asWorkspaces = oProductWorkspaceRepository.getWorkspaces(sProductName);

		if (asWorkspaces == null) {
			Utils.debugLog("WorkspaceResource.getWorkspaceListByProductName: Workspaces list is null");
			return null;
		}

		WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
		ArrayList<WorkspaceListInfoViewModel> aoResult = new ArrayList<WorkspaceListInfoViewModel>();

		// get workspace info for each workspace ID
		for (String sWorkspaceID : asWorkspaces) {

			Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspaceID);

			if (null != oWorkspace) {
				WorkspaceListInfoViewModel oTemp = new WorkspaceListInfoViewModel();
				oTemp.setWorkspaceId(oWorkspace.getWorkspaceId());
				oTemp.setWorkspaceName(oWorkspace.getName());
				oTemp.setOwnerUserId(oWorkspace.getUserId());
				aoResult.add(oTemp);
			}
		}

		return aoResult;
	}


	/**
	 * Get a list of workspaces of a user
	 * @param sSessionId User Session Id
	 * @return List of Workspace List Info View Model
	 */
	@GET
	@Path("/byuser")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public ArrayList<WorkspaceListInfoViewModel> getListByUser(@HeaderParam("x-session-token") String sSessionId) {

		Utils.debugLog("WorkspaceResource.GetListByUser()");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		ArrayList<WorkspaceListInfoViewModel> aoWSList = new ArrayList<>();

		try {
			if(Utils.isNullOrEmpty(sSessionId)) {
				Utils.debugLog("WorkspaceResource.GetListByUser: null session");
				return aoWSList;
			}
			// Domain Check
			if (oUser == null) {
				Utils.debugLog("WorkspaceResource.GetListByUser: invalid session: " + sSessionId);
				return aoWSList;
			}
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				return aoWSList;
			}

			Utils.debugLog("WorkspaceResource.GetListByUser: workspaces for " + oUser.getUserId());

			// Create repo
			WorkspaceRepository oWSRepository = new WorkspaceRepository();
			WorkspaceSharingRepository oWorkspaceSharingRepository = new WorkspaceSharingRepository();

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

				// Get Sharings
				List<WorkspaceSharing> aoSharings = oWorkspaceSharingRepository
						.getWorkspaceSharingByWorkspace(oWorkspace.getWorkspaceId());

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

			// Get the list of workspace shared with this user
			List<WorkspaceSharing> aoSharedWorkspaces = oWorkspaceSharingRepository.getWorkspaceSharingByUser(oUser.getUserId());

			if (aoSharedWorkspaces.size() > 0) {
				// For each
				for (int iWorkspaces = 0; iWorkspaces < aoSharedWorkspaces.size(); iWorkspaces++) {

					// Create View Model
					WorkspaceListInfoViewModel oWSViewModel = new WorkspaceListInfoViewModel();
					Workspace oWorkspace = oWSRepository.getWorkspace(aoSharedWorkspaces.get(iWorkspaces).getWorkspaceId());

					if (oWorkspace == null) {
						Utils.debugLog("WorkspaceResult.getListByUser: WS Shared not available " + aoSharedWorkspaces.get(iWorkspaces).getWorkspaceId());
						continue;
					}

					oWSViewModel.setOwnerUserId(oWorkspace.getUserId());
					oWSViewModel.setWorkspaceId(oWorkspace.getWorkspaceId());
					oWSViewModel.setWorkspaceName(oWorkspace.getName());

					// Get Sharings
					List<WorkspaceSharing> aoSharings = oWorkspaceSharingRepository.getWorkspaceSharingByWorkspace(oWorkspace.getWorkspaceId());

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

		return aoWSList;
	}

	@GET
	@Path("getws")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public WorkspaceEditorViewModel getWorkspaceEditorViewModel(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("workspace") String sWorkspaceId) {

		Utils.debugLog("WorkspaceResource.GetWorkspaceEditorViewModel( WS: " + sWorkspaceId + ")");

		WorkspaceEditorViewModel oVM = new WorkspaceEditorViewModel();

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			Utils.debugLog("WorkspaceResource.GetWorkspaceEditorViewModel: invalid session");
			return null;
		}
		if (Utils.isNullOrEmpty(oUser.getUserId()))
			return null;

		try {
			// Domain Check
			if (sWorkspaceId == null) {
				return oVM;
			}
			if (Utils.isNullOrEmpty(sWorkspaceId)) {
				return oVM;
			}

			Utils.debugLog("WorkspaceResource.GetWorkspaceEditorViewModel: read workspaces " + sWorkspaceId);

			// Create repo
			WorkspaceRepository oWSRepository = new WorkspaceRepository();
			
			WorkspaceSharingRepository oWorkspaceSharingRepository = new WorkspaceSharingRepository();
			
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
					oVM.setApiUrl(oWorkspaceNode.getNodeBaseAddress());
					
					if (!Utils.isNullOrEmpty(oWorkspaceNode.getCloudProvider())) {
						oVM.setCloudProvider(oWorkspaceNode.getCloudProvider());
					}
					else {
						oVM.setCloudProvider(oWorkspaceNode.getNodeCode());
					}
				}
			
			}
			else {
				oVM.setCloudProvider("wasdi");
			}

			// Get Sharings
			List<WorkspaceSharing> aoSharings = oWorkspaceSharingRepository
					.getWorkspaceSharingByWorkspace(oWorkspace.getWorkspaceId());
			// Add Sharings to View Model
			if (aoSharings != null) {
				for (int iSharings = 0; iSharings < aoSharings.size(); iSharings++) {
					if (oVM.getSharedUsers() == null) {
						oVM.setSharedUsers(new ArrayList<String>());
					}
					oVM.getSharedUsers().add(aoSharings.get(iSharings).getUserId());
				}
			}
		} catch (Exception oEx) {
			Utils.debugLog( "WorkspaceResource.getWorkspaceEditorViewModel: " + oEx);
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

		Utils.debugLog("WorkspaceResource.CreateWorkspace(" + sName + ", " + sNodeCode + " )");

		if (Utils.isNullOrEmpty(sSessionId)) {
			Utils.debugLog("WorkspaceResource.CreateWorkspace: session is null or empty, aborting");
			return null;
		}

		// sName and sNodeCode can be null, and will be defaulted

		// Validate Session
		User oUser = Wasdi.getUserFromSession(sSessionId);
		if (oUser == null) {
			Utils.debugLog("WorkspaceResource.CreateWorkspace: invalid session");
			return null;
		}
		if (Utils.isNullOrEmpty(oUser.getUserId())) {
			Utils.debugLog("WorkspaceResource.CreateWorkspace: userId from session is null or empty, aborting");
			return null;
		}

		// Create New Workspace
		Workspace oWorkspace = new Workspace();

		if (Utils.isNullOrEmpty(sName)) {
			Utils.debugLog("WorkspaceResource.CreateWorkspace: name is null or empty, defaulting");
			sName = "Untitled Workspace";
		}

		WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();

		while (oWorkspaceRepository.getByNameAndNode(sName, sNodeCode) != null) {
			sName = sName + "_02";
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

			PrimitiveResult oResult = new PrimitiveResult();
			oResult.setStringValue(oWorkspace.getWorkspaceId());

			return oResult;
		} else {
			Utils.debugLog("WorkspaceResource.CreateWorkspace( " + sName + ", " + sNodeCode + " ): insertion FAILED");
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

		Utils.debugLog("WorkspaceResource.UpdateWorkspace");

		// Validate Session
		User oUser = Wasdi.getUserFromSession(sSessionId);
		if (oUser == null) {
			Utils.debugLog("WorkspaceResource.UpdateWorkspace: invalid session");
			return null;
		}
		if (Utils.isNullOrEmpty(oUser.getUserId()))
			return null;

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

			
			// if present, the node code must be updated
			if(oWorkspaceEditorViewModel.getNodeCode() != null) {
				oWorkspace.setNodeCode(oWorkspaceEditorViewModel.getNodeCode());
				oWorkspaceRepository.updateWorkspaceNodeCode(oWorkspace);
			}

			if (oWorkspaceRepository.updateWorkspaceName(oWorkspace)) {

				PrimitiveResult oResult = new PrimitiveResult();
				oResult.setStringValue(oWorkspace.getWorkspaceId());

				return oWorkspaceEditorViewModel;
			} else {
				return null;
			}
		} catch (Exception oEx) {
			Utils.debugLog("WorkspaceResource.UpdateWorkspace: Error update workspace: " + oEx);
		}

		return null;
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
			// before any operation check that this is not an injection attempt from the user
			if ( sWorkspaceId.contains("/") || sWorkspaceId.contains("\\") || sWorkspaceId.contains(File.separator)) {
				Utils.debugLog("WorkspaceResource.deleteWorkspace: Injection attempt from users");
				return Response.status(400).build();
			}

			// Validate Session
			oUser = Wasdi.getUserFromSession(sSessionId);
			if (oUser == null) {
				Utils.debugLog("WorkspaceResource.DeleteWorkspace: invalid session");
				return Response.status(401).build();
			}

			//check user is valid
			if (Utils.isNullOrEmpty(oUser.getUserId())) {
				Utils.debugLog("WorkspaceResource.DeleteWorkspace: userId is null");
				return Response.status(401).build();
			}

			//check user can access given workspace
			if(!PermissionsUtils.canUserAccessWorkspace(oUser.getUserId(), sWorkspaceId)) {
				Utils.debugLog("WorkspaceResource.DeleteWorkspace: " + sWorkspaceId + " cannot be accessed by " + oUser.getUserId() + ", aborting");
				return Response.status(403).build();
			}
		} catch (Exception oE) {
			Utils.debugLog("WorkspaceResource.DeleteWorkspace( " + sSessionId + ", " + sWorkspaceId + " ): cannot complete checks due to " + oE);
		}

		try {
			// workspace repository
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();

			//check that the workspace really exists
			Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspaceId);
			if(null==oWorkspace) {
				Utils.debugLog("WorkspaceResource.DeleteWorkspace: " + sWorkspaceId + " is not a valid workspace, aborting");
				return Response.status(400).build();
			}

			//delete sharing if the user is not the owner
			String sWorkspaceOwner = Wasdi.getWorkspaceOwner(sWorkspaceId);
			if (!sWorkspaceOwner.equals(oUser.getUserId())) {
				// This is not the owner of the workspace
				Utils.debugLog("User " + oUser.getUserId() + " is not the owner [" + sWorkspaceOwner + "]: delete the sharing, not the ws");
				WorkspaceSharingRepository oWorkspaceSharingRepository = new WorkspaceSharingRepository();
				oWorkspaceSharingRepository.deleteByUserIdWorkspaceId(oUser.getUserId(), sWorkspaceId);
				return Response.ok().build();
			}

			//kill active processes
			//TODO test
			m_oProcessWorkspaceService.killProcessesInWorkspace(sWorkspaceId);

			//TODO test
			//TODO move to service
			//big list...
			ProcessWorkspaceRepository oProcessWorkspaceRepository = new ProcessWorkspaceRepository();
			List<String> asWorkspaceProcessesList = oProcessWorkspaceRepository.getProcessObjIdsFromWorkspaceId(sWorkspaceId);
			ProcessorLogRepository oProcessorLogRepository = new ProcessorLogRepository();
			oProcessorLogRepository.deleteLogsByProcessWorkspaceIds(asWorkspaceProcessesList);


			// Delete all the process-workspaces
			oProcessWorkspaceRepository.deleteProcessWorkspaceByWorkspaceId(sWorkspaceId);


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
							List<PublishedBand> aoPublishedBands = oPublishRepository.getPublishedBandsByProductName(sProductName);

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
				WorkspaceSharingRepository oWorkspaceSharingRepository = new WorkspaceSharingRepository();
				oWorkspaceSharingRepository.deleteByWorkspaceId(sWorkspaceId);

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
	 * @param sUserId User id that will receive the workspace in sharing.
	 * @return Primitive Result with boolValue = true and stringValue = Done if ok. False and error description otherwise
	 */
	@PUT
	@Path("share/add")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult shareWorkspace(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("workspace") String sWorkspaceId, @QueryParam("userId") String sUserId) {

		Utils.debugLog("WorkspaceResource.ShareWorkspace( WS: " + sWorkspaceId + ", User: " + sUserId + " )");

		// Validate Session
		
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);

		User oRequesterUser = Wasdi.getUserFromSession(sSessionId);
		if (oRequesterUser == null) {
			Utils.debugLog("WorkspaceResource.ShareWorkspace: invalid session");
			return oResult;
		}

		if (Utils.isNullOrEmpty(oRequesterUser.getUserId())) {
			Utils.debugLog("WorkspaceResource.ShareWorkspace: invalid user");
			oResult.setStringValue("Invalid user.");
			return oResult;
		}
		
		WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
		Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspaceId);
		
		if (oWorkspace == null) {
			Utils.debugLog("WorkspaceResource.ShareWorkspace: invalid workspace");
			oResult.setStringValue("Invalid workspace.");
			return oResult;			
		}
		
		if (oRequesterUser.getUserId().equals(sUserId)) {
			Utils.debugLog("WorkspaceResource.ShareWorkspace: auto sharing not so smart");
			oResult.setStringValue("Impossible to autoshare.");
			return oResult;				
		}		
		
		if (!oWorkspace.getUserId().equals(oRequesterUser.getUserId())) {
			
			// Is he trying to share with the owner?
			if (oWorkspace.getUserId().equals(sUserId)) {
				oResult.setStringValue("Cannot Share with owner");
				return oResult;					
			}			
			
			WorkspaceSharingRepository oWorkspaceSharingRepository = new WorkspaceSharingRepository();
			
			if (!oWorkspaceSharingRepository.isSharedWithUser(oRequesterUser.getUserId(), sWorkspaceId)) {
				//No. So it is neither the owner or a shared one
				oResult.setStringValue("Unauthorized");
				return oResult;
			}
		}

		UserRepository oUserRepository = new UserRepository();
		User oDestinationUser = oUserRepository.getUser(sUserId);

		if (oDestinationUser == null) {
			//No. So it is neither the owner or a shared one
			oResult.setStringValue("Destination user does not exists");
			return oResult;
		}

		try {
			WorkspaceSharing oWorkspaceSharing = new WorkspaceSharing();
			WorkspaceSharingRepository oWorkspaceSharingRepository = new WorkspaceSharingRepository();
			
			if (!oWorkspaceSharingRepository.isSharedWithUser(sUserId, sWorkspaceId)) {
				Timestamp oTimestamp = new Timestamp(System.currentTimeMillis());
				oWorkspaceSharing.setOwnerId(oRequesterUser.getUserId());
				oWorkspaceSharing.setUserId(sUserId);
				oWorkspaceSharing.setWorkspaceId(sWorkspaceId);
				oWorkspaceSharing.setShareDate((double) oTimestamp.getTime());
				oWorkspaceSharingRepository.insertWorkspaceSharing(oWorkspaceSharing);				
			}
			else {
				Utils.debugLog("WorkspaceResource.ShareWorkspace: already shared!");
				oResult.setStringValue("Already Shared.");
				oResult.setBoolValue(true);
				return oResult;
			}
			
		} catch (Exception oEx) {
			Utils.debugLog("WorkspaceResource.ShareWorkspace: " + oEx);

			oResult.setStringValue("Error in save proccess");
			oResult.setBoolValue(false);

			return oResult;
		}

		oResult.setStringValue("Done");
		oResult.setBoolValue(true);
		
		try {
			String sMercuriusAPIAddress = WasdiConfig.Current.notifications.mercuriusAPIAddress;
			
			if(Utils.isNullOrEmpty(sMercuriusAPIAddress)) {
				Utils.debugLog("WorkspaceResource.ShareWorkspace: sMercuriusAPIAddress is null");
			}
			else {
				
				Utils.debugLog("WorkspaceResource.ShareWorkspace: send notification");
				
				MercuriusAPI oAPI = new MercuriusAPI(sMercuriusAPIAddress);			
				Message oMessage = new Message();
				
				String sTitle = "Workspace " + oWorkspace.getName() + " Shared";
				
				oMessage.setTilte(sTitle);
				
				String sSender = WasdiConfig.Current.notifications.sftpManagementMailSender;
				if (sSender==null) {
					sSender = "wasdi@wasdi.net";
				}
				
				oMessage.setSender(sSender);
				
				String sMessage = "The user " + oRequesterUser.getUserId() +  " shared with you the workspace: " + oWorkspace.getName();
								
				oMessage.setMessage(sMessage);
		
				Integer iPositiveSucceded = 0;
								
				iPositiveSucceded = oAPI.sendMailDirect(sUserId, oMessage);
				
				Utils.debugLog("WorkspaceResource.ShareWorkspace: notification sent with result " + iPositiveSucceded);
			}
				
		}
		catch (Exception oEx) {
			Utils.debugLog("WorkspaceResource.ShareWorkspace: notification exception " + oEx.toString());
		}

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
	public List<WorkspaceSharingViewModel> getEnableUsersSharedWorksace(@HeaderParam("x-session-token") String sSessionId, @QueryParam("workspace") String sWorkspaceId) {

		Utils.debugLog("WorkspaceResource.getEnableUsersSharedWorksace( WS: " + sWorkspaceId + " )");

	
		List<WorkspaceSharing> aoWorkspaceSharing = null;
		List<WorkspaceSharingViewModel> aoWorkspaceSharingViewModels = new ArrayList<WorkspaceSharingViewModel>();

		User oOwnerUser = Wasdi.getUserFromSession(sSessionId);
		if (oOwnerUser == null) {
			Utils.debugLog("WorkspaceResource.getEnableUsersSharedWorksace: invalid session");
			return aoWorkspaceSharingViewModels;
		}

		if (Utils.isNullOrEmpty(oOwnerUser.getUserId())) {
			return aoWorkspaceSharingViewModels;
		}

		try {
			WorkspaceSharingRepository oWorkspaceSharingRepository = new WorkspaceSharingRepository();
			aoWorkspaceSharing = oWorkspaceSharingRepository.getWorkspaceSharingByWorkspace(sWorkspaceId);

			if (aoWorkspaceSharing != null) {
				for (WorkspaceSharing oWorkspaceSharing : aoWorkspaceSharing) {
					WorkspaceSharingViewModel oWorkspaceSharingViewModel = new WorkspaceSharingViewModel();
					oWorkspaceSharingViewModel.setOwnerId(oWorkspaceSharing.getUserId());
					oWorkspaceSharingViewModel.setUserId(oWorkspaceSharing.getUserId());
					oWorkspaceSharingViewModel.setWorkspaceId(oWorkspaceSharing.getWorkspaceId());

					aoWorkspaceSharingViewModels.add(oWorkspaceSharingViewModel);
				}

			}

		} catch (Exception oEx) {
			Utils.debugLog("WorkspaceResource.getEnableUsersSharedWorksace: " + oEx);
			return aoWorkspaceSharingViewModels;
		}

		return aoWorkspaceSharingViewModels;

	}

	@DELETE
	@Path("share/delete")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult deleteUserSharedWorkspace(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("workspace") String sWorkspaceId, @QueryParam("userId") String sUserId) {

		Utils.debugLog("WorkspaceResource.deleteUserSharedWorkspace( WS: " + sWorkspaceId + ", User:" + sUserId + " )");
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);
		// Validate Session
		User oRequestingUser = Wasdi.getUserFromSession(sSessionId);

		if (oRequestingUser == null) {
			Utils.debugLog("WorkspaceResource.deleteUserSharedWorkspace: invalid session");
			return oResult;
		}

		if (Utils.isNullOrEmpty(oRequestingUser.getUserId())) {
			oResult.setStringValue("Invalid user");
			return oResult;
		}

		try {

			UserRepository oUserRepository = new UserRepository();
			User oDestinationUser = oUserRepository.getUser(sUserId);

			if (oDestinationUser == null) {
				oResult.setStringValue("Invalid destination user");
				return oResult;
			}

			WorkspaceSharingRepository oWorkspaceSharingRepository = new WorkspaceSharingRepository();
			oWorkspaceSharingRepository.deleteByUserIdWorkspaceId(sUserId, sWorkspaceId);
		} catch (Exception oEx) {
			Utils.debugLog("WorkspaceResource.deleteUserSharedWorkspace: " + oEx);
			oResult.setStringValue("Error in delete proccess");
			return oResult;
		}

		oResult.setStringValue("Done");
		oResult.setBoolValue(true);
		return oResult;

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

		if(Utils.isNullOrEmpty(sWorkspaceId)) {
			Utils.debugLog("WorkspaceResource.getWorkspaceNameById: workspace is null or empty, aborting");
			return Response.status(Status.BAD_REQUEST).entity("workspaceId is null or empty").build();
		}
		User oUser = Wasdi.getUserFromSession(sSessionId);
		if(null==oUser) {
			Utils.debugLog("WorkspaceResource.getWorkspaceNameById: session is invalid, aborting");
			return Response.status(Status.UNAUTHORIZED).entity("session invalid").build();
		}
		if(Utils.isNullOrEmpty(oUser.getUserId())) {
			Utils.debugLog("WorkspaceResource.getWorkspaceNameById: user from session is null or empty, aborting");
			return Response.status(Status.UNAUTHORIZED).entity("session invalid").build();
		}
		
		//check the user can access the workspace
		if(!PermissionsUtils.canUserAccessWorkspace(oUser.getUserId(), sWorkspaceId)) {
			Utils.debugLog("WorkspaceResource.getWorkspaceNameById: user cannot access workspace info, aborting");
			return Response.status(Status.FORBIDDEN).build();
		}
		
		try {
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();

			Workspace oWorkspace = oWorkspaceRepository.getWorkspace(sWorkspaceId);
			if (oWorkspace!= null) {
				String sName = oWorkspace.getName();

				if (!Utils.isNullOrEmpty(sName)) {
					return Response.status(Status.OK).entity(sName).build();
				}
			}
		}
		catch (Exception oEx) {
			Utils.debugLog("WorkspaceResource.getWorkspaceNameById: " + oEx);
			return Response.status(500).build();
		}

		return Response.status(400).build();
	} 
}
