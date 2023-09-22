package wasdi.shared.viewmodels.organizations;

import wasdi.shared.business.UserResourcePermission;

/**
 * Represents the sharing of an subscription with a user
 * @author PetruPetrescu on 16/01/2023
 *
 */

public class SubscriptionSharingViewModel {

	private String subscriptionId;
	private String userId;
	private String ownerId;
	private String role;
	
	public SubscriptionSharingViewModel() {
		
	}

	public SubscriptionSharingViewModel(UserResourcePermission oSharing) {
		super();
		this.subscriptionId = oSharing.getResourceId();
		this.userId = oSharing.getUserId();
		this.ownerId = oSharing.getOwnerId();

		if (oSharing.getPermissions() != null && oSharing.getPermissions().contains("subscription:write")) {
			role = "MANAGER";
		} else {
			role = "USER";
		}
	}

	public String getSubscriptionId() {
		return subscriptionId;
	}

	public void setSubscriptionId(String subscriptionId) {
		this.subscriptionId = subscriptionId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

}
