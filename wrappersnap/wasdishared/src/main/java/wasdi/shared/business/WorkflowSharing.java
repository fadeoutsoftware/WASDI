package wasdi.shared.business;

/**
 * Workflow Sharing Entity
 * Represent the association between a workflow, his owner and the user that can access it
 * Workflows are referred as "snapWorkflow" or "Graphs" in the code
 * 
 * @author M.Menapace on 29/04/2021 
 *
 */
public class WorkflowSharing {
	/**
	 * Workflow Id
	 */
    private  String workflowId;
    
    /**
     * User that can access
     */
    private  String userId;
    
    /**
     * Workflow Owner
     */
    private  String ownerId;
    
    /**
     * Sharing grant timestamp
     */
    private Double shareDate;

	public String getWorkflowId() {
		return workflowId;
	}

	public void setWorkflowId(String workflowId) {
		this.workflowId = workflowId;
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

	public Double getShareDate() {
		return shareDate;
	}

	public void setShareDate(Double shareDate) {
		this.shareDate = shareDate;
	}
}
