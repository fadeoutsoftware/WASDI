package wasdi.shared.viewmodels.monitoring;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class DataBlockPercentage {

	private DataEntryPercentage available;
	private DataEntryPercentage used;

}