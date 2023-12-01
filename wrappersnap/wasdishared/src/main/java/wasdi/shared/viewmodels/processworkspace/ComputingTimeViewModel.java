package wasdi.shared.viewmodels.processworkspace;

/**
 * Used by the process resource to get statistics about the overall computing time of a project
 * @author valentina.leone
 *
 */

public class ComputingTimeViewModel {
	
	// The id of the subscription
	private String subscriptionId;
	
	// The id of the project
	private String projectId;
	
	// The id of the user the computing time refers to. If null, the processing time refers to the time computed considering all the users who worked on the project. 
	private String userId;
	
	// The computing time (in milliseconds) 
	private Long computingTime;
	
	
	public String getSubscriptionId() {
		return subscriptionId;
	}

	public void setSubscriptionId(String subscriptionId) {
		this.subscriptionId = subscriptionId;
	}

	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public Long getComputingTime() {
		return computingTime;
	}

	public void setComputingTime(Long processingTime) {
		this.computingTime = processingTime;
	}
	
	

}
