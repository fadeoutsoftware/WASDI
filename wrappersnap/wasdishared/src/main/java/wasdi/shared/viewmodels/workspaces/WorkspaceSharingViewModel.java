package wasdi.shared.viewmodels.workspaces;

/**
 * Represents the sharing of a workspace.
 * 
 * @author p.campanella
 *
 */
public class WorkspaceSharingViewModel {
	/**
	 * Workspace Id
	 */
    private  String workspaceId;
    /**
     * User that can access
     */
    private  String userId;
    
    /**
     * Workspace Owner
     */
    private  String ownerId;
    
    /**
     * Sharing permissions
     */
    private  String permissions;

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
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

	public String getPermissions() {
		return permissions;
	}

	public void setPermissions(String permissions) {
		this.permissions = permissions;
	}
}
