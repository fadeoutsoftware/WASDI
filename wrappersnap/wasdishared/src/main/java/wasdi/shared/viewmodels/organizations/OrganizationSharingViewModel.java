package wasdi.shared.viewmodels.organizations;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import wasdi.shared.business.UserResourcePermission;

/**
 * Represents the sharing of an organization with a user
 * @author PetruPetrescu on 16/01/2023
 *
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationSharingViewModel {

	private String organizationId;
	private String userId;
	private String ownerId;
	private String role;

	public OrganizationSharingViewModel(UserResourcePermission oSharing) {
		super();
		this.organizationId = oSharing.getResourceId();
		this.userId = oSharing.getUserId();
		this.ownerId = oSharing.getOwnerId();

		if (oSharing.getPermissions() != null && oSharing.getPermissions().contains("organization:write")) {
			role = "MANAGER";
		} else {
			role = "USER";
		}
	}

}
