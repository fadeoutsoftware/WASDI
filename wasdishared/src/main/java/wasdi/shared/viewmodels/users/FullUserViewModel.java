package wasdi.shared.viewmodels.users;

import wasdi.shared.business.users.User;
import wasdi.shared.business.users.UserType;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.Utils;

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
	private String publicNickName;
	
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
		if (oFullUserViewModel.getConfirmationDate() == null) oFullUserViewModel.setConfirmationDate("");
		
		oFullUserViewModel.setDefaultNode(oUser.getDefaultNode());
		if (oFullUserViewModel.getDefaultNode() == null) oFullUserViewModel.setDefaultNode("");
		
		oFullUserViewModel.setDescription(oUser.getDescription());
		if (oFullUserViewModel.getDescription() == null) oFullUserViewModel.setDescription("");
		
		oFullUserViewModel.setLink(oUser.getLink());
		if (oFullUserViewModel.getLink() == null) oFullUserViewModel.setLink("");
		
		oFullUserViewModel.setName(oUser.getName());
		if (oFullUserViewModel.getName() == null) oFullUserViewModel.setName("");
		
		oFullUserViewModel.setSurname(oUser.getSurname());
		if (oFullUserViewModel.getSurname() == null) oFullUserViewModel.setSurname("");
		
		oFullUserViewModel.setRegistrationDate(oUser.getRegistrationDate());
		if (oFullUserViewModel.getRegistrationDate() == null) oFullUserViewModel.setRegistrationDate("");
		
		oFullUserViewModel.setRole(oUser.getRole());
		if (oFullUserViewModel.getRole() == null) oFullUserViewModel.setRole("");
		
		oFullUserViewModel.setType(PermissionsUtils.getUserType(oUser.getUserId()));
		if (oFullUserViewModel.getType() == null) oFullUserViewModel.setType(UserType.NONE.name());
		
		oFullUserViewModel.setUserId(oUser.getUserId());
		if (oFullUserViewModel.getUserId() == null) oFullUserViewModel.setUserId("");
		
		oFullUserViewModel.setLastLogin(oUser.getLastLogin());
		if (oFullUserViewModel.getLastLogin() == null) oFullUserViewModel.setLastLogin("");
		
		oFullUserViewModel.setPublicNickName(oUser.getPublicNickName());
		if (Utils.isNullOrEmpty(oFullUserViewModel.getPublicNickName())) {
			String sPublicNick = oFullUserViewModel.getName();
			oFullUserViewModel.setPublicNickName(sPublicNick);
		}
		
		return oFullUserViewModel;
	}
	
	public String getLastLogin() {
		return lastLogin;
	}
	public void setLastLogin(String lastLogin) {
		this.lastLogin = lastLogin;
	}
	public String getPublicNickName() {
		return publicNickName;
	}
	public void setPublicNickName(String publicNickName) {
		this.publicNickName = publicNickName;
	}
}
