package wasdi.shared.business;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.organizations.SubscriptionType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {

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

}
