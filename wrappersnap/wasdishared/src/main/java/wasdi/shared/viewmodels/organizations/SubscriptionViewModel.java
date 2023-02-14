package wasdi.shared.viewmodels.organizations;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

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

	@JsonFormat(shape=JsonFormat.Shape.STRING)
	@JsonProperty("buyDate")
	private Date buyDate;

	@JsonFormat(shape=JsonFormat.Shape.STRING)
	@JsonProperty("startDate")
	private Date startDate;

	@JsonFormat(shape=JsonFormat.Shape.STRING)
	@JsonProperty("endDate")
	private Date endDate;

	private int durationDays;
	private String userId;
	private String organizationId;
	private String organizationName;
	private boolean buySuccess;

}
