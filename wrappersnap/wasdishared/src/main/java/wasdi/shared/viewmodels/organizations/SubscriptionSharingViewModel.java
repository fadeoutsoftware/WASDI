package wasdi.shared.viewmodels.organizations;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import wasdi.shared.business.UserResourcePermission;

/**
 * Represents the sharing of an subscription with a user
 * @author PetruPetrescu on 16/01/2023
 *
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionSharingViewModel {

	private String subscriptionId;
	private String userId;
	private String ownerId;

	public SubscriptionSharingViewModel(UserResourcePermission oSharing) {
		super();
		this.subscriptionId = oSharing.getResourceId();
		this.userId = oSharing.getUserId();
		this.ownerId = oSharing.getOwnerId();
	}

}
