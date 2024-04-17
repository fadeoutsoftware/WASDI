package wasdi.shared.viewmodels.users;

import wasdi.shared.business.users.User;
import wasdi.shared.utils.PermissionsUtils;

public class FullUserViewModel {
	
	private String userId;
    private String name;
    private String surname;
	private String link;
	private String type;
	private String role;
	private boolean active;
	private String defaultNode;
	private String registrationDate;
	private String confirmationDate;
	private String lastLogin;
	private String description;
	
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
	public String getLink() {
		return link;
	}
	public void setLink(String link) {
		this.link = link;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getRole() {
		return role;
	}
	public void setRole(String role) {
		this.role = role;
	}
	public boolean isActive() {
		return active;
	}
	public void setActive(boolean active) {
		this.active = active;
	}
	public String getDefaultNode() {
		return defaultNode;
	}
	public void setDefaultNode(String defaultNode) {
		this.defaultNode = defaultNode;
	}
	public String getRegistrationDate() {
		return registrationDate;
	}
	public void setRegistrationDate(String registrationDate) {
		this.registrationDate = registrationDate;
	}
	public String getConfirmationDate() {
		return confirmationDate;
	}
	public void setConfirmationDate(String confirmationDate) {
		this.confirmationDate = confirmationDate;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	public static FullUserViewModel fromUser(User oUser) {
		FullUserViewModel oFullUserViewModel = new FullUserViewModel();
		if (oUser == null) return oFullUserViewModel;
		
		oFullUserViewModel.setActive(oUser.getValidAfterFirstAccess());
		oFullUserViewModel.setConfirmationDate(oUser.getConfirmationDate());
		oFullUserViewModel.setDefaultNode(oUser.getDefaultNode());
		oFullUserViewModel.setDescription(oUser.getDescription());
		oFullUserViewModel.setLink(oUser.getLink());
		oFullUserViewModel.setName(oUser.getName());
		oFullUserViewModel.setSurname(oUser.getSurname());
		oFullUserViewModel.setRegistrationDate(oUser.getRegistrationDate());
		oFullUserViewModel.setRole(oUser.getRole());
		oFullUserViewModel.setType(PermissionsUtils.getUserType(oUser.getUserId()));
		oFullUserViewModel.setUserId(oUser.getUserId());
		oFullUserViewModel.setLastLogin(oUser.getLastLogin());
		
		return oFullUserViewModel;
	}
	public String getLastLogin() {
		return lastLogin;
	}
	public void setLastLogin(String lastLogin) {
		this.lastLogin = lastLogin;
	}
}
