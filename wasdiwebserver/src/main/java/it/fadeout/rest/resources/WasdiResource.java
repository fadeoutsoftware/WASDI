package it.fadeout.rest.resources;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import it.fadeout.Wasdi;
import it.fadeout.mercurius.business.Message;
import it.fadeout.mercurius.client.MercuriusAPI;
import wasdi.shared.business.User;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.feedback.FeedbackViewModel;

/**
 * Wasdi Resource.
 * 
 * Hosts API for:
 * 	.Keep Alive
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
		Utils.debugLog("WasdiResource.hello");
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
		Utils.debugLog("WasdiResource.feedback");

		PrimitiveResult oPrimitiveResult = new PrimitiveResult();

		if (Utils.isNullOrEmpty(sSessionId)) {
			Utils.debugLog("WasdiResource.feedback( Session: " + sSessionId + " ): invalid session");
			oPrimitiveResult.setIntValue(401);
			return oPrimitiveResult;
		}

		if (oFeedback == null
				|| Utils.isNullOrEmpty(oFeedback.getTitle())
				|| Utils.isNullOrEmpty(oFeedback.getMessage())) {
			Utils.debugLog("WasdiResource.feedback: empty or invalid payload");
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

		sendEmail(sUserId, sTitle, sMessage);

		oPrimitiveResult.setIntValue(201);
		oPrimitiveResult.setBoolValue(true);

		return oPrimitiveResult;
	}

	private void sendEmail(String sUserId, String sTitle, String sMessage) {
		try {
			String sMercuriusAPIAddress = WasdiConfig.Current.notifications.mercuriusAPIAddress;

			if(Utils.isNullOrEmpty(sMercuriusAPIAddress)) {
				Utils.debugLog("WasdiResource.sendEmail: sMercuriusAPIAddress is null");
			} else {
				Utils.debugLog("WasdiResource.sendEmail: send notification");

				MercuriusAPI oAPI = new MercuriusAPI(sMercuriusAPIAddress);			
				Message oMessage = new Message();

				oMessage.setTilte(sTitle);
				
				oMessage.setSender(sUserId);

				oMessage.setMessage(sMessage);

				Integer iPositiveSucceded = 0;
				
				String sWasdiAdminMail = WasdiConfig.Current.notifications.wasdiAdminMail;

				if (Utils.isNullOrEmpty(sWasdiAdminMail)) {
					sWasdiAdminMail = "info@fadeout.biz";
				}
				
				sWasdiAdminMail += ";" + sUserId;

				iPositiveSucceded = oAPI.sendMailDirect(sWasdiAdminMail, oMessage);

				Utils.debugLog("WasdiResource.sendEmail: notification sent with result " + iPositiveSucceded);
			}
		} catch (Exception oEx) {
			Utils.debugLog("WasdiResource.sendEmail: notification exception " + oEx.toString());
		}
	}
}
