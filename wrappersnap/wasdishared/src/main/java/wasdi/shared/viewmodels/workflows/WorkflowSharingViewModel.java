package wasdi.shared.viewmodels.workflows;

import wasdi.shared.business.WorkflowSharing;

/**
 * Represents the sharing of a workflow with a user
 * @author p.campanella
 *
 */
public class WorkflowSharingViewModel {
	private String userId;

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public WorkflowSharingViewModel() {
		super();
	}

	public WorkflowSharingViewModel(WorkflowSharing oSharing) {
		super();
		this.userId = oSharing.getUserId();
	}
	
	
	
	
}
