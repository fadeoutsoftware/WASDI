package wasdi.shared.viewmodels.monitoring;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class MetricsEntry {

	private String node;
	private Timestamp timestamp;
	private Cpu cpu;
	private List<Disk> disks;
	private Memory memory;
	private List<License> licenses;

}
