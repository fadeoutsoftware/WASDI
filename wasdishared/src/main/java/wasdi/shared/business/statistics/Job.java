package wasdi.shared.business.statistics;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class Job {
	
	public Job() {
		
	}
	
	public Job(ProcessWorkspace oProcessWorkspace) {
		
		if (oProcessWorkspace == null) return;
		
		this.processObjId = oProcessWorkspace.getProcessObjId();
		this.name = oProcessWorkspace.getProductName();
		this.workspaceId = oProcessWorkspace.getWorkspaceId();
		this.userId = oProcessWorkspace.getUserId();
		this.operationType = oProcessWorkspace.getOperationType();
		this.operationSubType = oProcessWorkspace.getOperationSubType();
		
		if (oProcessWorkspace.getOperationTimestamp()!= null) {
			this.createdTimestamp = oProcessWorkspace.getOperationTimestamp().longValue();
		}
		
		if (oProcessWorkspace.getOperationStartTimestamp()!=null) {
			this.startTimestamp = oProcessWorkspace.getOperationStartTimestamp().longValue();
		}
		
		if (oProcessWorkspace.getOperationEndTimestamp()!=null) {
			this.endTimestamp = oProcessWorkspace.getOperationEndTimestamp().longValue();
		}
		
		
		this.runningTime = oProcessWorkspace.getRunningTime();
		this.fileSize = oProcessWorkspace.getFileSize();
		this.status = oProcessWorkspace.getStatus();
		this.progressPerc = oProcessWorkspace.getProgressPerc();
		this.nodeCode = oProcessWorkspace.getNodeCode();
		this.parentId = oProcessWorkspace.getParentId();
		this.projectId = oProcessWorkspace.getProjectId();
		this.subscriptionId = oProcessWorkspace.getSubscriptionId();
		
		if (Utils.isNullOrEmpty(this.nodeCode)) {
			this.nodeCode = WasdiConfig.Current.nodeCode;
		}
		
		if (this.startTimestamp == null || this.startTimestamp<=0) {
			this.startTimestamp = this.createdTimestamp;
		}
		
		if (this.endTimestamp == null || this.endTimestamp<=0) {
			
			boolean bInserted = false;
			
			if (oProcessWorkspace.getLastStateChangeTimestamp()!=null) {
				long lLastStateChange = oProcessWorkspace.getLastStateChangeTimestamp().longValue();
				if (lLastStateChange>this.startTimestamp) {
					bInserted = true;
					this.endTimestamp = lLastStateChange;
				}
			}
			
			if (!bInserted) {
				this.endTimestamp = this.startTimestamp;
			}
		}
		
		try {
			long lEnd = this.endTimestamp.longValue();
			long lStart = this.startTimestamp.longValue();
			this.totalSpentAfterStartedTime = lEnd-lStart;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("Job.Job: error computing running time");
		}		
		
		if (this.runningTime <= 0) {
			if (this.totalSpentAfterStartedTime!=null) {
				this.runningTime = this.totalSpentAfterStartedTime;
			}
		}
		
		try {
			this.waitingChildTime = this.totalSpentAfterStartedTime-this.runningTime;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("Job.Job: error computing running time");
		}			
		
		try {
			long lCreated = this.createdTimestamp.longValue();
			long lStart = this.startTimestamp.longValue();
			this.waitingBeforeStartTime = lStart-lCreated;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("Job.Job: error computing waiting time");
		}	
	}
	
    /**
     * Unique identifier of the job
     */
    private String processObjId;	
	/**
	 * Involved product or application or...
	 */
    private String name;
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
    private Long createdTimestamp;
    /**
     * Start date of the operation
     */
    private Long startTimestamp = 0L;    
    /**
     * End date of the operation
     */
    private Long endTimestamp = 0L;
    /**
     * Running time of the process (in milliseconds)
     * as effective time spend in RUNNING Status
     */
    private Long runningTime = 0L;
    /**
     * Total time spent between end and start
     */
    private Long totalSpentAfterStartedTime = 0L;
    /**
     * Total time spent waiting or ready
     */
    private Long waitingChildTime = 0L;    
    /**
     * Waiting time of the process (in milliseconds) to be started
     */
    private Long waitingBeforeStartTime = 0L;    
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
     * Code of the computing node of the operation
     */
    private String nodeCode="";
    /**
     * Parent Process Workspace Id
     */
    private String parentId;
	/**
	 * Project Id
	 */
	private String projectId;
	/**
	 * Subscription Id
	 */
	private String subscriptionId;
	
	public String getProcessObjId() {
		return processObjId;
	}
	public void setProcessObjId(String processObjId) {
		this.processObjId = processObjId;
	}
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
	public String getOperationType() {
		return operationType;
	}
	public void setOperationType(String operationType) {
		this.operationType = operationType;
	}
	public String getOperationSubType() {
		return operationSubType;
	}
	public void setOperationSubType(String operationSubType) {
		this.operationSubType = operationSubType;
	}
	public Long getCreatedTimestamp() {
		return createdTimestamp;
	}
	public void setCreatedTimestamp(Long createdTimeStamp) {
		this.createdTimestamp = createdTimeStamp;
	}
	public Long getStartTimestamp() {
		return startTimestamp;
	}
	public void setStartTimestamp(Long startTimestamp) {
		this.startTimestamp = startTimestamp;
	}
	public Long getEndTimestamp() {
		return endTimestamp;
	}
	public void setEndTimestamp(Long endTimestamp) {
		this.endTimestamp = endTimestamp;
	}
	public Long getRunningTime() {
		return runningTime;
	}
	public void setRunningTime(Long runningTime) {
		this.runningTime = runningTime;
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
	public String getNodeCode() {
		return nodeCode;
	}
	public void setNodeCode(String nodeCode) {
		this.nodeCode = nodeCode;
	}
	public String getParentId() {
		return parentId;
	}
	public void setParentId(String parentId) {
		this.parentId = parentId;
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getWaitingChildTime() {
		return waitingChildTime;
	}

	public void setWaitingChildTime(Long waitingChildTime) {
		this.waitingChildTime = waitingChildTime;
	}

	public Long getTotalSpentAfterStartedTime() {
		return totalSpentAfterStartedTime;
	}

	public void setTotalSpentAfterStartedTime(Long totalSpentAfterStartedTime) {
		this.totalSpentAfterStartedTime = totalSpentAfterStartedTime;
	}

	public Long getWaitingBeforeStartTime() {
		return waitingBeforeStartTime;
	}

	public void setWaitingBeforeStartTime(Long waitingBeforeStartTime) {
		this.waitingBeforeStartTime = waitingBeforeStartTime;
	}

}
