package wasdi.shared.viewmodels;

/**
 * Created by c.nattero on 2018.10.26
 */

public class ChangeUserPasswordViewModel {
    private String currentPassword;
    private String newPassword;
    
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
