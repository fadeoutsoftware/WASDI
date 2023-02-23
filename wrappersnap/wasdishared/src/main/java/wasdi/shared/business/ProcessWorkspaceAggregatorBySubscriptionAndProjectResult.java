package wasdi.shared.business;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Delegate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProcessWorkspaceAggregatorBySubscriptionAndProjectResult {

	@Delegate
	private Id _id;

	private Long total;

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@ToString
	private static class Id {

		private String subscriptionId;
		private String projectId;

	}

}
