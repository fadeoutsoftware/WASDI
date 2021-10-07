package wasdi.shared.viewmodels.processors;

public class ProcessorLogViewModel {
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

	public String getProcessWorkspaceId() {
		return processWorkspaceId;
	}

	public void setProcessWorkspaceId(String processWorkspaceId) {
		this.processWorkspaceId = processWorkspaceId;
	}

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
}
