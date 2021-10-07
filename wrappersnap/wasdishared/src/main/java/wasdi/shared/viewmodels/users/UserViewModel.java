package wasdi.shared.viewmodels.users;

import wasdi.shared.utils.Utils;

/**
 * Created by p.campanella on 21/10/2016.
 */
public class UserViewModel {

	private String userId;
    private String name;
    private String surname;
	private String authProvider;
	private String link;
	private String description;
	private String sessionId;
	
	//singleton pattern
	private static UserViewModel s_oInvalid;
	
    static {
    	UserViewModel oViewModel = new UserViewModel();
    	oViewModel.userId = "";
    	oViewModel.name = null;
    	oViewModel.surname = null;
    	oViewModel.sessionId = null;
    	oViewModel.authProvider = null;
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
        	this.sessionId.equals( oUserViewModel.sessionId ) &&
        	this.authProvider.equals(oUserViewModel.authProvider) ) {
        	return true;
        } else
        	return false;
    }
    
    @Override
    public int hashCode() {
    	
    	String sUserId = "";
    	String sName = "";
    	String sSurname = "";
    	String sSessionId = "";
    	String sAuthProvider = "";
    	
    	if (!Utils.isNullOrEmpty(userId)) sUserId = userId;
    	if (!Utils.isNullOrEmpty(name)) sName = name;
    	if (!Utils.isNullOrEmpty(surname)) sSurname = surname;
    	if (!Utils.isNullOrEmpty(sessionId)) sSessionId = sessionId;
    	if (!Utils.isNullOrEmpty(authProvider)) sAuthProvider = authProvider;
    	
    	String sHashCode = sUserId+sName+sSurname+sSessionId+sAuthProvider;
    	
    	return sHashCode.hashCode();
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

	public String getAuthProvider() {
		return authProvider;
	}

	public void setAuthProvider(String authProvider) {
		this.authProvider = authProvider;
	}

    public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

}
