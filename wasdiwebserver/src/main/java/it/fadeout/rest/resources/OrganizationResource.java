package it.fadeout.rest.resources;

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
import wasdi.shared.business.Organization;
import wasdi.shared.business.Subscription;
import wasdi.shared.business.users.ResourceTypes;
import wasdi.shared.business.users.User;
import wasdi.shared.business.users.UserAccessRights;
import wasdi.shared.business.users.UserApplicationRole;
import wasdi.shared.business.users.UserResourcePermission;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.OrganizationRepository;
import wasdi.shared.data.SubscriptionRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.data.UserResourcePermissionRepository;
import wasdi.shared.utils.MailUtils;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.ClientMessageCodes;
import wasdi.shared.viewmodels.ErrorResponse;
import wasdi.shared.viewmodels.SuccessResponse;
import wasdi.shared.viewmodels.organizations.OrganizationEditorViewModel;
import wasdi.shared.viewmodels.organizations.OrganizationListViewModel;
import wasdi.shared.viewmodels.organizations.OrganizationSharingViewModel;
import wasdi.shared.viewmodels.organizations.OrganizationViewModel;

/**
 * Organizations Resource
 * 
 *  Hosts the API to let the users create and manage their own Organizations
 *  
 *  	.Create, Update and Delete Organizations
 *  	.Read Organizations
 *  	.Share organizations with other users
 * 
 * @author p.campanella
 *
 */
@Path("/organizations")
public class OrganizationResource {

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

		User oUser = Wasdi.getUserFromSession(sSessionId);

		// Domain Check
		if (oUser == null) {
			WasdiLog.warnLog("OrganizationResource.getListByUser: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}
		
		WasdiLog.debugLog("OrganizationResource.getListByUser: organizations for " + oUser.getUserId());

		try {
			
			List<OrganizationListViewModel> aoOrganizationLVM = new ArrayList<>();
			
			// Get the list of Organizations owned by the user
			List<Organization> aoOwnedOrganizations = getOrganizationsOwnedByUser(oUser.getUserId());

			List<OrganizationListViewModel> aoOwnedOrganizationLVM = aoOwnedOrganizations.stream()
					.map(oOrganization -> convert(oOrganization, oUser.getUserId()))
					.collect(Collectors.toList());
			
			for (OrganizationListViewModel oOwnedOrganization : aoOwnedOrganizationLVM) {
				oOwnedOrganization.setReadOnly(false);
			}

			aoOrganizationLVM.addAll(aoOwnedOrganizationLVM);


			// Get the list of Organizations shared with this user

			List<Organization> aoSharedOrganizations = getOrganizationsSharedWithUser(oUser.getUserId());

			List<OrganizationListViewModel> aoSharedOrganizationLVM = aoSharedOrganizations.stream()
					.map(t -> convert(t, oUser.getUserId()))
					.collect(Collectors.toList());
			
			for (OrganizationListViewModel oSharedOrganization : aoSharedOrganizationLVM) {
				oSharedOrganization.setReadOnly(!PermissionsUtils.canUserWriteOrganization(oUser.getUserId(), oSharedOrganization.getOrganizationId()));
			}			

			aoOrganizationLVM.addAll(aoSharedOrganizationLVM);

			return Response.ok(aoOrganizationLVM).build();
		} 
		catch (Exception oEx) {
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

		OrganizationViewModel oOrganizationViewModel = new OrganizationViewModel();

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.warnLog("OrganizationResource.getOrganizationViewModel: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		try {
			// Domain Check
			if (Utils.isNullOrEmpty(sOrganizationId)) {
				WasdiLog.warnLog("OrganizationResource.getOrganizationViewModel: organization id null or empty");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("Invalid organizationId.")).build();
			}

			if (!PermissionsUtils.canUserAccessOrganization(oUser.getUserId(), sOrganizationId)) {
				WasdiLog.warnLog("OrganizationResource.getOrganizationViewModel: user cannot access organization, aborting");
				return Response.status(Status.FORBIDDEN).entity(new ErrorResponse("The user cannot access the organization info.")).build();
			}

			WasdiLog.debugLog("OrganizationResource.getOrganizationViewModel: read organizations " + sOrganizationId);

			// Create repo
			OrganizationRepository oOrganizationRepository = new OrganizationRepository();
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();

			// Get requested organization
			Organization oOrganization = oOrganizationRepository.getById(sOrganizationId);

			oOrganizationViewModel = convert(oOrganization);
			
			oOrganizationViewModel.setReadOnly(!PermissionsUtils.canUserWriteOrganization(oUser.getUserId(), sOrganizationId));

			// Get Sharings
			List<UserResourcePermission> aoSharings = oUserResourcePermissionRepository.getOrganizationSharingsByOrganizationId(oOrganization.getOrganizationId());

			// Add Sharings to View Model
			if (aoSharings != null) {
				if (oOrganizationViewModel.getSharedUsers() == null) {
					oOrganizationViewModel.setSharedUsers(new ArrayList<String>());
				}

				for (UserResourcePermission oSharing : aoSharings) {
					oOrganizationViewModel.getSharedUsers().add(oSharing.getUserId());
				}
			}

			return Response.ok(oOrganizationViewModel).build();
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
			WasdiLog.warnLog("OrganizationResource.createOrganization: null body");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_ORGANIZATION.name())).build();			
		}

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.warnLog("OrganizationResource.createOrganization: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}
		
		try {
			OrganizationRepository oOrganizationRepository = new OrganizationRepository();

			Organization oExistingOrganization = oOrganizationRepository.getByName(oOrganizationEditorViewModel.getName());

			if (oExistingOrganization != null) {
				WasdiLog.warnLog("OrganizationResource.createOrganization: a different organization with the same name already exists");
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
		catch (Exception oEx) {
			WasdiLog.errorLog("OrganizationResource.createOrganization: " + oEx);
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
			WasdiLog.warnLog("OrganizationResource.updateOrganization: body null");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_ORGANIZATION.name())).build();			
		}

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.warnLog("OrganizationResource.updateOrganization: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}
		
		if (!PermissionsUtils.canUserWriteOrganization(oUser.getUserId(), oOrganizationEditorViewModel.getOrganizationId())) {
			WasdiLog.warnLog("OrganizationResource.updateOrganization: user cannot access the organization");
			return Response.status(Status.FORBIDDEN).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_NO_ACCESS_RIGHTS_OBJECT_ORGANIZATION.name())).build();			
		}

		OrganizationRepository oOrganizationRepository = new OrganizationRepository();

		Organization oExistingOrganization = oOrganizationRepository.getById(oOrganizationEditorViewModel.getOrganizationId());

		if (oExistingOrganization == null) {
			WasdiLog.warnLog("OrganizationResource.updateOrganization: organization does not exist");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("No organization with the Id exists.")).build();
		}

		Organization oExistingOrganizationWithTheSameName = oOrganizationRepository.getByName(oOrganizationEditorViewModel.getName());

		if (oExistingOrganizationWithTheSameName != null
				&& !oExistingOrganizationWithTheSameName.getOrganizationId().equalsIgnoreCase(oExistingOrganization.getOrganizationId())) {
			WasdiLog.warnLog("OrganizationResource.updateOrganization: a different organization with the same name already exists");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("An organization with the same name already exists.")).build();
		}


		Organization oOrganization = convert(oOrganizationEditorViewModel);
		oOrganization.setUserId(oUser.getUserId());

		if (oOrganizationRepository.updateOrganization(oOrganization)) {
			return Response.ok(new SuccessResponse(oOrganization.getOrganizationId())).build();
		} else {
			WasdiLog.warnLog("OrganizationResource.updateOrganization( " + oOrganizationEditorViewModel.getName() + " ): update failed");
			return Response.serverError().build();
		}
	}

	/**
	 * Deletes and organization
	 * @param sSessionId Session Id
	 * @param sOrganizationId Organization Id
	 * @return 
	 */
	@DELETE
	@Path("/delete")
	@Produces({"application/json", "application/xml", "text/xml" })
	public Response deleteOrganization(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("organization") String sOrganizationId) {
		WasdiLog.debugLog("OrganizationResource.deleteOrganization( Organization: " + sOrganizationId + " )");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.warnLog("OrganizationResource.deleteOrganization: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		if (Utils.isNullOrEmpty(sOrganizationId)) {
			WasdiLog.warnLog("OrganizationResource.deleteOrganization: sOrganizationId null or empty");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("Invalid organizationId.")).build();
		}
		
		try  {
			OrganizationRepository oOrganizationRepository = new OrganizationRepository();
			Organization oOrganization = oOrganizationRepository.getById(sOrganizationId);

			if (oOrganization == null) {
				WasdiLog.warnLog("OrganizationResource.deleteOrganization: organization does not exist");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("No organization with the name exists.")).build();
			}

			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();

			String sOrganizationOwner = oOrganization.getUserId();

			if (!sOrganizationOwner.equals(oUser.getUserId())) {
				// The current uses is not the owner of the organization
				WasdiLog.warnLog("OrganizationResource.deleteOrganization: user " + oUser.getUserId() + " is not the owner [" + sOrganizationOwner + "]: delete the sharing, not the organization");
				oUserResourcePermissionRepository.deletePermissionsByUserIdAndOrganizationId(oUser.getUserId(), sOrganizationId);

				return Response.ok(new SuccessResponse(sOrganizationId)).build();
			}
			
			WasdiLog.debugLog("OrganizationResource.deleteOrganization: cleaning permissions of the organization");
			oUserResourcePermissionRepository.deletePermissionsByOrganizationId(sOrganizationId);

			SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();
			List<Subscription> aoInvolvedSubscriptions = oSubscriptionRepository.getSubscriptionsByOrganization(sOrganizationId);
			
			if (aoInvolvedSubscriptions != null) {
				if (aoInvolvedSubscriptions.size()>0) WasdiLog.debugLog("OrganizationResource.deleteOrganization: cleaning subscriptions related to the deleting organization");
				for (Subscription oSubscription : aoInvolvedSubscriptions) {
					oSubscription.setOrganizationId("");
					oSubscriptionRepository.updateSubscription(oSubscription);
				}
			}

			if (oOrganizationRepository.deleteOrganization(sOrganizationId)) {
				return Response.ok(new SuccessResponse(sOrganizationId)).build();
			} else {
				WasdiLog.warnLog("OrganizationResource.deleteOrganization( " + sOrganizationId + " ): deletion failed");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("The deletion of the organization failed.")).build();
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("OrganizationResource.deleteOrganization error: " + oEx);
			return Response.serverError().build();
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
	public Response shareOrganization(@HeaderParam("x-session-token") String sSessionId, @QueryParam("organization") String sOrganizationId, @QueryParam("userId") String sDestinationUserId, @QueryParam("rights") String sRights) {

		WasdiLog.debugLog("OrganizationResource.ShareOrganization( Organization: " + sOrganizationId + ", User: " + sDestinationUserId + " )");

		// Validate Session
		User oRequesterUser = Wasdi.getUserFromSession(sSessionId);
		if (oRequesterUser == null) {
			WasdiLog.warnLog("OrganizationResource.shareOrganization: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}
		
		// Use Read By default
		if (!UserAccessRights.isValidAccessRight(sRights)) {
			sRights = UserAccessRights.READ.getAccessRight();
		}		

		if (Utils.isNullOrEmpty(sOrganizationId)) {
			WasdiLog.warnLog("OrganizationResource.shareOrganization: invalid organization");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("Invalid organizationId.")).build();
		}
		
		// Check if the organization exists
		OrganizationRepository oOrganizationRepository = new OrganizationRepository();
		Organization oOrganization = oOrganizationRepository.getById(sOrganizationId);
		
		if (oOrganization == null) {
			WasdiLog.warnLog("OrganizationResource.ShareOrganization: invalid organization");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_ORGANIZATION.name())).build();
		}

		// Can the user access this resource?
		if (!PermissionsUtils.canUserWriteOrganization(oRequesterUser.getUserId(), sOrganizationId)
				&& !UserApplicationRole.isAdmin(oRequesterUser)) {
			WasdiLog.warnLog("OrganizationResource.shareOrganization: " + sOrganizationId + " cannot be accessed by " + oRequesterUser.getUserId() + ", aborting");

			return Response.status(Status.FORBIDDEN).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_NO_ACCESS_RIGHTS_OBJECT_ORGANIZATION.name())).build();
		}

		// Cannot Autoshare
		if (oRequesterUser.getUserId().equals(sDestinationUserId)) {
			if (UserApplicationRole.isAdmin(oRequesterUser)) {
				// A user that has Admin rights should be able to auto-share the resource.
			} else {
				WasdiLog.warnLog("OrganizationResource.shareOrganization: auto sharing not so smart");

				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_SHARING_WITH_ONESELF.name())).build();
			}
		}

		// Cannot share with the owner
		if (sDestinationUserId.equals(oOrganization.getUserId())) {
			WasdiLog.warnLog("OrganizationResource.shareOrganization: sharing with the owner not so smart");

			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_SHARING_WITH_OWNER.name())).build();
		}

		UserRepository oUserRepository = new UserRepository();
		User oDestinationUser = oUserRepository.getUser(sDestinationUserId);

		if (oDestinationUser == null) {
			//No. So it is neither the owner or a shared one
			WasdiLog.warnLog("OrganizationResource.shareOrganization: Destination user does not exists");

			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_SHARING_WITH_NON_EXISTENT_USER.name())).build();
		}

		try {
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();

			if (!oUserResourcePermissionRepository.isOrganizationSharedWithUser(sDestinationUserId, sOrganizationId)) {
				UserResourcePermission oOrganizationSharing =
						new UserResourcePermission(ResourceTypes.ORGANIZATION.getResourceType(), sOrganizationId, sDestinationUserId, oOrganization.getUserId(), oRequesterUser.getUserId(), sRights);

				oUserResourcePermissionRepository.insertPermission(oOrganizationSharing);
				
				try {
					String sTitle = "Organization " +  oOrganization.getName() + " Shared";
					String sMessage = "The user " + oRequesterUser.getUserId() + " shared with you the organization: " + oOrganization.getName();
					MailUtils.sendEmail(WasdiConfig.Current.notifications.sftpManagementMailSender, sDestinationUserId, sTitle, sMessage);
				}
				catch (Exception oEx) {
					WasdiLog.errorLog("OrganizationResource.shareOrganization: notification exception " + oEx.toString());
				}				

				return Response.ok(new SuccessResponse("Done")).build();
			} else {
				WasdiLog.warnLog("OrganizationResource.shareOrganization: already shared!");
				return Response.ok(new SuccessResponse("Already Shared.")).build();
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("OrganizationResource.shareOrganization: " + oEx);

			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_IN_INSERT_PROCESS.name())).build();
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
	public Response getEnabledUsersSharedOrganization(@HeaderParam("x-session-token") String sSessionId, @QueryParam("organization") String sOrganizationId) {

		WasdiLog.debugLog("OrganizationResource.getEnabledUsersSharedOrganization( Organization: " + sOrganizationId + " )");

	
		List<UserResourcePermission> aoOrganizationSharing = null;
		List<OrganizationSharingViewModel> aoOrganizationSharingViewModels = new ArrayList<>();

		User oOwnerUser = Wasdi.getUserFromSession(sSessionId);
		if (oOwnerUser == null) {
			WasdiLog.warnLog("OrganizationResource.getEnableUsersSharedOrganization: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}
		
		if (Utils.isNullOrEmpty(sOrganizationId)) {
			WasdiLog.warnLog("OrganizationResource.getEnableUsersSharedOrganization: invalid organization");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("Invalid organizationId.")).build();
		}
		
		if (!PermissionsUtils.canUserAccessOrganization(oOwnerUser.getUserId(), sOrganizationId)) {
			WasdiLog.warnLog("OrganizationResource.getEnableUsersSharedOrganization: user cannot access organization");
			return Response.status(Status.FORBIDDEN).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();			
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
					oOrganizationSharingViewModel.setPermissions(oOrganizationSharing.getPermissions());
					

					aoOrganizationSharingViewModels.add(oOrganizationSharingViewModel);
				}
			}

			return Response.ok(aoOrganizationSharingViewModels).build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("OrganizationResource.getEnabledUsersSharedOrganization: " + oEx);
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
			WasdiLog.warnLog("OrganizationResource.deleteUserSharedOrganization: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		if (Utils.isNullOrEmpty(sOrganizationId)) {
			WasdiLog.warnLog("OrganizationResource.deleteUserSharedOrganization: invalid organization id");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("Invalid organizationId.")).build();
		}
		
		if (!PermissionsUtils.canUserWriteOrganization(oRequestingUser.getUserId(), sOrganizationId)) {
			WasdiLog.warnLog("OrganizationResource.getEnableUsersSharedOrganization: user cannot write organization");
			return Response.status(Status.FORBIDDEN).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();			
		}		

		try {

			UserRepository oUserRepository = new UserRepository();
			User oDestinationUser = oUserRepository.getUser(sUserId);

			if (oDestinationUser == null) {
				WasdiLog.warnLog("OrganizationResource.deleteUserSharedOrganization: invalid destination user");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_DESTINATION_USER.name())).build();
			}

			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			oUserResourcePermissionRepository.deletePermissionsByUserIdAndOrganizationId(sUserId, sOrganizationId);

			return Response.ok(new SuccessResponse("Done")).build();
		} 
		catch (Exception oEx) {
			WasdiLog.errorLog("OrganizationResource.deleteUserSharedOrganization: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_IN_DELETE_PROCESS.name())).build();
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

		return oOrganizationViewModel;
	}

	private static OrganizationListViewModel convert(Organization oOrganization, String sCurrentUserId) {
		OrganizationListViewModel oOrganizationListViewModel = new OrganizationListViewModel();
		oOrganizationListViewModel.setOrganizationId(oOrganization.getOrganizationId());
		oOrganizationListViewModel.setName(oOrganization.getName());
		oOrganizationListViewModel.setOwnerUserId(oOrganization.getUserId());

		return oOrganizationListViewModel;
	}

	private static Organization convert(OrganizationEditorViewModel oOrganizationEditorViewModel) {
		Organization oOrganization = new Organization();
		
		oOrganization.setOrganizationId(oOrganizationEditorViewModel.getOrganizationId());
		oOrganization.setName(oOrganizationEditorViewModel.getName());
		oOrganization.setDescription(oOrganizationEditorViewModel.getDescription());
		oOrganization.setAddress(oOrganizationEditorViewModel.getAddress());
		oOrganization.setEmail(oOrganizationEditorViewModel.getEmail());
		oOrganization.setUrl(oOrganizationEditorViewModel.getUrl());

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

		return oOrganizationRepository.getOrganizationsOwnedByUser(sUserId);
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
