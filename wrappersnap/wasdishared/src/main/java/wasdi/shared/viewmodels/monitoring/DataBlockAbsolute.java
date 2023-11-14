package wasdi.shared.viewmodels.monitoring;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class DataBlockAbsolute {

	private DataEntryAbsolute available;
	private DataEntryAbsolute total;
	private DataEntryAbsolute used;
	private DataEntryAbsolute free;

}