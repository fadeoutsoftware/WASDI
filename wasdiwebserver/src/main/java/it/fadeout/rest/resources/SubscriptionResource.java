package it.fadeout.rest.resources;

import static wasdi.shared.business.UserApplicationPermission.ADMIN_DASHBOARD;
import static wasdi.shared.business.UserApplicationPermission.SUBSCRIPTION_READ;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import it.fadeout.Wasdi;
import it.fadeout.services.StripeService;
import wasdi.shared.business.Organization;
import wasdi.shared.business.Subscription;
import wasdi.shared.business.User;
import wasdi.shared.business.UserApplicationRole;
import wasdi.shared.business.UserResourcePermission;
import wasdi.shared.config.StripeProductConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.OrganizationRepository;
import wasdi.shared.data.SubscriptionRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.data.UserResourcePermissionRepository;
import wasdi.shared.rabbit.Send;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.TimeEpochUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.ErrorResponse;
import wasdi.shared.viewmodels.SuccessResponse;
import wasdi.shared.viewmodels.organizations.ProjectEditorViewModel;
import wasdi.shared.viewmodels.organizations.ProjectListViewModel;
import wasdi.shared.viewmodels.organizations.StripePaymentDetail;
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
	public Response getListByUser(@HeaderParam("x-session-token") String sSessionId, @QueryParam("valid") Boolean bValid) {
		
		if (bValid == null) bValid = false;

		WasdiLog.debugLog("SubscriptionResource.getListByUser");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		List<SubscriptionListViewModel> aoSubscriptionLVM = new ArrayList<>();

		// Domain Check
		if (oUser == null) {
			WasdiLog.debugLog("SubscriptionResource.getListByUser: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(MSG_ERROR_INVALID_SESSION)).build();
		}

		try {

			WasdiLog.debugLog("SubscriptionResource.getListByUser: subscriptions for " + oUser.getUserId() + " Valid = " + bValid);


			// 1. subscriptions directly owned by the user
			// 2. subscriptions belonging to organizations owned by the user
			// 3. subscriptions belonging to organizations shared with the user
			// 4. subscriptions shared with the user on individual basis


			// Get the list of Subscriptions owned by the user
			SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();

			List<Subscription> aoOwnedSubscriptions = oSubscriptionRepository.getSubscriptionsByUser(oUser.getUserId());

			Set<String> asOwnedSubscriptionIds = aoOwnedSubscriptions.stream()
					.map(Subscription::getSubscriptionId)
					.collect(Collectors.toSet());

			Set<String> asOrganizationIdsOfOwnedSubscriptions = aoOwnedSubscriptions.stream()
					.map(Subscription::getOrganizationId)
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());

			Map<String, String> aoOrganizationNamesOfOwnedSubscriptions = getOrganizationNamesById(asOrganizationIdsOfOwnedSubscriptions);

			// For each
			for (Subscription oSubscription : aoOwnedSubscriptions) {
				
				if (bValid) {
					if (!oSubscription.isValid()) continue;
				}
				// Create View Model
				SubscriptionListViewModel oSubscriptionViewModel = convert(oSubscription, oUser.getUserId(), aoOrganizationNamesOfOwnedSubscriptions.get(oSubscription.getOrganizationId()), "owner");

				aoSubscriptionLVM.add(oSubscriptionViewModel);
			}


			// Get the list of Subscriptions shared by organizations

			Set<String> asOrganizationIdsOfOwnedByOrSharedWithUser = new OrganizationResource().getIdsOfOrganizationsOwnedByOrSharedWithUser(oUser.getUserId());

			Map<String, String> aoOrganizationNamesOfOwnedByOrSharedWithUser = getOrganizationNamesById(asOrganizationIdsOfOwnedByOrSharedWithUser);

			List<Subscription> aoOrganizationalSubscriptions = getSubscriptionsSharedByOrganizations(asOrganizationIdsOfOwnedByOrSharedWithUser);
			
			for (Subscription oSubscription : aoOrganizationalSubscriptions) {
				if (!asOwnedSubscriptionIds.contains(oSubscription.getSubscriptionId())) {
					boolean bToAdd=true;
					if (bValid) {
						bToAdd = oSubscription.isValid();
					}
					
					if (bToAdd) {
						SubscriptionListViewModel oSubscriptionViewModel = convert(oSubscription, oUser.getUserId(), aoOrganizationNamesOfOwnedByOrSharedWithUser.get(oSubscription.getOrganizationId()), "shared by " + aoOrganizationNamesOfOwnedByOrSharedWithUser.get(oSubscription.getOrganizationId()));
						aoSubscriptionLVM.add(oSubscriptionViewModel);						
					}
				}
			}
			

			// Get the list of Subscriptions shared with this user
			List<Subscription> aoSharedSubscriptions = getSubscriptionsSharedWithUser(oUser.getUserId());


			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			List<UserResourcePermission> aoSubscriptionSharings = oUserResourcePermissionRepository.getSubscriptionSharingsByUserId(oUser.getUserId());

			Map<String, String> aoSubscriptionUser = aoSubscriptionSharings.stream()
					.collect(Collectors.toMap(UserResourcePermission::getResourceId, UserResourcePermission::getUserId));


			List<String> asOrganizationIdsOfDirectSubscriptionSharings = aoSharedSubscriptions.stream()
					.map(Subscription::getOrganizationId)
					.collect(Collectors.toList());

			Map<String, String> asNamesOfOrganizationsOfDirectSubscriptionSharings = getOrganizationNamesById(asOrganizationIdsOfDirectSubscriptionSharings);

			for (Subscription oSubscription : aoSharedSubscriptions) {
				boolean bToAdd=true;
				if (bValid) {
					bToAdd = oSubscription.isValid();
				}				
				
				if (bToAdd) {
					SubscriptionListViewModel oSubscriptionViewModel = convert(oSubscription, oUser.getUserId(),
							asNamesOfOrganizationsOfDirectSubscriptionSharings.get(oSubscription.getOrganizationId()),
							"shared by " + aoSubscriptionUser.get(oSubscription.getSubscriptionId()));
	
					aoSubscriptionLVM.add(oSubscriptionViewModel);
				}
			}


			if (!aoSubscriptionLVM.isEmpty()) {
				try  {
					
					Map<String, SubscriptionListViewModel> aoSubscriptionListViewModelMap = new HashMap<String, SubscriptionListViewModel>();
					
					for (SubscriptionListViewModel oVMToAdd : aoSubscriptionLVM) {
						if (aoSubscriptionListViewModelMap.containsKey(oVMToAdd.getSubscriptionId()) ==false) {
							aoSubscriptionListViewModelMap.put(oVMToAdd.getSubscriptionId(), oVMToAdd);
						}
					}

					Set<String> asSubscriptionIds_2 = aoSubscriptionListViewModelMap.keySet();

					Map<String, Map<String, Long>> aoRunningTimeBySubscriptionAndProject =
							new ProcessWorkspaceResource().getRunningTimeBySubscriptionAndProject(sSessionId);

					for (String sSubscriptionId : asSubscriptionIds_2) {
						Map<String, Long> aoRunningTimeByProject = aoRunningTimeBySubscriptionAndProject.get(sSubscriptionId);

						if (aoRunningTimeByProject != null) {
							long lTotalRunningTime = 0;

							for (Long lRunningTime : aoRunningTimeByProject.values()) {
								lTotalRunningTime += lRunningTime;
							}

							aoSubscriptionListViewModelMap.get(sSubscriptionId).setRunningTime(lTotalRunningTime);
						}
					}					
				}
				catch (Exception oEx) {
					WasdiLog.errorLog("SubscriptionResource.getListByUser: exception computing running time for projects ", oEx);
				}
			}

			return Response.ok(aoSubscriptionLVM).build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionResource.getListByUser error: " + oEx);
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
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(MSG_ERROR_INVALID_SESSION)).build();
		}

		try {
			// Domain Check
			if (Utils.isNullOrEmpty(sSubscriptionId)) {
				WasdiLog.debugLog("SubscriptionResource.getSubscriptionViewModel: invalid subscription id");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("Invalid subscriptionId.")).build();
			}

			if (!PermissionsUtils.canUserAccessSubscription(oUser.getUserId(), sSubscriptionId)) {
				WasdiLog.debugLog("SubscriptionResource.getSubscriptionViewModel: user cannot access subscription info, aborting");
				return Response.status(Status.FORBIDDEN).entity(new ErrorResponse("The user cannot access the subscription info.")).build();
			}

			// Create repo
			SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();
			// Get requested subscription
			Subscription oSubscription = oSubscriptionRepository.getSubscriptionById(sSubscriptionId);

			if (oSubscription == null) {
				WasdiLog.debugLog("SubscriptionResource.getSubscriptionViewModel: the subscription cannot be found, aborting");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("The subscription cannot be found.")).build();
			}

			String sOrganizationName = null;

			if (oSubscription.getOrganizationId() != null) {
				OrganizationRepository oOrganizationRepository = new OrganizationRepository();
				Organization oOrganization = oOrganizationRepository.getById(oSubscription.getOrganizationId());
				sOrganizationName = oOrganization.getName();
			}

			oVM = convert(oSubscription, sOrganizationName);

			return Response.ok(oVM).build();
		} catch (Exception oEx) {
			WasdiLog.errorLog( "SubscriptionResource.getSubscriptionViewModel: " + oEx);
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
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(MSG_ERROR_INVALID_SESSION)).build();
		}

		SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();

		Subscription oExistingSubscription = oSubscriptionRepository.getByName(oSubscriptionViewModel.getName());

		if (oExistingSubscription != null) {
			WasdiLog.debugLog("SubscriptionResource.createSubscription: a different subscription with the same name already exists");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("A subscription with the same name already exists.")).build();
		}

		Subscription oSubscription = convert(oSubscriptionViewModel);
		oSubscription.setUserId(oUser.getUserId());
		oSubscription.setSubscriptionId(Utils.getRandomName());

		if (oSubscriptionRepository.insertSubscription(oSubscription)) {
			ProjectEditorViewModel oProjectEditorViewModel = new ProjectEditorViewModel();
			oProjectEditorViewModel.setName(oSubscription.getName());
			oProjectEditorViewModel.setDescription("Project automatically created for the " + oSubscription.getName() + " subscription");
			oProjectEditorViewModel.setSubscriptionId(oSubscription.getSubscriptionId());
			oProjectEditorViewModel.setActiveProject(oUser.getActiveProjectId() == null);

			new ProjectResource().createProject(sSessionId, oProjectEditorViewModel);

			return Response.ok(new SuccessResponse(oSubscription.getSubscriptionId())).build();
		} 
		else {
			WasdiLog.debugLog("SubscriptionResource.createSubscription: insertion failed");
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
		WasdiLog.debugLog("SubscriptionResource.updateSubscription");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.debugLog("SubscriptionResource.updateSubscription: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(MSG_ERROR_INVALID_SESSION)).build();
		}
		
		if (oSubscriptionViewModel == null) {
			WasdiLog.debugLog("SubscriptionResource.updateSubscription: invalid body");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(MSG_ERROR_INVALID_SUBSCRIPTION)).build();			
		}
		
		if (PermissionsUtils.canUserAccessSubscription(oUser.getUserId(), oSubscriptionViewModel.getSubscriptionId())) {
			WasdiLog.debugLog("SubscriptionResource.updateSubscription: user cannot access subscription");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(MSG_ERROR_NO_ACCESS_RIGHTS_OBJECT_SUBSCRIPTION)).build();						
		}

		SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();

		Subscription oExistingSubscription = oSubscriptionRepository.getSubscriptionById(oSubscriptionViewModel.getSubscriptionId());

		if (oExistingSubscription == null) {
			WasdiLog.debugLog("SubscriptionResource.updateSubscription: subscription does not exist");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("No subscription with the Id exists.")).build();
		}

		Subscription oExistingSubscriptionWithTheSameName = oSubscriptionRepository.getByName(oSubscriptionViewModel.getName());

		if (oExistingSubscriptionWithTheSameName != null
				&& !oExistingSubscriptionWithTheSameName.getSubscriptionId().equalsIgnoreCase(oExistingSubscription.getSubscriptionId())) {
			WasdiLog.debugLog("SubscriptionResource.updateSubscription: a different subscription with the same name already exists");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("A subscription with the same name already exists.")).build();
		}

		Subscription oSubscription = convert(oSubscriptionViewModel);

		if (oSubscriptionRepository.updateSubscription(oSubscription)) {
			return Response.ok(new SuccessResponse(oSubscription.getSubscriptionId())).build();
		}
		else {WasdiLog.debugLog("SubscriptionResource.updateSubscription( " + oSubscriptionViewModel.getName() + " ): update failed");
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
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(MSG_ERROR_INVALID_SESSION)).build();
		}

		if (Utils.isNullOrEmpty(sSubscriptionId)) {
			WasdiLog.debugLog("SubscriptionResource.deleteSubscription: invalid subscription id");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("Invalid subscriptionId.")).build();
		}

		SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();
		Subscription oSubscription = oSubscriptionRepository.getSubscriptionById(sSubscriptionId);

		if (oSubscription == null) {
			WasdiLog.debugLog("SubscriptionResource.deleteSubscription: subscription does not exist");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("No subscription with the name exists.")).build();
		}
		
		if (!PermissionsUtils.canUserAccessSubscription(oUser.getUserId(), sSubscriptionId)) {
			WasdiLog.debugLog("SubscriptionResource.deleteSubscription: user cannot access subscription");
			return Response.status(Status.FORBIDDEN).entity(new ErrorResponse("Forbidden.")).build();
		}

		UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();

		String sSubscriptionOwner = oSubscription.getUserId();

		if (!sSubscriptionOwner.equals(oUser.getUserId())) {
			// The current uses is not the owner of the subscription
			UserResourcePermission oPermission = oUserResourcePermissionRepository.getSubscriptionSharingByUserIdAndSubscriptionId(oUser.getUserId(), sSubscriptionId);

			if (oPermission == null) {
				WasdiLog.debugLog("SubscriptionResource.deleteSubscription: the subscription is shared by an organization and cannot be individually removed:" + sSubscriptionOwner);

				return Response.ok(new SuccessResponse("Subscriptions shared by an organization cannot be removed")).build();
			} else {
				WasdiLog.debugLog("SubscriptionResource.deleteSubscription: user " + oUser.getUserId() + " is not the owner [" + sSubscriptionOwner + "]: delete the sharing, not the subscription");
				oUserResourcePermissionRepository.deletePermissionsByUserIdAndSubscriptionId(oUser.getUserId(), sSubscriptionId);

				return Response.ok(new SuccessResponse("Sharing removed")).build();
			}
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


		ProjectResource oProjectResource = new ProjectResource();
		Response oResponse = oProjectResource.getListBySubscription(sSessionId, sSubscriptionId);

		@SuppressWarnings("unchecked")
		List<ProjectListViewModel> aoProjectList = (List<ProjectListViewModel>) oResponse.getEntity();

		if (!aoProjectList.isEmpty()) {
			WasdiLog.debugLog("SubscriptionResource.deleteSubscription: the subscription has associated projects");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("The subscription cannot be removed as it is used by projects. Before deleting the subscription, please remove the projects.")).build();
		}

		if (oSubscriptionRepository.deleteSubscription(sSubscriptionId)) {
			return Response.ok(new SuccessResponse("Done")).build();
		} 
		else {
			WasdiLog.debugLog("SubscriptionResource.deleteSubscription: deletion failed");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("The deletion of the subscription failed.")).build();
		}
	}

	@GET
	@Path("/types")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response getSubscriptionTypes(@HeaderParam("x-session-token") String sSessionId) {
		WasdiLog.debugLog("SubscriptionResource.getSubscriptionTypes");
		return Response.ok(convert(Arrays.asList(SubscriptionType.values()))).build();
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
	public Response shareSubscription(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("subscription") String sSubscriptionId, @QueryParam("userId") String sDestinationUserId) {

		WasdiLog.debugLog("SubscriptionResource.shareSubscription( WS: " + sSubscriptionId + ", User: " + sDestinationUserId + " )");

		// Validate Session
		User oRequestingUser = Wasdi.getUserFromSession(sSessionId);
		if (oRequestingUser == null) {
			WasdiLog.debugLog("SubscriptionResource.shareSubscription: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(MSG_ERROR_INVALID_SESSION)).build();
		}

		// Domain Check
		if (Utils.isNullOrEmpty(sSubscriptionId)) {
			WasdiLog.debugLog("SubscriptionResource.shareSubscription: invalid subscription id");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("Invalid subscriptionId.")).build();
		}

		// Check if the subscription exists
		SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();
		Subscription oSubscription = oSubscriptionRepository.getSubscriptionById(sSubscriptionId);
		
		if (oSubscription == null) {
			WasdiLog.debugLog("SubscriptionResource.shareSubscription: invalid subscription");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(MSG_ERROR_INVALID_SUBSCRIPTION)).build();
		}
		
		// Can the user access this resource?
		if (!PermissionsUtils.canUserAccessSubscription(oRequestingUser.getUserId(), sSubscriptionId)
				&& !UserApplicationRole.userHasRightsToAccessApplicationResource(oRequestingUser.getRole(), ADMIN_DASHBOARD)) {
			WasdiLog.debugLog("SubscriptionResource.shareSubscription:user cannot access subscription");
			return Response.status(Status.FORBIDDEN).entity(new ErrorResponse(MSG_ERROR_NO_ACCESS_RIGHTS_OBJECT_SUBSCRIPTION)).build();
		}

		// Cannot Autoshare
		if (oRequestingUser.getUserId().equals(sDestinationUserId)) {
			WasdiLog.debugLog("SubscriptionResource.shareSubscription: auto sharing not so smart");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(MSG_ERROR_SHARING_WITH_ONESELF)).build();
		}

		// Cannot share with the owner
		if (sDestinationUserId.equals(oSubscription.getUserId())) {
			WasdiLog.debugLog("SubscriptionResource.shareSubscription: sharing with the owner not so smart");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(MSG_ERROR_SHARING_WITH_OWNER)).build();
		}

		UserRepository oUserRepository = new UserRepository();
		User oDestinationUser = oUserRepository.getUser(sDestinationUserId);

		if (oDestinationUser == null) {
			//No. So it is neither the owner or a shared one
			WasdiLog.debugLog("SubscriptionResource.shareSubscription: Destination user does not exists");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(MSG_ERROR_SHARING_WITH_NON_EXISTENT_USER)).build();
		}

		try {
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();

			if (!oUserResourcePermissionRepository.isSubscriptionSharedWithUser(sDestinationUserId, sSubscriptionId)) {
				UserResourcePermission oSubscriptionSharing =
						new UserResourcePermission("subscription", sSubscriptionId, sDestinationUserId, oSubscription.getUserId(), oRequestingUser.getUserId(), "write");

				oUserResourcePermissionRepository.insertPermission(oSubscriptionSharing);

//				sendNotificationEmail(oRequesterUser.getUserId(), sDestinationUserId, oSubscription.getName());

				return Response.ok(new SuccessResponse("Done")).build();
			} else {
				WasdiLog.debugLog("SubscriptionResource.shareSubscription: already shared!");
				return Response.ok(new SuccessResponse("Already Shared.")).build();
			}
		} 
		catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionResource.shareSubscription error: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(MSG_ERROR_IN_INSERT_PROCESS)).build();
		}
	}

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
	public Response getEnableUsersSharedSubscription(@HeaderParam("x-session-token") String sSessionId, @QueryParam("subscription") String sSubscriptionId) {

		WasdiLog.debugLog("SubscriptionResource.getEnableUsersSharedSubscription( Subscription: " + sSubscriptionId + " )");

	
		List<UserResourcePermission> aoSubscriptionSharing = null;
		List<SubscriptionSharingViewModel> aoSubscriptionSharingViewModels = new ArrayList<SubscriptionSharingViewModel>();

		User oRequestingUser = Wasdi.getUserFromSession(sSessionId);
		
		if (oRequestingUser == null) {
			WasdiLog.debugLog("SubscriptionResource.getEnableUsersSharedSubscription: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(MSG_ERROR_INVALID_SESSION)).build();
		}
		
		if (!PermissionsUtils.canUserAccessSubscription(oRequestingUser.getUserId(), sSubscriptionId)) {
			WasdiLog.debugLog("SubscriptionResource.getEnableUsersSharedSubscription: invalid session");
			return Response.status(Status.FORBIDDEN).entity(new ErrorResponse(MSG_ERROR_NO_ACCESS_RIGHTS_OBJECT_SUBSCRIPTION)).build();			
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

			return Response.ok(aoSubscriptionSharingViewModels).build();
		} 
		catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionResource.getEnableUsersSharedSubscription error: " + oEx);
			return Response.serverError().build();
		}

	}

	@DELETE
	@Path("share/delete")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response deleteUserSharedSubscription(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("subscription") String sSubscriptionId, @QueryParam("userId") String sUserId) {

		WasdiLog.debugLog("SubscriptionResource.deleteUserSharedSubscription( WS: " + sSubscriptionId + ", User:" + sUserId + " )");

		// Validate Session
		User oRequestingUser = Wasdi.getUserFromSession(sSessionId);

		if (oRequestingUser == null) {
			WasdiLog.debugLog("SubscriptionResource.deleteUserSharedSubscription: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(MSG_ERROR_INVALID_SESSION)).build();
		}

		// Domain Check
		if (Utils.isNullOrEmpty(sSubscriptionId)) {
			WasdiLog.debugLog("SubscriptionResource.deleteUserSharedSubscription: invalid subscription");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("Invalid subscriptionId.")).build();
		}
		
		// Can the user access this resource?
		if (!PermissionsUtils.canUserAccessSubscription(oRequestingUser.getUserId(), sSubscriptionId)
				&& !UserApplicationRole.userHasRightsToAccessApplicationResource(oRequestingUser.getRole(), ADMIN_DASHBOARD)) {
			WasdiLog.debugLog("SubscriptionResource.deleteUserSharedSubscription:user cannot access subscription");
			return Response.status(Status.FORBIDDEN).entity(new ErrorResponse(MSG_ERROR_NO_ACCESS_RIGHTS_OBJECT_SUBSCRIPTION)).build();
		}		

		try {
			UserRepository oUserRepository = new UserRepository();
			User oDestinationUser = oUserRepository.getUser(sUserId);

			if (oDestinationUser == null) {
				WasdiLog.debugLog("SubscriptionResource.deleteUserSharedSubscription: invalid destination user");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(MSG_ERROR_INVALID_DESTINATION_USER)).build();
			}

			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			oUserResourcePermissionRepository.deletePermissionsByUserIdAndSubscriptionId(sUserId, sSubscriptionId);

			return Response.ok(new SuccessResponse("Done")).build();
		} catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionResource.deleteUserSharedSubscription: " + oEx);

			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(MSG_ERROR_IN_DELETE_PROCESS)).build();
		}
	}




	@GET
	@Path("/stripe/paymentUrl")
	public Response getStripePaymentUrl(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("subscription") String sSubscriptionId, @QueryParam("workspace") String sWorkspaceId) {
		WasdiLog.debugLog("SubscriptionResource.getStripePaymentUrl( " + "Subscription: " + sSubscriptionId + ", "
				+ "Workspace: " + sWorkspaceId + ")");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.debugLog("SubscriptionResource.getStripePaymentUrl: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(MSG_ERROR_INVALID_SESSION)).build();
		}

		try {
			// Domain Check
			if (Utils.isNullOrEmpty(sSubscriptionId)) {
				WasdiLog.debugLog("SubscriptionResource.getStripePaymentUrl: invalid subscription id");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("Invalid subscriptionId.")).build();
			}

			if (!PermissionsUtils.canUserAccessSubscription(oUser.getUserId(), sSubscriptionId)) {
				WasdiLog.debugLog("SubscriptionResource.getStripePaymentUrl: user cannot access subscription info");
				return Response.status(Status.FORBIDDEN).entity(new ErrorResponse("The user cannot access the subscription info.")).build();
			}

			// Create repo
			SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();

			// Get requested subscription
			Subscription oSubscription = oSubscriptionRepository.getSubscriptionById(sSubscriptionId);

			if (oSubscription == null) {
				WasdiLog.debugLog("SubscriptionResource.getStripePaymentUrl: subscription does not exist");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("The subscription cannot be found.")).build();
			}

			String sSubscriptionType = oSubscription.getType();

			if (Utils.isNullOrEmpty(sSubscriptionType)) {
				WasdiLog.debugLog("SubscriptionResource.getStripePaymentUrl: the subscription does not have a valid type, aborting");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("The subscription does not have a valid type.")).build();
			}

			List<StripeProductConfig> aoProductConfigList = WasdiConfig.Current.stripe.products;

			Map<String, String> aoProductConfigMap = aoProductConfigList.stream()
					.collect(Collectors.toMap(t -> t.id, t -> t.url));

			SubscriptionType oSubscriptionType = SubscriptionType.get(sSubscriptionType);

			if (oSubscriptionType == null) {
				WasdiLog.debugLog("SubscriptionResource.getStripePaymentUrl: the subscription does not have a valid type, aborting");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("The subscription does not have a valid type.")).build();
			} 
			else {
				String sBaseUrl = aoProductConfigMap.get(sSubscriptionType);

				if (Utils.isNullOrEmpty(sBaseUrl)) {
					WasdiLog.debugLog("SubscriptionResource.getStripePaymentUrl: the config does not contain a valid configuration for the subscription");
					return Response.serverError().build();
				} 
				else {
					String sUrl = sBaseUrl + "?client_reference_id=" + sSubscriptionId;

					if (!Utils.isNullOrEmpty(sWorkspaceId)) {
						sUrl += "_" + sWorkspaceId;
					}

					return Response.ok(new SuccessResponse(sUrl)).build();
				}
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionResource.getStripePaymentUrl error " + oEx);
			return Response.serverError().build();
		}
	}

	@GET
	@Path("/stripe/confirmation/{CHECKOUT_SESSION_ID}")
	public String confirmation(@PathParam("CHECKOUT_SESSION_ID") String sCheckoutSessionId) {
		WasdiLog.debugLog("SubscriptionResource.confirmation( sCheckoutSessionId: " + sCheckoutSessionId + ")");

		if (Utils.isNullOrEmpty(sCheckoutSessionId)) {
			WasdiLog.debugLog("SubscriptionResource.confirmation: Stripe returned a null CHECKOUT_SESSION_ID, aborting");
			return null;
		}

		StripeService oStripeResource = new StripeService();
		StripePaymentDetail oStripePaymentDetail = oStripeResource.retrieveStripePaymentDetail(sCheckoutSessionId);

		String sClientReferenceId = oStripePaymentDetail.getClientReferenceId();

		if (oStripePaymentDetail == null || Utils.isNullOrEmpty(sClientReferenceId)) {
			WasdiLog.debugLog("SubscriptionResource.confirmation: Stripe returned an invalid result, aborting");

			return null;
		}

		String sSubscriptionId = null;
		String sWorkspaceId = null;

		if (sClientReferenceId.contains("_")) {
			String[] asClientReferenceId = sClientReferenceId.split("_");
			sSubscriptionId = asClientReferenceId[0];
			sWorkspaceId = asClientReferenceId[1];
		} else {
			sSubscriptionId = sClientReferenceId;
		}

		WasdiLog.debugLog("SubscriptionResource.confirmation( sSubscriptionId: " + sSubscriptionId + ")");

		if (oStripePaymentDetail != null) {

			SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();

			Subscription oSubscription = oSubscriptionRepository.getSubscriptionById(sSubscriptionId);

			if (oSubscription == null) {
				WasdiLog.debugLog("SubscriptionResource.confirmation: subscription does not exist");
			} else {
				oSubscription.setBuyDate(Utils.nowInMillis());
				oSubscription.setBuySuccess(true);

				oSubscriptionRepository.updateSubscription(oSubscription);

				if (!Utils.isNullOrEmpty(sWorkspaceId)) {
					sendRabbitMessage(sWorkspaceId, oStripePaymentDetail);
				}
			}
		}

		String sHtmlContent = "<script type=\"text/javascript\">\r\n" + 
				"setTimeout(\r\n" + 
				"function ( )\r\n" + 
				"{\r\n" + 
				"  self.close();\r\n" + 
				"}, 1000 );\r\n" + 
				"</script>";
		
		return sHtmlContent;
	}

	private void sendRabbitMessage(String sWorkspaceId, StripePaymentDetail oStripePaymentDetail) {
		try {
			// Search for exchange name
			String sExchange = WasdiConfig.Current.rabbit.exchange;

			// Set default if is empty
			if (Utils.isNullOrEmpty(sExchange)) {
				sExchange = "amq.topic";
			}

			// Send the Asynch Message to the clients
			Send oSendToRabbit = new Send(sExchange);
			oSendToRabbit.SendRabbitMessage(true, "SUBSCRIPTION", sWorkspaceId, oStripePaymentDetail, sWorkspaceId);
			oSendToRabbit.Free();
		} catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionResource.sendRabbitMessage: exception sending asynch notification");
		}
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
	
	public Collection<String> getIdsOfSubscriptionsAssociatedWithUser(String sUserId) {
		Set<String> asCompleteSubscriptionIds = new HashSet<>();

		asCompleteSubscriptionIds.addAll(getIdsOfSubscriptionsOwnedByUser(sUserId));

		asCompleteSubscriptionIds.addAll(getIdsOfSubscriptionsSharedWithUser(sUserId));

		Set<String> asOrganizationIdsOfOwnedByOrSharedWithUser = new OrganizationResource().getIdsOfOrganizationsOwnedByOrSharedWithUser(sUserId);
		asCompleteSubscriptionIds.addAll(getIdsOfSubscriptionsShareddByOrganizations(asOrganizationIdsOfOwnedByOrSharedWithUser));

		return asCompleteSubscriptionIds;
	}

	private Map<String, String> getOrganizationNamesById(Collection<String> asOrganizationIds) {
		OrganizationRepository oOrganizationRepository = new OrganizationRepository();
		List<Organization> aoOrganizations = oOrganizationRepository.getOrganizations(asOrganizationIds);

		return aoOrganizations.stream()
				.collect(Collectors.toMap(Organization::getOrganizationId, Organization::getName));
	}

	private Set<String> getIdsOfSubscriptionsOwnedByUser(String sUserId) {
		SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();

		List<Subscription> aoSubscriptions = oSubscriptionRepository.getSubscriptionsByUser(sUserId);

		return aoSubscriptions.stream().map(Subscription::getSubscriptionId).collect(Collectors.toSet());
	}

	private List<Subscription> getSubscriptionsSharedWithUser(String sUserId) {
		List<String> aoSubscriptionIds = getIdsOfSubscriptionsSharedWithUser(sUserId);

		if (aoSubscriptionIds.isEmpty()) {
			return Collections.emptyList();
		}

		SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();

		return oSubscriptionRepository.getSubscriptionsBySubscriptionIds(aoSubscriptionIds);
	}

	private List<String> getIdsOfSubscriptionsSharedWithUser(String sUserId) {
		UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
		List<UserResourcePermission> aoSharings = oUserResourcePermissionRepository.getSubscriptionSharingsByUserId(sUserId);

		return aoSharings.stream().map(UserResourcePermission::getResourceId).collect(Collectors.toList());
	}

	private List<Subscription> getSubscriptionsSharedByOrganizations(Collection<String> asOrganizationIds) {
		SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();

		return oSubscriptionRepository.getSubscriptionsByOrganizations(asOrganizationIds);
	}

	private Set<String> getIdsOfSubscriptionsShareddByOrganizations(Collection<String> asOrganizationIds) {
		return getSubscriptionsSharedByOrganizations(asOrganizationIds).stream().map(Subscription::getSubscriptionId).collect(Collectors.toSet());
	}
	
	private static List<SubscriptionTypeViewModel> convert(List<SubscriptionType> aoSubscriptionTypes) {
		return aoSubscriptionTypes.stream().map(SubscriptionResource::convert).collect(Collectors.toList());
	}

	private static SubscriptionTypeViewModel convert(SubscriptionType oSubscriptionType) {
		return new SubscriptionTypeViewModel(oSubscriptionType.name(), oSubscriptionType.getTypeName(), oSubscriptionType.getTypeDescription());
	}


}
