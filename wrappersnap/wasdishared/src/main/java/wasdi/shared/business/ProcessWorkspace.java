package wasdi.shared.business;

import java.util.Objects;

import wasdi.shared.utils.log.WasdiLog;

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
    private Double operationTimestamp;
    /**
     * Start date of the operation
     */
    private Double operationStartTimestamp;    
    /**
     * End date of the operation
     */
    private Double operationEndTimestamp;
    /**
     * Date and time when the process status was modified
     */
    private Double lastStateChangeTimestamp;

    /**
     * Running time of the process (in milliseconds)
     */
    private Long runningTime = 0L;

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
    private int pid=-1;
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

	/**
	 * Project Id
	 */
	private String projectId;

	/**
	 * Subscription Id
	 */
	private String subscriptionId;
	
	/**
	 * If the system id Dockerized (shellExecLocally==false) the launcher is 
	 * identified not by the pid but by this container name
	 */
	private String containerId;


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

    public Double getOperationTimestamp() {
        return operationTimestamp;
    }

    public void setOperationTimestamp(Double operationTimestamp) {
        this.operationTimestamp = operationTimestamp;
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

	public Double getOperationEndTimestamp() {
		return operationEndTimestamp;
	}

	public void setOperationEndTimestamp(Double operationEndTimestamp) {
		this.operationEndTimestamp = operationEndTimestamp;
	}

	public Double getOperationStartTimestamp() {
		return operationStartTimestamp;
	}

	public void setOperationStartTimestamp(Double operationStartTimestamp) {
		this.operationStartTimestamp = operationStartTimestamp;
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

	public Double getLastStateChangeTimestamp() {
		return lastStateChangeTimestamp;
	}

	public void setLastStateChangeTimestamp(Double lastStateChangeTimestamp) {
		this.lastStateChangeTimestamp = lastStateChangeTimestamp;
	}

	public Long getRunningTime() {
		return runningTime;
	}

	public void setRunningTime(Long runningTime) {
		this.runningTime = runningTime;
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

	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public String getSubscriptionId() {
		return subscriptionId;
	}

	public void setSubscriptionId(String subscriptionId) {
		this.subscriptionId = subscriptionId;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(containerId, fileSize, lastStateChangeTimestamp, nodeCode, operationEndTimestamp,
				operationStartTimestamp, operationSubType, operationTimestamp, operationType, parentId, payload, pid,
				processObjId, productName, progressPerc, projectId, runningTime, status, subprocessPid, subscriptionId,
				userId, workspaceId);
	}

	@Override
	public boolean equals(Object obj) {
		try {
			if (obj instanceof ProcessWorkspace) {
				ProcessWorkspace oTheOther = (ProcessWorkspace) obj;
				if (oTheOther.getProcessObjId().equals(this.processObjId)) return true;
				else return false;
			}			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessWorkspace.equals: ", oEx);
		}
		
		return super.equals(obj);
	}

	public String getContainerId() {
		return containerId;
	}

	public void setContainerId(String containerId) {
		this.containerId = containerId;
	}
	
}
