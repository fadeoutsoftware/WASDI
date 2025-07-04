package wasdi.shared.business.users;

import com.fasterxml.jackson.annotation.JsonIgnore;

import wasdi.shared.utils.Utils;

/**
 * WASDI User
 * Created by p.campanella on 21/10/2016.
 */
public class User {
	
	/**
	 * Unique int id
	 */
    private int id;
    /**
     * User ID
     */
    private String userId;
    /**
     * Name
     */
    private String name;
    /**
     * Surname
     */
    private String surname;
    /**
     * Password
     */
    private String password;
    
    /**
     * Flag to check the first Access
     */
    private Boolean validAfterFirstAccess;
    
    /**
     * UUID for the confirmation mail
     */
    private String firstAccessUUID;
    
    /**
     * Internal code of the authentication provider
     */
    private String authServiceProvider;
    
    /**
     * Google Id Token for google users
     */
    private String googleIdToken;

    /**
     * User registration date
     */
    private String m_sRegistrationDate = null;

    /**
     * User confirmation
     */
    private String m_sConfirmationDate = null;

    /**
     * User last login date
     */
    private String m_sLastLogin = null;
    
    /**
     * User default node
     */
    private String m_sDefaultNode = "wasdi";

    /**
     * Singleton invalid User
     */
    private static User s_oInvalid;
    
    /**
     * Personal link
     */
	private String link;
	
	/**
	 * Description
	 */
	private String description;

	/**
	 * Active subscription Id
	 */
	private String activeSubscriptionId;

	/**
	 * Active project Id
	 */
	private String activeProjectId;

	/**
	 * User role
	 */
	private String role = UserApplicationRole.USER.name();
	
	/**
	 * User type: is it free, standard or professional?!?
	 */
	private String type = UserType.FREE.name();
	
	/**
	 * If the user exceed the space will receive a warning. In case, here we have the timstamp of the moment we sent this advice 
	 */
	private Double storageWarningSentDate = 0.0;
	
	/**
	 * Public Nick Name the user decide to expose
	 */
	private String publicNickName;
	
	/**
	 * Skin profile of the user
	 */
	private String skin = "wasdi";

	static {
    	s_oInvalid = new User();
    	s_oInvalid.id = -1;
    	s_oInvalid.userId = null;
    	s_oInvalid.name = null;
    	s_oInvalid.surname = null;
    	s_oInvalid.password = null;
    	s_oInvalid.validAfterFirstAccess = null;
    	s_oInvalid.firstAccessUUID = null;
    	s_oInvalid.authServiceProvider = null;
    	s_oInvalid.m_sDefaultNode = null;
    }

    public static User getInvalid() {
		return s_oInvalid;
	}
    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

	public Boolean getValidAfterFirstAccess() {
		return validAfterFirstAccess;
	}

	public void setValidAfterFirstAccess(Boolean validAfterFirstAccess) {
		this.validAfterFirstAccess = validAfterFirstAccess;
	}

	public String getFirstAccessUUID() {
		return firstAccessUUID;
	}

	public void setFirstAccessUUID(String firstAccessUUID) {
		this.firstAccessUUID = firstAccessUUID;
	}

	public String getAuthServiceProvider() {
		return authServiceProvider;
	}

	public void setAuthServiceProvider(String authServiceProvider) {
		this.authServiceProvider = authServiceProvider;
	}

	public String getGoogleIdToken() {
		return googleIdToken;
	}

	public void setGoogleIdToken(String googleIdToken) {
		this.googleIdToken = googleIdToken;
	}

	public String getRegistrationDate() {
		return m_sRegistrationDate;
	}

	public void setRegistrationDate(String sRegistrationDate) {
		this.m_sRegistrationDate = sRegistrationDate;
	}

	public String getLastLogin() {
		return m_sLastLogin;
	}

	public void setLastLogin(String sLastLogin) {
		this.m_sLastLogin = sLastLogin;
	}

	public String getConfirmationDate() {
		return m_sConfirmationDate;
	}

	public void setConfirmationDate(String oConfirmationDate) {
		this.m_sConfirmationDate = oConfirmationDate;
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


	/**
	 * @return the defaultNode
	 */
	public String getDefaultNode() {
		return m_sDefaultNode;
	}

	/**
	 * @param defaultNode the defaultNode to set
	 */
	public void setDefaultNode(String defaultNode) {
		this.m_sDefaultNode = defaultNode;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}
    public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public String getActiveSubscriptionId() {
		return activeSubscriptionId;
	}

	public void setActiveSubscriptionId(String activeSubscriptionId) {
		this.activeSubscriptionId = activeSubscriptionId;
	}

	public String getActiveProjectId() {
		return activeProjectId;
	}

	public void setActiveProjectId(String activeProjectId) {
		this.activeProjectId = activeProjectId;
	}
	
	/**
	 * Get a safe full user name string.
	 * 
	 * @return
	 */
	@JsonIgnore
	public String getSafeUserName() {
		
		String sUserName = "";
		
		if (!Utils.isNullOrEmpty(this.getName())) sUserName += this.getName();
		
		if (!Utils.isNullOrEmpty(this.getSurname()))  {
			if (!Utils.isNullOrEmpty(sUserName)) sUserName = sUserName + " ";
			sUserName += this.getSurname();
		}
		
		if (Utils.isNullOrEmpty(sUserName)) sUserName = this.getUserId();
		
		return sUserName;
		
	}

	public Double getStorageWarningSentDate() {
		return storageWarningSentDate;
	}

	public void setStorageWarningSentDate(Double storageWarningSentDate) {
		this.storageWarningSentDate = storageWarningSentDate;
	}

	public String getPublicNickName() {
		return publicNickName;
	}

	public void setPublicNickName(String publicNickName) {
		this.publicNickName = publicNickName;
	}

	public String getSkin() {
		return skin;
	}

	public void setSkin(String skin) {
		this.skin = skin;
	}
}
