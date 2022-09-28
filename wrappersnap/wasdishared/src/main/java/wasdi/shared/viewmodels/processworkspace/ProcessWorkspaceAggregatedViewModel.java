package wasdi.shared.viewmodels.processworkspace;

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
public class ProcessWorkspaceAggregatedViewModel {

	private String schedulerName;
	private String operationType;
	private String operationSubType;

	private Integer procCreated = 0;
	private Integer procRunning = 0;
	private Integer procWaiting = 0;
	private Integer procReady = 0;

	public int getNumberOfUnfinishedProcesses() {
		int iTotal = 0;

		if (this.procCreated != null) {
			iTotal += this.procCreated.intValue();
		}

		if (this.procRunning != null) {
			iTotal += this.procRunning.intValue();
		}

		if (this.procWaiting != null) {
			iTotal += this.procWaiting.intValue();
		}

		if (this.procReady != null) {
			iTotal += this.procReady.intValue();
		}

		return iTotal;
	}
}
