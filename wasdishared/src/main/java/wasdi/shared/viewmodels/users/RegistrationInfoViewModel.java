package wasdi.shared.viewmodels.users;

/**
 * Registration info View Model 
 * 
 * Created by c.nattero on 2018.10.26
 */

public class RegistrationInfoViewModel {
	private String userId; //NOTE: user email (as of 2018.10.26)
	private String name;
	private String surname;
    private String password;
    private String googleIdToken = null;
    private String optionalValidationToken = null;
   
    //singleton pattern
    private static  RegistrationInfoViewModel s_oInvalid;
    
    static {
    	s_oInvalid = new RegistrationInfoViewModel();
    	s_oInvalid.userId = null;
    	s_oInvalid.name = null;
    	s_oInvalid.surname = null;
    	s_oInvalid.password = null;
    	s_oInvalid.googleIdToken = null;
    }
    
    public static RegistrationInfoViewModel getInvalid() {
		return s_oInvalid;
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
	public String getGoogleIdToken() {
		return googleIdToken;
	}
	public void setGoogleIdToken(String googleIdToken) {
		this.googleIdToken = googleIdToken;
	}

	public String getOptionalValidationToken() {
		return optionalValidationToken;
	}

	public void setOptionalValidationToken(String optionalValidationToken) {
		this.optionalValidationToken = optionalValidationToken;
	}
	
}
