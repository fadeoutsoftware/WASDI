package wasdi.shared.business;

/**
 * Snap Workflow Entity
 * Represent a snap xml workflow imported in WASDI
 * @author p.campanella
 *
 */
public class SnapWorkflow {
	/**
	 * Workflow Identifier
	 */
	private String workflowId;
	/**
	 * Name
	 */
	private String name;
	/**
	 * Description 
	 */
	private String description;
	/**
	 * User Owner
	 */
	private String userId;
	/**
	 * Full xml file path
	 */
	private String filePath;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	public String getWorkflowId() {
		return workflowId;
	}
	public void setWorkflowId(String workflowId) {
		this.workflowId = workflowId;
	}
}
