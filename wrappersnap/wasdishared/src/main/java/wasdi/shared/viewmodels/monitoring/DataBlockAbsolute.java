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

	public DataEntryAbsolute available;
	public DataEntryAbsolute total;
	public DataEntryAbsolute used;
	public DataEntryAbsolute free;

}