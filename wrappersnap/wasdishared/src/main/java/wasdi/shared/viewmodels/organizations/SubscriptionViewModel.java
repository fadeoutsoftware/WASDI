package wasdi.shared.viewmodels.organizations;

public class SubscriptionViewModel {

	private String subscriptionId;
	private String name;
	private String description;
	private String typeId;
	private String typeName;
	private String buyDate;
	private String startDate;
	private String endDate;
	private int durationDays;
	private String userId;
	private String organizationId;
	private String organizationName;
	private boolean buySuccess;
	
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
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
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
	public String getBuyDate() {
		return buyDate;
	}
	public void setBuyDate(String buyDate) {
		this.buyDate = buyDate;
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
	public int getDurationDays() {
		return durationDays;
	}
	public void setDurationDays(int durationDays) {
		this.durationDays = durationDays;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getOrganizationId() {
		return organizationId;
	}
	public void setOrganizationId(String organizationId) {
		this.organizationId = organizationId;
	}
	public String getOrganizationName() {
		return organizationName;
	}
	public void setOrganizationName(String organizationName) {
		this.organizationName = organizationName;
	}
	public boolean isBuySuccess() {
		return buySuccess;
	}
	public void setBuySuccess(boolean buySuccess) {
		this.buySuccess = buySuccess;
	}

}
