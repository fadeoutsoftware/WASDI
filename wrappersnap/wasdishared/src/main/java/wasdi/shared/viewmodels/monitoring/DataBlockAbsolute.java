package wasdi.shared.viewmodels.monitoring;

public class DataBlockAbsolute {

	private DataEntryAbsolute available;
	private DataEntryAbsolute total;
	private DataEntryAbsolute used;
	private DataEntryAbsolute free;
	public DataEntryAbsolute getAvailable() {
		return available;
	}
	public void setAvailable(DataEntryAbsolute available) {
		this.available = available;
	}
	public DataEntryAbsolute getTotal() {
		return total;
	}
	public void setTotal(DataEntryAbsolute total) {
		this.total = total;
	}
	public DataEntryAbsolute getUsed() {
		return used;
	}
	public void setUsed(DataEntryAbsolute used) {
		this.used = used;
	}
	public DataEntryAbsolute getFree() {
		return free;
	}
	public void setFree(DataEntryAbsolute free) {
		this.free = free;
	}

}