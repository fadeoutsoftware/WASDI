package it.fadeout.rest.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
import wasdi.shared.business.Project;
import wasdi.shared.business.Subscription;
import wasdi.shared.business.users.ResourceTypes;
import wasdi.shared.business.users.User;
import wasdi.shared.business.users.UserAccessRights;
import wasdi.shared.business.users.UserApplicationRole;
import wasdi.shared.business.users.UserResourcePermission;
import wasdi.shared.config.StripeProductConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.OrganizationRepository;
import wasdi.shared.data.ProjectRepository;
import wasdi.shared.data.SubscriptionRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.data.UserResourcePermissionRepository;
import wasdi.shared.rabbit.Send;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.TimeEpochUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.ClientMessageCodes;
import wasdi.shared.viewmodels.ErrorResponse;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.SuccessResponse;
import wasdi.shared.viewmodels.organizations.ProjectEditorViewModel;
import wasdi.shared.viewmodels.organizations.StripePaymentDetail;
import wasdi.shared.viewmodels.organizations.SubscriptionListViewModel;
import wasdi.shared.viewmodels.organizations.SubscriptionSharingViewModel;
import wasdi.shared.viewmodels.organizations.SubscriptionType;
import wasdi.shared.viewmodels.organizations.SubscriptionTypeViewModel;
import wasdi.shared.viewmodels.organizations.SubscriptionViewModel;

/**
 * Subscriptions Resource.
 * 
 * Host the API to let the user create (and buy) new subscriptions, and to handle the existing ones.
 * 
 * The payment is implemented using Stripe ( https://stripe.com/ )
 * When the user tries to buy a subscription:
 * 	.an unique code is created
 * 	.an entity not cofirmed is added to the database
 * 	.a link to the stripe platform for paying is created
 * 	.The user is redirected to stripe to pay
 * 	.if the payment is ok, stripe will call back a WASDI api with the secret code
 * 	.WASDI can retrive information from stripe to check that the payment is ok and activate it
 * 
 * Everytime a subscription is created, a project with the same name is also created by default.
 * 
 * 	.Create, edit and delete subscriptions
 * 	.Share subscriptions with other users
 * 	.Generate the Stripe payment url
 * 	.Confirm the Stripe payment results
 * 
 * @author p.campanella
 *
 */
@Path("/subscriptions")
public class SubscriptionResource {
	
	/**
	 * Extract a List of subscriptions for the user
	 * @param oUser Target User
	 * @param bValid True to get only valid subscriptions
	 * @return
	 */
	protected List<SubscriptionListViewModel> getUsersSubscriptionsList(User oUser, boolean bValid) {
		
		List<SubscriptionListViewModel> aoSubscriptionLVM = new ArrayList<>();

		// Domain Check
		if (oUser == null) {
			WasdiLog.warnLog("SubscriptionResource.getUsersSubscriptionsList: invalid session");
			return aoSubscriptionLVM;
		}

		try {
			// 1. subscriptions directly owned by the user
			// 2. subscriptions belonging to organizations owned by the user
			// 3. subscriptions belonging to organizations shared with the user
			// 4. subscriptions shared with the user on individual basis


			// Get the list of Subscriptions owned by the user
			SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();
			List<Subscription> aoOwnedSubscriptions = oSubscriptionRepository.getSubscriptionsByUser(oUser.getUserId());

			Set<String> asOwnedSubscriptionIds = aoOwnedSubscriptions.stream().map(Subscription::getSubscriptionId).collect(Collectors.toSet());
			Set<String> asOrganizationIdsOfOwnedSubscriptions = aoOwnedSubscriptions.stream().map(Subscription::getOrganizationId).filter(Objects::nonNull).collect(Collectors.toSet());
			Map<String, String> aoOrganizationNamesOfOwnedSubscriptions = getOrganizationNamesById(asOrganizationIdsOfOwnedSubscriptions);

			// For each
			for (Subscription oSubscription : aoOwnedSubscriptions) {
				
				if (bValid) {
					if (!oSubscription.isValid()) continue;
				}				
				
				// Create View Model
				SubscriptionListViewModel oSubscriptionViewModel = convertSubscriptionToViewModel(oSubscription, oUser.getUserId(), aoOrganizationNamesOfOwnedSubscriptions.get(oSubscription.getOrganizationId()), "owner");
				
				if (oSubscriptionViewModel != null) {
					oSubscriptionViewModel.setOrganizationId(oSubscription.getOrganizationId());
					oSubscriptionViewModel.setReadOnly(false);
					aoSubscriptionLVM.add(oSubscriptionViewModel);
				}
				else {
					WasdiLog.warnLog("SubscriptionResource.getUsersSubscriptionsList: Error converting a Owned Subscription, jumping");
				}
			}
			
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			
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
						SubscriptionListViewModel oSubscriptionViewModel = convertSubscriptionToViewModel(oSubscription, oUser.getUserId(), aoOrganizationNamesOfOwnedByOrSharedWithUser.get(oSubscription.getOrganizationId()), "shared by " + aoOrganizationNamesOfOwnedByOrSharedWithUser.get(oSubscription.getOrganizationId()));
						
						if (oSubscriptionViewModel != null) {
							
							oSubscriptionViewModel.setReadOnly(!PermissionsUtils.canUserWriteSubscription(oUser.getUserId(), oSubscriptionViewModel.getSubscriptionId()));
							aoSubscriptionLVM.add(oSubscriptionViewModel);
						}
						else {
							WasdiLog.warnLog("SubscriptionResource.getUsersSubscriptionsList: Error converting an Organization Subscription, jumping");
						}						
												
					}
				}
			}
			

			// Get the list of Subscriptions shared with this user
			List<Subscription> aoSharedSubscriptions = getSubscriptionsSharedWithUser(oUser.getUserId());
			List<UserResourcePermission> aoSubscriptionSharings = oUserResourcePermissionRepository.getSubscriptionSharingsByUserId(oUser.getUserId());

			Map<String, String> aoSubscriptionUser = aoSubscriptionSharings.stream().collect(Collectors.toMap(UserResourcePermission::getResourceId, UserResourcePermission::getUserId));


			List<String> asOrganizationIdsOfDirectSubscriptionSharings = aoSharedSubscriptions.stream().map(Subscription::getOrganizationId).collect(Collectors.toList());

			Map<String, String> asNamesOfOrganizationsOfDirectSubscriptionSharings = getOrganizationNamesById(asOrganizationIdsOfDirectSubscriptionSharings);

			for (Subscription oSubscription : aoSharedSubscriptions) {
				
				boolean bToAdd=true;

				if (bValid) {
					bToAdd = oSubscription.isValid();
				}
				
				if (bToAdd) {
					SubscriptionListViewModel oSubscriptionViewModel = convertSubscriptionToViewModel(oSubscription, oUser.getUserId(),
							asNamesOfOrganizationsOfDirectSubscriptionSharings.get(oSubscription.getOrganizationId()),
							"shared by " + aoSubscriptionUser.get(oSubscription.getSubscriptionId()));
	
					if (oSubscriptionViewModel!=null) {
						oSubscriptionViewModel.setReadOnly(!PermissionsUtils.canUserWriteSubscription(oUser.getUserId(), oSubscriptionViewModel.getSubscriptionId()));
						aoSubscriptionLVM.add(oSubscriptionViewModel);
					}
					else {
						WasdiLog.warnLog("SubscriptionResource.getUsersSubscriptionsList: Error converting a Shared Subscription, jumping");
					}						
				}
			}
			
			return aoSubscriptionLVM;
		} 
		catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionResource.getUsersSubscriptionsList error: " + oEx);
			return aoSubscriptionLVM;
		}		
	}
	
	
	@GET
	@Path("/active")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response getActiveByUser(@HeaderParam("x-session-token") String sSessionId) {
		

		WasdiLog.debugLog("SubscriptionResource.getActiveByUser");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		// Domain Check
		if (oUser == null) {
			WasdiLog.warnLog("SubscriptionResource.getActiveByUser: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}
		
		SubscriptionListViewModel oSubscriptionLVM = new SubscriptionListViewModel();

		try {

			WasdiLog.debugLog("SubscriptionResource.getActiveByUser: subscriptions for " + oUser.getUserId());

			List<SubscriptionListViewModel> aoSubscriptions = getUsersSubscriptionsList(oUser, true);
			
			if (aoSubscriptions == null) {
				return Response.status(Status.NOT_FOUND).build();
			}
			
			if (aoSubscriptions.size()<=0) {
				return Response.status(Status.NOT_FOUND).build();
			}
			
			if (aoSubscriptions.size()==1) {
				return Response.ok(aoSubscriptions.get(0)).build();
			}
			
			oSubscriptionLVM = aoSubscriptions.get(0);
			
			for (SubscriptionListViewModel oSLVM : aoSubscriptions) {
				if (oSubscriptionLVM.getEndDate().compareTo(oSLVM.getEndDate())<0) {
					oSubscriptionLVM = oSLVM;					
				}
			}
			
			return Response.ok(oSubscriptionLVM).build();
		} 
		catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionResource.getActiveByUser error: " + oEx);
			return Response.serverError().build();
		}
	}

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

		// Domain Check
		if (oUser == null) {
			WasdiLog.warnLog("SubscriptionResource.getListByUser: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}
		
		List<SubscriptionListViewModel> aoSubscriptionLVM = new ArrayList<>();

		try {

			WasdiLog.debugLog("SubscriptionResource.getListByUser: subscriptions for " + oUser.getUserId() + " Valid = " + bValid);

			aoSubscriptionLVM = getUsersSubscriptionsList(oUser, bValid);
			
			return Response.ok(aoSubscriptionLVM).build();
		} 
		catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionResource.getListByUser error: " + oEx);
			return Response.serverError().build();
		}
	}
	
	@GET
	@Path("/count")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response getSubscriptionsCount(@HeaderParam("x-session-token") String sSessionId) {
		
		WasdiLog.debugLog("SubscriptionResource.getSubscriptionsCount");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.warnLog("SubscriptionResource.getSubscriptionsCount: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}
		
		boolean bAdmin = UserApplicationRole.isAdmin(oUser);

		if (!bAdmin) {
			WasdiLog.warnLog("SubscriptionResource.getSubscriptionsCount: user not admin, aborting");
			return Response.status(Status.FORBIDDEN).entity(new ErrorResponse("The user cannot access the subscription count.")).build();
		}		

		try {
			// Create repo
			SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();
			// Get requested subscription
			long lCount = oSubscriptionRepository.getSubscriptionsCount();
			
			Long oCount = Long.valueOf(lCount);
			
			PrimitiveResult oPrimitiveResult = new PrimitiveResult();
			oPrimitiveResult.setIntValue(oCount.intValue()); 

			return Response.ok(oPrimitiveResult).build();
		} 
		catch (Exception oEx) {
			WasdiLog.errorLog( "SubscriptionResource.getSubscriptionsCount: " + oEx);
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
	public Response getSubscriptionViewModel(@HeaderParam("x-session-token") String sSessionId, @QueryParam("subscription") String sSubscriptionId) {
		WasdiLog.debugLog("SubscriptionResource.getSubscriptionViewModel( Subscription: " + sSubscriptionId + ")");

		SubscriptionViewModel oSubscriptionViewModel = new SubscriptionViewModel();

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.warnLog("SubscriptionResource.getSubscriptionViewModel: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}
		
		// Domain Check
		if (Utils.isNullOrEmpty(sSubscriptionId)) {
			WasdiLog.warnLog("SubscriptionResource.getSubscriptionViewModel: invalid subscription id");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("Invalid subscriptionId.")).build();
		}
		
		boolean bAdmin = UserApplicationRole.isAdmin(oUser);

		if (!PermissionsUtils.canUserAccessSubscription(oUser.getUserId(), sSubscriptionId) && !bAdmin) {
			WasdiLog.warnLog("SubscriptionResource.getSubscriptionViewModel: user cannot access subscription info, aborting");
			return Response.status(Status.FORBIDDEN).entity(new ErrorResponse("The user cannot access the subscription info.")).build();
		}		

		try {
			// Create repo
			SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();
			// Get requested subscription
			Subscription oSubscription = oSubscriptionRepository.getSubscriptionById(sSubscriptionId);

			if (oSubscription == null) {
				WasdiLog.warnLog("SubscriptionResource.getSubscriptionViewModel: the subscription cannot be found, aborting");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("The subscription cannot be found.")).build();
			}

			String sOrganizationName = null;

			if (oSubscription.getOrganizationId() != null) {
				OrganizationRepository oOrganizationRepository = new OrganizationRepository();
				Organization oOrganization = oOrganizationRepository.getById(oSubscription.getOrganizationId());
				sOrganizationName = oOrganization.getName();
			}

			oSubscriptionViewModel = convert(oSubscription, sOrganizationName);
			
			if (bAdmin) oSubscriptionViewModel.setReadOnly(false);
			else oSubscriptionViewModel.setReadOnly(!PermissionsUtils.canUserWriteSubscription(oUser.getUserId(), sSubscriptionId));

			return Response.ok(oSubscriptionViewModel).build();
		} 
		catch (Exception oEx) {
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
			WasdiLog.warnLog("SubscriptionResource.createSubscription: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}
		
		try {
			SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();
			
			String sName = oSubscriptionViewModel.getName();			
			Subscription oSubscription = convertViewModelToSubscription(oSubscriptionViewModel);

			String sUserId = oSubscriptionViewModel.getUserId();
			
			if (!UserApplicationRole.isAdmin(oUser)) {
				sUserId = oUser.getUserId();
				if (oSubscriptionViewModel.isBuySuccess()) {
					WasdiLog.warnLog("SubscriptionResource.createSubscription: the user is not an admin so CANNOT set buy success true");
					oSubscription.setBuySuccess(false);		
				}
			}
				
			if (Utils.isNullOrEmpty(sUserId)) sUserId = oUser.getUserId();
			
			UserRepository oUserRepo = new UserRepository();
			User oTargetUser = oUserRepo.getUser(sUserId);

			while (oSubscriptionRepository.getByNameAndUserId(sName, sUserId) != null) {
				sName = Utils.cloneName(sName);
				WasdiLog.debugLog("SubscriptionResource.createSubscription: a subscription with the same name already exists. Changing the name to " + sName);
			}
			
			oSubscription.setSubscriptionId(Utils.getRandomName());
			oSubscription.setName(sName);			
			oSubscription.setUserId(sUserId);

			if (oSubscriptionRepository.insertSubscription(oSubscription)) {
				ProjectEditorViewModel oProjectEditorViewModel = new ProjectEditorViewModel();
				oProjectEditorViewModel.setName(sName);
				oProjectEditorViewModel.setDescription("Project automatically created for the " + oSubscription.getName() + " subscription");
				oProjectEditorViewModel.setSubscriptionId(oSubscription.getSubscriptionId());
				oProjectEditorViewModel.setActiveProject(oTargetUser.getActiveProjectId() == null);
				oProjectEditorViewModel.setTargetUser(sUserId);

				new ProjectResource().createProject(sSessionId, oProjectEditorViewModel);

				return Response.ok(new SuccessResponse(oSubscription.getSubscriptionId())).build();
			} 
			else {
				WasdiLog.debugLog("SubscriptionResource.createSubscription: insertion failed");
				return Response.serverError().build();
			}			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionResource.createSubscription: error " ,  oEx);
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
			WasdiLog.warnLog("SubscriptionResource.updateSubscription: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}
		
		if (oSubscriptionViewModel == null) {
			WasdiLog.warnLog("SubscriptionResource.updateSubscription: invalid body");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SUBSCRIPTION.name())).build();			
		}
		
		try {
			boolean bAdmin = UserApplicationRole.isAdmin(oUser);
			
			if (!PermissionsUtils.canUserWriteSubscription(oUser.getUserId(), oSubscriptionViewModel.getSubscriptionId()) && !bAdmin) {
				WasdiLog.warnLog("SubscriptionResource.updateSubscription: user cannot write subscription");
				return Response.status(Status.FORBIDDEN).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_NO_ACCESS_RIGHTS_OBJECT_SUBSCRIPTION.name())).build();						
			}

			SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();

			Subscription oExistingSubscription = oSubscriptionRepository.getSubscriptionById(oSubscriptionViewModel.getSubscriptionId());

			if (oExistingSubscription == null) {
				WasdiLog.warnLog("SubscriptionResource.updateSubscription: subscription does not exist");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("No subscription with the Id exists.")).build();
			}

			Subscription oExistingSubscriptionWithTheSameName = oSubscriptionRepository.getByName(oSubscriptionViewModel.getName());

			if (oExistingSubscriptionWithTheSameName != null
					&& !oExistingSubscriptionWithTheSameName.getSubscriptionId().equalsIgnoreCase(oExistingSubscription.getSubscriptionId())) {
				WasdiLog.warnLog("SubscriptionResource.updateSubscription: a different subscription with the same name already exists");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("A subscription with the same name already exists.")).build();
			}

			Subscription oSubscription = convertViewModelToSubscription(oSubscriptionViewModel);
			
			if (!bAdmin) {
				if (oSubscriptionViewModel.isBuySuccess() != oExistingSubscription.isBuySuccess()) {
					WasdiLog.warnLog("SubscriptionResource.updateSubscription: the user is not an admin so CANNOT change buy success true");
					oSubscription.setBuySuccess(oExistingSubscription.isBuySuccess());					
				}
			}		

			if (oSubscriptionRepository.updateSubscription(oSubscription)) {
				return Response.ok(new SuccessResponse(oSubscription.getSubscriptionId())).build();
			}
			else {WasdiLog.debugLog("SubscriptionResource.updateSubscription( " + oSubscriptionViewModel.getName() + " ): update failed");
				return Response.serverError().build();
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionResource.updateSubscription: error ", oEx);
			return Response.serverError().build();
		}
	}

	/**
	 * Deletes a Subscription
	 * @param sSessionId Session Id
	 * @param sSubscriptionId Subscription Id
	 * @return SuccessResponse or ErrorResponse: both are a view model with a simple message property
	 */
	@DELETE
	@Path("/delete")
	@Produces({"application/json", "application/xml", "text/xml" })
	public Response deleteSubscription(@HeaderParam("x-session-token") String sSessionId, @QueryParam("subscription") String sSubscriptionId) {
		
		WasdiLog.debugLog("SubscriptionResource.deleteSubscription( Subscription: " + sSubscriptionId + " )");
		
		// Check the user id
		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.warnLog("SubscriptionResource.deleteSubscription: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}
		
		// And the subscription id
		if (Utils.isNullOrEmpty(sSubscriptionId)) {
			WasdiLog.warnLog("SubscriptionResource.deleteSubscription: invalid subscription id");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("Invalid subscriptionId.")).build();
		}		
		
		try {
			
			// Take the repository and read the subscription
			SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();
			Subscription oSubscription = oSubscriptionRepository.getSubscriptionById(sSubscriptionId);
			
			// Subscription must exists
			if (oSubscription == null) {
				WasdiLog.warnLog("SubscriptionResource.deleteSubscription: subscription does not exist");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("No subscription with the name exists.")).build();
			}
			
			boolean bAdmin = UserApplicationRole.isAdmin(oUser);
			
			// And the user must be able to access it
			if (!PermissionsUtils.canUserAccessSubscription(oUser.getUserId(), sSubscriptionId) && !bAdmin) {
				WasdiLog.warnLog("SubscriptionResource.deleteSubscription: user cannot access subscription");
				return Response.status(Status.FORBIDDEN).entity(new ErrorResponse("Forbidden.")).build();
			}

			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();

			String sSubscriptionOwner = oSubscription.getUserId();

			if (!sSubscriptionOwner.equals(oUser.getUserId()) && !bAdmin) {
				// The current user is not the owner of the subscription
				UserResourcePermission oPermission = oUserResourcePermissionRepository.getSubscriptionSharingByUserIdAndSubscriptionId(oUser.getUserId(), sSubscriptionId);

				if (oPermission == null) {
					WasdiLog.errorLog("SubscriptionResource.deleteSubscription: our user " + oUser.getUserId() + " is not the owner of the Subscription " + sSubscriptionId + " [" + sSubscriptionOwner+ "] but has not Perimssions: how can access it?!?!");
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse("Subscription cannot be removed")).build();
				} 
				else {
					WasdiLog.debugLog("SubscriptionResource.deleteSubscription: user " + oUser.getUserId() + " is not the owner [" + sSubscriptionOwner + "]: delete the sharing, not the subscription");
					oUserResourcePermissionRepository.deletePermissionsByUserIdAndSubscriptionId(oUser.getUserId(), sSubscriptionId);
					return Response.ok(new SuccessResponse("Sharing removed")).build();
				}
			}
			else {
				WasdiLog.debugLog("This is the owner or an admin: lets clean all");
				oUserResourcePermissionRepository.deletePermissionsBySubscriptionId(sSubscriptionId);
				ProjectRepository oProjectRepository = new ProjectRepository();
				oProjectRepository.deleteBySubscription(sSubscriptionId);
				
				if (oSubscriptionRepository.deleteSubscription(sSubscriptionId)) {
					return Response.ok(new SuccessResponse("Done")).build();
				} 
				else {
					WasdiLog.warnLog("SubscriptionResource.deleteSubscription: deletion failed");
					return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("Error deleting the subscription.")).build();
				}				
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionResource.deleteSubscription: error ", oEx);
			return Response.serverError().build();			
		}
	}

	/**
	 * Get the types of available subscriptions
	 * 
	 * @param sSessionId Session Id
	 * @return List of SubscriptionTypeViewModel
	 */
	@GET
	@Path("/types")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response getSubscriptionTypes(@HeaderParam("x-session-token") String sSessionId) {
		WasdiLog.debugLog("SubscriptionResource.getSubscriptionTypes");
		try {
			return Response.ok(convert(Arrays.asList(SubscriptionType.values()))).build();
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionResource.getSubscriptionTypes: error ", oEx);
			return Response.serverError().build();			
		}		
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
			@QueryParam("subscription") String sSubscriptionId, @QueryParam("userId") String sDestinationUserId, @QueryParam("rights") String sRights) {

		WasdiLog.debugLog("SubscriptionResource.shareSubscription( WS: " + sSubscriptionId + ", User: " + sDestinationUserId + " )");

		// Validate Session
		User oRequestingUser = Wasdi.getUserFromSession(sSessionId);
		if (oRequestingUser == null) {
			WasdiLog.warnLog("SubscriptionResource.shareSubscription: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}
		
		// Use Read By default
		if (!UserAccessRights.isValidAccessRight(sRights)) {
			sRights = UserAccessRights.READ.getAccessRight();
		}		

		// Domain Check
		if (Utils.isNullOrEmpty(sSubscriptionId)) {
			WasdiLog.warnLog("SubscriptionResource.shareSubscription: invalid subscription id");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("Invalid subscriptionId.")).build();
		}

		// Check if the subscription exists
		SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();
		Subscription oSubscription = oSubscriptionRepository.getSubscriptionById(sSubscriptionId);
		
		if (oSubscription == null) {
			WasdiLog.warnLog("SubscriptionResource.shareSubscription: invalid subscription");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SUBSCRIPTION.name())).build();
		}
		
		// Can the user access this resource?
		if (!PermissionsUtils.canUserWriteSubscription(oRequestingUser.getUserId(), sSubscriptionId) && !UserApplicationRole.isAdmin(oRequestingUser)) {
			WasdiLog.warnLog("SubscriptionResource.shareSubscription:user cannot access subscription");
			return Response.status(Status.FORBIDDEN).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_NO_ACCESS_RIGHTS_OBJECT_SUBSCRIPTION.name())).build();
		}

		// Cannot Autoshare
		if (oRequestingUser.getUserId().equals(sDestinationUserId)) {
			WasdiLog.warnLog("SubscriptionResource.shareSubscription: auto sharing not so smart");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_SHARING_WITH_ONESELF.name())).build();
		}

		// Cannot share with the owner
		if (sDestinationUserId.equals(oSubscription.getUserId())) {
			WasdiLog.warnLog("SubscriptionResource.shareSubscription: sharing with the owner not so smart");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_SHARING_WITH_OWNER.name())).build();
		}

		UserRepository oUserRepository = new UserRepository();
		User oDestinationUser = oUserRepository.getUser(sDestinationUserId);

		if (oDestinationUser == null) {
			//No. So it is neither the owner or a shared one
			WasdiLog.warnLog("SubscriptionResource.shareSubscription: Destination user does not exists");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_SHARING_WITH_NON_EXISTENT_USER.name())).build();
		}

		try {
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();

			if (!oUserResourcePermissionRepository.isSubscriptionSharedWithUser(sDestinationUserId, sSubscriptionId)) {
				
				UserResourcePermission oSubscriptionSharing = new UserResourcePermission(ResourceTypes.SUBSCRIPTION.getResourceType(), sSubscriptionId, sDestinationUserId, oSubscription.getUserId(), oRequestingUser.getUserId(), sRights);
				oUserResourcePermissionRepository.insertPermission(oSubscriptionSharing);

				return Response.ok(new SuccessResponse("Done")).build();
			} else {
				WasdiLog.debugLog("SubscriptionResource.shareSubscription: already shared!");
				return Response.ok(new SuccessResponse("Already Shared.")).build();
			}
		} 
		catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionResource.shareSubscription error: " + oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_IN_INSERT_PROCESS.name())).build();
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
			WasdiLog.warnLog("SubscriptionResource.getEnableUsersSharedSubscription: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}
		
		if (!PermissionsUtils.canUserAccessSubscription(oRequestingUser.getUserId(), sSubscriptionId)) {
			WasdiLog.warnLog("SubscriptionResource.getEnableUsersSharedSubscription: invalid session");
			return Response.status(Status.FORBIDDEN).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_NO_ACCESS_RIGHTS_OBJECT_SUBSCRIPTION.name())).build();			
		}

		try {
			UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
			aoSubscriptionSharing = oUserResourcePermissionRepository.getSubscriptionSharingsBySubscriptionId(sSubscriptionId);

			if (aoSubscriptionSharing != null) {
				for (UserResourcePermission oSubscriptionSharing : aoSubscriptionSharing) {
					SubscriptionSharingViewModel oSubscriptionSharingViewModel = new SubscriptionSharingViewModel(oSubscriptionSharing);
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

	/**
	 * Removes the sharing of an user with the subscription
	 * @param sSessionId Session Id 
	 * @param sSubscriptionId Subscription id
	 * @param sUserId User to remove from the subscription 
	 * @return SuccessResponse or ErrorResponse
	 */
	@DELETE
	@Path("share/delete")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response deleteUserSharedSubscription(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("subscription") String sSubscriptionId, @QueryParam("userId") String sUserId) {

		WasdiLog.debugLog("SubscriptionResource.deleteUserSharedSubscription( WS: " + sSubscriptionId + ", User:" + sUserId + " )");

		// Validate Session
		User oRequestingUser = Wasdi.getUserFromSession(sSessionId);

		if (oRequestingUser == null) {
			WasdiLog.warnLog("SubscriptionResource.deleteUserSharedSubscription: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		// Domain Check
		if (Utils.isNullOrEmpty(sSubscriptionId)) {
			WasdiLog.warnLog("SubscriptionResource.deleteUserSharedSubscription: invalid subscription");
			return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("Invalid subscriptionId.")).build();
		}
		
		// Can the user access this resource?
		if (!PermissionsUtils.canUserAccessSubscription(oRequestingUser.getUserId(), sSubscriptionId)
				&& !UserApplicationRole.isAdmin(oRequestingUser)) {
			WasdiLog.warnLog("SubscriptionResource.deleteUserSharedSubscription:user cannot access subscription");
			return Response.status(Status.FORBIDDEN).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_NO_ACCESS_RIGHTS_OBJECT_SUBSCRIPTION.name())).build();
		}		

		try {
			
			// Take the destination user
			UserRepository oUserRepository = new UserRepository();
			User oDestinationUser = oUserRepository.getUser(sUserId);

			if (oDestinationUser == null) {
				WasdiLog.warnLog("SubscriptionResource.deleteUserSharedSubscription: invalid destination user");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_DESTINATION_USER.name())).build();
			}
			
			// Read the subscription
			SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();
			Subscription oSubscription = oSubscriptionRepository.getSubscriptionById(sSubscriptionId);
			
			if (oSubscription == null) {
				WasdiLog.warnLog("SubscriptionResource.deleteUserSharedSubscription: invalid subscription");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SUBSCRIPTION.name())).build();				
			}
			
			// Is this the owner removing his self?
			if (oSubscription.getUserId().equals(oDestinationUser.getUserId())) {
				// Cannot be done
				WasdiLog.warnLog("SubscriptionResource.deleteUserSharedSubscription: do not share with the owner!");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_SHARING_WITH_OWNER.name())).build();								
			}
			else {
				
				// Let see if this permission exist
				UserResourcePermissionRepository oUserResourcePermissionRepository = new UserResourcePermissionRepository();
				UserResourcePermission oPermission = oUserResourcePermissionRepository.getSubscriptionSharingByUserIdAndSubscriptionId(sUserId, sSubscriptionId);
				
				if (oPermission == null) {
					WasdiLog.warnLog("SubscriptionResource.deleteUserSharedSubscription: do not share with the owner!");
					return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_DESTINATION_USER.name())).build();					
				}
				else {
					
					// If the user can write or is the actual beneficiay of the share
					if (PermissionsUtils.canUserWriteSubscription(oRequestingUser.getUserId(), sSubscriptionId) ||
							oPermission.getUserId().equals(sUserId)) {
						oUserResourcePermissionRepository.deletePermissionsByUserIdAndSubscriptionId(sUserId, sSubscriptionId);
						
						return Response.ok(new SuccessResponse("Done")).build();
					}
					else {
						// Otherwise, cannot be done
						WasdiLog.warnLog("SubscriptionResource.deleteUserSharedSubscription: user is not the owner and cannot write the subscription");
						return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_NO_ACCESS_RIGHTS_OBJECT_SUBSCRIPTION.name())).build();						
					}
				}
			}

		} catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionResource.deleteUserSharedSubscription: " + oEx);

			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_IN_DELETE_PROCESS.name())).build();
		}
	}
	
	/**
	 * Generates the Stripe Payment Url
	 * @param sSessionId Session Id
	 * @param sSubscriptionId Subscription Id
	 * @param sWorkspaceId Workspace Id
	 * @return SuccessResponse with url in the message or ErrorResponse with the code of the error 
	 */
	@GET
	@Path("/stripe/paymentUrl")
	@Produces({"application/json", "application/xml", "text/xml" })
	public Response getStripePaymentUrl(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("subscription") String sSubscriptionId, @QueryParam("workspace") String sWorkspaceId) {
		WasdiLog.debugLog("SubscriptionResource.getStripePaymentUrl( " + "Subscription: " + sSubscriptionId + ", "
				+ "Workspace: " + sWorkspaceId + ")");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.warnLog("SubscriptionResource.getStripePaymentUrl: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		try {
			// Domain Check
			if (Utils.isNullOrEmpty(sSubscriptionId)) {
				WasdiLog.warnLog("SubscriptionResource.getStripePaymentUrl: invalid subscription id");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("Invalid subscriptionId.")).build();
			}

			if (!PermissionsUtils.canUserAccessSubscription(oUser.getUserId(), sSubscriptionId)) {
				WasdiLog.warnLog("SubscriptionResource.getStripePaymentUrl: user cannot access subscription info");
				return Response.status(Status.FORBIDDEN).entity(new ErrorResponse("The user cannot access the subscription info.")).build();
			}

			// Create repo
			SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();

			// Get requested subscription
			Subscription oSubscription = oSubscriptionRepository.getSubscriptionById(sSubscriptionId);

			if (oSubscription == null) {
				WasdiLog.warnLog("SubscriptionResource.getStripePaymentUrl: subscription does not exist");
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
				WasdiLog.warnLog("SubscriptionResource.getStripePaymentUrl: the subscription does not have a valid type, aborting");
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
		} 
		catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionResource.getStripePaymentUrl error " + oEx);
			return Response.serverError().build();
		}
	}
	
	/**
	 * Confirms the subscription after the successful transaction from Stripe
	 * The API should be called from Stripe. It connects again to Stripe to verify that the info is correct
	 * If all is fine it activates the subscription
	 * 
	 * @param sCheckoutSessionId Secret Checkout code used to link the stripe payment with the subscription
	 * @return
	 */
	@GET
	@Path("/stripe/confirmation/{CHECKOUT_SESSION_ID}")
	@Produces({"application/json", "application/xml", "text/xml" })
	public Response confirmation(@PathParam("CHECKOUT_SESSION_ID") String sCheckoutSessionId) {
		WasdiLog.debugLog("SubscriptionResource.confirmation( sCheckoutSessionId: " + sCheckoutSessionId + ")");

		if (Utils.isNullOrEmpty(sCheckoutSessionId)) {
			WasdiLog.warnLog("SubscriptionResource.confirmation: Stripe returned a null CHECKOUT_SESSION_ID, aborting");
			return Response.status(Status.UNAUTHORIZED).build();
		}

		try {
			StripeService oStripeService = new StripeService();
			StripePaymentDetail oStripePaymentDetail = oStripeService.retrieveStripePaymentDetail(sCheckoutSessionId);

			String sClientReferenceId = oStripePaymentDetail.getClientReferenceId();

			if (oStripePaymentDetail == null || Utils.isNullOrEmpty(sClientReferenceId)) {
				WasdiLog.warnLog("SubscriptionResource.confirmation: Stripe returned an invalid result, aborting");
				return Response.status(Status.INTERNAL_SERVER_ERROR).build();
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
				} 
				else {
					oSubscription.setBuyDate(Utils.nowInMillis());
					oSubscription.setBuySuccess(true);

					oSubscriptionRepository.updateSubscription(oSubscription);
					
					UserRepository oUserRepository = new UserRepository();
					User oUser = oUserRepository.getUser(oSubscription.getUserId());
					
					if (Utils.isNullOrEmpty(oUser.getActiveProjectId())) {
						WasdiLog.debugLog("SubscriptionResource.confirmation: user has no active project: set the new one");
						ProjectRepository oProjectRepository = new ProjectRepository();
						List<Project> aoProjects = oProjectRepository.getProjectsBySubscription(sSubscriptionId);
						
						if (aoProjects != null) {
							if (aoProjects.size()>0) {
								String sProjectId = aoProjects.get(0).getProjectId();
								oUser.setActiveProjectId(sProjectId);
								WasdiLog.debugLog("SubscriptionResource.confirmation: user has no active project: set the new one " + sProjectId);
							}
						}
					}				

					if (!Utils.isNullOrEmpty(sWorkspaceId)) {
						sendRabbitMessage(sWorkspaceId, oStripePaymentDetail);
					}
				}
			}		
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionResource.confirmation error ", oEx);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
		
		return Response.ok().build();

	}

	
	/**
	 * Get the list of subscriptions associated to a user.
	 * @param sSessionId User Session Id
	 * @return a View Model with the Subscription Name and 
	 * 	a flag to know if the user is admin or not of the subscription
	 */
	@GET
	@Path("/list")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response getSortedList(@HeaderParam("x-session-token") String sSessionId, 
			@QueryParam("userfilter") String sUserFilter, 
			@QueryParam("idfilter") String sIdFilter, 
			@QueryParam("namefilter") String sNameFilter,
			@QueryParam("statusfilter") String sStatus,
			@QueryParam("offset") Integer iOffset,
			@QueryParam("limit") Integer iLimit,
			@QueryParam("sortby") String sSorting,
			@QueryParam("order") String sOrder) {

		WasdiLog.debugLog("SubscriptionResource.getList");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		// Domain Check
		if (oUser == null) {
			WasdiLog.warnLog("SubscriptionResource.getListByUser: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}
		
		// Can the user access this section?
		if (!UserApplicationRole.isAdmin(oUser)) {
			return Response.status(Status.FORBIDDEN).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_NO_ACCESS_RIGHTS_ADMIN_DASHBOARD.name())).build();
		}
		
		// cleaning of the input
		if (iOffset == null) {
			iOffset = 0;
			WasdiLog.debugLog("SubscriptionResource.getList: setting offset to 0 as default");
		}

		if (iLimit == null) {
			iLimit = 10;
			WasdiLog.debugLog("SubscriptionResource.getList: setting limit to 10 as default");
		}
		
		if (Utils.isNullOrEmpty(sSorting)) {
			sSorting = "name";
			WasdiLog.debugLog("SubscriptionResource.getList: setting sorting to 'name' as default");
		}
		
		if (Utils.isNullOrEmpty(sOrder)) {
			sOrder = "asc";
			WasdiLog.debugLog("SubscriptionResource.getList: setting ordering to 'asc' as default");
		}
		
		// validation of the inputs
		if (! (sSorting.equals("name") 
				|| sSorting.equals("userId") 
				|| sSorting.equals("endDate")
				|| sSorting.equals(""))) {
			sSorting = "userId"; 
			WasdiLog.warnLog("SubscriptionResource.getList: setting the sorting to 'userId' because of a not valid option " + sSorting);
		}
		
		int iOrder = 1;
		
		if (sOrder.equals("desc") || sOrder.equals("des") || sOrder.equals("0") || sOrder.equals("-1")) {
			iOrder = -1;
			WasdiLog.debugLog("SubscriptionResource.getList: setting iOrder to -1 due to order= " + sOrder);
		}

		
		List<SubscriptionListViewModel> aoSubscriptionLVM = new ArrayList<>();

		try {

			WasdiLog.debugLog("SubscriptionResource.getList: subscriptions with User filter " + sUserFilter + " sIdFilter " + sIdFilter+ " sNameFilter " + sNameFilter 
					+ ", sorting " + sSorting + " ordering " + iOrder);

			// Get the list of Subscriptions owned by the user
			SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();
			List<Subscription> aoSubscriptions = oSubscriptionRepository.findSubscriptionsByFilters(sNameFilter, sIdFilter, sUserFilter, sSorting, iOrder);
			
			Set<String> asOrganizationIds = aoSubscriptions.stream()
					.map(Subscription::getOrganizationId)
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());
			Map<String, String> aoOrganizationNames = getOrganizationNamesById(asOrganizationIds);

			// For each
			for (int iSubscriptions = 0; iSubscriptions<aoSubscriptions.size(); iSubscriptions ++) {
				
				if (iSubscriptions<iOffset) continue;
				if (iSubscriptions>=iOffset+iLimit) break;
				
				Subscription oSubscription = aoSubscriptions.get(iSubscriptions);
				
				// Create View Model
				SubscriptionListViewModel oSubscriptionViewModel = convertSubscriptionToViewModel(oSubscription, oUser.getUserId(), aoOrganizationNames.get(oSubscription.getOrganizationId()), "owner");
				
				if (oSubscriptionViewModel != null) {
					oSubscriptionViewModel.setOrganizationId(oSubscription.getOrganizationId());
					oSubscriptionViewModel.setReadOnly(false);
					aoSubscriptionLVM.add(oSubscriptionViewModel);
				}
				else {
					WasdiLog.warnLog("SubscriptionResource.getList: Error converting a Subscription, jumping");
				}
			}
			
			return Response.ok(aoSubscriptionLVM).build();
		} 
		catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionResource.getListByUser error: " + oEx);
			return Response.serverError().build();
		}
	}
	
	
	
	
	/**
	 * Utility method to send a rabbit message
	 * @param sWorkspaceId
	 * @param oStripePaymentDetail
	 */
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

	/**
	 * Converts a Subscription entity to a View Model
	 * @param oSubscription
	 * @param sOrganizationName
	 * @return
	 */
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

	/**
	 * Convert a Subscription entity to a view model
	 * @param oSubscription
	 * @param sCurrentUserId
	 * @param sOrganizationName
	 * @param sReason
	 * @return
	 */
	private static SubscriptionListViewModel convertSubscriptionToViewModel(Subscription oSubscription, String sCurrentUserId, String sOrganizationName, String sReason) {
		try {
			SubscriptionListViewModel oSubscriptionListViewModel = new SubscriptionListViewModel();
			oSubscriptionListViewModel.setSubscriptionId(oSubscription.getSubscriptionId());
			oSubscriptionListViewModel.setName(oSubscription.getName());
			oSubscriptionListViewModel.setTypeId(oSubscription.getType());
			oSubscriptionListViewModel.setTypeName(SubscriptionType.get(oSubscription.getType()).getTypeName());
			oSubscriptionListViewModel.setOrganizationName(sOrganizationName);
			oSubscriptionListViewModel.setReason(sReason);
			oSubscriptionListViewModel.setStartDate(TimeEpochUtils.fromEpochToDateString(oSubscription.getStartDate()));
			oSubscriptionListViewModel.setEndDate(TimeEpochUtils.fromEpochToDateString(oSubscription.getEndDate()));		
			oSubscriptionListViewModel.setBuySuccess(oSubscription.isBuySuccess());
			oSubscriptionListViewModel.setOwnerUserId(oSubscription.getUserId());

			return oSubscriptionListViewModel;			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionResource.convert exception: ", oEx);
			if (oSubscription!=null) {
				String sSubId = oSubscription.getSubscriptionId();
				if (Utils.isNullOrEmpty(sSubId)) sSubId = "Subscritption Id == NULL";
				
				WasdiLog.errorLog("SubscriptionResource.convert Subscription Id: " + sSubId);
			}
			return null;
		}
	}

	/**
	 * Converts a Subscription View Model to  an entity
	 * @param oSubscriptionViewModel
	 * @return
	 */
	private static Subscription convertViewModelToSubscription(SubscriptionViewModel oSubscriptionViewModel) {
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
