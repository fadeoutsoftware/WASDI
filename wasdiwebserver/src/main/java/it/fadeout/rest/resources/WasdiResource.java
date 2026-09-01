package it.fadeout.rest.resources;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;

import io.swagger.v3.oas.annotations.Operation;
import it.fadeout.Wasdi;
import wasdi.shared.business.users.User;
import wasdi.shared.utils.MailUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.feedback.FeedbackViewModel;

/**
 * Wasdi Resource.
 * 
 * Hosts API for:
 * 	.Keep Alive (hello)
 * 	.Send Feedback
 * 
 * @author p.campanella
 *
 */
@Path("wasdi")
public class WasdiResource {
	
	/**
	 * Hello API (is alive)
	 * @return
	 */
	@GET
	@Path("/hello")
	@Produces({ "application/xml", "application/json", "text/xml" })
	@Operation(summary = "Check service availability", description = "Returns a simple greeting used to verify that the WASDI REST service is available.")
	public PrimitiveResult hello() {
		WasdiLog.debugLog("WasdiResource.hello");
		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setStringValue("Hello Wasdi!!");
		return oResult;
	}
	
	/**
	 * Feedback
	 * @return
	 */
	@POST
	@Path("/feedback")
	@Produces({ "application/json", "text/xml" })
	@Operation(summary = "Send user feedback", description = "Sends an authenticated user's feedback message by email after validating that its title and message are present.")
	public PrimitiveResult feedback(@Context ContainerRequestContext oRequestContext, FeedbackViewModel oFeedback) {
		WasdiLog.debugLog("WasdiResource.feedback");

		PrimitiveResult oPrimitiveResult = new PrimitiveResult();
		
		String sSessionId = (String) oRequestContext.getProperty("session-id");

		if (Utils.isNullOrEmpty(sSessionId)) {
			WasdiLog.warnLog("WasdiResource.feedback: invalid session");
			oPrimitiveResult.setIntValue(401);
			return oPrimitiveResult;
		}

		if (oFeedback == null
				|| Utils.isNullOrEmpty(oFeedback.getTitle())
				|| Utils.isNullOrEmpty(oFeedback.getMessage())) {
			WasdiLog.warnLog("WasdiResource.feedback: empty or invalid payload");
			oPrimitiveResult.setIntValue(404);
			return oPrimitiveResult;
		}

		User oUser = (User) oRequestContext.getProperty("authenticated-user");

		if (oUser == null || Utils.isNullOrEmpty(oUser.getUserId())) {
			oPrimitiveResult.setIntValue(401);
			return oPrimitiveResult;
		}

		String sUserId = oUser.getUserId();

		String sTitle = oFeedback.getTitle();
		String sMessage = oFeedback.getMessage();

		MailUtils.sendEmail(sUserId, sUserId, sTitle, sMessage, true);

		oPrimitiveResult.setIntValue(201);
		oPrimitiveResult.setBoolValue(true);

		return oPrimitiveResult;
	}
	
}
