package wasdi.shared.viewmodels;

/**
 * View model to represent a multitude of heterogeneous resource.
 * Every resource in WASDI having:
 * - having an unique identifier
 * - having a user associated with that resource
 * - being classified with a specific resource type
 * can be represented by this view model.
 *
 */

public class GenericResourceViewModel {
	
	private String resourceType;
	private String resourceId;
	private String userId;

	public GenericResourceViewModel(String resourceType, String resourceId, String userId) {
		this.resourceType = resourceType;
		this.resourceId = resourceId;
		this.userId = userId;
	}

	public String getResourceType() {
		return resourceType;
	}

	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}

	public String getResourceId() {
		return resourceId;
	}

	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	

}
