package wasdi.shared.business;

/**
 * Represent a row of the log of a custom processor.
 * Each log has a rowNumber obtained with a Counter 
 * that uses the ProcessWorspaceId as sequence.
 * 
 * @author p.campanella
 *
 */
public class ProcessorLog {
	
	/**
	 * Log Date
	 */
	private String logDate;
	
	/**
	 * Log Text
	 */
	private String logRow;
	
	/**
	 * Row Number
	 */
	private int rowNumber;
	
	/**
	 * Process Workspace Id reference
	 */
	private String processWorkspaceId;
	
	public String getLogDate() {
		return logDate;
	}
	public void setLogDate(String logDate) {
		this.logDate = logDate;
	}
	public String getLogRow() {
		return logRow;
	}
	public void setLogRow(String logRow) {
		this.logRow = logRow;
	}
	public int getRowNumber() {
		return rowNumber;
	}
	public void setRowNumber(int rowNumber) {
		this.rowNumber = rowNumber;
	}
	public String getProcessWorkspaceId() {
		return processWorkspaceId;
	}
	public void setProcessWorkspaceId(String processWorkspaceId) {
		this.processWorkspaceId = processWorkspaceId;
	}
	/*public ObjectId get_id() {
		return _id;
	}
	public void set_id(ObjectId _id) {
		this._id = _id;
	}*/
	
}
