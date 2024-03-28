package it.fadeout.rest.resources;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

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
	public PrimitiveResult feedback(@HeaderParam("x-session-token") String sSessionId, FeedbackViewModel oFeedback) {
		WasdiLog.debugLog("WasdiResource.feedback");

		PrimitiveResult oPrimitiveResult = new PrimitiveResult();

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

		User oUser = Wasdi.getUserFromSession(sSessionId);

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
