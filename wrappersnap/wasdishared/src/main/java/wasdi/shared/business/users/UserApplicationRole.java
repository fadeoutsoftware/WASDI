package wasdi.shared.business.users;

import wasdi.shared.utils.Utils;

public enum UserApplicationRole {
	ADMIN("ADMIN"),
	DEVELOPER("DEVELOPER"),
	USER("USER");
	
	private final String role;
	
	UserApplicationRole(String sAccessRight) {
		this.role = sAccessRight;
	}

	public String getRole() {
		return role;
	}		
	
	public static boolean isValidRole(String sRole) {
		if (Utils.isNullOrEmpty(sRole)) return false;
		
		if (sRole.equals(ADMIN.getRole())) return true;
		if (sRole.equals(DEVELOPER.getRole())) return true;
		if (sRole.equals(USER.getRole())) return true;
		
		return false;
	}
	
	public static boolean isAdmin(User oUser) {
		if (oUser == null) return false;
		if (!isValidRole(oUser.getRole())) return false;
		
		if (oUser.getRole().equals(ADMIN.getRole())) return true;
		
		return false;
	}
}
