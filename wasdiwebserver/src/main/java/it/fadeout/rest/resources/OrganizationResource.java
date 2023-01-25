package it.fadeout.rest.resources;

import static wasdi.shared.business.UserApplicationPermission.ADMIN_DASHBOARD;
import static wasdi.shared.business.UserApplicationPermission.ORGANIZATION_READ;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response.Status;

import it.fadeout.Wasdi;
import it.fadeout.mercurius.business.Message;
import it.fadeout.mercurius.client.MercuriusAPI;
import wasdi.shared.business.Organization;
import wasdi.shared.business.User;
import wasdi.shared.business.UserApplicationRole;
import wasdi.shared.business.UserResourcePermission;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.OrganizationRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.data.UserResourcePermissionRepository;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.organizations.OrganizationEditorViewModel;
import wasdi.shared.viewmodels.organizations.OrganizationListViewModel;
import wasdi.shared.viewmodels.organizations.OrganizationViewModel;
import wasdi.shared.viewmodels.organizations.OrganizationSharingViewModel;

@Path("/organizations")
public class OrganizationResource {

	private static final String MSG_ERROR_INVALID_SESSION = "MSG_ERROR_INVALID_SESSION";

	private static final String MSG_ERROR_NO_ACCESS_RIGHTS_APPLICATION_RESOURCE_ORGANIZATION = "MSG_ERROR_NO_ACCESS_RIGHTS_APPLICATION_RESOURCE_ORGANIZATION";
	private static final String MSG_ERROR_NO_ACCESS_RIGHTS_OBJECT_ORGANIZATION = "MSG_ERROR_NO_ACCESS_RIGHTS_OBJECT_ORGANIZATION";

	private static final String MSG_ERROR_SHARING_WITH_OWNER = "MSG_ERROR_SHARING_WITH_OWNER";
	private static final String MSG_ERROR_SHARING_WITH_ONESELF = "MSG_ERROR_SHARING_WITH_ONESELF";
	private static final String MSG_ERROR_SHARING_WITH_NON_EXISTENT_USER = "MSG_ERROR_SHARING_WITH_NON_EXISTENT_USER";

	private static final String MSG_ERROR_INVALID_ORGANIZATION = "MSG_ERROR_INVALID_ORGANIZATION";
	private static final String MSG_ERROR_INVALID_DESTINATION_USER = "MSG_ERROR_INVALID_DESTINATION_USER";
	private static final String MSG_ERROR_IN_DELETE_PROCESS = "MSG_ERROR_IN_DELETE_PROCESS";
	private static final String MSG_ERROR_IN_INSERT_PROCESS = "MSG_ERROR_IN_INSERT_PROCESS";

	/**
	 * Get the list of organizations associated to a user.
	 * @param sSessionId User Session Id
	 * @return a View Model with the Organization Name and 
	 * 	a flag to know if the user is admin or not of the organization
	 */
	@GET
	@Path("/byuser")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public List<OrganizationListViewModel> getListByUser(@HeaderParam("x-session-token") String sSessionId) {

		WasdiLog.debugLog("OrganizationResource.getListByUser()");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		List<OrganizationListViewModel> aoOrganizationList = new ArrayList<>();

		// Domain Check
		if (oUser == null) {
			WasdiLog.debugLog("OrganizationResource.getListByUser: invalid session: " + sSessionId);
			return aoOrganizationList;
		}

		try {
			if (!UserApplicationRole.userHasRightsToAccessApplicationResource(oUser.getRole(), ORGANIZATION_READ)) {
				return aoOrganizationList;
			}

			WasdiLog.debugLog("OrganizationResource.getListByUser: organizations for " + oUser.getUserId());

			// Create repo
			OrganizationRepository oOrganizationRepository = new OrganizationRepository();
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();

			// Get Organization List
			List<Organization> aoOrganizations = oOrganizationRepository.getOrganizationByUser(oUser.getUserId());
//			List<Organization> aoOrganizations = oOrganizationRepository.getOrganizationsList();

			// For each
			for (Organization oOrganization : aoOrganizations) {
				// Create View Model
				OrganizationListViewModel oOrganizationViewModel = convert(oOrganization, oUser.getUserId());

//				// Get Sharings
//				List<UserResourcePermission> aoPermissions = oUserResourcePermissionRepository
//						.getOrganizationSharingsByOrganizationId(oOrganization.getOrganizationId());
//
//				// Add Sharings to View Model
//				if (aoPermissions != null) {
//					for (UserResourcePermission oUserResourcePermission : aoPermissions) {
//						if (oOrganizationViewModel.getSharedUsers() == null) {
//							oOrganizationViewModel.setSharedUsers(new ArrayList<String>());
//						}
//
//						oOrganizationViewModel.getSharedUsers().add(oUserResourcePermission.getUserId());
//					}
//				}

				aoOrganizationList.add(oOrganizationViewModel);
			}

			// Get the list of organizations shared with this user
			List<UserResourcePermission> aoSharedOrganizations = oUserResourcePermissionRepository.getOrganizationSharingsByUserId(oUser.getUserId());

			if (aoSharedOrganizations.size() > 0) {
				// For each
				for (UserResourcePermission oSharedOrganization : aoSharedOrganizations) {
					Organization oOrganization = oOrganizationRepository.getOrganization(oSharedOrganization.getResourceId());

					if (oOrganization == null) {
						WasdiLog.debugLog("OrganizationResource.getListByUser: Organization Shared not available " + oSharedOrganization.getResourceId());
						continue;
					}

					OrganizationListViewModel oOrganizationViewModel = convert(oOrganization, oUser.getUserId());

//					// Get Sharings
//					List<UserResourcePermission> aoSharings = oUserResourcePermissionRepository.getOrganizationSharingsByOrganizationId(oOrganization.getOrganizationId());
//
//					// Add Sharings to View Model
//					if (aoSharings != null) {
//						for (UserResourcePermission oSharing : aoSharings) {
//							if (oOrganizationViewModel.getSharedUsers() == null) {
//								oOrganizationViewModel.setSharedUsers(new ArrayList<String>());
//							}
//
//							oOrganizationViewModel.getSharedUsers().add(oSharing.getUserId());
//						}
//					}

					aoOrganizationList.add(oOrganizationViewModel);
				}
			}
		} catch (Exception oEx) {
			oEx.toString();
		}

		return aoOrganizationList;
	}

//	public List<OrganizationListViewModel> mockListByUser() {
//		List<OrganizationListViewModel> list = new ArrayList<>();
//
//		OrganizationListViewModel wasdi = new OrganizationListViewModel();
//		wasdi.setOrganizationId("org-1");
//		wasdi.setName("WASDI");
//		wasdi.setOwnerUserId("p.campanella@fadeout.it");
//		wasdi.setAdminRole(true);
//
//		list.add(wasdi);
//
//		OrganizationListViewModel fadeout = new OrganizationListViewModel();
//		fadeout.setOrganizationId("org-2");
//		fadeout.setName("Fadeout Software");
//		fadeout.setOwnerUserId("p.campanella@fadeout.it");
//		fadeout.setAdminRole(false);
//
//		list.add(fadeout);
//
//		return list;
//	}

	/**
	 * Get an organization by its Id.
	 * @param sSessionId User Session Id
	 * @param sOrganizationId the organization Id
	 * @return the full view model of the organization
	 */
	@GET
	@Path("/byId")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public OrganizationViewModel getOrganizationViewModel(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("organization") String sOrganizationId) {
		WasdiLog.debugLog("OrganizationResource.getOrganizationViewModel( Organization: " + sOrganizationId + ")");

		OrganizationViewModel oVM = new OrganizationViewModel();

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.debugLog("OrganizationResource.getOrganizationViewModel: invalid session");
			return null;
		}

		try {
			// Domain Check
			if (Utils.isNullOrEmpty(sOrganizationId)) {
				return oVM;
			}

			if (!PermissionsUtils.canUserAccessOrganization(oUser.getUserId(), sOrganizationId)) {
				WasdiLog.debugLog("OrganizationResource.getOrganizationViewModel: user cannot access organization info, aborting");
				return oVM;
			}

			WasdiLog.debugLog("OrganizationResource.getOrganizationViewModel: read organizations " + sOrganizationId);

			// Create repo
			OrganizationRepository oOrganizationRepository = new OrganizationRepository();
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();

			// Get requested organization
			Organization oOrganization = oOrganizationRepository.getOrganization(sOrganizationId);

			oVM = convert(oOrganization);

			// Get Sharings
			List<UserResourcePermission> aoSharings = oUserResourcePermissionRepository
					.getOrganizationSharingsByOrganizationId(oOrganization.getOrganizationId());
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
			WasdiLog.debugLog( "OrganizationResource.getOrganizationViewModel: " + oEx);
		}

		return oVM;
	}

	/**
	 * Create a new Organization.
	 * @param sSessionId User Session Id
	 * @param oOrganizationViewModel the organization to be created
	 * @return a primitive result containing the outcome of the operation
	 */
	@POST
	@Path("/add")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult createOrganization(@HeaderParam("x-session-token") String sSessionId, OrganizationEditorViewModel oOrganizationEditorViewModel) {
		WasdiLog.debugLog("OrganizationResource.createOrganization( Organization: " + oOrganizationEditorViewModel.toString() + ")");

		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.debugLog("OrganizationResource.createOrganization: invalid session");
			oResult.setStringValue("Invalid session.");
			return oResult;
		}

		OrganizationRepository oOrganizationRepository = new OrganizationRepository();

		Organization oExistingOrganization = oOrganizationRepository.getByName(oOrganizationEditorViewModel.getName());

		if (oExistingOrganization != null) {
			WasdiLog.debugLog("OrganizationResource.createOrganization: a different organization with the same name already exists");
			oResult.setStringValue("An organization with the same name already exists.");
			return oResult;
		}

		Organization oOrganization = convert(oOrganizationEditorViewModel);
		oOrganization.setUserId(oUser.getUserId());
		oOrganization.setOrganizationId(Utils.getRandomName());

		if (oOrganizationRepository.insertOrganization(oOrganization)) {
			oResult.setBoolValue(true);
			oResult.setStringValue(oOrganization.getOrganizationId());
		} else {WasdiLog.debugLog("OrganizationResource.createOrganization( " + oOrganizationEditorViewModel.getName() + " ): insertion failed");
			oResult.setStringValue("The creation of the organization failed.");
		}

		return oResult;
	}

	/**
	 * Update an Organization.
	 * @param sSessionId User Session Id
	 * @param oOrganizationViewModel the organization to be updated
	 * @return a primitive result containing the outcome of the operation
	 */
	@PUT
	@Path("/update")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult upateOrganization(@HeaderParam("x-session-token") String sSessionId, OrganizationEditorViewModel oOrganizationEditorViewModel) {
		WasdiLog.debugLog("OrganizationResource.updateOrganization( Organization: " + oOrganizationEditorViewModel.toString() + ")");

		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.debugLog("OrganizationResource.updateOrganization: invalid session");
			oResult.setStringValue("Invalid session.");
			return oResult;
		}

		OrganizationRepository oOrganizationRepository = new OrganizationRepository();

		Organization oExistingOrganization = oOrganizationRepository.getById(oOrganizationEditorViewModel.getOrganizationId());

		if (oExistingOrganization == null) {
			WasdiLog.debugLog("OrganizationResource.updateOrganization: organization does not exist");
			oResult.setStringValue("No organization with the Id exists.");
			return oResult;
		}

		Organization oExistingOrganizationWithTheSameName = oOrganizationRepository.getByName(oOrganizationEditorViewModel.getName());

		if (oExistingOrganizationWithTheSameName != null
				&& !oExistingOrganizationWithTheSameName.getOrganizationId().equalsIgnoreCase(oExistingOrganization.getOrganizationId())) {
			WasdiLog.debugLog("OrganizationResource.updateOrganization: a different organization with the same name already exists");
			oResult.setStringValue("An organization with the same name already exists.");
			return oResult;
		}



		Organization oOrganization = convert(oOrganizationEditorViewModel);
		oOrganization.setUserId(oUser.getUserId());

		if (oOrganizationRepository.updateOrganization(oOrganization)) {
			oResult.setBoolValue(true);
			oResult.setStringValue(oOrganization.getOrganizationId());
		} else {
			WasdiLog.debugLog("OrganizationResource.updateOrganization( " + oOrganizationEditorViewModel.getName() + " ): update failed");
			oResult.setStringValue("The update of the organization failed.");
		}

		return oResult;
	}

	@DELETE
	@Path("/delete")
	public PrimitiveResult deleteOrganization(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("organization") String sOrganizationId) {
		WasdiLog.debugLog("OrganizationResource.deleteOrganization( Organization: " + sOrganizationId + " )");

		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.debugLog("OrganizationResource.deleteOrganization: invalid session");
			oResult.setStringValue("Invalid session.");
			return oResult;
		}

		OrganizationRepository oOrganizationRepository = new OrganizationRepository();

		Organization oOrganization = oOrganizationRepository.getById(sOrganizationId);

		if (oOrganization == null) {
			WasdiLog.debugLog("OrganizationResource.deleteOrganization: organization does not exist");
			oResult.setStringValue("No organization with the name already exists.");
			return oResult;
		}

		String sOrganizationOwner = oOrganization.getUserId();

		if (!sOrganizationOwner.equals(oUser.getUserId())) {
			// The current uses is not the owner of the organization
			WasdiLog.debugLog("OrganizationResource.deleteOrganization: user " + oUser.getUserId() + " is not the owner [" + sOrganizationOwner + "]: delete the sharing, not the organization");
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			oUserResourcePermissionRepository.deletePermissionsByUserIdAndOrganizationId(oUser.getUserId(), sOrganizationId);

			oResult.setBoolValue(true);
			oResult.setStringValue(sOrganizationId);

			return oResult;
		}

		if (oOrganizationRepository.deleteOrganization(sOrganizationId)) {
			oResult.setBoolValue(true);
			oResult.setStringValue(sOrganizationId);
		} else {
			WasdiLog.debugLog("OrganizationResource.deleteOrganization( " + sOrganizationId + " ): deletion failed");
			oResult.setStringValue("The deletion of the organization failed.");
		}

		return oResult;
	}

	/**
	 * Share a organization with another user.
	 *
	 * @param sSessionId User Session Id
	 * @param sOrganizationId Organization Id
	 * @param sDestinationUserId User id that will receive the organization in sharing.
	 * @return Primitive Result with boolValue = true and stringValue = Done if ok. False and error description otherwise
	 */
	@POST
	@Path("share/add")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult shareOrganization(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("organization") String sOrganizationId, @QueryParam("userId") String sDestinationUserId) {

		WasdiLog.debugLog("OrganizationResource.ShareOrganization( Organization: " + sOrganizationId + ", User: " + sDestinationUserId + " )");

		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);

		// Validate Session
		User oRequesterUser = Wasdi.getUserFromSession(sSessionId);
		if (oRequesterUser == null) {
			WasdiLog.debugLog("OrganizationResource.shareOrganization: invalid session");

			oResult.setIntValue(Status.UNAUTHORIZED.getStatusCode());
			oResult.setStringValue(MSG_ERROR_INVALID_SESSION);

			return oResult;
		}
		
		// Check if the organization exists
		OrganizationRepository oOrganizationRepository = new OrganizationRepository();
		Organization oOrganization = oOrganizationRepository.getOrganization(sOrganizationId);
		
		if (oOrganization == null) {
			WasdiLog.debugLog("OrganizationResource.ShareOrganization: invalid organization");

			oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
			oResult.setStringValue(MSG_ERROR_INVALID_ORGANIZATION);

			return oResult;
		}

		// Can the user access this section?
		if (!UserApplicationRole.userHasRightsToAccessApplicationResource(oRequesterUser.getRole(), ORGANIZATION_READ)) {
			WasdiLog.debugLog("OrganizationResource.shareOrganization: " + oRequesterUser.getUserId() + " cannot access the section " + ", aborting");

			oResult.setIntValue(Status.FORBIDDEN.getStatusCode());
			oResult.setStringValue(MSG_ERROR_NO_ACCESS_RIGHTS_APPLICATION_RESOURCE_ORGANIZATION);

			return oResult;
		}

		// Can the user access this resource?
		if (!PermissionsUtils.canUserAccessOrganization(oRequesterUser.getUserId(), sOrganizationId)
				&& !UserApplicationRole.userHasRightsToAccessApplicationResource(oRequesterUser.getRole(), ADMIN_DASHBOARD)) {
			WasdiLog.debugLog("OrganizationResource.shareOrganization: " + sOrganizationId + " cannot be accessed by " + oRequesterUser.getUserId() + ", aborting");

			oResult.setIntValue(Status.FORBIDDEN.getStatusCode());
			oResult.setStringValue(MSG_ERROR_NO_ACCESS_RIGHTS_OBJECT_ORGANIZATION);

			return oResult;
		}

		// Cannot Autoshare
		if (oRequesterUser.getUserId().equals(sDestinationUserId)) {
			if (UserApplicationRole.userHasRightsToAccessApplicationResource(oRequesterUser.getRole(), ADMIN_DASHBOARD)) {
				// A user that has Admin rights should be able to auto-share the resource.
			} else {
				WasdiLog.debugLog("OrganizationResource.shareOrganization: auto sharing not so smart");

				oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
				oResult.setStringValue(MSG_ERROR_SHARING_WITH_ONESELF);

				return oResult;
			}
		}

		// Cannot share with the owner
		if (sDestinationUserId.equals(oOrganization.getUserId())) {
			WasdiLog.debugLog("OrganizationResource.shareOrganization: sharing with the owner not so smart");

			oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
			oResult.setStringValue(MSG_ERROR_SHARING_WITH_OWNER);

			return oResult;
		}

		UserRepository oUserRepository = new UserRepository();
		User oDestinationUser = oUserRepository.getUser(sDestinationUserId);

		if (oDestinationUser == null) {
			//No. So it is neither the owner or a shared one
			WasdiLog.debugLog("OrganizationResource.shareOrganization: Destination user does not exists");

			oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
			oResult.setStringValue(MSG_ERROR_SHARING_WITH_NON_EXISTENT_USER);

			return oResult;
		}

		try {
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();

			if (!oUserResourcePermissionRepository.isOrganizationSharedWithUser(sDestinationUserId, sOrganizationId)) {
				UserResourcePermission oOrganizationSharing =
						new UserResourcePermission("organization", sOrganizationId, sDestinationUserId, oOrganization.getUserId(), oRequesterUser.getUserId(), "write");

				oUserResourcePermissionRepository.insertPermission(oOrganizationSharing);				
			} else {
				WasdiLog.debugLog("OrganizationResource.shareOrganization: already shared!");
				oResult.setStringValue("Already Shared.");
				oResult.setBoolValue(true);
				oResult.setIntValue(Status.OK.getStatusCode());

				return oResult;
			}
		} catch (Exception oEx) {
			WasdiLog.debugLog("OrganizationResource.shareOrganization: " + oEx);

			oResult.setIntValue(Status.INTERNAL_SERVER_ERROR.getStatusCode());
			oResult.setStringValue(MSG_ERROR_IN_INSERT_PROCESS);

			return oResult;
		}

		//TODO - uncomment the line below
		// sendNotificationEmail(oRequesterUser.getUserId(), sDestinationUserId, oOrganization.getName());

		oResult.setStringValue("Done");
		oResult.setBoolValue(true);

		return oResult;
	}

	private static void sendNotificationEmail(String sRequesterUserId, String sDestinationUserId, String sOrganizationName) {
		try {
			String sMercuriusAPIAddress = WasdiConfig.Current.notifications.mercuriusAPIAddress;

			if(Utils.isNullOrEmpty(sMercuriusAPIAddress)) {
				WasdiLog.debugLog("OrganizationResource.shareOrganization: sMercuriusAPIAddress is null");
			}
			else {

				WasdiLog.debugLog("OrganizationResource.shareOrganization: send notification");

				MercuriusAPI oAPI = new MercuriusAPI(sMercuriusAPIAddress);			
				Message oMessage = new Message();

				String sTitle = "Organization " + sOrganizationName + " Shared";

				oMessage.setTilte(sTitle);

				String sSender = WasdiConfig.Current.notifications.sftpManagementMailSender;
				if (sSender==null) {
					sSender = "wasdi@wasdi.net";
				}

				oMessage.setSender(sSender);

				String sMessage = "The user " + sRequesterUserId + " shared with you the organization: " + sOrganizationName;

				oMessage.setMessage(sMessage);

				Integer iPositiveSucceded = 0;

				iPositiveSucceded = oAPI.sendMailDirect(sDestinationUserId, oMessage);
				
				WasdiLog.debugLog("OrganizationResource.ShareOrganization: notification sent with result " + iPositiveSucceded);
			}

		}
		catch (Exception oEx) {
			WasdiLog.debugLog("OrganizationResource.shareOrganization: notification exception " + oEx.toString());
		}
	}

	/**
	 * Get the list of users that have an Organization in sharing.
	 *
	 * @param sSessionId User Session
	 * @param sOrganizationId Organization Id
	 * @return list of Organization Sharing View Models
	 */
	@GET
	@Path("share/byorganization")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public List<OrganizationSharingViewModel> getEnableUsersSharedWorksace(@HeaderParam("x-session-token") String sSessionId, @QueryParam("organization") String sOrganizationId) {

		WasdiLog.debugLog("OrganizationResource.getEnableUsersSharedWorksace( Organization: " + sOrganizationId + " )");

	
		List<UserResourcePermission> aoOrganizationSharing = null;
		List<OrganizationSharingViewModel> aoOrganizationSharingViewModels = new ArrayList<>();

		User oOwnerUser = Wasdi.getUserFromSession(sSessionId);
		if (oOwnerUser == null) {
			WasdiLog.debugLog("OrganizationResource.getEnableUsersSharedWorksace: invalid session");
			return aoOrganizationSharingViewModels;
		}

		try {
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			aoOrganizationSharing = oUserResourcePermissionRepository.getOrganizationSharingsByOrganizationId(sOrganizationId);

			if (aoOrganizationSharing != null) {
				for (UserResourcePermission oOrganizationSharing : aoOrganizationSharing) {
					OrganizationSharingViewModel oOrganizationSharingViewModel = new OrganizationSharingViewModel();
					oOrganizationSharingViewModel.setOwnerId(oOrganizationSharing.getUserId());
					oOrganizationSharingViewModel.setUserId(oOrganizationSharing.getUserId());
					oOrganizationSharingViewModel.setOrganizationId(oOrganizationSharing.getResourceId());

					aoOrganizationSharingViewModels.add(oOrganizationSharingViewModel);
				}

			}

		} catch (Exception oEx) {
			WasdiLog.debugLog("OrganizationResource.getEnableUsersSharedWorksace: " + oEx);
			return aoOrganizationSharingViewModels;
		}

		return aoOrganizationSharingViewModels;

	}

	@DELETE
	@Path("share/delete")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult deleteUserSharedOrganization(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("organization") String sOrganizationId, @QueryParam("userId") String sUserId) {

		WasdiLog.debugLog("OrganizationResource.deleteUserSharedOrganization( Organization: " + sOrganizationId + ", User:" + sUserId + " )");
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);

		// Validate Session
		User oRequestingUser = Wasdi.getUserFromSession(sSessionId);

		if (oRequestingUser == null) {
			WasdiLog.debugLog("OrganizationResource.deleteUserSharedOrganization: invalid session");

			oResult.setIntValue(Status.UNAUTHORIZED.getStatusCode());
			oResult.setStringValue(MSG_ERROR_INVALID_SESSION);

			return oResult;
		}

		try {

			UserRepository oUserRepository = new UserRepository();
			User oDestinationUser = oUserRepository.getUser(sUserId);

			if (oDestinationUser == null) {
				WasdiLog.debugLog("OrganizationResource.deleteUserSharedOrganization: invalid destination user");

				oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
				oResult.setStringValue(MSG_ERROR_INVALID_DESTINATION_USER);

				return oResult;
			}

			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			oUserResourcePermissionRepository.deletePermissionsByUserIdAndOrganizationId(sUserId, sOrganizationId);
		} catch (Exception oEx) {
			WasdiLog.debugLog("OrganizationResource.deleteUserSharedOrganization: " + oEx);

			oResult.setIntValue(Status.INTERNAL_SERVER_ERROR.getStatusCode());
			oResult.setStringValue(MSG_ERROR_IN_DELETE_PROCESS);

			return oResult;
		}

		oResult.setStringValue("Done");
		oResult.setBoolValue(true);
		oResult.setIntValue(Status.OK.getStatusCode());

		return oResult;
	}

	private static OrganizationViewModel convert(Organization oOrganization) {
		OrganizationViewModel oOrganizationViewModel = new OrganizationViewModel();
		oOrganizationViewModel.setOrganizationId(oOrganization.getOrganizationId());
		oOrganizationViewModel.setUserId(oOrganization.getUserId());
		oOrganizationViewModel.setName(oOrganization.getName());
		oOrganizationViewModel.setDescription(oOrganization.getDescription());
		oOrganizationViewModel.setAddress(oOrganization.getAddress());
		oOrganizationViewModel.setEmail(oOrganization.getEmail());
		oOrganizationViewModel.setUrl(oOrganization.getUrl());
//		oOrganizationViewModel.setlogo(oOrganization.getLogo());

		return oOrganizationViewModel;
	}

	private static OrganizationListViewModel convert(Organization oOrganization, String sCurrentUserId) {
		OrganizationListViewModel oOrganizationListViewModel = new OrganizationListViewModel();
		oOrganizationListViewModel.setOrganizationId(oOrganization.getOrganizationId());
		oOrganizationListViewModel.setName(oOrganization.getName());
		oOrganizationListViewModel.setOwnerUserId(oOrganization.getUserId());
		oOrganizationListViewModel.setAdminRole(sCurrentUserId.equalsIgnoreCase(oOrganization.getUserId()));

		return oOrganizationListViewModel;
	}

	private static Organization convert(OrganizationViewModel oOrganizationViewModel) {
		Organization oOrganization = new Organization();
		oOrganization.setOrganizationId(oOrganizationViewModel.getOrganizationId());
		oOrganization.setUserId(oOrganizationViewModel.getUserId());
		oOrganization.setName(oOrganizationViewModel.getName());
		oOrganization.setDescription(oOrganizationViewModel.getDescription());
		oOrganization.setAddress(oOrganizationViewModel.getAddress());
		oOrganization.setEmail(oOrganizationViewModel.getEmail());
		oOrganization.setUrl(oOrganizationViewModel.getUrl());
//		oOrganization.setlogo(oOrganizationViewModel.getLogo());

		return oOrganization;
	}

	private static Organization convert(OrganizationEditorViewModel oOrganizationEditorViewModel) {
		Organization oOrganization = new Organization();
		oOrganization.setOrganizationId(oOrganizationEditorViewModel.getOrganizationId());
//		oOrganization.setUserId(oOrganizationEditorViewModel.getUserId());
		oOrganization.setName(oOrganizationEditorViewModel.getName());
		oOrganization.setDescription(oOrganizationEditorViewModel.getDescription());
		oOrganization.setAddress(oOrganizationEditorViewModel.getAddress());
		oOrganization.setEmail(oOrganizationEditorViewModel.getEmail());
		oOrganization.setUrl(oOrganizationEditorViewModel.getUrl());
//		oOrganization.setlogo(oOrganizationEditorViewModel.getLogo());

		return oOrganization;
	}

}
