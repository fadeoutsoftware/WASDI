package wasdi.shared.viewmodels.organizations;

public class SubscriptionListViewModel {

	private String subscriptionId;
	private String ownerUserId;
	private String name;
//	private String description;
	private String typeId;
	private String typeName;
//	private Double buyDate;
	private String startDate;
	private String endDate;
//	private int durationDays;
//	private String userId;
//	private String organizationId;
	private String organizationName;
	private String reason;
	private boolean buySuccess;
	private boolean adminRole;
	private Long runningTime;
	public String getSubscriptionId() {
		return subscriptionId;
	}
	public void setSubscriptionId(String subscriptionId) {
		this.subscriptionId = subscriptionId;
	}
	public String getOwnerUserId() {
		return ownerUserId;
	}
	public void setOwnerUserId(String ownerUserId) {
		this.ownerUserId = ownerUserId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getTypeId() {
		return typeId;
	}
	public void setTypeId(String typeId) {
		this.typeId = typeId;
	}
	public String getTypeName() {
		return typeName;
	}
	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}
	public String getStartDate() {
		return startDate;
	}
	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}
	public String getEndDate() {
		return endDate;
	}
	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}
	public String getOrganizationName() {
		return organizationName;
	}
	public void setOrganizationName(String organizationName) {
		this.organizationName = organizationName;
	}
	public String getReason() {
		return reason;
	}
	public void setReason(String reason) {
		this.reason = reason;
	}
	public boolean isBuySuccess() {
		return buySuccess;
	}
	public void setBuySuccess(boolean buySuccess) {
		this.buySuccess = buySuccess;
	}
	public boolean isAdminRole() {
		return adminRole;
	}
	public void setAdminRole(boolean adminRole) {
		this.adminRole = adminRole;
	}
	public Long getRunningTime() {
		return runningTime;
	}
	public void setRunningTime(Long runningTime) {
		this.runningTime = runningTime;
	}

}
