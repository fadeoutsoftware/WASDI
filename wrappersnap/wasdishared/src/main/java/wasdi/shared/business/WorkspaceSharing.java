package wasdi.shared.business;

import java.util.Date;

/**
 * Created by p.campanella on 25/10/2016.
 */
public class WorkspaceSharing {
    private  String workspaceId;
    private  String userId;
    private  String ownerId;
    private  long shareDate;

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

    public long getShareDate() {
        return shareDate;
    }

    public void setShareDate(long shareDate) {
        this.shareDate = shareDate;
    }
}
