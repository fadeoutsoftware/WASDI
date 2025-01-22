package wasdi.shared.viewmodels.workspaces;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Represents the Workspace representation as requested by the client Editor Section.
 * 
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
    private boolean activeNode;
    private long processesCount;
    private String cloudProvider;
    private String slaLink;
    private String storageSize = "";
    private boolean isPublic = false;
    private boolean readOnly = false;
   
    
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

	public boolean getActiveNode() {
		return activeNode;
	}

	public void setActiveNode(boolean bActiveNode) {
		this.activeNode = bActiveNode;
	}
	
	public String getSlaLink() {
		return slaLink;
	}

	public void setSlaLink(String slaLink) {
		this.slaLink = slaLink;
	}
	
	public String getStorageSize() {
		return storageSize;
	}

	public void setStorageSize(String storageSize) {
		this.storageSize = storageSize;
	}

	public boolean isPublic() {
		return isPublic;
	}

	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	@Override
	public String toString() {
		return "WorkspaceEditorViewModel [workspaceId=" + workspaceId + ", name=" + name + ", userId=" + userId
				+ ", apiUrl=" + apiUrl + ", creationDate=" + creationDate + ", lastEditDate=" + lastEditDate
				+ ", sharedUsers=" + sharedUsers + ", nodeCode=" + nodeCode + ", processesCount=" + processesCount
				+ ", cloudProvider=" + cloudProvider + "]";
	}
}
