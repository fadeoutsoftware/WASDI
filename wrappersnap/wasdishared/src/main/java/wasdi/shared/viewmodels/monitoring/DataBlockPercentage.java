package wasdi.shared.viewmodels.monitoring;

public class DataBlockPercentage {

	private DataEntryPercentage available;
	private DataEntryPercentage used;
	public DataEntryPercentage getAvailable() {
		return available;
	}
	public void setAvailable(DataEntryPercentage available) {
		this.available = available;
	}
	public DataEntryPercentage getUsed() {
		return used;
	}
	public void setUsed(DataEntryPercentage used) {
		this.used = used;
	}

}