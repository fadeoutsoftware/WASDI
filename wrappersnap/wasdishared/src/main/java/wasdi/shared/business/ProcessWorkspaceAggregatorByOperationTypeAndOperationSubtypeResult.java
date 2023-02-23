package wasdi.shared.business;

import lombok.AllArgsConstructor;
import lombok.experimental.Delegate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProcessWorkspaceAggregatorByOperationTypeAndOperationSubtypeResult {

	@Delegate
	private Id _id;

	private Integer count;

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@ToString
	private static class Id {

		private String operationType;
		private String operationSubType;
		private String status;

	}

}
