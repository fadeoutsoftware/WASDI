package wasdi.shared.viewmodels.organizations;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class SubscriptionViewModel {

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

}
