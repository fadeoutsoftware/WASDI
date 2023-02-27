package wasdi.shared.business;

/**
 * Represent a kind of index of the Ogc Processes API task created.
 * OGC Processes API is an alternative interface to WASDI processors. The tasks API are mapped to the WASDI Process Workspace equivalent.
 * Since WASDI apps are distributed across nodes, while the OGC Interface is offered only on the main server
 * this entity is needed to keep on the main server a list of the ogc processes executed. It can be used by the OGC web server 
 * to quickly get the address of the real computational node used to retrive the status, progress and results.
 * 
 * @author p.campanella
 *
 */
public class OgcProcessesTask {
	
	/**
	 * Process Workspace Id
	 */
	private String processWorkspaceId;
	
	/**
	 * Workpsace Id	
	 */
	private String workspaceId;
	
	/**
	 * User Id
	 */
	private String userId;

	public String getProcessWorkspaceId() {
		return processWorkspaceId;
	}

	public void setProcessWorkspaceId(String processWorkspaceId) {
		this.processWorkspaceId = processWorkspaceId;
	}

	public String getWorkspaceId() {
		return workspaceId;
	}

	public void setWorkspaceId(String workspaceId) {
		this.workspaceId = workspaceId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}
}
