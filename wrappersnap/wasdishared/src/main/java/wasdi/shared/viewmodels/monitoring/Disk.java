package wasdi.shared.viewmodels.monitoring;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class Disk {

	public String mountpoint;
	public DataBlockAbsolute absolute;
	public DataBlockPercentage percentage;

}