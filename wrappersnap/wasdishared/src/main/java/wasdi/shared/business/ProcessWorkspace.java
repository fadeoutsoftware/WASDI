package wasdi.shared.business;

/**
 * Created by s.adamo on 31/01/2017.
 */
public class ProcessWorkspace {

    private String productName;
    private String workspaceId;
    private String userId;
    private String operationType;
    private String operationDate;
    private String processObjId;
    private String fileSize;
    private String status;
    private int progressPerc;
    private int pid;

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getOperationDate() {
        return operationDate;
    }

    public void setOperationDate(String operationDate) {
        this.operationDate = operationDate;
    }

    public String getProcessObjId() {
        return processObjId;
    }

    public void setProcessObjId(String processObjId) {
        this.processObjId = processObjId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public int getProgressPerc() {
		return progressPerc;
	}

	public void setProgressPerc(int progressPerc) {
		this.progressPerc = progressPerc;
	}
    
}
