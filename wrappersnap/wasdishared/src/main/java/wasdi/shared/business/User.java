package wasdi.shared.business;

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
    private Boolean validAfterFirstAccess;
    private String firstAccessUUID;
    private String authServiceProvider;
    
    //singleton pattern
    private static User s_oInvalid;
    
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

}
