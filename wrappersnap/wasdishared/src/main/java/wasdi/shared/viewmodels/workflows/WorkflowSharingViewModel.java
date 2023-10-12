package wasdi.shared.viewmodels.workflows;

import wasdi.shared.business.users.UserResourcePermission;

/**
 * Represents the sharing of a workflow with a user
 * @author p.campanella
 *
 */
public class WorkflowSharingViewModel {
	private String userId;
	private String permissions;

	public WorkflowSharingViewModel() {
	}

	public WorkflowSharingViewModel(UserResourcePermission oSharing) {
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
