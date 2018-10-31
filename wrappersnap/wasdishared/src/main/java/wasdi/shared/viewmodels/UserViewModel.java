package wasdi.shared.viewmodels;

import java.util.UUID;

/**
 * Created by p.campanella on 21/10/2016.
 */
public class UserViewModel {

	private String userId;
    private String name;
    private String surname;
    private String sessionId;
	
	//singleton pattern
	private static UserViewModel s_oInvalid;
	
    static {
    	UserViewModel oViewModel = new UserViewModel();
    	oViewModel.userId = "";
    	oViewModel.name = null;
    	oViewModel.surname = null;
    	oViewModel.sessionId = null;
    	s_oInvalid = oViewModel;
    }
    
    public static UserViewModel getInvalid() {    	
    	return s_oInvalid;
    }
        
    @Override
    public boolean equals(Object oOther) {
    	if (oOther == null) return false;
        if (oOther == this) return true;
        if (!(oOther instanceof UserViewModel))return false;
        UserViewModel oUserViewModel = (UserViewModel)oOther;
        if( this.userId.equals( oUserViewModel.userId ) &&
        	this.name.equals( oUserViewModel.name ) &&
        	this.surname.equals( oUserViewModel.surname ) &&
        	this.sessionId.equals( oUserViewModel.sessionId ) ) {
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
