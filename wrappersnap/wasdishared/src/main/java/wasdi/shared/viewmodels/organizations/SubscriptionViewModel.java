package wasdi.shared.viewmodels.organizations;

import java.util.Date;

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
	private String typeId;
	private String typeName;
	private Date buyDate;
	private Date startDate;
	private Date endDate;
	private int durationDays;
	private String userId;
	private String organizationId;
	private String organizationName;
	private boolean buySuccess;

}
