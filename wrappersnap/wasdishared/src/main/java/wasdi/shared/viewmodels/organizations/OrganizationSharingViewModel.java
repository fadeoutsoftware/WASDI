package wasdi.shared.viewmodels.organizations;

import wasdi.shared.business.UserResourcePermission;

/**
 * Represents the sharing of an organization with a user
 * @author PetruPetrescu on 16/01/2023
 *
 */
public class OrganizationSharingViewModel {

	private String organizationId;
	private String userId;
	private String ownerId;
	private String role;
	
	public OrganizationSharingViewModel() {
		
	}

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

	public String getOrganizationId() {
		return organizationId;
	}

	public void setOrganizationId(String organizationId) {
		this.organizationId = organizationId;
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
