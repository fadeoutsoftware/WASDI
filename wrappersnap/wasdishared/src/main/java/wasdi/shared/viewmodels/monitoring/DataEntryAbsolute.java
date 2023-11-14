package wasdi.shared.viewmodels.monitoring;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class DataEntryAbsolute {

	private String unit;
	private Long value;

}