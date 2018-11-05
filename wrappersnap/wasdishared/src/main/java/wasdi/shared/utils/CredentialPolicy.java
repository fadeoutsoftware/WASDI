package wasdi.shared.utils;

import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.validator.routines.EmailValidator;

import wasdi.shared.business.User;
import wasdi.shared.viewmodels.LoginInfo;
import wasdi.shared.viewmodels.UserViewModel;

public class CredentialPolicy {
	
	//TODO read constants from config file
	//UUID are 36 characters long (32 alphanumeric + 4 hyphens "-" )
	
	private static int MINUSERIDLENGTH = 4;
	private static int MINPASSWORDLENGTH = 8;
	private static int MINGUIDLENGTH = 31;
	private static int MINGOOGLEIDLENGTH = MINGUIDLENGTH;
	private static int MINSESSIONIDLENGTH = MINGUIDLENGTH;
	
	
	//probably we may raise these values to 2 but no more: some (sur)names can be very short (e.g. "Li")
	private static int MINNAMELENGTH = 1;
	private static int MINSURNAMELENGTH = 1;
	
	//TODO private methods, from utils
	
	private boolean isNullOrEmpty(String sString) {
        if (sString == null) {
        	return true;
        }
        if (sString.isEmpty()) {
        	return  true;
        }
        return  false;
    }
	
	private Boolean validPassword(String sPassword ) {
		if(isNullOrEmpty(sPassword)) {
			return false;
		}
		if(sPassword.length() < MINPASSWORDLENGTH ) {
			return false;
		} else {
			return true;
		}
	}
	
	private Boolean validGoogleIdToken(String sGoogleId) {
		if(isNullOrEmpty(sGoogleId)) {
			return false;
		}
		if(sGoogleId.length() < MINGOOGLEIDLENGTH ) {
			return false;
		} else {
			return true;
		}
	}
	
	public Boolean validEmail(String sEmail ) {
		return validUserId(sEmail);
	}
	
	public Boolean validUserId(String sUserId) {
		if(isNullOrEmpty(sUserId)) {
			return false;
		}
		if(sUserId.length() < MINUSERIDLENGTH) {
			return false;
		} else if(!EmailValidator.getInstance().isValid(sUserId) ){
			return false;
		} else {
			return true;
		}
	}
	
	private Boolean validName(String sName) {
		if(isNullOrEmpty(sName)) {
			return false;
		}
		if(sName.length() < MINNAMELENGTH) {
			return false;
		} else {
			return true;
		}
	}
	
	private Boolean validSurname(String sSurname) {
		if(isNullOrEmpty(sSurname)) {
			return false;
		}
		if(sSurname.length() < MINSURNAMELENGTH ) {
			return false;
		} else {
			return true;
		}
	}
	
	public Boolean validSessionId(String sSessionId) {
		if(isNullOrEmpty(sSessionId)) {
			return false;
		}
		if( sSessionId.length() < MINSESSIONIDLENGTH) {
			return false;
		} else {
			return true;
		}
	}
	
	
	public Boolean satisfies( LoginInfo oLoginInfo ) {
		//TODO check after refactoring: due to googleId modifications these conditions may change
		if(null == oLoginInfo) {
			throw new NullArgumentException();
		}
		
		if( validGoogleIdToken(oLoginInfo.getGoogleIdToken()) ) {
			return true;
		} else if( !validUserId(oLoginInfo.getUserId() )) {
			return false;
		} else if(!validPassword(oLoginInfo.getUserPassword())) {
			return false;
		} else {
			return true;
		}
	}
	
	public Boolean satisfies( UserViewModel oUserVM ) {
		if( null == oUserVM) {
			throw new NullArgumentException();
		}
		if(!validUserId(oUserVM.getUserId())) {
			return false;
		} else if(!validName(oUserVM.getName() )) {
			return false;
		} else if(!validSurname(oUserVM.getSurname())) {
			return false;
		} else if(!validSessionId(oUserVM.getSessionId() )) {
			return false;
		} else {
			return true;
		}
	}
	
	public boolean satisfies(User oUser) {
		if(null==oUser) {
			throw new NullArgumentException();
		}
		if(null==oUser.getUserId()) {
			return false;
		}
		return false;
	}
	
	//TODO implement for RegistrationInfoViewModel
}
