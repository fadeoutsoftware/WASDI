package wasdi.shared.viewmodels.monitoring;

public class Cpu {

	private Count count;
	private Frequency frequency;
	private Load load;
	public Count getCount() {
		return count;
	}
	public void setCount(Count count) {
		this.count = count;
	}
	public Frequency getFrequency() {
		return frequency;
	}
	public void setFrequency(Frequency frequency) {
		this.frequency = frequency;
	}
	public Load getLoad() {
		return load;
	}
	public void setLoad(Load load) {
		this.load = load;
	}

}