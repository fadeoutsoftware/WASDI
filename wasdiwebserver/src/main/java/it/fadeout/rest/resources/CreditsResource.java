package it.fadeout.rest.resources;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import it.fadeout.Wasdi;
import it.fadeout.services.StripeService;
import wasdi.shared.business.CreditPackage;
import wasdi.shared.business.Project;
import wasdi.shared.business.Subscription;
import wasdi.shared.business.users.User;
import wasdi.shared.business.users.UserApplicationRole;
import wasdi.shared.config.StripeProductConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.CreditsPagackageRepository;
import wasdi.shared.data.ProjectRepository;
import wasdi.shared.data.SubscriptionRepository;
import wasdi.shared.data.UserRepository;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.ClientMessageCodes;
import wasdi.shared.viewmodels.ErrorResponse;
import wasdi.shared.viewmodels.SuccessResponse;
import wasdi.shared.viewmodels.organizations.CreditPackageType;
import wasdi.shared.viewmodels.organizations.CreditsPackageViewModel;
import wasdi.shared.viewmodels.organizations.StripePaymentDetail;
import wasdi.shared.viewmodels.organizations.SubscriptionType;
import wasdi.shared.viewmodels.organizations.SubscriptionTypeViewModel;

@Path("/credits")
public class CreditsResource {
	
	/**
	 * Get the types of available credits that a user can buy
	 * 
	 * @param sSessionId Session Id
	 * @return List of view models (the same as the subcription view model) to represent the credit packages types
	 */
	@GET
	@Path("/types")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response getCreditsPackages(@HeaderParam("x-session-token") String sSessionId) {
		WasdiLog.debugLog("CreditsResource.getCreditsPackages");
		try {
			return Response.ok(convert(Arrays.asList(CreditPackageType.values()))).build();
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("CreditsResource.getCreditsPackages: error ", oEx);
			return Response.serverError().build();			
		}		
	}
	
	private static List<SubscriptionTypeViewModel> convert(List<CreditPackageType> aoSubscriptionTypes) {
		return aoSubscriptionTypes.stream()
				.map(CreditsResource::convert)
				.collect(Collectors.toList());
	}

	private static SubscriptionTypeViewModel convert(CreditPackageType oSubscriptionType) {
		return new SubscriptionTypeViewModel(oSubscriptionType.name(), oSubscriptionType.getTypeName(), oSubscriptionType.getTypeDescription());
	}
	
	/**
	 * Create a new credit package for a user.
	 * @param sSessionId User Session Id
	 * @param oSubscriptionViewModel the subscription to be created
	 * @return a primitive result containing the outcome of the operation
	 */
	@POST
	@Path("/add")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response addCreditPackage(@HeaderParam("x-session-token") String sSessionId, CreditsPackageViewModel oCreditsPackageViewModel) {
		WasdiLog.debugLog("CreditsResource.addCreditPackage");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.warnLog("CreditsResource.addCreditPackage: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}
		
		try {
			
			CreditsPagackageRepository oCreditsPackageRepository = new CreditsPagackageRepository();
			
			String sCreditPackageId = Utils.getRandomName();
			while (oCreditsPackageRepository.getCreditPackageById(sCreditPackageId) != null) {
				sCreditPackageId = Utils.getRandomName();
			}
			
			String sUserId = oCreditsPackageViewModel.getUserId();
			if (Utils.isNullOrEmpty(sUserId)) 
				sUserId = oUser.getUserId();
				
			String sName = oCreditsPackageViewModel.getName();	
			while (oCreditsPackageRepository.getCreditPackageByNameAndUserId(sName, sUserId) != null) {
				sName = Utils.cloneName(sName);
				WasdiLog.debugLog("CreditsResource.addCreditPackage: a credit package with the same name already exists. Changing the name to " + sName);
			}
			
			String sDescription = oCreditsPackageViewModel.getDescription();
			
			String sType = oCreditsPackageViewModel.getType();
			CreditPackageType oPackageType = CreditPackageType.valueOf(sType);
			if (oPackageType == null) {
				WasdiLog.errorLog("CreditsResource.addCreditPackage: no credit package corresponding to " + sType);
				return Response.status(Status.BAD_REQUEST).build();
			}
			
			float fCredits = oPackageType.getCredits();

			boolean bIsBuySuccess = oCreditsPackageViewModel.isBuySuccess();
			if (!UserApplicationRole.isAdmin(oUser) && bIsBuySuccess) {
					WasdiLog.warnLog("CreditsResource.addCreditPackage: the user is not an admin so CANNOT set buy success true");
					bIsBuySuccess = false;					
			}
						
			CreditPackage oCreditPackage = new CreditPackage(sCreditPackageId, sName, sDescription, sType, 0d, sUserId, bIsBuySuccess, fCredits, 0d);

			if (oCreditsPackageRepository.insertCreditPackage(oCreditPackage)) {
				return Response.ok(new SuccessResponse(oCreditPackage.getCreditPackageId())).build();
			} 
			else {
				WasdiLog.debugLog("CreditsResource.addCreditPackage: insertion failed");
				return Response.serverError().build();
			}			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("CreditsResource.addCreditPackage: error " ,  oEx);
			return Response.serverError().build();
		}
	}
	
	/**
	 * Generates the Stripe Payment Url
	 * @param sSessionId Session Id
	 * @param sCredits package id
	 * @return SuccessResponse with url in the message or ErrorResponse with the code of the error 
	 */
	@GET
	@Path("/stripe/paymentUrl")
	@Produces({"application/json", "application/xml", "text/xml" })
	public Response getStripePaymentUrl(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("subscription") String sCreditPackageId) {
		WasdiLog.debugLog("CreditsResource.getStripePaymentUrl( " + "Credits package id: " + sCreditPackageId + ")");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.warnLog("CreditsResource.getStripePaymentUrl: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(ClientMessageCodes.MSG_ERROR_INVALID_SESSION.name())).build();
		}

		try {
			// Domain Check
			if (Utils.isNullOrEmpty(sCreditPackageId)) {
				WasdiLog.warnLog("CreditsResource.getStripePaymentUrl: invalid credits package id");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("Invalid credits package.")).build();
			}
			
			CreditsPagackageRepository oCreditsPackageRepository = new CreditsPagackageRepository();
			
			CreditPackage oCreditPackage = oCreditsPackageRepository.getCreditPackageById(sCreditPackageId);
			
			if (oCreditPackage == null) {
				WasdiLog.warnLog("CreditsResource.getStripePaymentUrl: credits package does not exist");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("The credit package cannot be found.")).build();
			}

			if (!oCreditPackage.getUserId().equals(oUser.getUserId())) {
				WasdiLog.warnLog("CreditsResource.getStripePaymentUrl: user cannot access credits package info");
				return Response.status(Status.FORBIDDEN).entity(new ErrorResponse("The user cannot access the credits package info.")).build();
			}

			String sCreditsPackageType = oCreditPackage.getType();
			if (Utils.isNullOrEmpty(oCreditPackage.getType())) {
				WasdiLog.debugLog("CreditsResource.getStripePaymentUrl: the credit package does not have a valid type, aborting");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("The credit package does not have a valid type.")).build();
			}

			List<StripeProductConfig> aoProductConfigList = WasdiConfig.Current.stripe.products;

			Map<String, String> aoProductConfigMap = aoProductConfigList.stream()
					.collect(Collectors.toMap(t -> t.id, t -> t.url));

			CreditPackageType oCreditPackageType = CreditPackageType.get(sCreditsPackageType);

			if (oCreditPackageType == null) {
				WasdiLog.warnLog("CreditsResource.getStripePaymentUrl: the credit package type does not have a valid type, aborting");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("The credit package does not have a valid type.")).build();
			} 
			
			String sBaseUrl = aoProductConfigMap.get(oCreditPackageType.getTypeId());

			if (Utils.isNullOrEmpty(sBaseUrl)) {
				WasdiLog.debugLog("CreditsResource.getStripePaymentUrl: the config does not contain a valid configuration for the subscription");
				return Response.serverError().build();
			} 
			else {
				String sUrl = sBaseUrl + "?client_reference_id=" + sCreditPackageId;

				return Response.ok(new SuccessResponse(sUrl)).build();
			}
			
		} 
		catch (Exception oEx) {
			WasdiLog.errorLog("CreditsResource.getStripePaymentUrl error " + oEx);
			return Response.serverError().build();
		}
	}
	
	/**
	 * Confirms the credits package payment after the successful transaction from Stripe
	 * The API should be called from Stripe. It connects again to Stripe to verify that the info is correct
	 * If all is fine it activates the subscription
	 * 
	 * @param sCheckoutSessionId Secret Checkout code used to link the stripe payment with the subscription
	 * @return
	 */
	@GET
	@Path("/stripe/confirmation/{CHECKOUT_SESSION_ID}")
	@Produces({"application/json", "application/xml", "text/xml" })
	public String confirmation(@PathParam("CHECKOUT_SESSION_ID") String sCheckoutSessionId) {
		WasdiLog.debugLog("CreditsResource.confirmation( sCheckoutSessionId: " + sCheckoutSessionId + ")");

		if (Utils.isNullOrEmpty(sCheckoutSessionId)) {
			WasdiLog.warnLog("CreditsResource.confirmation: Stripe returned a null CHECKOUT_SESSION_ID, aborting");
			return null;
		}

		try {
			StripeService oStripeService = new StripeService();
			StripePaymentDetail oStripePaymentDetail = oStripeService.retrieveStripePaymentDetail(sCheckoutSessionId);

			String sCreditsPackagegeId = oStripePaymentDetail.getClientReferenceId();

			if (oStripePaymentDetail == null || Utils.isNullOrEmpty(sCreditsPackagegeId)) {
				WasdiLog.warnLog("CreditsResource.confirmation: Stripe returned an invalid result, aborting");
				return null;
			}

			WasdiLog.debugLog("CreditsResource.confirmation( sCreditPackageId: " + sCreditsPackagegeId + ")");

			if (oStripePaymentDetail != null) {

				CreditsPagackageRepository oCreditsPackageRepository = new CreditsPagackageRepository();

				CreditPackage oCreditsPackage = oCreditsPackageRepository.getCreditPackageById(sCreditsPackagegeId);

				if (oCreditsPackage == null) {
					WasdiLog.debugLog("CreditsResource.confirmation: credits package does not exist");
				} 
				else {
					Double dNow = Utils.nowInMillis();
					oCreditsPackage.setBuyDate(dNow);
					oCreditsPackage.setLastUpdate(dNow);
					oCreditsPackage.setBuySuccess(true);

					if (!oCreditsPackageRepository.updateCreditPackage(oCreditsPackage)) {
						WasdiLog.errorLog("CreditsResource.confirmation: there was an error updating the credits package");
					}
				}				
			}	
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionResource.confirmation error ", oEx);
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

	

}
