package it.fadeout.rest.resources;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;

import it.fadeout.Wasdi;
import wasdi.shared.business.DownloadedFile;
import wasdi.shared.business.ProductWorkspace;
import wasdi.shared.business.PublishedBand;
import wasdi.shared.business.User;
import wasdi.shared.business.UserSession;
import wasdi.shared.business.Workspace;
import wasdi.shared.business.WorkspaceSharing;
import wasdi.shared.data.DownloadedFilesRepository;
import wasdi.shared.data.ProductWorkspaceRepository;
import wasdi.shared.data.PublishedBandsRepository;
import wasdi.shared.data.SessionRepository;
import wasdi.shared.data.WorkspaceRepository;
import wasdi.shared.data.WorkspaceSharingRepository;
import wasdi.shared.geoserver.GeoServerManager;
import wasdi.shared.utils.CredentialPolicy;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WorkspacePolicy;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.WorkspaceEditorViewModel;
import wasdi.shared.viewmodels.WorkspaceListInfoViewModel;

@Path("/ws")
public class WorkspaceResource {

	private CredentialPolicy m_oCredentialPolicy = new CredentialPolicy();
	private WorkspacePolicy m_oWorkspacePolicy = new WorkspacePolicy();

	@Context
	ServletConfig m_oServletConfig;

	@GET
	@Path("/workspacelistbyproductname")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public ArrayList<WorkspaceListInfoViewModel> getWorkspaceListByProductName(
			@HeaderParam("x-session-token") String sSessionId, @QueryParam("productname") String sProductName) {
		Utils.debugLog("WorkspaceResource.getWorkspaceListByProductName( " + sSessionId + ", " + sProductName + " )");

		// input validation
		if (null == sSessionId || null == sProductName) {
			Utils.debugLog("WorkspaceResource.getWorkspaceListByProductName: invalid input");
			return null;
		} else if (!m_oCredentialPolicy.validSessionId(sSessionId)) {
			Utils.debugLog("WorkspaceResource.getWorkspaceListByProductName: invalid sSessionId");
			return null;
		} else if (!m_oWorkspacePolicy.validProductName(sProductName)) {
			Utils.debugLog("WorkspaceResource.getWorkspaceListByProductName: invalid sProductName");
			return null;
		}

		// session validation
		SessionRepository oSessionRepo = new SessionRepository();
		UserSession oUserSession = oSessionRepo.GetSession(sSessionId);
		if (null == oUserSession) {
			Utils.debugLog("WorkspaceResource.getWorkspaceListByProductName: invalid sSessionId");
			return null;
		}

		// get list of workspaces ID by productname
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
			Workspace oWorkspace = oWorkspaceRepository.GetWorkspace(sWorkspaceID);
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

	@GET
	@Path("/byuser")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public ArrayList<WorkspaceListInfoViewModel> getListByUser(@HeaderParam("x-session-token") String sSessionId) {

		Utils.debugLog("WorkspaceResource.GetListByUser( " + sSessionId + " )");

		User oUser = Wasdi.GetUserFromSession(sSessionId);

		ArrayList<WorkspaceListInfoViewModel> aoWSList = new ArrayList<>();

		try {
			// Domain Check
			if (oUser == null) {
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
			List<Workspace> aoWorkspaces = oWSRepository.GetWorkspaceByUser(oUser.getUserId());

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
						.GetWorkspaceSharingByWorkspace(oWorkspace.getWorkspaceId());

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
			List<WorkspaceSharing> aoSharedWorkspaces = oWorkspaceSharingRepository.GetWorkspaceSharingByUser(oUser.getUserId());

			if (aoSharedWorkspaces.size() > 0) {
				// For each
				for (int iWorkspaces = 0; iWorkspaces < aoSharedWorkspaces.size(); iWorkspaces++) {

					// Create View Model
					WorkspaceListInfoViewModel oWSViewModel = new WorkspaceListInfoViewModel();
					Workspace oWorkspace = oWSRepository.GetWorkspace(aoSharedWorkspaces.get(iWorkspaces).getWorkspaceId());

					if (oWorkspace == null) {
						Utils.debugLog("WorkspaceResult.getListByUser: WS Shared not available " + aoSharedWorkspaces.get(iWorkspaces).getWorkspaceId());
						continue;
					}

					oWSViewModel.setOwnerUserId(oWorkspace.getUserId());
					oWSViewModel.setWorkspaceId(oWorkspace.getWorkspaceId());
					oWSViewModel.setWorkspaceName(oWorkspace.getName());

					// Get Sharings
					List<WorkspaceSharing> aoSharings = oWorkspaceSharingRepository
							.GetWorkspaceSharingByWorkspace(oWorkspace.getWorkspaceId());

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
	@Path("")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public WorkspaceEditorViewModel getWorkspaceEditorViewModel(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("sWorkspaceId") String sWorkspaceId) {

		Utils.debugLog("WorkspaceResource.GetWorkspaceEditorViewModel( " + sWorkspaceId + " )");

		WorkspaceEditorViewModel oVM = new WorkspaceEditorViewModel();

		User oUser = Wasdi.GetUserFromSession(sSessionId);

		if (oUser == null)
			return null;
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

			// Get Workspace List
			Workspace oWorkspace = oWSRepository.GetWorkspace(sWorkspaceId);

			oVM.setUserId(oWorkspace.getUserId());
			oVM.setWorkspaceId(oWorkspace.getWorkspaceId());
			oVM.setName(oWorkspace.getName());
			oVM.setCreationDate(Utils.getDate(oWorkspace.getCreationDate()));
			oVM.setLastEditDate(Utils.getDate(oWorkspace.getLastEditDate()));
			
			// If the workspace is on another node, copy the url to the view model
			if (oWorkspace.getNodeCode().equals(Wasdi.s_sMyNodeCode) == false) {
				// Get the Node
				wasdi.shared.data.NodeRepository oNodeRepository = new wasdi.shared.data.NodeRepository();
				wasdi.shared.business.Node oWorkspaceNode = oNodeRepository.GetNodeByCode(oWorkspace.getNodeCode());
				oVM.setApiUrl(oWorkspaceNode.getNodeBaseAddress());
			}

			// Get Sharings
			List<WorkspaceSharing> aoSharings = oWorkspaceSharingRepository
					.GetWorkspaceSharingByWorkspace(oWorkspace.getWorkspaceId());

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
			oEx.toString();
		}

		return oVM;
	}

	@GET
	@Path("create")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult createWorkspace(@HeaderParam("x-session-token") String sSessionId) {

		Utils.debugLog("WorkspaceResource.CreateWorkspace( " + sSessionId + " )");

		// Validate Session
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null)
			return null;
		if (Utils.isNullOrEmpty(oUser.getUserId()))
			return null;

		// Create New Workspace
		Workspace oWorkspace = new Workspace();

		// Default values
		oWorkspace.setCreationDate((double) new Date().getTime());
		oWorkspace.setLastEditDate((double) new Date().getTime());
		oWorkspace.setName("Untitled Workspace");
		oWorkspace.setUserId(oUser.getUserId());
		oWorkspace.setWorkspaceId(Utils.GetRandomName());

		WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
		if (oWorkspaceRepository.InsertWorkspace(oWorkspace)) {

			PrimitiveResult oResult = new PrimitiveResult();
			oResult.setStringValue(oWorkspace.getWorkspaceId());

			return oResult;
		} else {
			return null;
		}

	}

	@POST
	@Path("update")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public WorkspaceEditorViewModel updateWorkspace(@HeaderParam("x-session-token") String sSessionId,
			WorkspaceEditorViewModel oViewModel) {

		Utils.debugLog("WorkspaceResource.UpdateWorkspace( " + sSessionId + ", ... )");

		// Validate Session
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null)
			return null;
		if (Utils.isNullOrEmpty(oUser.getUserId()))
			return null;

		try {
			// Create New Workspace
			Workspace oWorkspace = new Workspace();

			// Default values
			oWorkspace.setCreationDate((double) oViewModel.getCreationDate().getTime());
			oWorkspace.setLastEditDate((double) oViewModel.getLastEditDate().getTime());
			oWorkspace.setName(oViewModel.getName());
			oWorkspace.setUserId(oViewModel.getUserId());
			oWorkspace.setWorkspaceId(oViewModel.getWorkspaceId());

			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			if (oWorkspaceRepository.UpdateWorkspace(oWorkspace)) {

				PrimitiveResult oResult = new PrimitiveResult();
				oResult.setStringValue(oWorkspace.getWorkspaceId());

				return oViewModel;
			} else {
				return null;
			}
		} catch (Exception oEx) {
			Utils.debugLog("WorkspaceResource.UpdateWorkspace: Error update workspace: " + oEx);
		}

		return null;

	}

	@DELETE
	@Path("delete")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response deleteWorkspace(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("sWorkspaceId") String sWorkspaceId, @QueryParam("bDeleteLayer") Boolean bDeleteLayer,
			@QueryParam("bDeleteFile") Boolean bDeleteFile) {

		Utils.debugLog("WorkspaceResource.DeleteWorkspace( " + sSessionId + ", " + sWorkspaceId + ", " + bDeleteLayer
				+ ", " + bDeleteFile + " )");

		// Validate Session
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null)
			return null;
		if (Utils.isNullOrEmpty(oUser.getUserId()))
			return null;

		try {
			// repositories
			ProductWorkspaceRepository oProductWorkspaceRepository = new ProductWorkspaceRepository();
			PublishedBandsRepository oPublishRepository = new PublishedBandsRepository();
			WorkspaceRepository oWorkspaceRepository = new WorkspaceRepository();
			DownloadedFilesRepository oDownloadedFilesRepository = new DownloadedFilesRepository();

			String sWorkspaceOwner = Wasdi.getWorkspaceOwner(sWorkspaceId);

			if (!sWorkspaceOwner.equals(oUser.getUserId())) {
				// This is not the owner of the workspace
				Utils.debugLog("User " + oUser.getUserId() + " is not the owner [" + sWorkspaceOwner
						+ "]: delete the sharing, not the ws");
				WorkspaceSharingRepository oWorkspaceSharingRepository = new WorkspaceSharingRepository();
				oWorkspaceSharingRepository.DeleteByUserIdWorkspaceId(oUser.getUserId(), sWorkspaceId);
				return Response.ok().build();
			}

			// get workspace path
			String sDownloadPath = Wasdi.getWorkspacePath(m_oServletConfig, sWorkspaceOwner, sWorkspaceId);

			if (oWorkspaceRepository.DeleteWorkspace(sWorkspaceId)) {
				// get all product in workspace
				List<ProductWorkspace> aoProducts = oProductWorkspaceRepository.GetProductsByWorkspace(sWorkspaceId);

				if (bDeleteFile) {
					try {

						Utils.debugLog("WorkspaceResource.DeleteWorkspace: Delete workspace " + sDownloadPath);
						// delete directory
						FileUtils.deleteDirectory(new File(sDownloadPath));
						// delete download on data base
						for (ProductWorkspace oProductWorkspace : aoProducts) {
							try {
								String sFilePath = sDownloadPath + oProductWorkspace.getProductName();
								oDownloadedFilesRepository.DeleteByFilePath(sFilePath);
							} catch (Exception oEx) {
								Utils.debugLog(
										"WorkspaceResource.DeleteWorkspace: Error deleting download on data base: "
												+ oEx);
							}
						}

					} catch (Exception oEx) {
						Utils.debugLog("WorkspaceResource.DeleteWorkspace: Error deleting workspace directory: " + oEx);
					}
				}

				if (bDeleteLayer) {

					GeoServerManager gsManager = new GeoServerManager(m_oServletConfig.getInitParameter("GS_URL"),
							m_oServletConfig.getInitParameter("GS_USER"),
							m_oServletConfig.getInitParameter("GS_PASSWORD"));
					String gsWorkspace = m_oServletConfig.getInitParameter("GS_WORKSPACE");
					Utils.debugLog("WorkspaceResource.DeleteWorkspace: gsWorkspace = " + gsWorkspace);

					for (ProductWorkspace oProductWorkspace : aoProducts) {
						List<PublishedBand> aoPublishedBands = oPublishRepository
								.GetPublishedBandsByProductName(oProductWorkspace.getProductName());
						for (PublishedBand oPublishedBand : aoPublishedBands) {
							try {

								if (!gsManager.removeLayer(oPublishedBand.getLayerId())) {
									Utils.debugLog("ProductResource.DeleteProduct: error deleting layer "
											+ oPublishedBand.getLayerId() + " from geoserver");
								}

								try {

									DownloadedFile oDownloadedFile = oDownloadedFilesRepository
											.GetDownloadedFileByPath(oProductWorkspace.getProductName());
									// delete published band on data base
									oPublishRepository.DeleteByProductNameLayerId(
											oDownloadedFile.getProductViewModel().getName(),
											oPublishedBand.getLayerId());
								} catch (Exception oEx) {
									Utils.debugLog(
											"WorkspaceResource.DeleteWorkspace: error deleting published band on data base "
													+ oEx.toString());
								}

							} catch (Exception oEx) {
								Utils.debugLog(
										"WorkspaceResource.DeleteWorkspace: error deleting layer id " + oEx.toString());
							}

						}
					}

				}
				
				// Delete also the sharings, it is deleted by the owner..
				WorkspaceSharingRepository oWorkspaceSharingRepository = new WorkspaceSharingRepository();
				oWorkspaceSharingRepository.DeleteByWorkspaceId(sWorkspaceId);


				return Response.ok().build();
			} else
				Utils.debugLog("WorkspaceResource.DeleteWorkspace: Error deleting workspace on data base");

		} catch (Exception oEx) {
			Utils.debugLog("WorkspaceResource.DeleteWorkspace: " + oEx);
		}

		return null;
	}

	@PUT
	@Path("share")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult shareWorkspace(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("sWorkspaceId") String sWorkspaceId, @QueryParam("sUserId") String sUserId) {

		Utils.debugLog("WorkspaceResource.ShareWorkspace( " + sSessionId + ", " + sWorkspaceId + ", " + sUserId + " )");

		// Validate Session
		User oOwnerUser = Wasdi.GetUserFromSession(sSessionId);
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);

		if (oOwnerUser == null) {
			oResult.setStringValue("Invalid user.");
			return oResult;
		}

		if (Utils.isNullOrEmpty(oOwnerUser.getUserId())) {
			oResult.setStringValue("Invalid user.");
			return oResult;
		}

		if (m_oCredentialPolicy.validUserId(sUserId) == false) {
			oResult.setStringValue("Invalid user.");
			return oResult;
		}

		if (m_oWorkspacePolicy.validWorkspaceId(sWorkspaceId) == false) {
			oResult.setStringValue("Invalid workspace.");
			return oResult;
		}

		try {
			WorkspaceSharing oWorkspaceSharing = new WorkspaceSharing();
			WorkspaceSharingRepository oWorkspaceSharingRepository = new WorkspaceSharingRepository();
			Timestamp oTimestamp = new Timestamp(System.currentTimeMillis());
			oWorkspaceSharing.setOwnerId(oOwnerUser.getUserId());
			oWorkspaceSharing.setUserId(sUserId);
			oWorkspaceSharing.setWorkspaceId(sWorkspaceId);
			oWorkspaceSharing.setShareDate((double) oTimestamp.getTime());
			oWorkspaceSharingRepository.InsertWorkspaceSharing(oWorkspaceSharing);
		} catch (Exception oEx) {
			Utils.debugLog("WorkspaceResource.ShareWorkspace: " + oEx);

			oResult.setStringValue("Error in save proccess");
			oResult.setBoolValue(false);

			return oResult;
		}

		oResult.setStringValue("Done");
		oResult.setBoolValue(true);

		return oResult;

	}

	@GET
	@Path("enableusersworkspace")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public List<WorkspaceSharing> getEnableUsersSharedWorksace(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("sWorkspaceId") String sWorkspaceId) {

		Utils.debugLog("WorkspaceResource.getUsersSharedWorksace( " + sSessionId + ", " + sWorkspaceId + " )");

		// Validate Session
		User oOwnerUser = Wasdi.GetUserFromSession(sSessionId);
		List<WorkspaceSharing> aoWorkspaceSharing = null;

		if (oOwnerUser == null) {
			return null;
		}

		if (Utils.isNullOrEmpty(oOwnerUser.getUserId())) {
			return null;
		}

		if (m_oWorkspacePolicy.validWorkspaceId(sWorkspaceId) == false) {
			return null;
		}

		try {
			WorkspaceSharingRepository oWorkspaceSharingRepository = new WorkspaceSharingRepository();
			aoWorkspaceSharing = oWorkspaceSharingRepository.GetWorkspaceSharingByWorkspace(sWorkspaceId);
		} catch (Exception oEx) {
			Utils.debugLog("WorkspaceResource.getUsersSharedWorksace: " + oEx);
			return null;
		}

		return aoWorkspaceSharing;

	}

	@DELETE
	@Path("deleteworkspacesharing")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult deleteUserSharedWorkspace(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("sWorkspaceId") String sWorkspaceId, @QueryParam("sUserId") String sUserId) {

		Utils.debugLog("WorkspaceResource.deleteUserSharedWorkspace( " + sSessionId + ", " + sWorkspaceId + ", "
				+ sUserId + " )");
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);
		// Validate Session
		User oOwnerUser = Wasdi.GetUserFromSession(sSessionId);

		if (oOwnerUser == null) {
			oResult.setStringValue("Invalid user.");
			return oResult;
		}

		if (Utils.isNullOrEmpty(oOwnerUser.getUserId())) {
			oResult.setStringValue("Invalid user.");
			return oResult;
		}

		if (m_oWorkspacePolicy.validWorkspaceId(sWorkspaceId) == false) {
			oResult.setStringValue("Invalid workspace.");
			return oResult;
		}

		if (m_oCredentialPolicy.validUserId(sUserId) == false) {
			oResult.setStringValue("Invalid user.");
			return oResult;
		}
		try {
			WorkspaceSharingRepository oWorkspaceSharingRepository = new WorkspaceSharingRepository();

			oWorkspaceSharingRepository.DeleteByUserIdWorkspaceId(sUserId, sWorkspaceId);
		} catch (Exception oEx) {
			Utils.debugLog("WorkspaceResource.deleteUserSharedWorkspace: " + oEx);
			oResult.setStringValue("Error in delete proccess");
			return oResult;
		}

		oResult.setStringValue("Done");
		oResult.setBoolValue(true);
		return oResult;

	}

}
