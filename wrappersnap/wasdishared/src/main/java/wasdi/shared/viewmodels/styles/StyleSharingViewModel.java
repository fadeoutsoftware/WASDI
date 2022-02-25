package wasdi.shared.viewmodels.styles;

import wasdi.shared.business.StyleSharing;

/**
 * Represents the sharing of a style with a user
 * @author PetruPetrescu on 23/02/2022
 *
 */
public class StyleSharingViewModel {

	private String userId;

	public StyleSharingViewModel() {
		super();
	}

	public StyleSharingViewModel(StyleSharing oSharing) {
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
