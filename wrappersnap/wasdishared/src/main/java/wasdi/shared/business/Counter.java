package wasdi.shared.business;

/**
 * Represent a set of counters
 * @author p.campanella
 *
 */
public class Counter {
	private String sequence;
	private int value;
	
	public String getSequence() {
		return sequence;
	}
	public void setSequence(String sequence) {
		this.sequence = sequence;
	}
	public int getValue() {
		return value;
	}
	public void setValue(int value) {
		this.value = value;
	}
	
	
}
