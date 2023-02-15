package it.fadeout.rest.resources;

import static wasdi.shared.business.UserApplicationPermission.ADMIN_DASHBOARD;
import static wasdi.shared.business.UserApplicationPermission.SUBSCRIPTION_READ;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
import wasdi.shared.business.User;
import wasdi.shared.business.UserApplicationRole;
import wasdi.shared.business.UserResourcePermission;
import wasdi.shared.data.OrganizationRepository;
import wasdi.shared.data.SubscriptionRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.data.UserResourcePermissionRepository;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.TimeEpochUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.ErrorResponse;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.SuccessResponse;
import wasdi.shared.viewmodels.organizations.OrganizationListViewModel;
import wasdi.shared.viewmodels.organizations.ProjectEditorViewModel;
import wasdi.shared.viewmodels.organizations.SubscriptionListViewModel;
import wasdi.shared.viewmodels.organizations.SubscriptionSharingViewModel;
import wasdi.shared.viewmodels.organizations.SubscriptionType;
import wasdi.shared.viewmodels.organizations.SubscriptionTypeViewModel;
import wasdi.shared.viewmodels.organizations.SubscriptionViewModel;

@Path("/subscriptions")
public class SubscriptionResource {

	private static final String MSG_ERROR_INVALID_SESSION = "MSG_ERROR_INVALID_SESSION";

	private static final String MSG_ERROR_NO_ACCESS_RIGHTS_APPLICATION_RESOURCE_SUBSCRIPTION = "MSG_ERROR_NO_ACCESS_RIGHTS_APPLICATION_RESOURCE_SUBSCRIPTION";
	private static final String MSG_ERROR_NO_ACCESS_RIGHTS_OBJECT_SUBSCRIPTION = "MSG_ERROR_NO_ACCESS_RIGHTS_OBJECT_SUBSCRIPTION";

	private static final String MSG_ERROR_SHARING_WITH_OWNER = "MSG_ERROR_SHARING_WITH_OWNER";
	private static final String MSG_ERROR_SHARING_WITH_ONESELF = "MSG_ERROR_SHARING_WITH_ONESELF";
	private static final String MSG_ERROR_SHARING_WITH_NON_EXISTENT_USER = "MSG_ERROR_SHARING_WITH_NON_EXISTENT_USER";

	private static final String MSG_ERROR_INVALID_SUBSCRIPTION = "MSG_ERROR_INVALID_SUBSCRIPTION";
	private static final String MSG_ERROR_INVALID_DESTINATION_USER = "MSG_ERROR_INVALID_DESTINATION_USER";
	private static final String MSG_ERROR_IN_DELETE_PROCESS = "MSG_ERROR_IN_DELETE_PROCESS";
	private static final String MSG_ERROR_IN_INSERT_PROCESS = "MSG_ERROR_IN_INSERT_PROCESS";

	/**
	 * Get the list of subscriptions associated to a user.
	 * @param sSessionId User Session Id
	 * @return a View Model with the Subscription Name and 
	 * 	a flag to know if the user is admin or not of the subscription
	 */
	@GET
	@Path("/byuser")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response getListByUser(@HeaderParam("x-session-token") String sSessionId) {

		WasdiLog.debugLog("SubscriptionResource.getListByUser()");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		List<SubscriptionListViewModel> aoSubscriptionList = new ArrayList<>();

		// Domain Check
		if (oUser == null) {
			WasdiLog.debugLog("SubscriptionResource.getListByUser: invalid session: " + sSessionId);
			return Response.status(Status.UNAUTHORIZED).build();
		}

		try {
			if (!UserApplicationRole.userHasRightsToAccessApplicationResource(oUser.getRole(), SUBSCRIPTION_READ)) {
				return Response.status(Status.FORBIDDEN).build();
			}

			WasdiLog.debugLog("SubscriptionResource.getListByUser: subscriptions for " + oUser.getUserId());

			// Create repo
			SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();

			// Get Subscription List directly owned by the user
			List<Subscription> aoDirectSubscriptions = oSubscriptionRepository.getSubscriptionsByUser(oUser.getUserId());

			Set<String> asUniqueSubscriptionIds = aoDirectSubscriptions.stream()
					.map(Subscription::getSubscriptionId)
					.collect(Collectors.toSet());

			Set<String> asUniqueOrganizationIds = aoDirectSubscriptions.stream()
					.map(Subscription::getOrganizationId)
					.filter(t -> t != null)
					.collect(Collectors.toSet());

			OrganizationRepository oOrganizationRepository = new OrganizationRepository();
			List<Organization> aoOrganizations = oOrganizationRepository.getOrganizations(asUniqueOrganizationIds);

			Map<String, String> aoOrganizationNames = aoOrganizations.stream()
					.collect(Collectors.toMap(Organization::getOrganizationId, Organization::getName));

			// For each
			for (Subscription oSubscription : aoDirectSubscriptions) {
				// Create View Model
				SubscriptionListViewModel oSubscriptionViewModel = convert(oSubscription, oUser.getUserId(), aoOrganizationNames.get(oSubscription.getOrganizationId()), "owner");

				aoSubscriptionList.add(oSubscriptionViewModel);
			}

			//TODO
			// 1. subscriptions directly owned by the user
			// 2. subscriptions belonging to organizations owned by the user
			// 3. subscriptions belonging to organizations shared with the user
			// 4. subscriptions shared with the user on individual basis


			Response oResponse = new OrganizationResource().getListByUser(sSessionId);

			@SuppressWarnings("unchecked")
			List<OrganizationListViewModel> aoOrganizationLVMs = (List<OrganizationListViewModel>) oResponse.getEntity();

			List<String> asOrganizationIds = aoOrganizationLVMs.stream()
					.map(OrganizationListViewModel::getOrganizationId)
					.collect(Collectors.toList());

			Map<String, String> aoOrgNames = aoOrganizationLVMs.stream()
				      .collect(Collectors.toMap(OrganizationListViewModel::getOrganizationId, OrganizationListViewModel::getName));

			List<Subscription> aoOrganizationalSubscriptions = oSubscriptionRepository.getSubscriptionsByOrganizations(asOrganizationIds);

			aoOrganizationalSubscriptions.forEach((Subscription oSubscription) -> {
				if (!asUniqueSubscriptionIds.contains(oSubscription.getSubscriptionId())) {
					SubscriptionListViewModel oSubscriptionViewModel = convert(oSubscription, oUser.getUserId(), aoOrgNames.get(oSubscription.getOrganizationId()), "shared by " + aoOrgNames.get(oSubscription.getOrganizationId()));
					aoSubscriptionList.add(oSubscriptionViewModel);

					asUniqueSubscriptionIds.add(oSubscription.getSubscriptionId());
				}
			});

			// Get the list of subscriptions shared with this user
			List<UserResourcePermission> aoSharedSubscriptions = oUserResourcePermissionRepository.getSubscriptionSharingsByUserId(oUser.getUserId());

			if (aoSharedSubscriptions.size() > 0) {
				// For each
				for (UserResourcePermission oSharedSubscription : aoSharedSubscriptions) {
					Subscription oSubscription = oSubscriptionRepository.getSubscriptionById(oSharedSubscription.getResourceId());

					if (oSubscription == null) {
						WasdiLog.debugLog("SubscriptionResource.getListByUser: Subscription Shared not available " + oSharedSubscription.getResourceId());
						continue;
					}

					if (!asUniqueSubscriptionIds.contains(oSubscription.getSubscriptionId())) {
						SubscriptionListViewModel oSubscriptionViewModel = convert(oSubscription, oUser.getUserId(), null, "shared by " + oSharedSubscription.getOwnerId());

						aoSubscriptionList.add(oSubscriptionViewModel);
						asUniqueSubscriptionIds.add(oSubscription.getSubscriptionId());
					}
				}
			}

			return Response.ok(aoSubscriptionList).build();
		} catch (Exception oEx) {
			WasdiLog.debugLog("SubscriptionResource.getListByUser: " + oEx);
			return Response.serverError().build();
		}
	}

	/**
	 * Get an subscription by its Id.
	 * @param sSessionId User Session Id
	 * @param sSubscriptionId the subscription Id
	 * @return the full view model of the subscription
	 */
	@GET
	@Path("/byId")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response getSubscriptionViewModel(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("subscription") String sSubscriptionId) {
		WasdiLog.debugLog("SubscriptionResource.getSubscriptionViewModel( Subscription: " + sSubscriptionId + ")");

		SubscriptionViewModel oVM = new SubscriptionViewModel();

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.debugLog("SubscriptionResource.getSubscriptionViewModel: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse("Invalid session.")).build();
		}

		try {
			// Domain Check
			if (Utils.isNullOrEmpty(sSubscriptionId)) {
				return Response.status(400).entity(new ErrorResponse("Invalid subscriptionId.")).build();
			}

			if (!PermissionsUtils.canUserAccessSubscription(oUser.getUserId(), sSubscriptionId)) {
				WasdiLog.debugLog("SubscriptionResource.getSubscriptionViewModel: user cannot access subscription info, aborting");
				return Response.status(Status.FORBIDDEN).entity(new ErrorResponse("The user cannot access the subscription info.")).build();
			}

			WasdiLog.debugLog("SubscriptionResource.getSubscriptionViewModel: read subscriptions " + sSubscriptionId);

			// Create repo
			SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();

			// Get requested subscription
			Subscription oSubscription = oSubscriptionRepository.getSubscriptionById(sSubscriptionId);

			if (oSubscription == null) {
				WasdiLog.debugLog("SubscriptionResource.getSubscriptionViewModel: the subscription cannot be found, aborting");

				return Response.status(400).entity(new ErrorResponse("The subscription cannot be found.")).build();
			}

			String sOrganizationName = null;

			if (oSubscription.getOrganizationId() != null) {
				OrganizationRepository oOrganizationRepository = new OrganizationRepository();
				Organization oOrganization = oOrganizationRepository.getOrganizationById(oSubscription.getOrganizationId());

				sOrganizationName = oOrganization.getName();
			}

			oVM = convert(oSubscription, sOrganizationName);

			return Response.ok(oVM).build();
		} catch (Exception oEx) {
			WasdiLog.debugLog( "SubscriptionResource.getSubscriptionViewModel: " + oEx);
			return Response.serverError().build();
		}
	}

	/**
	 * Create a new Subscription.
	 * @param sSessionId User Session Id
	 * @param oSubscriptionViewModel the subscription to be created
	 * @return a primitive result containing the outcome of the operation
	 */
	@POST
	@Path("/add")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response createSubscription(@HeaderParam("x-session-token") String sSessionId, SubscriptionViewModel oSubscriptionViewModel) {
		WasdiLog.debugLog("SubscriptionResource.createSubscription( Subscription: " + oSubscriptionViewModel.toString() + ")");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.debugLog("SubscriptionResource.createSubscription: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse("Invalid session.")).build();
		}

		SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();

		Subscription oExistingSubscription = oSubscriptionRepository.getByName(oSubscriptionViewModel.getName());

		if (oExistingSubscription != null) {
			WasdiLog.debugLog("SubscriptionResource.createSubscription: a different subscription with the same name already exists");
			return Response.status(400).entity(new ErrorResponse("A subscription with the same name already exists.")).build();
		}

		Subscription oSubscription = convert(oSubscriptionViewModel);
		oSubscription.setUserId(oUser.getUserId());
		oSubscription.setSubscriptionId(Utils.getRandomName());

		if (oSubscriptionRepository.insertSubscription(oSubscription)) {
			ProjectEditorViewModel oProjectEditorViewModel = new ProjectEditorViewModel();
			oProjectEditorViewModel.setName(oSubscription.getName());
			oProjectEditorViewModel.setDescription("project automatically created for the " + oSubscription.getName() + " subscription");
			oProjectEditorViewModel.setSubscriptionId(oSubscription.getSubscriptionId());
			oProjectEditorViewModel.setActiveProject(oUser.getActiveProjectId() == null);

			new ProjectResource().createProject(sSessionId, oProjectEditorViewModel);

			return Response.ok(new SuccessResponse(oSubscription.getSubscriptionId())).build();
		} else {WasdiLog.debugLog("SubscriptionResource.createSubscription( " + oSubscriptionViewModel.getName() + " ): insertion failed");
			return Response.serverError().build();
		}
	}

	/**
	 * Update an Subscription.
	 * @param sSessionId User Session Id
	 * @param oSubscriptionViewModel the subscription to be updated
	 * @return a primitive result containing the outcome of the operation
	 */
	@PUT
	@Path("/update")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response upateSubscription(@HeaderParam("x-session-token") String sSessionId, SubscriptionViewModel oSubscriptionViewModel) {
		WasdiLog.debugLog("SubscriptionResource.updateSubscription( Subscription: " + oSubscriptionViewModel.toString() + ")");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.debugLog("SubscriptionResource.updateSubscription: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse("Invalid session.")).build();
		}

		SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();

		Subscription oExistingSubscription = oSubscriptionRepository.getSubscriptionById(oSubscriptionViewModel.getSubscriptionId());

		if (oExistingSubscription == null) {
			WasdiLog.debugLog("SubscriptionResource.updateSubscription: subscription does not exist");
			return Response.status(400).entity(new ErrorResponse("No subscription with the Id exists.")).build();
		}

		Subscription oExistingSubscriptionWithTheSameName = oSubscriptionRepository.getByName(oSubscriptionViewModel.getName());

		if (oExistingSubscriptionWithTheSameName != null
				&& !oExistingSubscriptionWithTheSameName.getSubscriptionId().equalsIgnoreCase(oExistingSubscription.getSubscriptionId())) {
			WasdiLog.debugLog("SubscriptionResource.updateSubscription: a different subscription with the same name already exists");
			return Response.status(400).entity(new ErrorResponse("A subscription with the same name already exists.")).build();
		}

		Subscription oSubscription = convert(oSubscriptionViewModel);

		if (oSubscriptionRepository.updateSubscription(oSubscription)) {
			return Response.ok(new SuccessResponse(oSubscription.getSubscriptionId())).build();
		} else {WasdiLog.debugLog("SubscriptionResource.updateSubscription( " + oSubscriptionViewModel.getName() + " ): update failed");
			return Response.serverError().build();
		}
	}

	@DELETE
	@Path("/delete")
	public Response deleteSubscription(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("subscription") String sSubscriptionId) {
		WasdiLog.debugLog("SubscriptionResource.deleteSubscription( Subscription: " + sSubscriptionId + " )");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.debugLog("SubscriptionResource.deleteSubscription: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse("Invalid session.")).build();
		}

		if (Utils.isNullOrEmpty(sSubscriptionId)) {
			return Response.status(400).entity(new ErrorResponse("Invalid subscriptionId.")).build();
		}

		SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();

		Subscription oSubscription = oSubscriptionRepository.getSubscriptionById(sSubscriptionId);

		if (oSubscription == null) {
			WasdiLog.debugLog("SubscriptionResource.deleteSubscription: subscription does not exist");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("No subscription with the name exists.")).build();
		}

		UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();

		String sSubscriptionOwner = oSubscription.getUserId();

		if (!sSubscriptionOwner.equals(oUser.getUserId())) {
			// The current uses is not the owner of the subscription
			WasdiLog.debugLog("SubscriptionResource.deleteSubscription: user " + oUser.getUserId() + " is not the owner [" + sSubscriptionOwner + "]: delete the sharing, not the subscription");
			oUserResourcePermissionRepository.deletePermissionsByUserIdAndSubscriptionId(oUser.getUserId(), sSubscriptionId);

			return Response.ok(new SuccessResponse(sSubscriptionId)).build();
		}

		if (oUserResourcePermissionRepository.isSubscriptionShared(sSubscriptionId)) {
			WasdiLog.debugLog("SubscriptionResource.deleteSubscription: the subscription is shared with users");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("The subscription cannot be removed as it is shared with users. Before deleting the subscription, please remove the sharings.")).build();
		}

		String sOrganizationId = oSubscription.getOrganizationId();

		if (sOrganizationId != null) {
			WasdiLog.debugLog("SubscriptionResource.deleteSubscription: the subscription is shared with an organization");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("The subscription cannot be removed as it is shared with an organization. Before deleting the subscription, please remove the sharing.")).build();
		}

		if (oSubscriptionRepository.deleteSubscription(sSubscriptionId)) {
			return Response.ok(new SuccessResponse(sSubscriptionId)).build();
		} else {
			WasdiLog.debugLog("SubscriptionResource.deleteSubscription( " + sSubscriptionId + " ): deletion failed");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("The deletion of the subscription failed.")).build();
		}
	}

	@GET
	@Path("/types")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public List<SubscriptionTypeViewModel> getSubscriptionTypes(@HeaderParam("x-session-token") String sSessionId) {
		WasdiLog.debugLog("SubscriptionResource.getSubscriptionTypes()");

		return convert(Arrays.asList(SubscriptionType.values()));
	}

	private static List<SubscriptionTypeViewModel> convert(List<SubscriptionType> aoSubscriptionTypes) {
		return aoSubscriptionTypes.stream().map(SubscriptionResource::convert).collect(Collectors.toList());
	}

	private static SubscriptionTypeViewModel convert(SubscriptionType oSubscriptionType) {
		return new SubscriptionTypeViewModel(oSubscriptionType.name(), oSubscriptionType.getTypeName(), oSubscriptionType.getTypeDescription());
	}

	/**
	 * Share a subscription with another user.
	 *
	 * @param sSessionId User Session Id
	 * @param sSubscriptionId Subscription Id
	 * @param sDestinationUserId User id that will receive the subscription in sharing.
	 * @return Primitive Result with boolValue = true and stringValue = Done if ok. False and error description otherwise
	 */
	@POST
	@Path("share/add")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult shareSubscription(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("subscription") String sSubscriptionId, @QueryParam("userId") String sDestinationUserId) {

		WasdiLog.debugLog("SubscriptionResource.ShareSubscription( WS: " + sSubscriptionId + ", User: " + sDestinationUserId + " )");

		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);


		// Validate Session
		User oRequesterUser = Wasdi.getUserFromSession(sSessionId);
		if (oRequesterUser == null) {
			WasdiLog.debugLog("SubscriptionResource.shareSubscription: invalid session");

			oResult.setIntValue(Status.UNAUTHORIZED.getStatusCode());
			oResult.setStringValue(MSG_ERROR_INVALID_SESSION);

			return oResult;
		}
		
		// Check if the subscription exists
		SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();
		Subscription oSubscription = oSubscriptionRepository.getSubscriptionById(sSubscriptionId);
		
		if (oSubscription == null) {
			WasdiLog.debugLog("SubscriptionResource.ShareSubscription: invalid subscription");

			oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
			oResult.setStringValue(MSG_ERROR_INVALID_SUBSCRIPTION);

			return oResult;
		}

		// Can the user access this section?
		if (!UserApplicationRole.userHasRightsToAccessApplicationResource(oRequesterUser.getRole(), SUBSCRIPTION_READ)) {
			WasdiLog.debugLog("SubscriptionResource.shareSubscription: " + oRequesterUser.getUserId() + " cannot access the section " + ", aborting");

			oResult.setIntValue(Status.FORBIDDEN.getStatusCode());
			oResult.setStringValue(MSG_ERROR_NO_ACCESS_RIGHTS_APPLICATION_RESOURCE_SUBSCRIPTION);

			return oResult;
		}

		// Can the user access this resource?
		if (!PermissionsUtils.canUserAccessSubscription(oRequesterUser.getUserId(), sSubscriptionId)
				&& !UserApplicationRole.userHasRightsToAccessApplicationResource(oRequesterUser.getRole(), ADMIN_DASHBOARD)) {
			WasdiLog.debugLog("SubscriptionResource.shareSubscription: " + sSubscriptionId + " cannot be accessed by " + oRequesterUser.getUserId() + ", aborting");

			oResult.setIntValue(Status.FORBIDDEN.getStatusCode());
			oResult.setStringValue(MSG_ERROR_NO_ACCESS_RIGHTS_OBJECT_SUBSCRIPTION);

			return oResult;
		}

		// Cannot Autoshare
		if (oRequesterUser.getUserId().equals(sDestinationUserId)) {
			if (UserApplicationRole.userHasRightsToAccessApplicationResource(oRequesterUser.getRole(), ADMIN_DASHBOARD)) {
				// A user that has Admin rights should be able to auto-share the resource.
			} else {
				WasdiLog.debugLog("SubscriptionResource.ShareSubscription: auto sharing not so smart");

				oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
				oResult.setStringValue(MSG_ERROR_SHARING_WITH_ONESELF);

				return oResult;
			}
		}

		// Cannot share with the owner
		if (sDestinationUserId.equals(oSubscription.getUserId())) {
			WasdiLog.debugLog("SubscriptionResource.ShareSubscription: sharing with the owner not so smart");

			oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
			oResult.setStringValue(MSG_ERROR_SHARING_WITH_OWNER);

			return oResult;
		}

		UserRepository oUserRepository = new UserRepository();
		User oDestinationUser = oUserRepository.getUser(sDestinationUserId);

		if (oDestinationUser == null) {
			//No. So it is neither the owner or a shared one
			WasdiLog.debugLog("SubscriptionResource.ShareSubscription: Destination user does not exists");

			oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
			oResult.setStringValue(MSG_ERROR_SHARING_WITH_NON_EXISTENT_USER);

			return oResult;
		}

		try {
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();

			if (!oUserResourcePermissionRepository.isSubscriptionSharedWithUser(sDestinationUserId, sSubscriptionId)) {
				UserResourcePermission oSubscriptionSharing =
						new UserResourcePermission("subscription", sSubscriptionId, sDestinationUserId, oSubscription.getUserId(), oRequesterUser.getUserId(), "write");

				oUserResourcePermissionRepository.insertPermission(oSubscriptionSharing);				
			} else {
				WasdiLog.debugLog("SubscriptionResource.ShareSubscription: already shared!");
				oResult.setStringValue("Already Shared.");
				oResult.setBoolValue(true);
				oResult.setIntValue(Status.OK.getStatusCode());

				return oResult;
			}
		} catch (Exception oEx) {
			WasdiLog.debugLog("SubscriptionResource.ShareSubscription: " + oEx);

			oResult.setIntValue(Status.INTERNAL_SERVER_ERROR.getStatusCode());
			oResult.setStringValue(MSG_ERROR_IN_INSERT_PROCESS);

			return oResult;
		}

		//TODO - uncomment the line below
//		sendNotificationEmail(oRequesterUser.getUserId(), sDestinationUserId, oSubscription.getName());

		oResult.setStringValue("Done");
		oResult.setBoolValue(true);

		return oResult;
	}

//	private static void sendNotificationEmail(String sRequesterUserId, String sDestinationUserId, String sSubscriptionName) {
//		try {
//			String sMercuriusAPIAddress = WasdiConfig.Current.notifications.mercuriusAPIAddress;
//
//			if(Utils.isNullOrEmpty(sMercuriusAPIAddress)) {
//				WasdiLog.debugLog("SubscriptionResource.ShareSubscription: sMercuriusAPIAddress is null");
//			}
//			else {
//
//				WasdiLog.debugLog("SubscriptionResource.ShareSubscription: send notification");
//
//				MercuriusAPI oAPI = new MercuriusAPI(sMercuriusAPIAddress);			
//				Message oMessage = new Message();
//
//				String sTitle = "Subscription " + sSubscriptionName + " Shared";
//
//				oMessage.setTilte(sTitle);
//
//				String sSender = WasdiConfig.Current.notifications.sftpManagementMailSender;
//				if (sSender==null) {
//					sSender = "wasdi@wasdi.net";
//				}
//
//				oMessage.setSender(sSender);
//
//				String sMessage = "The user " + sRequesterUserId + " shared with you the subscription: " + sSubscriptionName;
//
//				oMessage.setMessage(sMessage);
//
//				Integer iPositiveSucceded = 0;
//
//				iPositiveSucceded = oAPI.sendMailDirect(sDestinationUserId, oMessage);
//				
//				WasdiLog.debugLog("SubscriptionResource.ShareSubscription: notification sent with result " + iPositiveSucceded);
//			}
//
//		}
//		catch (Exception oEx) {
//			WasdiLog.debugLog("SubscriptionResource.ShareSubscription: notification exception " + oEx.toString());
//		}
//	}

	/**
	 * Get the list of users that has a Subscription in sharing.
	 *
	 * @param sSessionId User Session
	 * @param sSubscriptionId Subscription Id
	 * @return list of Subscription Sharing View Models
	 */
	@GET
	@Path("share/bysubscription")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public List<SubscriptionSharingViewModel> getEnableUsersSharedWorksace(@HeaderParam("x-session-token") String sSessionId, @QueryParam("subscription") String sSubscriptionId) {

		WasdiLog.debugLog("SubscriptionResource.getEnableUsersSharedWorksace( WS: " + sSubscriptionId + " )");

	
		List<UserResourcePermission> aoSubscriptionSharing = null;
		List<SubscriptionSharingViewModel> aoSubscriptionSharingViewModels = new ArrayList<SubscriptionSharingViewModel>();

		User oOwnerUser = Wasdi.getUserFromSession(sSessionId);
		if (oOwnerUser == null) {
			WasdiLog.debugLog("SubscriptionResource.getEnableUsersSharedWorksace: invalid session");
			return aoSubscriptionSharingViewModels;
		}

		try {
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			aoSubscriptionSharing = oUserResourcePermissionRepository.getSubscriptionSharingsBySubscriptionId(sSubscriptionId);

			if (aoSubscriptionSharing != null) {
				for (UserResourcePermission oSubscriptionSharing : aoSubscriptionSharing) {
					SubscriptionSharingViewModel oSubscriptionSharingViewModel = new SubscriptionSharingViewModel();
					oSubscriptionSharingViewModel.setOwnerId(oSubscriptionSharing.getUserId());
					oSubscriptionSharingViewModel.setUserId(oSubscriptionSharing.getUserId());
					oSubscriptionSharingViewModel.setSubscriptionId(oSubscriptionSharing.getResourceId());

					aoSubscriptionSharingViewModels.add(oSubscriptionSharingViewModel);
				}

			}

		} catch (Exception oEx) {
			WasdiLog.debugLog("SubscriptionResource.getEnableUsersSharedWorksace: " + oEx);
			return aoSubscriptionSharingViewModels;
		}

		return aoSubscriptionSharingViewModels;

	}

	@DELETE
	@Path("share/delete")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public PrimitiveResult deleteUserSharedSubscription(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("subscription") String sSubscriptionId, @QueryParam("userId") String sUserId) {

		WasdiLog.debugLog("SubscriptionResource.deleteUserSharedSubscription( WS: " + sSubscriptionId + ", User:" + sUserId + " )");
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);
		// Validate Session
		User oRequestingUser = Wasdi.getUserFromSession(sSessionId);

		if (oRequestingUser == null) {
			WasdiLog.debugLog("SubscriptionResource.deleteUserSharedSubscription: invalid session");

			oResult.setIntValue(Status.UNAUTHORIZED.getStatusCode());
			oResult.setStringValue(MSG_ERROR_INVALID_SESSION);

			return oResult;
		}

		try {

			UserRepository oUserRepository = new UserRepository();
			User oDestinationUser = oUserRepository.getUser(sUserId);

			if (oDestinationUser == null) {
				WasdiLog.debugLog("SubscriptionResource.deleteUserSharedSubscription: invalid destination user");

				oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
				oResult.setStringValue(MSG_ERROR_INVALID_DESTINATION_USER);

				return oResult;
			}

			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			oUserResourcePermissionRepository.deletePermissionsByUserIdAndSubscriptionId(sUserId, sSubscriptionId);
		} catch (Exception oEx) {
			WasdiLog.debugLog("SubscriptionResource.deleteUserSharedSubscription: " + oEx);

			oResult.setIntValue(Status.INTERNAL_SERVER_ERROR.getStatusCode());
			oResult.setStringValue(MSG_ERROR_IN_DELETE_PROCESS);

			return oResult;
		}

		oResult.setStringValue("Done");
		oResult.setBoolValue(true);
		oResult.setIntValue(Status.OK.getStatusCode());

		return oResult;
	}

	private static SubscriptionViewModel convert(Subscription oSubscription, String sOrganizationName) {
		SubscriptionViewModel oSubscriptionViewModel = new SubscriptionViewModel();
		oSubscriptionViewModel.setSubscriptionId(oSubscription.getSubscriptionId());
		oSubscriptionViewModel.setName(oSubscription.getName());
		oSubscriptionViewModel.setDescription(oSubscription.getDescription());
		oSubscriptionViewModel.setTypeId(oSubscription.getType());
		oSubscriptionViewModel.setTypeName(SubscriptionType.get(oSubscription.getType()).getTypeName());
		oSubscriptionViewModel.setBuyDate(TimeEpochUtils.fromEpochToDateString(oSubscription.getBuyDate()));
		oSubscriptionViewModel.setStartDate(TimeEpochUtils.fromEpochToDateString(oSubscription.getStartDate()));
		oSubscriptionViewModel.setEndDate(TimeEpochUtils.fromEpochToDateString(oSubscription.getEndDate()));
		oSubscriptionViewModel.setDurationDays(oSubscription.getDurationDays());
		oSubscriptionViewModel.setUserId(oSubscription.getUserId());
		oSubscriptionViewModel.setOrganizationId(oSubscription.getOrganizationId());
		oSubscriptionViewModel.setOrganizationName(sOrganizationName);
		oSubscriptionViewModel.setBuySuccess(oSubscription.isBuySuccess());

		return oSubscriptionViewModel;
	}

	private static SubscriptionListViewModel convert(Subscription oSubscription, String sCurrentUserId, String sOrganizationName, String sReason) {
		SubscriptionListViewModel oSubscriptionListViewModel = new SubscriptionListViewModel();
		oSubscriptionListViewModel.setSubscriptionId(oSubscription.getSubscriptionId());
		oSubscriptionListViewModel.setName(oSubscription.getName());
		oSubscriptionListViewModel.setTypeId(oSubscription.getType());
		oSubscriptionListViewModel.setTypeName(SubscriptionType.get(oSubscription.getType()).getTypeName());
		oSubscriptionListViewModel.setOrganizationName(sOrganizationName);
		oSubscriptionListViewModel.setReason(sReason);
		oSubscriptionListViewModel.setBuySuccess(oSubscription.isBuySuccess());
		oSubscriptionListViewModel.setOwnerUserId(oSubscription.getUserId());
		oSubscriptionListViewModel.setAdminRole(sCurrentUserId.equalsIgnoreCase(oSubscription.getUserId()));

		return oSubscriptionListViewModel;
	}

	private static Subscription convert(SubscriptionViewModel oSubscriptionViewModel) {
		Subscription oSubscription = new Subscription();
		oSubscription.setSubscriptionId(oSubscriptionViewModel.getSubscriptionId());
		oSubscription.setName(oSubscriptionViewModel.getName());
		oSubscription.setDescription(oSubscriptionViewModel.getDescription());
		oSubscription.setType(oSubscriptionViewModel.getTypeId());
		oSubscription.setBuyDate(Utils.getDateAsDouble(Utils.getYyyyMMddTZDate(oSubscriptionViewModel.getBuyDate())));
		oSubscription.setStartDate(Utils.getDateAsDouble(Utils.getYyyyMMddTZDate(oSubscriptionViewModel.getStartDate())));
		oSubscription.setEndDate(Utils.getDateAsDouble(Utils.getYyyyMMddTZDate(oSubscriptionViewModel.getEndDate())));
		oSubscription.setDurationDays(oSubscriptionViewModel.getDurationDays());
		oSubscription.setUserId(oSubscriptionViewModel.getUserId());
		oSubscription.setOrganizationId(oSubscriptionViewModel.getOrganizationId());
		oSubscription.setBuySuccess(oSubscriptionViewModel.isBuySuccess());

		return oSubscription;
	}

}
