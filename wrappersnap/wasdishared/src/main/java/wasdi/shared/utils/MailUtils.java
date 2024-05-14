package wasdi.shared.utils;

import it.fadeout.mercurius.business.Message;
import it.fadeout.mercurius.client.MercuriusAPI;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.log.WasdiLog;

public class MailUtils {
	
	/**
	 * Send an email from deafault sender to recipient with title and message
	 * @param sRecipient Recipient 
	 * @param sTitle Title
	 * @param sMessage Message
	 * @return true if sent false otherwise
	 */
	public static boolean sendEmail(String sRecipient, String sTitle, String sMessage) {
		return sendEmail(null, sRecipient, sTitle, sMessage);
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
				WasdiLog.debugLog("MailUtils.sendEmail: mercuriusAPIAddress is null");
				return false;
			} else {
				WasdiLog.debugLog("MailUtils.sendEmail: send notification");

				MercuriusAPI oAPI = new MercuriusAPI(sMercuriusAPIAddress);			
				Message oMessage = new Message();

				oMessage.setTilte(sTitle);
				
				if (Utils.isNullOrEmpty(sSender)) {
					sSender = WasdiConfig.Current.notifications.sftpManagementMailSender;
					if (Utils.isNullOrEmpty(sSender)) {
						sSender = "info@wasdi.net";
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
					WasdiLog.debugLog("MailUtils.sendEmail: notification sent with result " + iPositiveSucceded);
					return true;
				}				
				else {
					WasdiLog.debugLog("MailUtils.sendEmail: notification NOT sent with result " + iPositiveSucceded);
					return false;
				}				
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("MailUtils.sendEmail: notification exception " + oEx.toString());
		}
		
		return false;
	}
}
