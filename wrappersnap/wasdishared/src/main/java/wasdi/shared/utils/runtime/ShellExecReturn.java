package wasdi.shared.utils.runtime;


/**
 * Wraps the return values of a Shell Exec Operation.
 * 
 * It contains:
 * 	.
 * 
 * @author p.campanella
 *
 */
public class ShellExecReturn {
	
	/**
	 * Flag to decide if the operation has been started asynch (true) or synch (false)
	 * If it is asynch, logs and operation return will not be avaiable
	 */
	private boolean m_bAsynchOperation=false;
	
	/**
	 * Flag on the success of the operation. If it is false, the operation neither started probably
	 */
	private boolean m_bOperationOk=false;
	
	/**
	 * Code returned by the operation
	 */
	private int m_iOperationReturn = -1;
	
	/**
	 * Logs of the operation (if collected)
	 */
	private String m_sOperationLogs = "";
	
	/**
	 * Flag on the success of the operation. If it is false, the operation neither started probably
	 * @return
	 */
	public boolean isOperationOk() {
		return m_bOperationOk;
	}
	
	/**
	 * Set the flag
	 * @param bOperationOk
	 */
	public void setOperationOk(boolean bOperationOk) {
		this.m_bOperationOk = bOperationOk;
	}
	
	/**
	 * Code returned by the operation
	 * @return
	 */
	public int getOperationReturn() {
		return m_iOperationReturn;
	}
	
	/**
	 * Set the code of the operation
	 * @param iOperationReturn
	 */
	public void setOperationReturn(int iOperationReturn) {
		this.m_iOperationReturn = iOperationReturn;
	}
	
	/**
	 * Get the operation logs
	 * @return
	 */
	public String getOperationLogs() {
		return m_sOperationLogs;
	}
	
	/**
	 * Set the operation logs
	 * @param sOperationLogs
	 */
	public void setOperationLogs(String sOperationLogs) {
		this.m_sOperationLogs = sOperationLogs;
	}

	/**
	 * Flag to decide if the operation has been started asynch (true) or synch (false)
	 * @return
	 */
	public boolean isAsynchOperation() {
		return m_bAsynchOperation;
	}

	/**
	 * Set the asynch flag
	 * @param bAsynchOperation
	 */
	public void setAsynchOperation(boolean bAsynchOperation) {
		this.m_bAsynchOperation = bAsynchOperation;
	}

}
