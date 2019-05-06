package wasdi.shared.business;

/**
 * Represent a row of the log of a custom processor
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
}
