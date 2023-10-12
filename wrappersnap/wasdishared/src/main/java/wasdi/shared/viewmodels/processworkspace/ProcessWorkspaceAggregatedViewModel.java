package wasdi.shared.viewmodels.processworkspace;

public class ProcessWorkspaceAggregatedViewModel {

	private String schedulerName;
	private String operationType;
	private String operationSubType;

	private Integer procCreated = 0;
	private Integer procRunning = 0;
	private Integer procWaiting = 0;
	private Integer procReady = 0;

	public int getNumberOfUnfinishedProcesses() {
		int iTotal = 0;

		if (this.procCreated != null) {
			iTotal += this.procCreated.intValue();
		}

		if (this.procRunning != null) {
			iTotal += this.procRunning.intValue();
		}

		if (this.procWaiting != null) {
			iTotal += this.procWaiting.intValue();
		}

		if (this.procReady != null) {
			iTotal += this.procReady.intValue();
		}

		return iTotal;
	}

	public String getSchedulerName() {
		return schedulerName;
	}

	public void setSchedulerName(String schedulerName) {
		this.schedulerName = schedulerName;
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

	public Integer getProcCreated() {
		return procCreated;
	}

	public void setProcCreated(Integer procCreated) {
		this.procCreated = procCreated;
	}

	public Integer getProcRunning() {
		return procRunning;
	}

	public void setProcRunning(Integer procRunning) {
		this.procRunning = procRunning;
	}

	public Integer getProcWaiting() {
		return procWaiting;
	}

	public void setProcWaiting(Integer procWaiting) {
		this.procWaiting = procWaiting;
	}

	public Integer getProcReady() {
		return procReady;
	}

	public void setProcReady(Integer procReady) {
		this.procReady = procReady;
	}
}
