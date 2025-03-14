package wasdi.shared.viewmodels.users;

import java.util.Collections;
import java.util.Set;

import wasdi.shared.business.users.UserType;
import wasdi.shared.utils.Utils;

/**
 * User View Model: represents basic info about the user
 * 
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
	private String publicNickName;
	
	private String type;

	private String role;
	private Set<String> grantedAuthorities;
	
	//singleton pattern
	private static UserViewModel s_oInvalid;
	
    static {
    	UserViewModel oViewModel = new UserViewModel();
    	oViewModel.userId = "";
    	oViewModel.name = null;
    	oViewModel.surname = null;
    	oViewModel.sessionId = null;
    	oViewModel.authProvider = null;
    	oViewModel.type = UserType.FREE.name();

    	oViewModel.role = null;
    	oViewModel.grantedAuthorities = Collections.emptySet();

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

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public Set<String> getGrantedAuthorities() {
		return grantedAuthorities;
	}

	public void setGrantedAuthorities(Set<String> grantedAuthorities) {
		this.grantedAuthorities = grantedAuthorities;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getPublicNickName() {
		return publicNickName;
	}

	public void setPublicNickName(String publicNickName) {
		this.publicNickName = publicNickName;
	}
}
