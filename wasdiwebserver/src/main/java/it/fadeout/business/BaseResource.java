package it.fadeout.business;

import it.fadeout.Wasdi;
import wasdi.shared.business.User;
import wasdi.shared.utils.Utils;

public class BaseResource {
	
	public User getUser(String sSessionId){
		
		if (Utils.isNullOrEmpty(sSessionId)) {
			return null;
		}
		User oUser = Wasdi.GetUserFromSession(sSessionId);
		if (oUser == null) {
			return null;
		}
		if (Utils.isNullOrEmpty(oUser.getUserId())) {
			return null;
		}
		return oUser;
		
	}
}
