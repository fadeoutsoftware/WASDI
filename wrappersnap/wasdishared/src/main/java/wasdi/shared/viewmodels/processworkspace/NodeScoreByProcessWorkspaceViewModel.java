package wasdi.shared.viewmodels.processworkspace;

public class NodeScoreByProcessWorkspaceViewModel {

	private String nodeCode;
	private Integer numberOfProcesses;

	private Double diskPercentageUsed = 0.0;
	private Double diskPercentageAvailable = 0.0;

	private Long diskAbsoluteTotal = 0L;
	private Long diskAbsoluteUsed = 0L;
	private Long diskAbsoluteAvailable = 0L;

	private Double memoryPercentageUsed = 0.0;
	private Double memoryPercentageAvailable = 0.0;

	private Long memoryAbsoluteTotal = 0L;
	private Long memoryAbsoluteUsed = 0L;
	private Long memoryAbsoluteAvailable = 0L;

	private String licenses;

	private String timestampAsString;

	public String getNodeCode() {
		return nodeCode;
	}

	public void setNodeCode(String nodeCode) {
		this.nodeCode = nodeCode;
	}

	public Integer getNumberOfProcesses() {
		return numberOfProcesses;
	}

	public void setNumberOfProcesses(Integer numberOfProcesses) {
		this.numberOfProcesses = numberOfProcesses;
	}

	public Double getDiskPercentageUsed() {
		return diskPercentageUsed;
	}

	public void setDiskPercentageUsed(Double diskPercentageUsed) {
		this.diskPercentageUsed = diskPercentageUsed;
	}

	public Double getDiskPercentageAvailable() {
		return diskPercentageAvailable;
	}

	public void setDiskPercentageAvailable(Double diskPercentageAvailable) {
		this.diskPercentageAvailable = diskPercentageAvailable;
	}

	public Long getDiskAbsoluteTotal() {
		return diskAbsoluteTotal;
	}

	public void setDiskAbsoluteTotal(Long diskAbsoluteTotal) {
		this.diskAbsoluteTotal = diskAbsoluteTotal;
	}

	public Long getDiskAbsoluteUsed() {
		return diskAbsoluteUsed;
	}

	public void setDiskAbsoluteUsed(Long diskAbsoluteUsed) {
		this.diskAbsoluteUsed = diskAbsoluteUsed;
	}

	public Long getDiskAbsoluteAvailable() {
		return diskAbsoluteAvailable;
	}

	public void setDiskAbsoluteAvailable(Long diskAbsoluteAvailable) {
		this.diskAbsoluteAvailable = diskAbsoluteAvailable;
	}

	public Double getMemoryPercentageUsed() {
		return memoryPercentageUsed;
	}

	public void setMemoryPercentageUsed(Double memoryPercentageUsed) {
		this.memoryPercentageUsed = memoryPercentageUsed;
	}

	public Double getMemoryPercentageAvailable() {
		return memoryPercentageAvailable;
	}

	public void setMemoryPercentageAvailable(Double memoryPercentageAvailable) {
		this.memoryPercentageAvailable = memoryPercentageAvailable;
	}

	public Long getMemoryAbsoluteTotal() {
		return memoryAbsoluteTotal;
	}

	public void setMemoryAbsoluteTotal(Long memoryAbsoluteTotal) {
		this.memoryAbsoluteTotal = memoryAbsoluteTotal;
	}

	public Long getMemoryAbsoluteUsed() {
		return memoryAbsoluteUsed;
	}

	public void setMemoryAbsoluteUsed(Long memoryAbsoluteUsed) {
		this.memoryAbsoluteUsed = memoryAbsoluteUsed;
	}

	public Long getMemoryAbsoluteAvailable() {
		return memoryAbsoluteAvailable;
	}

	public void setMemoryAbsoluteAvailable(Long memoryAbsoluteAvailable) {
		this.memoryAbsoluteAvailable = memoryAbsoluteAvailable;
	}

	public String getLicenses() {
		return licenses;
	}

	public void setLicenses(String licenses) {
		this.licenses = licenses;
	}

	public String getTimestampAsString() {
		return timestampAsString;
	}

	public void setTimestampAsString(String timestampAsString) {
		this.timestampAsString = timestampAsString;
	}

}
