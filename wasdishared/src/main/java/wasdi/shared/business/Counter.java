package wasdi.shared.business;

/**
 * Represent a set of counters.
 * Since mongo does not have native counters, this can be used
 * to simulate an automatic sequence of relationals db.
 * 
 * The user can decide a name of a sequence. This entity contains the sequence name
 * and the last number used for that sequence.
 * 
 * the Repository has methods to create a new sequence (Counter) and to get the
 * next valid number.
 * 
 * At the end, in the db, there should be only one Counter entity for each sequence.
 *  
 * @author p.campanella
 *
 */
public class Counter {
	
	/**
	 * Sequence Id
	 */
	private String sequence;
	
	/**
	 * Last value
	 */
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
