package wasdi.shared.business;

/**
 * Created by p.campanella on 25/10/2016.
 */
public class Workspace {

    private String workspaceId;
    private String name;
    private String userId;
    private Double creationDate;
    private Double lastEditDate;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Double getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Double creationDate) {
        this.creationDate = creationDate;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public Double getLastEditDate() {
        return lastEditDate;
    }

    public void setLastEditDate(Double lastEditDate) {
        this.lastEditDate = lastEditDate;
    }
}
