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
    private Boolean firstAccessValidated;
    private String authServiceProvider;
    private String email;
    
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

	public Boolean getFirstAccessValidated() {
		return firstAccessValidated;
	}

	public void setFirstAccessValidated(Boolean firstAccessValidated) {
		this.firstAccessValidated = firstAccessValidated;
	}

	public String getAuthServiceProvider() {
		return authServiceProvider;
	}

	public void setAuthServiceProvider(String authServiceProvider) {
		this.authServiceProvider = authServiceProvider;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}
}
