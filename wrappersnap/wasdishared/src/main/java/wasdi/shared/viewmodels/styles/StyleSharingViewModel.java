package wasdi.shared.viewmodels.styles;

import wasdi.shared.business.users.UserResourcePermission;

/**
 * Represents the sharing of a style with a user
 * @author PetruPetrescu on 23/02/2022
 *
 */
public class StyleSharingViewModel {

	private String userId;
	private String permissions;
	
	public StyleSharingViewModel() {
		
	}

	public StyleSharingViewModel(UserResourcePermission oSharing) {
		super();
		this.userId = oSharing.getUserId();
		this.permissions = oSharing.getPermissions();
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getPermissions() {
		return permissions;
	}

	public void setPermissions(String permissions) {
		this.permissions = permissions;
	}

}
