package it.fadeout.rest.resources;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import it.fadeout.Wasdi;
import it.fadeout.mercurius.business.Message;
import it.fadeout.mercurius.client.MercuriusAPI;
import wasdi.shared.business.users.User;
import wasdi.shared.config.WasdiConfig;
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

		sendEmail(sUserId, sUserId, sTitle, sMessage, true);

		oPrimitiveResult.setIntValue(201);
		oPrimitiveResult.setBoolValue(true);

		return oPrimitiveResult;
	}
	
	/**
	 * Send an email from sender to recipient with title and message
	 * @param sSender Sender of the mail
	 * @param sRecipient Recipient 
	 * @param sTitle Title
	 * @param sMessage Message
	 * @return true if sent false otherwise
	 */
	public static boolean sendEmail(String sSender, String sRecipient, String sTitle, String sMessage) {
		return sendEmail(sSender, sRecipient, sTitle, sMessage, false);
	}
	
	/**
	 * Send an email from sender to recipient with title and message
	 * @param sSender Sender of the mail
	 * @param sRecipient Recipient 
	 * @param sTitle Title
	 * @param sMessage Message
	 * @param bAddAdminToRecipient Set true to add by default the WADSI admin to the recipient
	 * @return true if sent false otherwise
	 */
	public static boolean sendEmail(String sSender, String sRecipient, String sTitle, String sMessage, boolean bAddAdminToRecipient) {
		try {
			String sMercuriusAPIAddress = WasdiConfig.Current.notifications.mercuriusAPIAddress;

			if(Utils.isNullOrEmpty(sMercuriusAPIAddress)) {
				WasdiLog.debugLog("WasdiResource.sendEmail: sMercuriusAPIAddress is null");
				return false;
			} else {
				WasdiLog.debugLog("WasdiResource.sendEmail: send notification");

				MercuriusAPI oAPI = new MercuriusAPI(sMercuriusAPIAddress);			
				Message oMessage = new Message();

				oMessage.setTilte(sTitle);
				
				if (Utils.isNullOrEmpty(sSender)) {
					sSender = WasdiConfig.Current.notifications.sftpManagementMailSender;
					if (Utils.isNullOrEmpty(sSender)) {
						sSender = "admin@wasdi.net";
					}
				}				
				
				oMessage.setSender(sSender);

				oMessage.setMessage(sMessage);

				Integer iPositiveSucceded = 0;
				
				String sWasdiAdminMail = sRecipient;

				if (!Utils.isNullOrEmpty(WasdiConfig.Current.notifications.wasdiAdminMail) && bAddAdminToRecipient) {
					sWasdiAdminMail += ";" + WasdiConfig.Current.notifications.wasdiAdminMail;
				}

				iPositiveSucceded = oAPI.sendMailDirect(sWasdiAdminMail, oMessage);
				

				if(iPositiveSucceded > 0 ) {
					WasdiLog.debugLog("WasdiResource.sendEmail: notification sent with result " + iPositiveSucceded);
					return true;
				}				
				else {
					WasdiLog.debugLog("WasdiResource.sendEmail: notification NOT sent with result " + iPositiveSucceded);
					return false;
				}				
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("WasdiResource.sendEmail: notification exception " + oEx.toString());
		}
		
		return false;
	}
}
