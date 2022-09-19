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

	public String node;
	public Double timestamp;
	public Cpu cpu;
	public List<Disk> disks;
	public Memory memory;

}
