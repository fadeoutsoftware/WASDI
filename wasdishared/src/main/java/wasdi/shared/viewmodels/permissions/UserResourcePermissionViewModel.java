package wasdi.shared.viewmodels.permissions;

public class UserResourcePermissionViewModel {

	private String resourceId;
	private String resourceType;
	private String userId;
	private String ownerId;

	private String permissions;

	private String createdBy;
	private Double createdDate;
	
	public String getResourceId() {
		return resourceId;
	}
	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}
	public String getResourceType() {
		return resourceType;
	}
	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
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
	public String getPermissions() {
		return permissions;
	}
	public void setPermissions(String permissions) {
		this.permissions = permissions;
	}
	public String getCreatedBy() {
		return createdBy;
	}
	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}
	public Double getCreatedDate() {
		return createdDate;
	}
	public void setCreatedDate(Double createdDate) {
		this.createdDate = createdDate;
	}

}
