package wasdi.shared.viewmodels;

public class ProcessWorkspaceSummaryViewModel {
	private int userProcessWaiting;
	private int userDownloadWaiting;
	private int userProcessRunning;
	private int userDownloadRunning;
	
	private int allProcessWaiting;
	private int allDownloadWaiting;
	private int allProcessRunning;
	private int allDownloadRunning;
	
	public int getUserProcessWaiting() {
		return userProcessWaiting;
	}
	public void setUserProcessWaiting(int userProcessWaiting) {
		this.userProcessWaiting = userProcessWaiting;
	}
	public int getUserDownloadWaiting() {
		return userDownloadWaiting;
	}
	public void setUserDownloadWaiting(int userDownloadWaiting) {
		this.userDownloadWaiting = userDownloadWaiting;
	}
	public int getUserProcessRunning() {
		return userProcessRunning;
	}
	public void setUserProcessRunning(int userProcessRunning) {
		this.userProcessRunning = userProcessRunning;
	}
	public int getUserDownloadRunning() {
		return userDownloadRunning;
	}
	public void setUserDownloadRunning(int userDownloadRunning) {
		this.userDownloadRunning = userDownloadRunning;
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
	 * @return the allDownloadWaiting
	 */
	public int getAllDownloadWaiting() {
		return allDownloadWaiting;
	}
	/**
	 * @param allDownloadWaiting the allDownloadWaiting to set
	 */
	public void setAllDownloadWaiting(int allDownloadWaiting) {
		this.allDownloadWaiting = allDownloadWaiting;
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
	/**
	 * @return the allDownloadRunning
	 */
	public int getAllDownloadRunning() {
		return allDownloadRunning;
	}
	/**
	 * @param allDownloadRunning the allDownloadRunning to set
	 */
	public void setAllDownloadRunning(int allDownloadRunning) {
		this.allDownloadRunning = allDownloadRunning;
	}
}
