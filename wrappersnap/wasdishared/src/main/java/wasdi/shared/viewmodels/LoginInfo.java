package wasdi.shared.viewmodels;

import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by p.campanella on 21/10/2016.
 */
@XmlRootElement
public class LoginInfo {
    private String userId;
    private String userPassword;
    private String googleIdToken;
    
    //singleton pattern
    private static LoginInfo s_oInvalid;
    
    static {
    	LoginInfo oLoginInfo = new LoginInfo();
    	oLoginInfo.userId = "";
    	oLoginInfo.userPassword = "";
    	oLoginInfo.googleIdToken = "";
    	s_oInvalid = oLoginInfo;
    }
    
    public static LoginInfo getInvalid() {
    	return s_oInvalid;
    }
    
    
    @Override
    public boolean equals(Object oOther) {
    	if (oOther == null) return false;
        if (oOther == this) return true;
        if (!(oOther instanceof LoginInfo))return false;
        LoginInfo oLoginInfo = (LoginInfo)oOther;
        if( this.userId.equals( oLoginInfo.userId )&&
        	this.userPassword.equals( oLoginInfo.userPassword ) &&
        	this.googleIdToken.equals( oLoginInfo.getGoogleIdToken() )
        		) {
        	return true;
        } else
        	return false;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

	public String getGoogleIdToken() {
		return googleIdToken;
	}

	public void setGoogleIdToken(String googleIdToken) {
		this.googleIdToken = googleIdToken;
	}


}
