package wasdi.shared.viewmodels.ogcprocesses;

public class Subscriber {
	private String successUri = null;
	private String inProgressUri = null;
	private String failedUri = null;
	
	public String getSuccessUri() {
		return successUri;
	}
	public void setSuccessUri(String successUri) {
		this.successUri = successUri;
	}
	public String getInProgressUri() {
		return inProgressUri;
	}
	public void setInProgressUri(String inProgressUri) {
		this.inProgressUri = inProgressUri;
	}
	public String getFailedUri() {
		return failedUri;
	}
	public void setFailedUri(String failedUri) {
		this.failedUri = failedUri;
	}

}
