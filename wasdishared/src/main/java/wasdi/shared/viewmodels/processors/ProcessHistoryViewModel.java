package wasdi.shared.viewmodels.processors;

/**
 * Process History View Model
 * 
 * Single line of process history to let the user
 * re-open the old workspace used.
 * 
 * @author p.campanella
 *
 */
public class ProcessHistoryViewModel {
    private String processorName;
    private String operationDate;
    private String operationStartDate;
    private String operationEndDate;
    private String status;
    private String workspaceName;
    private String workspaceId;
    
	public String getProcessorName() {
		return processorName;
	}
	public void setProcessorName(String processorName) {
		this.processorName = processorName;
	}
	public String getOperationDate() {
		return operationDate;
	}
	public void setOperationDate(String operationDate) {
		this.operationDate = operationDate;
	}
	public String getOperationStartDate() {
		return operationStartDate;
	}
	public void setOperationStartDate(String operationStartDate) {
		this.operationStartDate = operationStartDate;
	}
	public String getOperationEndDate() {
		return operationEndDate;
	}
	public void setOperationEndDate(String operationEndDate) {
		this.operationEndDate = operationEndDate;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getWorkspaceName() {
		return workspaceName;
	}
	public void setWorkspaceName(String workspaceName) {
		this.workspaceName = workspaceName;
	}
	public String getWorkspaceId() {
		return workspaceId;
	}
	public void setWorkspaceId(String workspaceId) {
		this.workspaceId = workspaceId;
	}
}
