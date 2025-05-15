package wasdi.shared.viewmodels.users;

import javax.xml.bind.annotation.XmlRootElement;

import wasdi.shared.utils.Utils;

/**
 * View Model for access info of classic WASDI login
 * 
 * Created by p.campanella on 21/10/2016.
 */
@XmlRootElement
public class LoginInfo {
    private String userId;
    private String userPassword;
    private String googleIdToken;
    private String skin;
    
    //singleton pattern
    private static LoginInfo s_oInvalid;
    
    static {
    	LoginInfo oLoginInfo = new LoginInfo();
    	oLoginInfo.userId = "";
    	oLoginInfo.userPassword = "";
    	oLoginInfo.googleIdToken = "";
    	oLoginInfo.skin = "";
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
        	this.googleIdToken.equals( oLoginInfo.getGoogleIdToken() ) &&
        	this.skin.equals(oLoginInfo.getSkin())
        		) {
        	return true;
        } else
        	return false;
    }
    
    @Override
    public int hashCode() {
    	String sUserId = "";
    	String sUserPw = "";
    	String sGoogleToken = "";
    	String sSkin = "";
    	
    	if (!Utils.isNullOrEmpty(userId)) sUserId = userId;
    	if (!Utils.isNullOrEmpty(userPassword)) sUserPw = userId;
    	if (!Utils.isNullOrEmpty(googleIdToken)) sGoogleToken = googleIdToken;
    	if (!Utils.isNullOrEmpty(skin)) sSkin = skin;
    	
    	String sInternal = sUserId + sUserPw + sGoogleToken + sSkin;
    	return sInternal.hashCode();
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

	public String getSkin() {
		return skin;
	}
	
	public void setSkin(String skin) {
		this.skin = skin;
	}

}
