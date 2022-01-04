package wasdi.shared.business;

import lombok.EqualsAndHashCode;

/**
 * Process Workspace Entity
 * Represent any "job" ongoing on the WASDI server.
 * 
 * This entity links an operation to a workspace.
 * 
 * Operations can be or the launcher embedded operations or any user processor.
 * 
 * The status is updated in real time.
 * The payload is also stored here.
 * 
 * Created by s.adamo on 31/01/2017.
 */
@EqualsAndHashCode
public class ProcessWorkspace {

	/**
	 * Involved product
	 */
    private String productName;
    /**
     * Active Workspace 
     */
    private String workspaceId;
    /**
     * User owner of the job
     */
    private String userId;
    /**
     * Type of the operation
     */
    private String operationType;
    /**
     * Sub Type of the operation
     */
    private String operationSubType;
	/**
     * Creation date of the operation
     */
    private String operationDate;
    /**
     * Start date of the operation
     */
    private String operationStartDate;    
    /**
     * End date of the operation
     */
    private String operationEndDate;
    /**
     * Date and time when the process status was modified
     */
    private String lastStateChangeDate;
    /**
     * Unique identifier of the job
     */
    private String processObjId;
    /**
     * Size of the involved file
     */
    private String fileSize;
    /**
     * Status of the process
     */
    private String status;
    /**
     * Percentuage of the job done
     */
    private int progressPerc;
    /**
     * System pid
     */
    private int pid;
    /**
     * Process Payload
     */
    private String payload;
    /**
     * Code of the computing node of the operation
     */
    private String nodeCode="wasdi";
    
    /**
     * Parent Process Workspace Id
     */
    private String parentId;
    
    /**
     * Id of an eventual sub process started from launcher
     */
	private int subprocessPid;

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

	public String getOperationEndDate() {
		return operationEndDate;
	}

	public void setOperationEndDate(String operationEndDate) {
		this.operationEndDate = operationEndDate;
	}

	public String getOperationStartDate() {
		return operationStartDate;
	}

	public void setOperationStartDate(String operationStartDate) {
		this.operationStartDate = operationStartDate;
	}

    public String getNodeCode() {
		return nodeCode;
	}

	public void setNodeCode(String nodeCode) {
		this.nodeCode = nodeCode;
	}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}

	public String getLastStateChangeDate() {
		return lastStateChangeDate;
	}

	public void setLastStateChangeDate(String lastStateChangeDate) {
		this.lastStateChangeDate = lastStateChangeDate;
	}
	
   public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public int getSubprocessPid() {
		return subprocessPid;
	}

	public void setSubprocessPid(int subprocessPid) {
		this.subprocessPid = subprocessPid;
	}

    public String getOperationSubType() {
		return operationSubType;
	}

	public void setOperationSubType(String operationSubType) {
		this.operationSubType = operationSubType;
	}

}
