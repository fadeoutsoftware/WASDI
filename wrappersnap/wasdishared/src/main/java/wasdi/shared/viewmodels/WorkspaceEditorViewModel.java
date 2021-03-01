package wasdi.shared.viewmodels;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by p.campanella on 26/10/2016.
 */
public class WorkspaceEditorViewModel {

    private String workspaceId;
    private String name;
    private String userId;
    private String apiUrl;
	private Date creationDate;
    private Date lastEditDate;
    private List<String> sharedUsers = new ArrayList<>();
    private String nodeCode;
    private long processesCount;
    private String cloudProvider;
    


	public String getCloudProvider() {
		return cloudProvider;
	}

	public void setCloudProvider(String cloudProvider) {
		this.cloudProvider = cloudProvider;
	}

	public long getProcessesCount() {
		return processesCount;
	}

	public void setProcessesCount(long processesCount) {
		this.processesCount = processesCount;
	}

	public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

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

    public List<String> getSharedUsers() {
        return sharedUsers;
    }

    public void setSharedUsers(List<String> sharedUsers) {
        this.sharedUsers = sharedUsers;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public Date getLastEditDate() {
        return lastEditDate;
    }

    public void setLastEditDate(Date lastEditDate) {
        this.lastEditDate = lastEditDate;
    }
    
    public String getApiUrl() {
		return apiUrl;
	}

	public void setApiUrl(String apiUrl) {
		this.apiUrl = apiUrl;
	}

	public String getNodeCode() {
		return nodeCode;
	}

	public void setNodeCode(String nodeCode) {
		this.nodeCode = nodeCode;
	}

	@Override
	public String toString() {
		return "WorkspaceEditorViewModel [workspaceId=" + workspaceId + ", name=" + name + ", userId=" + userId
				+ ", apiUrl=" + apiUrl + ", creationDate=" + creationDate + ", lastEditDate=" + lastEditDate
				+ ", sharedUsers=" + sharedUsers + ", nodeCode=" + nodeCode + ", processesCount=" + processesCount
				+ ", cloudProvider=" + cloudProvider + "]";
	}



}
