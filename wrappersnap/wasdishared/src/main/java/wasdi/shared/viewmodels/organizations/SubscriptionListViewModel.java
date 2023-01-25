package wasdi.shared.viewmodels.organizations;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionListViewModel {

	private String subscriptionId;
	private String ownerUserId;
	private String name;
//	private String description;
//	private String typeId;
	private String typeName;
//	private Double buyDate;
//	private Double startDate;
//	private Double endDate;
//	private int durationDays;
//	private String userId;
//	private String organizationId;
	private String organizationName;
	private String reason;
//	private boolean buySuccess;
	private boolean adminRole;

}
