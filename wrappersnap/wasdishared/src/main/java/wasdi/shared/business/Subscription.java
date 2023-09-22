package wasdi.shared.business;

import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.organizations.SubscriptionType;


public class Subscription {
	
	public Subscription() {
		
	}

	private String subscriptionId;
	private String name;
	private String description;
	private String type;
	private Double buyDate;
	private Double startDate;
	private Double endDate;
	private int durationDays;
	private String userId;
	private String organizationId;
	private boolean buySuccess;
	
	public String getRelatedUserType() {
		return Subscription.fromSubscriptionTypeToUserType(type);
	}
	
	/**
	 * Check if a subscription is valid or not
	 * @return
	 */
	public boolean isValid() {
		if (this.getStartDate() == null
				|| this.getEndDate() == null) {
			return false;
		}

		double dNowInMillis = Utils.nowInMillis();

		return dNowInMillis >= this.getStartDate()
				&& dNowInMillis <= this.getEndDate();		
	}
	
	/**
	 * Converts a Subscription Type to the corresponding user type
	 * @param sSubscriptionType
	 * @return
	 */
	public static String  fromSubscriptionTypeToUserType(String sSubscriptionType) {
		
		if (Utils.isNullOrEmpty(sSubscriptionType)) return UserType.NONE.name();
		
		if (sSubscriptionType.equals(SubscriptionType.Free.name())) return UserType.FREE.name();
		
		if (sSubscriptionType.equals(SubscriptionType.OneDayStandard.name())) return UserType.STANDARD.name();
		if (sSubscriptionType.equals(SubscriptionType.OneWeekStandard.name())) return UserType.STANDARD.name();
		if (sSubscriptionType.equals(SubscriptionType.OneMonthStandard.name())) return UserType.STANDARD.name();
		if (sSubscriptionType.equals(SubscriptionType.OneYearStandard.name())) return UserType.STANDARD.name();
		
		if (sSubscriptionType.equals(SubscriptionType.OneMonthProfessional.name())) return UserType.PROFESSIONAL.name();
		if (sSubscriptionType.equals(SubscriptionType.OneYearProfessional.name())) return UserType.PROFESSIONAL.name();
		
		return UserType.NONE.name();
	}

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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Double getBuyDate() {
		return buyDate;
	}

	public void setBuyDate(Double buyDate) {
		this.buyDate = buyDate;
	}

	public Double getStartDate() {
		return startDate;
	}

	public void setStartDate(Double startDate) {
		this.startDate = startDate;
	}

	public Double getEndDate() {
		return endDate;
	}

	public void setEndDate(Double endDate) {
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

	public boolean isBuySuccess() {
		return buySuccess;
	}

	public void setBuySuccess(boolean buySuccess) {
		this.buySuccess = buySuccess;
	}

}
