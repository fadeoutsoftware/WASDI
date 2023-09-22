package wasdi.shared.business;

public class ProcessWorkspaceAggregatorBySubscriptionAndProjectResult {

	private Id _id;

	private Long total;

	private static class Id {

		private String subscriptionId;
		private String projectId;
		
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

	}
	
	public String getSubscriptionId() {
		return this._id.getSubscriptionId();
	}
	public void setSubscriptionId(String subscriptionId) {
		this._id.setSubscriptionId(subscriptionId);
	}
	public String getProjectId() {
		return this._id.getProjectId();
	}
	public void setProjectId(String projectId) {
		this._id.setProjectId(projectId);
	}
	public Id get_id() {
		return _id;
	}
	public void set_id(Id _id) {
		this._id = _id;
	}
	public Long getTotal() {
		return total;
	}
	public void setTotal(Long total) {
		this.total = total;
	}	

}
