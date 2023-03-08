package it.fadeout.rest.resources;

import static wasdi.shared.business.UserApplicationPermission.ADMIN_DASHBOARD;
import static wasdi.shared.business.UserApplicationPermission.ORGANIZATION_READ;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
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
import wasdi.shared.data.SubscriptionRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.data.UserResourcePermissionRepository;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.ErrorResponse;
import wasdi.shared.viewmodels.SuccessResponse;
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
	public Response getListByUser(@HeaderParam("x-session-token") String sSessionId) {

		WasdiLog.debugLog("OrganizationResource.getListByUser");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		// Domain Check
		if (oUser == null) {
			WasdiLog.debugLog("OrganizationResource.getListByUser: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(MSG_ERROR_INVALID_SESSION)).build();
		}

		try {
			
			List<OrganizationListViewModel> aoOrganizationLVM = new ArrayList<>();

			WasdiLog.debugLog("OrganizationResource.getListByUser: organizations for " + oUser.getUserId());


			// Get the list of Organizations owned by the user

			List<Organization> aoOwnedOrganizations = getOrganizationsOwnedByUser(oUser.getUserId());

			List<OrganizationListViewModel> aoOwnedOrganizationLVM = aoOwnedOrganizations.stream()
					.map(t -> convert(t, oUser.getUserId()))
					.collect(Collectors.toList());

			aoOrganizationLVM.addAll(aoOwnedOrganizationLVM);


			// Get the list of Organizations shared with this user

			List<Organization> aoSharedOrganizations = getOrganizationsSharedWithUser(oUser.getUserId());

			List<OrganizationListViewModel> aoSharedOrganizationLVM = aoSharedOrganizations.stream()
					.map(t -> convert(t, oUser.getUserId()))
					.collect(Collectors.toList());

			aoOrganizationLVM.addAll(aoSharedOrganizationLVM);

			return Response.ok(aoOrganizationLVM).build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("OrganizationResource.getListByUser: " + oEx);
			return Response.serverError().build();
		}
	}

	/**
	 * Get an organization by its Id.
	 * @param sSessionId User Session Id
	 * @param sOrganizationId the organization Id
	 * @return the full view model of the organization
	 */
	@GET
	@Path("/byId")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response getOrganizationViewModel(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("organization") String sOrganizationId) {
		WasdiLog.debugLog("OrganizationResource.getOrganizationViewModel( Organization: " + sOrganizationId + ")");

		OrganizationViewModel oVM = new OrganizationViewModel();

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.debugLog("OrganizationResource.getOrganizationViewModel: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(MSG_ERROR_INVALID_SESSION)).build();
		}

		try {
			// Domain Check
			if (Utils.isNullOrEmpty(sOrganizationId)) {
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("Invalid organizationId.")).build();
			}

			if (!PermissionsUtils.canUserAccessOrganization(oUser.getUserId(), sOrganizationId)) {
				WasdiLog.debugLog("OrganizationResource.getOrganizationViewModel: user cannot access organization info, aborting");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("The user cannot access the organization info.")).build();
			}

			WasdiLog.debugLog("OrganizationResource.getOrganizationViewModel: read organizations " + sOrganizationId);

			// Create repo
			OrganizationRepository oOrganizationRepository = new OrganizationRepository();
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();

			// Get requested organization
			Organization oOrganization = oOrganizationRepository.getOrganizationById(sOrganizationId);

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

			return Response.ok(oVM).build();
		} catch (Exception oEx) {
			WasdiLog.errorLog( "OrganizationResource.getOrganizationViewModel: " + oEx);
			return Response.serverError().build();
		}
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
	public Response createOrganization(@HeaderParam("x-session-token") String sSessionId, OrganizationEditorViewModel oOrganizationEditorViewModel) {
		WasdiLog.debugLog("OrganizationResource.createOrganization");
		
		if (oOrganizationEditorViewModel == null) {
			WasdiLog.debugLog("OrganizationResource.createOrganization: null body");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(MSG_ERROR_INVALID_ORGANIZATION)).build();			
		}

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.debugLog("OrganizationResource.createOrganization: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(MSG_ERROR_INVALID_SESSION)).build();
		}

		OrganizationRepository oOrganizationRepository = new OrganizationRepository();

		Organization oExistingOrganization = oOrganizationRepository.getByName(oOrganizationEditorViewModel.getName());

		if (oExistingOrganization != null) {
			WasdiLog.debugLog("OrganizationResource.createOrganization: a different organization with the same name already exists");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("An organization with the same name already exists.")).build();
		}

		Organization oOrganization = convert(oOrganizationEditorViewModel);
		oOrganization.setUserId(oUser.getUserId());
		oOrganization.setOrganizationId(Utils.getRandomName());

		if (oOrganizationRepository.insertOrganization(oOrganization)) {
			return Response.ok(new SuccessResponse(oOrganization.getOrganizationId())).build();
		} else {
			WasdiLog.debugLog("OrganizationResource.createOrganization( " + oOrganizationEditorViewModel.getName() + " ): insertion failed");
			return Response.serverError().build();
		}
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
	public Response upateOrganization(@HeaderParam("x-session-token") String sSessionId, OrganizationEditorViewModel oOrganizationEditorViewModel) {
		WasdiLog.debugLog("OrganizationResource.updateOrganization");
		
		if (oOrganizationEditorViewModel == null) {
			WasdiLog.debugLog("OrganizationResource.updateOrganization: body null");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(MSG_ERROR_INVALID_ORGANIZATION)).build();			
		}

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.debugLog("OrganizationResource.updateOrganization: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(MSG_ERROR_INVALID_SESSION)).build();
		}
		
		if (!PermissionsUtils.canUserAccessOrganization(oUser.getUserId(), oOrganizationEditorViewModel.getOrganizationId())) {
			WasdiLog.debugLog("OrganizationResource.updateOrganization: user cannot access the organization");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(MSG_ERROR_NO_ACCESS_RIGHTS_OBJECT_ORGANIZATION)).build();			
		}

		OrganizationRepository oOrganizationRepository = new OrganizationRepository();

		Organization oExistingOrganization = oOrganizationRepository.getById(oOrganizationEditorViewModel.getOrganizationId());

		if (oExistingOrganization == null) {
			WasdiLog.debugLog("OrganizationResource.updateOrganization: organization does not exist");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("No organization with the Id exists.")).build();
		}

		Organization oExistingOrganizationWithTheSameName = oOrganizationRepository.getByName(oOrganizationEditorViewModel.getName());

		if (oExistingOrganizationWithTheSameName != null
				&& !oExistingOrganizationWithTheSameName.getOrganizationId().equalsIgnoreCase(oExistingOrganization.getOrganizationId())) {
			WasdiLog.debugLog("OrganizationResource.updateOrganization: a different organization with the same name already exists");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("An organization with the same name already exists.")).build();
		}


		Organization oOrganization = convert(oOrganizationEditorViewModel);
		oOrganization.setUserId(oUser.getUserId());

		if (oOrganizationRepository.updateOrganization(oOrganization)) {
			return Response.ok(new SuccessResponse(oOrganization.getOrganizationId())).build();
		} else {
			WasdiLog.debugLog("OrganizationResource.updateOrganization( " + oOrganizationEditorViewModel.getName() + " ): update failed");
			return Response.serverError().build();
		}
	}

	@DELETE
	@Path("/delete")
	public Response deleteOrganization(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("organization") String sOrganizationId) {
		WasdiLog.debugLog("OrganizationResource.deleteOrganization( Organization: " + sOrganizationId + " )");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.debugLog("OrganizationResource.deleteOrganization: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(MSG_ERROR_INVALID_SESSION)).build();
		}

		if (Utils.isNullOrEmpty(sOrganizationId)) {
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("Invalid organizationId.")).build();
		}

		OrganizationRepository oOrganizationRepository = new OrganizationRepository();

		Organization oOrganization = oOrganizationRepository.getById(sOrganizationId);

		if (oOrganization == null) {
			WasdiLog.debugLog("OrganizationResource.deleteOrganization: organization does not exist");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("No organization with the name exists.")).build();
		}

		UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();

		String sOrganizationOwner = oOrganization.getUserId();

		if (!sOrganizationOwner.equals(oUser.getUserId())) {
			// The current uses is not the owner of the organization
			WasdiLog.debugLog("OrganizationResource.deleteOrganization: user " + oUser.getUserId() + " is not the owner [" + sOrganizationOwner + "]: delete the sharing, not the organization");
			oUserResourcePermissionRepository.deletePermissionsByUserIdAndOrganizationId(oUser.getUserId(), sOrganizationId);

			return Response.ok(new SuccessResponse(sOrganizationId)).build();
		}

		if (oUserResourcePermissionRepository.isOrganizationShared(sOrganizationId)) {
			WasdiLog.debugLog("OrganizationResource.deleteOrganization: the organization is shared with users");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("The organization cannot be removed as it has users. Before deleting the organization, please remove the users.")).build();
		}

		SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();

		if (oSubscriptionRepository.organizationHasSubscriptions(sOrganizationId)) {
			WasdiLog.debugLog("OrganizationResource.deleteOrganization: the organization shares subscriptions");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("The organization cannot be removed as it shares subscriptions. Before deleting the organization, please remove the sharings.")).build();
		}

		if (oOrganizationRepository.deleteOrganization(sOrganizationId)) {
			return Response.ok(new SuccessResponse(sOrganizationId)).build();
		} else {
			WasdiLog.debugLog("OrganizationResource.deleteOrganization( " + sOrganizationId + " ): deletion failed");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("The deletion of the organization failed.")).build();
		}
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
	public Response shareOrganization(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("organization") String sOrganizationId, @QueryParam("userId") String sDestinationUserId) {

		WasdiLog.debugLog("OrganizationResource.ShareOrganization( Organization: " + sOrganizationId + ", User: " + sDestinationUserId + " )");

		// Validate Session
		User oRequesterUser = Wasdi.getUserFromSession(sSessionId);
		if (oRequesterUser == null) {
			WasdiLog.debugLog("OrganizationResource.shareOrganization: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(MSG_ERROR_INVALID_SESSION)).build();
		}

		if (Utils.isNullOrEmpty(sOrganizationId)) {
			WasdiLog.debugLog("OrganizationResource.shareOrganization: invalid organization");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("Invalid organizationId.")).build();
		}
		
		// Check if the organization exists
		OrganizationRepository oOrganizationRepository = new OrganizationRepository();
		Organization oOrganization = oOrganizationRepository.getOrganizationById(sOrganizationId);
		
		if (oOrganization == null) {
			WasdiLog.debugLog("OrganizationResource.ShareOrganization: invalid organization");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(MSG_ERROR_INVALID_ORGANIZATION)).build();
		}

		// Can the user access this resource?
		if (!PermissionsUtils.canUserAccessOrganization(oRequesterUser.getUserId(), sOrganizationId)
				&& !UserApplicationRole.userHasRightsToAccessApplicationResource(oRequesterUser.getRole(), ADMIN_DASHBOARD)) {
			WasdiLog.debugLog("OrganizationResource.shareOrganization: " + sOrganizationId + " cannot be accessed by " + oRequesterUser.getUserId() + ", aborting");

			return Response.status(Status.FORBIDDEN).entity(new ErrorResponse(MSG_ERROR_NO_ACCESS_RIGHTS_OBJECT_ORGANIZATION)).build();
		}

		// Cannot Autoshare
		if (oRequesterUser.getUserId().equals(sDestinationUserId)) {
			if (UserApplicationRole.userHasRightsToAccessApplicationResource(oRequesterUser.getRole(), ADMIN_DASHBOARD)) {
				// A user that has Admin rights should be able to auto-share the resource.
			} else {
				WasdiLog.debugLog("OrganizationResource.shareOrganization: auto sharing not so smart");

				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(MSG_ERROR_SHARING_WITH_ONESELF)).build();
			}
		}

		// Cannot share with the owner
		if (sDestinationUserId.equals(oOrganization.getUserId())) {
			WasdiLog.debugLog("OrganizationResource.shareOrganization: sharing with the owner not so smart");

			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(MSG_ERROR_SHARING_WITH_OWNER)).build();
		}

		UserRepository oUserRepository = new UserRepository();
		User oDestinationUser = oUserRepository.getUser(sDestinationUserId);

		if (oDestinationUser == null) {
			//No. So it is neither the owner or a shared one
			WasdiLog.debugLog("OrganizationResource.shareOrganization: Destination user does not exists");

			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(MSG_ERROR_SHARING_WITH_NON_EXISTENT_USER)).build();
		}

		try {
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();

			if (!oUserResourcePermissionRepository.isOrganizationSharedWithUser(sDestinationUserId, sOrganizationId)) {
				UserResourcePermission oOrganizationSharing =
						new UserResourcePermission("organization", sOrganizationId, sDestinationUserId, oOrganization.getUserId(), oRequesterUser.getUserId(), "write");

				oUserResourcePermissionRepository.insertPermission(oOrganizationSharing);

				sendNotificationEmail(oRequesterUser.getUserId(), sDestinationUserId, oOrganization.getName());

				return Response.ok(new SuccessResponse("Done")).build();
			} else {
				WasdiLog.debugLog("OrganizationResource.shareOrganization: already shared!");
				return Response.ok(new SuccessResponse("Already Shared.")).build();
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("OrganizationResource.shareOrganization: " + oEx);

			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(MSG_ERROR_IN_INSERT_PROCESS)).build();
		}
	}

	private static void sendNotificationEmail(String sRequesterUserId, String sDestinationUserId, String sOrganizationName) {
		try {
			String sMercuriusAPIAddress = WasdiConfig.Current.notifications.mercuriusAPIAddress;

			if(Utils.isNullOrEmpty(sMercuriusAPIAddress)) {
				WasdiLog.debugLog("OrganizationResource.sendNotificationEmail: sMercuriusAPIAddress is null");
			}
			else {

				WasdiLog.debugLog("OrganizationResource.sendNotificationEmail: send notification");

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
				
				WasdiLog.debugLog("OrganizationResource.sendNotificationEmail: notification sent with result " + iPositiveSucceded);
			}

		}
		catch (Exception oEx) {
			WasdiLog.errorLog("OrganizationResource.sendNotificationEmail: notification exception " + oEx.toString());
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
	public Response getEnableUsersSharedOrganization(@HeaderParam("x-session-token") String sSessionId, @QueryParam("organization") String sOrganizationId) {

		WasdiLog.debugLog("OrganizationResource.getEnableUsersSharedOrganization( Organization: " + sOrganizationId + " )");

	
		List<UserResourcePermission> aoOrganizationSharing = null;
		List<OrganizationSharingViewModel> aoOrganizationSharingViewModels = new ArrayList<>();

		User oOwnerUser = Wasdi.getUserFromSession(sSessionId);
		if (oOwnerUser == null) {
			WasdiLog.debugLog("OrganizationResource.getEnableUsersSharedOrganization: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(MSG_ERROR_INVALID_SESSION)).build();
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

			return Response.ok(aoOrganizationSharingViewModels).build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("OrganizationResource.getEnableUsersSharedOrganization: " + oEx);
			return Response.serverError().build();
		}

	}

	@DELETE
	@Path("share/delete")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response deleteUserSharedOrganization(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("organization") String sOrganizationId, @QueryParam("userId") String sUserId) {

		WasdiLog.debugLog("OrganizationResource.deleteUserSharedOrganization( Organization: " + sOrganizationId + ", User:" + sUserId + " )");

		// Validate Session
		User oRequestingUser = Wasdi.getUserFromSession(sSessionId);

		if (oRequestingUser == null) {
			WasdiLog.debugLog("OrganizationResource.deleteUserSharedOrganization: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(MSG_ERROR_INVALID_SESSION)).build();
		}

		if (Utils.isNullOrEmpty(sOrganizationId)) {
			WasdiLog.debugLog("OrganizationResource.deleteUserSharedOrganization: invalid organization id");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("Invalid organizationId.")).build();
		}

		try {

			UserRepository oUserRepository = new UserRepository();
			User oDestinationUser = oUserRepository.getUser(sUserId);

			if (oDestinationUser == null) {
				WasdiLog.debugLog("OrganizationResource.deleteUserSharedOrganization: invalid destination user");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(MSG_ERROR_INVALID_DESTINATION_USER)).build();
			}

			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			oUserResourcePermissionRepository.deletePermissionsByUserIdAndOrganizationId(sUserId, sOrganizationId);

			return Response.ok(new SuccessResponse("Done")).build();
		} 
		catch (Exception oEx) {
			WasdiLog.errorLog("OrganizationResource.deleteUserSharedOrganization: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(MSG_ERROR_IN_DELETE_PROCESS)).build();
		}
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

	public Set<String> getIdsOfOrganizationsOwnedByOrSharedWithUser(String sUserId) {
		Set<String> aoOrganizationIds = new HashSet<>();

		aoOrganizationIds.addAll(getIdsOfOrganizationsOwnedByUser(sUserId));
		aoOrganizationIds.addAll(getIdsOfOrganizationsSharedWithUser(sUserId));

		return aoOrganizationIds;
	}

	private List<Organization> getOrganizationsOwnedByUser(String sUserId) {
		OrganizationRepository oOrganizationRepository = new OrganizationRepository();

		return oOrganizationRepository.getOrganizationsByUser(sUserId);
	}

	private Set<String> getIdsOfOrganizationsOwnedByUser(String sUserId) {
		return getOrganizationsOwnedByUser(sUserId).stream().map(Organization::getOrganizationId).collect(Collectors.toSet());
	}

	private List<Organization> getOrganizationsSharedWithUser(String sUserId) {
		Set<String> aoOrganizationIds = getIdsOfOrganizationsSharedWithUser(sUserId);

		if (aoOrganizationIds.isEmpty()) {
			return Collections.emptyList();
		}

		OrganizationRepository oOrganizationRepository = new OrganizationRepository();

		return oOrganizationRepository.getOrganizations(aoOrganizationIds);
	}

	private Set<String> getIdsOfOrganizationsSharedWithUser(String sUserId) {
		UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
		List<UserResourcePermission> aoSharings = oUserResourcePermissionRepository.getOrganizationSharingsByUserId(sUserId);

		return aoSharings.stream().map(UserResourcePermission::getResourceId).collect(Collectors.toSet());
	}	
}
