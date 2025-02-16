package it.fadeout.rest.resources;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import it.fadeout.Wasdi;
import wasdi.shared.business.CreditPackage;
import wasdi.shared.business.users.User;
import wasdi.shared.business.users.UserApplicationRole;
import wasdi.shared.data.CreditsPagackageRepository;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.ClientMessageCodes;
import wasdi.shared.viewmodels.ErrorResponse;
import wasdi.shared.viewmodels.SuccessResponse;
import wasdi.shared.viewmodels.organizations.CreditPackageType;
import wasdi.shared.viewmodels.organizations.CreditsPackageViewModel;
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
			
			Double dBuyDate = Utils.nowInMillis();
			
			CreditPackage oCreditPackage = new CreditPackage(sCreditPackageId, sName, sDescription, sType, dBuyDate, sUserId, bIsBuySuccess, fCredits, dBuyDate);

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


}
