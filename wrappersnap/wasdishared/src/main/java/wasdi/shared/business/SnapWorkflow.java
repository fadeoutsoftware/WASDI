package wasdi.shared.business;

import java.util.ArrayList;

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
	
	/**
	 * List of the name of the input nodes
	 */
	private ArrayList<String> inputNodeNames = new ArrayList<>();
	
	/**
	 * List of the name of the output nodes
	 */
	private ArrayList<String> outputNodeNames = new ArrayList<>();
	
	/**
	 * Flag to know if the workflow is public or not
	 */
	private boolean isPublic;
	
	/**
	 * Code of the WASDI node where the workflow has been uploaded
	 */
	private String nodeCode;
	
	/**
	 * Url  of the WASDI node where the workflow has been uploaded
	 */
	private String nodeUrl;
	
	public String getNodeCode() {
		return nodeCode;
	}
	public void setNodeCode(String nodeCode) {
		this.nodeCode = nodeCode;
	}
	public String getNodeUrl() {
		return nodeUrl;
	}
	public void setNodeUrl(String nodeUrl) {
		this.nodeUrl = nodeUrl;
	}
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
	public ArrayList<String> getInputNodeNames() {
		return inputNodeNames;
	}
	public void setInputNodeNames(ArrayList<String> inputNodeNames) {
		this.inputNodeNames = inputNodeNames;
	}
	public ArrayList<String> getOutputNodeNames() {
		return outputNodeNames;
	}
	public void setOutputNodeNames(ArrayList<String> outputNodeNames) {
		this.outputNodeNames = outputNodeNames;
	}
	public boolean getIsPublic() {
		return isPublic;
	}
	public void setIsPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}
}
