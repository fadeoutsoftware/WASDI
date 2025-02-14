package it.fadeout.rest.resources;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.organizations.CreditPackageType;
import wasdi.shared.viewmodels.organizations.SubscriptionType;
import wasdi.shared.viewmodels.organizations.SubscriptionTypeViewModel;

@Path("/credits")
public class CreditsResource {
	
	/**
	 * Get the types of available credits that a user can buy
	 * 
	 * @param sSessionId Session Id
	 * @return List of SubscriptionTypeViewModel
	 */
	@GET
	@Path("/types")
	@Produces({ "application/xml", "application/json", "text/xml" })
	public Response CreditsResource(@HeaderParam("x-session-token") String sSessionId) {
		WasdiLog.debugLog("SubscriptionResource.getSubscriptionTypes");
		try {
			return Response.ok(convert(Arrays.asList(CreditPackageType.values()))).build();
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("SubscriptionResource.getSubscriptionTypes: error ", oEx);
			return Response.serverError().build();			
		}		
	}
	
	private static List<SubscriptionTypeViewModel> convert(List<CreditPackageType> aoSubscriptionTypes) {
		return aoSubscriptionTypes.stream().map(CreditsResource::convert).collect(Collectors.toList());
	}

	private static SubscriptionTypeViewModel convert(CreditPackageType oSubscriptionType) {
		return new SubscriptionTypeViewModel(oSubscriptionType.name(), oSubscriptionType.getTypeName(), oSubscriptionType.getTypeDescription());
	}

}
