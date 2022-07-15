package wasdi.shared.viewmodels.workspaces;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents basic info of a Workspace, used by the summary shown in the workspaces section
 * 
 * Created by p.campanella on 25/10/2016.
 */
public class WorkspaceListInfoViewModel {
    private String workspaceId;
    private String workspaceName;
    private String ownerUserId;
    private List<String> sharedUsers = new ArrayList<>();
    private boolean activeNode;

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public List<String> getSharedUsers() {
        return sharedUsers;
    }

    public void setSharedUsers(List<String> sharedUsers) {
        this.sharedUsers = sharedUsers;
    }

	public boolean getActiveNode() {
		return activeNode;
	}

	public void setActiveNode(boolean bActiveNode) {
		this.activeNode = bActiveNode;
	}
}
