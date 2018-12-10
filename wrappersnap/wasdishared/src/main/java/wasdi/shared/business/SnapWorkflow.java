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
}
