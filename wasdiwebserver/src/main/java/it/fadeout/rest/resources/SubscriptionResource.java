package it.fadeout.rest.resources;

import static wasdi.shared.business.UserApplicationPermission.ADMIN_DASHBOARD;
import static wasdi.shared.business.UserApplicationPermission.SUBSCRIPTION_READ;

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
import wasdi.shared.business.Subscription;
import wasdi.shared.business.User;
import wasdi.shared.business.UserApplicationRole;
import wasdi.shared.business.UserResourcePermission;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.SubscriptionRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.data.UserResourcePermissionRepository;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.organizations.SubscriptionListViewModel;
import wasdi.shared.viewmodels.organizations.SubscriptionViewModel;
//import wasdi.shared.viewmodels.organizations.SubscriptionSharingViewModel;

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
	public List<SubscriptionListViewModel> getListByUser(@HeaderParam("x-session-token") String sSessionId) {

		WasdiLog.debugLog("SubscriptionResource.getListByUser()");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		List<SubscriptionListViewModel> aoSubscriptionList = new ArrayList<>();

		// Domain Check
		if (oUser == null) {
			WasdiLog.debugLog("SubscriptionResource.getListByUser: invalid session: " + sSessionId);
			return aoSubscriptionList;
		}

		try {
			if (!UserApplicationRole.userHasRightsToAccessApplicationResource(oUser.getRole(), SUBSCRIPTION_READ)) {
				return aoSubscriptionList;
			}

			WasdiLog.debugLog("SubscriptionResource.getListByUser: subscriptions for " + oUser.getUserId());

			// Create repo
			SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();

			// Get Subscription List
			List<Subscription> aoSubscriptions = oSubscriptionRepository.getSubscriptionByUser(oUser.getUserId());

			// For each
			for (Subscription oSubscription : aoSubscriptions) {
				// Create View Model
				SubscriptionListViewModel oSubscriptionViewModel = convert(oSubscription, oUser.getUserId());

//				// Get Sharings
//				List<UserResourcePermission> aoPermissions = oUserResourcePermissionRepository
//						.getSubscriptionSharingsBySubscriptionId(oSubscription.getSubscriptionId());
//
//				// Add Sharings to View Model
//				if (aoPermissions != null) {
//					for (UserResourcePermission oUserResourcePermission : aoPermissions) {
//						if (oSubscriptionViewModel.getSharedUsers() == null) {
//							oSubscriptionViewModel.setSharedUsers(new ArrayList<String>());
//						}
//
//						oSubscriptionViewModel.getSharedUsers().add(oUserResourcePermission.getUserId());
//					}
//				}

				aoSubscriptionList.add(oSubscriptionViewModel);
			}

			// Get the list of subscriptions shared with this user
			List<UserResourcePermission> aoSharedSubscriptions = oUserResourcePermissionRepository.getSubscriptionSharingsByUserId(oUser.getUserId());

			if (aoSharedSubscriptions.size() > 0) {
				// For each
				for (UserResourcePermission oSharedSubscription : aoSharedSubscriptions) {
					Subscription oSubscription = oSubscriptionRepository.getSubscription(oSharedSubscription.getResourceId());

					if (oSubscription == null) {
						WasdiLog.debugLog("SubscriptionResource.getListByUser: Subscription Shared not available " + oSharedSubscription.getResourceId());
						continue;
					}

					SubscriptionListViewModel oSubscriptionViewModel = convert(oSubscription, oUser.getUserId());

//					// Get Sharings
//					List<UserResourcePermission> aoSharings = oUserResourcePermissionRepository.getSubscriptionSharingsBySubscriptionId(oSubscription.getSubscriptionId());
//
//					// Add Sharings to View Model
//					if (aoSharings != null) {
//						for (UserResourcePermission oSharing : aoSharings) {
//							if (oSubscriptionViewModel.getSharedUsers() == null) {
//								oSubscriptionViewModel.setSharedUsers(new ArrayList<String>());
//							}
//
//							oSubscriptionViewModel.getSharedUsers().add(oSharing.getUserId());
//						}
//					}

					aoSubscriptionList.add(oSubscriptionViewModel);
				}
			}
		} catch (Exception oEx) {
			oEx.toString();
		}

		return aoSubscriptionList;
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
	public SubscriptionViewModel getSubscriptionViewModel(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("subscription") String sSubscriptionId) {
		WasdiLog.debugLog("SubscriptionResource.getSubscriptionViewModel( Subscription: " + sSubscriptionId + ")");

		SubscriptionViewModel oVM = new SubscriptionViewModel();

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.debugLog("SubscriptionResource.getSubscriptionViewModel: invalid session");
			return null;
		}

		try {
			// Domain Check
			if (Utils.isNullOrEmpty(sSubscriptionId)) {
				return oVM;
			}

			if (!PermissionsUtils.canUserAccessSubscription(oUser.getUserId(), sSubscriptionId)) {
				WasdiLog.debugLog("SubscriptionResource.getSubscriptionViewModel: user cannot access subscription info, aborting");
				return oVM;
			}

			WasdiLog.debugLog("SubscriptionResource.getSubscriptionViewModel: read subscriptions " + sSubscriptionId);

			// Create repo
			SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();
//			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();

			// Get requested subscription
			Subscription oSubscription = oSubscriptionRepository.getSubscription(sSubscriptionId);

			oVM = convert(oSubscription);

//			// Get Sharings
//			List<UserResourcePermission> aoSharings = oUserResourcePermissionRepository
//					.getSubscriptionSharingsBySubscriptionId(oSubscription.getSubscriptionId());
//			// Add Sharings to View Model
//			if (aoSharings != null) {
//				if (oVM.getSharedUsers() == null) {
//					oVM.setSharedUsers(new ArrayList<String>());
//				}
//
//				for (UserResourcePermission oSharing : aoSharings) {
//					oVM.getSharedUsers().add(oSharing.getUserId());
//				}
//			}
		} catch (Exception oEx) {
			WasdiLog.debugLog( "SubscriptionResource.getSubscriptionViewModel: " + oEx);
		}

		return oVM;
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
	public PrimitiveResult createSubscription(@HeaderParam("x-session-token") String sSessionId, SubscriptionViewModel oSubscriptionViewModel) {
		WasdiLog.debugLog("SubscriptionResource.createSubscription( Subscription: " + oSubscriptionViewModel.toString() + ")");

		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.debugLog("SubscriptionResource.createSubscription: invalid session");
			oResult.setStringValue("Invalid session.");
			return oResult;
		}

		SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();

		Subscription oExistingSubscription = oSubscriptionRepository.getByName(oSubscriptionViewModel.getName());

		if (oExistingSubscription != null) {
			WasdiLog.debugLog("SubscriptionResource.createSubscription: a different subscription with the same name already exists");
			oResult.setStringValue("An subscription with the same name already exists.");
			return oResult;
		}

		Subscription oSubscription = convert(oSubscriptionViewModel);
		oSubscription.setUserId(oUser.getUserId());
		oSubscription.setOrganizationId(Utils.getRandomName());

		if (oSubscriptionRepository.insertSubscription(oSubscription)) {
			oResult.setBoolValue(true);
			oResult.setStringValue(oSubscription.getSubscriptionId());
		} else {WasdiLog.debugLog("SubscriptionResource.createSubscription( " + oSubscriptionViewModel.getName() + " ): insertion failed");
			oResult.setStringValue("The creation of the subscription failed.");
		}

		return oResult;
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
	public PrimitiveResult upateSubscription(@HeaderParam("x-session-token") String sSessionId, SubscriptionViewModel oSubscriptionViewModel) {
		WasdiLog.debugLog("SubscriptionResource.updateSubscription( Subscription: " + oSubscriptionViewModel.toString() + ")");

		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.debugLog("SubscriptionResource.updateSubscription: invalid session");
			oResult.setStringValue("Invalid session.");
			return oResult;
		}

		SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();

		Subscription oExistingSubscription = oSubscriptionRepository.getById(oSubscriptionViewModel.getSubscriptionId());

		if (oExistingSubscription == null) {
			WasdiLog.debugLog("SubscriptionResource.updateSubscription: subscription does not exist");
			oResult.setStringValue("No subscription with the Id exists.");
			return oResult;
		}

		Subscription oExistingSubscriptionWithTheSameName = oSubscriptionRepository.getByName(oSubscriptionViewModel.getName());

		if (oExistingSubscriptionWithTheSameName != null
				&& oExistingSubscriptionWithTheSameName.getSubscriptionId() != oExistingSubscription.getSubscriptionId()) {
			WasdiLog.debugLog("SubscriptionResource.updateSubscription: a different subscription with the same name already exists");
			oResult.setStringValue("A subscription with the same name already exists.");
			return oResult;
		}

		Subscription oSubscription = convert(oSubscriptionViewModel);

		if (oSubscriptionRepository.updateSubscription(oSubscription)) {
			oResult.setBoolValue(true);
			oResult.setStringValue(oSubscription.getSubscriptionId());
		} else {WasdiLog.debugLog("SubscriptionResource.updateSubscription( " + oSubscriptionViewModel.getName() + " ): update failed");
			oResult.setStringValue("The update of the subscription failed.");
		}

		return oResult;
	}

	@DELETE
	@Path("/delete")
	public PrimitiveResult deleteSubscription(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("subscription") String sSubscriptionId) {
		WasdiLog.debugLog("SubscriptionResource.deleteSubscription( Subscription: " + sSubscriptionId + " )");

		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.debugLog("SubscriptionResource.deleteSubscription: invalid session");
			oResult.setStringValue("Invalid session.");
			return oResult;
		}

		SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();

		Subscription oExistingSubscription = oSubscriptionRepository.getById(sSubscriptionId);

		if (oExistingSubscription == null) {
			WasdiLog.debugLog("SubscriptionResource.deleteSubscription: subscription does not exist");
			oResult.setStringValue("No subscription with the name already exists.");
			return oResult;
		}

		if (oSubscriptionRepository.deleteSubscription(sSubscriptionId)) {
			oResult.setBoolValue(true);
			oResult.setStringValue(sSubscriptionId);
		} else {
			WasdiLog.debugLog("SubscriptionResource.deleteSubscription( " + sSubscriptionId + " ): deletion failed");
			oResult.setStringValue("The deletion of the subscription failed.");
		}

		return oResult;
	}

//	/**
//	 * Share a subscription with another user.
//	 *
//	 * @param sSessionId User Session Id
//	 * @param sSubscriptionId Subscription Id
//	 * @param sDestinationUserId User id that will receive the subscription in sharing.
//	 * @return Primitive Result with boolValue = true and stringValue = Done if ok. False and error description otherwise
//	 */
//	@PUT
//	@Path("share/add")
//	@Produces({ "application/xml", "application/json", "text/xml" })
//	public PrimitiveResult shareSubscription(@HeaderParam("x-session-token") String sSessionId,
//			@QueryParam("subscription") String sSubscriptionId, @QueryParam("userId") String sDestinationUserId) {
//
//		WasdiLog.debugLog("SubscriptionResource.ShareSubscription( WS: " + sSubscriptionId + ", User: " + sDestinationUserId + " )");
//
//		PrimitiveResult oResult = new PrimitiveResult();
//		oResult.setBoolValue(false);
//
//
//		// Validate Session
//		User oRequesterUser = Wasdi.getUserFromSession(sSessionId);
//		if (oRequesterUser == null) {
//			WasdiLog.debugLog("SubscriptionResource.shareSubscription: invalid session");
//
//			oResult.setIntValue(Status.UNAUTHORIZED.getStatusCode());
//			oResult.setStringValue(MSG_ERROR_INVALID_SESSION);
//
//			return oResult;
//		}
//		
//		// Check if the subscription exists
//		SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();
//		Subscription oSubscription = oSubscriptionRepository.getSubscription(sSubscriptionId);
//		
//		if (oSubscription == null) {
//			WasdiLog.debugLog("SubscriptionResource.ShareSubscription: invalid subscription");
//
//			oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
//			oResult.setStringValue(MSG_ERROR_INVALID_SUBSCRIPTION);
//
//			return oResult;
//		}
//
//		// Can the user access this section?
//		if (!UserApplicationRole.userHasRightsToAccessApplicationResource(oRequesterUser.getRole(), SUBSCRIPTION_READ)) {
//			WasdiLog.debugLog("SubscriptionResource.shareSubscription: " + oRequesterUser.getUserId() + " cannot access the section " + ", aborting");
//
//			oResult.setIntValue(Status.FORBIDDEN.getStatusCode());
//			oResult.setStringValue(MSG_ERROR_NO_ACCESS_RIGHTS_APPLICATION_RESOURCE_SUBSCRIPTION);
//
//			return oResult;
//		}
//
//		// Can the user access this resource?
//		if (!PermissionsUtils.canUserAccessSubscription(oRequesterUser.getUserId(), sSubscriptionId)
//				&& !UserApplicationRole.userHasRightsToAccessApplicationResource(oRequesterUser.getRole(), ADMIN_DASHBOARD)) {
//			WasdiLog.debugLog("SubscriptionResource.shareSubscription: " + sSubscriptionId + " cannot be accessed by " + oRequesterUser.getUserId() + ", aborting");
//
//			oResult.setIntValue(Status.FORBIDDEN.getStatusCode());
//			oResult.setStringValue(MSG_ERROR_NO_ACCESS_RIGHTS_OBJECT_SUBSCRIPTION);
//
//			return oResult;
//		}
//
//		// Cannot Autoshare
//		if (oRequesterUser.getUserId().equals(sDestinationUserId)) {
//			if (UserApplicationRole.userHasRightsToAccessApplicationResource(oRequesterUser.getRole(), ADMIN_DASHBOARD)) {
//				// A user that has Admin rights should be able to auto-share the resource.
//			} else {
//				WasdiLog.debugLog("SubscriptionResource.ShareSubscription: auto sharing not so smart");
//
//				oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
//				oResult.setStringValue(MSG_ERROR_SHARING_WITH_ONESELF);
//
//				return oResult;
//			}
//		}
//
//		// Cannot share with the owner
//		if (sDestinationUserId.equals(oSubscription.getUserId())) {
//			WasdiLog.debugLog("SubscriptionResource.ShareSubscription: sharing with the owner not so smart");
//
//			oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
//			oResult.setStringValue(MSG_ERROR_SHARING_WITH_OWNER);
//
//			return oResult;
//		}
//
//		UserRepository oUserRepository = new UserRepository();
//		User oDestinationUser = oUserRepository.getUser(sDestinationUserId);
//
//		if (oDestinationUser == null) {
//			//No. So it is neither the owner or a shared one
//			WasdiLog.debugLog("SubscriptionResource.ShareSubscription: Destination user does not exists");
//
//			oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
//			oResult.setStringValue(MSG_ERROR_SHARING_WITH_NON_EXISTENT_USER);
//
//			return oResult;
//		}
//
//		try {
//			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
//
//			if (!oUserResourcePermissionRepository.isSubscriptionSharedWithUser(sDestinationUserId, sSubscriptionId)) {
//				UserResourcePermission oSubscriptionSharing =
//						new UserResourcePermission("subscription", sSubscriptionId, sDestinationUserId, oSubscription.getUserId(), oRequesterUser.getUserId(), "write");
//
//				oUserResourcePermissionRepository.insertPermission(oSubscriptionSharing);				
//			} else {
//				WasdiLog.debugLog("SubscriptionResource.ShareSubscription: already shared!");
//				oResult.setStringValue("Already Shared.");
//				oResult.setBoolValue(true);
//				oResult.setIntValue(Status.OK.getStatusCode());
//
//				return oResult;
//			}
//		} catch (Exception oEx) {
//			WasdiLog.debugLog("SubscriptionResource.ShareSubscription: " + oEx);
//
//			oResult.setIntValue(Status.INTERNAL_SERVER_ERROR.getStatusCode());
//			oResult.setStringValue(MSG_ERROR_IN_INSERT_PROCESS);
//
//			return oResult;
//		}
//
//		sendNotificationEmail(oRequesterUser.getUserId(), sDestinationUserId, oSubscription.getName());
//
//		oResult.setStringValue("Done");
//		oResult.setBoolValue(true);
//
//		return oResult;
//	}
//
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
//
//	/**
//	 * Get the list of users that has a Subscription in sharing.
//	 *
//	 * @param sSessionId User Session
//	 * @param sSubscriptionId Subscription Id
//	 * @return list of Subscription Sharing View Models
//	 */
//	@GET
//	@Path("share/bysubscription")
//	@Produces({ "application/xml", "application/json", "text/xml" })
//	public List<SubscriptionSharingViewModel> getEnableUsersSharedWorksace(@HeaderParam("x-session-token") String sSessionId, @QueryParam("subscription") String sSubscriptionId) {
//
//		WasdiLog.debugLog("SubscriptionResource.getEnableUsersSharedWorksace( WS: " + sSubscriptionId + " )");
//
//	
//		List<UserResourcePermission> aoSubscriptionSharing = null;
//		List<SubscriptionSharingViewModel> aoSubscriptionSharingViewModels = new ArrayList<SubscriptionSharingViewModel>();
//
//		User oOwnerUser = Wasdi.getUserFromSession(sSessionId);
//		if (oOwnerUser == null) {
//			WasdiLog.debugLog("SubscriptionResource.getEnableUsersSharedWorksace: invalid session");
//			return aoSubscriptionSharingViewModels;
//		}
//
//		try {
//			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
//			aoSubscriptionSharing = oUserResourcePermissionRepository.getSubscriptionSharingsBySubscriptionId(sSubscriptionId);
//
//			if (aoSubscriptionSharing != null) {
//				for (UserResourcePermission oSubscriptionSharing : aoSubscriptionSharing) {
//					SubscriptionSharingViewModel oSubscriptionSharingViewModel = new SubscriptionSharingViewModel();
//					oSubscriptionSharingViewModel.setOwnerId(oSubscriptionSharing.getUserId());
//					oSubscriptionSharingViewModel.setUserId(oSubscriptionSharing.getUserId());
//					oSubscriptionSharingViewModel.setSubscriptionId(oSubscriptionSharing.getResourceId());
//
//					aoSubscriptionSharingViewModels.add(oSubscriptionSharingViewModel);
//				}
//
//			}
//
//		} catch (Exception oEx) {
//			WasdiLog.debugLog("SubscriptionResource.getEnableUsersSharedWorksace: " + oEx);
//			return aoSubscriptionSharingViewModels;
//		}
//
//		return aoSubscriptionSharingViewModels;
//
//	}
//
//	@DELETE
//	@Path("share/delete")
//	@Produces({ "application/xml", "application/json", "text/xml" })
//	public PrimitiveResult deleteUserSharedSubscription(@HeaderParam("x-session-token") String sSessionId,
//			@QueryParam("subscription") String sSubscriptionId, @QueryParam("userId") String sUserId) {
//
//		WasdiLog.debugLog("SubscriptionResource.deleteUserSharedSubscription( WS: " + sSubscriptionId + ", User:" + sUserId + " )");
//		PrimitiveResult oResult = new PrimitiveResult();
//		oResult.setBoolValue(false);
//		// Validate Session
//		User oRequestingUser = Wasdi.getUserFromSession(sSessionId);
//
//		if (oRequestingUser == null) {
//			WasdiLog.debugLog("SubscriptionResource.deleteUserSharedSubscription: invalid session");
//
//			oResult.setIntValue(Status.UNAUTHORIZED.getStatusCode());
//			oResult.setStringValue(MSG_ERROR_INVALID_SESSION);
//
//			return oResult;
//		}
//
//		try {
//
//			UserRepository oUserRepository = new UserRepository();
//			User oDestinationUser = oUserRepository.getUser(sUserId);
//
//			if (oDestinationUser == null) {
//				WasdiLog.debugLog("SubscriptionResource.deleteUserSharedSubscription: invalid destination user");
//
//				oResult.setIntValue(Status.BAD_REQUEST.getStatusCode());
//				oResult.setStringValue(MSG_ERROR_INVALID_DESTINATION_USER);
//
//				return oResult;
//			}
//
//			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
//			oUserResourcePermissionRepository.deletePermissionsByUserIdAndSubscriptionId(sUserId, sSubscriptionId);
//		} catch (Exception oEx) {
//			WasdiLog.debugLog("SubscriptionResource.deleteUserSharedSubscription: " + oEx);
//
//			oResult.setIntValue(Status.INTERNAL_SERVER_ERROR.getStatusCode());
//			oResult.setStringValue(MSG_ERROR_IN_DELETE_PROCESS);
//
//			return oResult;
//		}
//
//		oResult.setStringValue("Done");
//		oResult.setBoolValue(true);
//		oResult.setIntValue(Status.OK.getStatusCode());
//
//		return oResult;
//	}

	private static SubscriptionViewModel convert(Subscription oSubscription) {
		SubscriptionViewModel oSubscriptionViewModel = new SubscriptionViewModel();
		oSubscriptionViewModel.setSubscriptionId(oSubscription.getSubscriptionId());
		oSubscriptionViewModel.setName(oSubscription.getName());
		oSubscriptionViewModel.setDescription(oSubscription.getDescription());
		oSubscriptionViewModel.setType(oSubscription.getType());
		oSubscriptionViewModel.setBuyDate(oSubscription.getBuyDate());
		oSubscriptionViewModel.setStartDate(oSubscription.getStartDate());
		oSubscriptionViewModel.setEndDate(oSubscription.getEndDate());
		oSubscriptionViewModel.setDurationDays(oSubscription.getDurationDays());
		oSubscriptionViewModel.setUserId(oSubscription.getUserId());
		oSubscriptionViewModel.setOrganizationId(oSubscription.getOrganizationId());
		oSubscriptionViewModel.setBuySuccess(oSubscription.isBuySuccess());

		return oSubscriptionViewModel;
	}

	private static SubscriptionListViewModel convert(Subscription oSubscription, String sCurrentUserId) {
		SubscriptionListViewModel oSubscriptionListViewModel = new SubscriptionListViewModel();
		oSubscriptionListViewModel.setSubscriptionId(oSubscription.getSubscriptionId());
		oSubscriptionListViewModel.setName(oSubscription.getName());
		oSubscriptionListViewModel.setOwnerUserId(oSubscription.getUserId());
		oSubscriptionListViewModel.setAdminRole(sCurrentUserId.equalsIgnoreCase(oSubscription.getUserId()));

		return oSubscriptionListViewModel;
	}

	private static Subscription convert(SubscriptionViewModel oSubscriptionViewModel) {
		Subscription oSubscription = new Subscription();
		oSubscription.setSubscriptionId(oSubscriptionViewModel.getSubscriptionId());
		oSubscription.setName(oSubscriptionViewModel.getName());
		oSubscription.setDescription(oSubscriptionViewModel.getDescription());
		oSubscription.setType(oSubscriptionViewModel.getType());
		oSubscription.setBuyDate(oSubscriptionViewModel.getBuyDate());
		oSubscription.setStartDate(oSubscriptionViewModel.getStartDate());
		oSubscription.setEndDate(oSubscriptionViewModel.getEndDate());
		oSubscription.setDurationDays(oSubscriptionViewModel.getDurationDays());
		oSubscription.setUserId(oSubscriptionViewModel.getUserId());
		oSubscription.setOrganizationId(oSubscriptionViewModel.getOrganizationId());
		oSubscription.setBuySuccess(oSubscriptionViewModel.isBuySuccess());

		return oSubscription;
	}

}
