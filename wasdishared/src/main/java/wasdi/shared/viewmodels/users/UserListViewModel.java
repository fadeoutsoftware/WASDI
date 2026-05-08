package wasdi.shared.viewmodels.users;

/**
 * Represents the list of users that the admins can see in the 
 * admin backend session.
 * 
 */
public class UserListViewModel {
	private String userId;
	private boolean active;
	private String type;
	private String lastLogin;
	private String name;
	private String surname;
	private String publicNickName;
	
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public boolean isActive() {
		return active;
	}
	public void setActive(boolean active) {
		this.active = active;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getLastLogin() {
		return lastLogin;
	}
	public void setLastLogin(String lastLogin) {
		this.lastLogin = lastLogin;
	}
	public String getSurname() {
		return surname;
	}
	public void setSurname(String surname) {
		this.surname = surname;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPublicNickName() {
		return publicNickName;
	}
	public void setPublicNickName(String publicNickName) {
		this.publicNickName = publicNickName;
	}
	
}
