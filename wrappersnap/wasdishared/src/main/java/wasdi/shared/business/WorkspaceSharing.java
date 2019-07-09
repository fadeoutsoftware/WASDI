package wasdi.shared.business;

import java.util.Date;

/**
 * Workshape sharing entity
 * Represent the association between a workspace and the user that can access it
 * Created by p.campanella on 25/10/2016.
 */
public class WorkspaceSharing {
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
     * Sharing grant timestamp
     */
    private Double shareDate;

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

    public Double getShareDate() {
        return shareDate;
    }

    public void setShareDate(Double shareDate) {
        this.shareDate = shareDate;
    }
}
