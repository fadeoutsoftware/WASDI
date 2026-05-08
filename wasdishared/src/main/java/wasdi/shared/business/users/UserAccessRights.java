package wasdi.shared.business.users;

import wasdi.shared.utils.Utils;

public enum UserAccessRights {
	WRITE("write"),
	READ("read") ;
	
	private final String accessRight;

	UserAccessRights(String sAccessRight) {
		this.accessRight = sAccessRight;
	}

	public String getAccessRight() {
		return accessRight;
	}		
	
	public static boolean isValidAccessRight(String sAccessType) {
		if (Utils.isNullOrEmpty(sAccessType)) return false;
		
		if (sAccessType.equals(READ.getAccessRight())) return true;
		if (sAccessType.equals(WRITE.getAccessRight())) return true;
		
		return false;
	}
}
