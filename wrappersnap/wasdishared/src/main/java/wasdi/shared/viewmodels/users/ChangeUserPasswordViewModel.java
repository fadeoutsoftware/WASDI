package wasdi.shared.viewmodels.users;

/**
 * Used to request a password change
 * 
 * Created by c.nattero on 2018.10.26
 */

public class ChangeUserPasswordViewModel {
    private String currentPassword;
    private String newPassword;
    
    private static ChangeUserPasswordViewModel s_oInvalid;
    
    static {
    	s_oInvalid = new ChangeUserPasswordViewModel();
    	s_oInvalid.currentPassword = null;
    	s_oInvalid.newPassword = null;
    }
    
    public static ChangeUserPasswordViewModel getInvalid() {
		return s_oInvalid;
	}
    
	public String getCurrentPassword() {
		return currentPassword;
	}
	public void setCurrentPassword(String currentPassword) {
		this.currentPassword = currentPassword;
	}
	public String getNewPassword() {
		return newPassword;
	}
	public void setNewPassword(String newPassword) {
		this.newPassword = newPassword;
	}
}
