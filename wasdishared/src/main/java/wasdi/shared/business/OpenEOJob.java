package wasdi.shared.business;

/**
 * Represents an OpenEO Job.
 * The job Id, if the process is started, is the same of a corresponding WASDI Process Workspace Id.
 * 
 * Parameters is the JSON representation of the open EO Parameters received. 
 * 
 * @author p.campanella
 *
 */
public class OpenEOJob {
	
	private String jobId;
	
	private String parameters;
	
	private String userId;
	
	private boolean started = false;
	
	private String workspaceId;	

	public boolean isStarted() {
		return started;
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public String getParameters() {
		return parameters;
	}

	public void setParameters(String parameters) {
		this.parameters = parameters;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public boolean gestStarted() {
		return started;
	}

	public void setStarted(boolean started) {
		this.started = started;
	}
	
	public String getWorkspaceId() {
		return workspaceId;
	}

	public void setWorkspaceId(String workspaceId) {
		this.workspaceId = workspaceId;
	}
}
