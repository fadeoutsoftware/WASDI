package wasdi.shared.viewmodels.styles;

import wasdi.shared.business.UserResourcePermission;

/**
 * Represents the sharing of a style with a user
 * @author PetruPetrescu on 23/02/2022
 *
 */
public class StyleSharingViewModel {

	private String userId;

	public StyleSharingViewModel(UserResourcePermission oSharing) {
		super();
		this.userId = oSharing.getUserId();
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

}
