package wasdi.shared.viewmodels.monitoring;

import java.util.List;

public class MetricsEntry {

	private String node;
	private Timestamp timestamp;
	private Cpu cpu;
	private List<Disk> disks;
	private Memory memory;
	private List<License> licenses;
	
	public String getNode() {
		return node;
	}
	public void setNode(String node) {
		this.node = node;
	}
	public Timestamp getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Timestamp timestamp) {
		this.timestamp = timestamp;
	}
	public Cpu getCpu() {
		return cpu;
	}
	public void setCpu(Cpu cpu) {
		this.cpu = cpu;
	}
	public List<Disk> getDisks() {
		return disks;
	}
	public void setDisks(List<Disk> disks) {
		this.disks = disks;
	}
	public Memory getMemory() {
		return memory;
	}
	public void setMemory(Memory memory) {
		this.memory = memory;
	}
	public List<License> getLicenses() {
		return licenses;
	}
	public void setLicenses(List<License> licenses) {
		this.licenses = licenses;
	}

}
