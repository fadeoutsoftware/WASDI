package wasdi.shared.viewmodels.processworkspace;

/**
 * Represents the summary of Waiting and Running Process Workspaces for one user.
 * Used by the client in the process bar. 
 * 
 * @author p.campanella
 *
 */
public class ProcessWorkspaceSummaryViewModel {
	private int userProcessWaiting;
	private int userProcessRunning;
	
	private int allProcessWaiting;
	private int allProcessRunning;
	
	public int getUserProcessWaiting() {
		return userProcessWaiting;
	}
	public void setUserProcessWaiting(int userProcessWaiting) {
		this.userProcessWaiting = userProcessWaiting;
	}

	public int getUserProcessRunning() {
		return userProcessRunning;
	}
	public void setUserProcessRunning(int userProcessRunning) {
		this.userProcessRunning = userProcessRunning;
	}
	/**
	 * @return the allProcessWaiting
	 */
	public int getAllProcessWaiting() {
		return allProcessWaiting;
	}
	/**
	 * @param allProcessWaiting the allProcessWaiting to set
	 */
	public void setAllProcessWaiting(int allProcessWaiting) {
		this.allProcessWaiting = allProcessWaiting;
	}
	/**
	 * @return the allProcessRunning
	 */
	public int getAllProcessRunning() {
		return allProcessRunning;
	}
	/**
	 * @param allProcessRunning the allProcessRunning to set
	 */
	public void setAllProcessRunning(int allProcessRunning) {
		this.allProcessRunning = allProcessRunning;
	}

}
