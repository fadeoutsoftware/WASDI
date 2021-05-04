package wasdi.shared.viewmodels;

import java.util.ArrayList;
/**
 * View model class to pass data from SnapWorkflow to UI 
 * aka Graph, Workflows
 * @author marco
 *
 */
public class SnapWorkflowViewModel {
	private String workflowId;
	private String name;
	private String description;
	private boolean isPublic;
	private String userId;
	private String nodeUrl;
	// This field should be initialized before return the view model checking in the workflow sharing
	// through the repositories 
	// default value to false;
	private boolean sharedWithMe = false;

	private ArrayList<String> inputNodeNames = new ArrayList<>();
	private ArrayList<String> inputFileNames = new ArrayList<>();
	
	private ArrayList<String> outputNodeNames = new ArrayList<>();
	private ArrayList<String> outputFileNames = new ArrayList<>();
 
	//Default constructor
	public SnapWorkflowViewModel() {};
	// Parameterized constructor with all fields except sharing  
	public SnapWorkflowViewModel(String workflowId, String name, String description, boolean isPublic, String userId,
			String nodeUrl, ArrayList<String> inputNodeNames, ArrayList<String> inputFileNames,
			ArrayList<String> outputNodeNames, ArrayList<String> outputFileNames) {
		super();
		this.workflowId = workflowId;
		this.name = name;
		this.description = description;
		this.isPublic = isPublic;
		this.userId = userId;
		this.nodeUrl = nodeUrl;
		this.inputNodeNames = inputNodeNames;
		this.inputFileNames = inputFileNames;
		this.outputNodeNames = outputNodeNames;
		this.outputFileNames = outputFileNames;
	}
	public String getNodeUrl() {
		return nodeUrl;
	}
	public void setNodeUrl(String nodeUrl) {
		this.nodeUrl = nodeUrl;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
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
	public ArrayList<String> getInputFileNames() {
		return inputFileNames;
	}
	public void setInputFileNames(ArrayList<String> inputFileNames) {
		this.inputFileNames = inputFileNames;
	}
	public ArrayList<String> getOutputNodeNames() {
		return outputNodeNames;
	}
	public void setOutputNodeNames(ArrayList<String> outputNodeNames) {
		this.outputNodeNames = outputNodeNames;
	}
	public ArrayList<String> getOutputFileNames() {
		return outputFileNames;
	}
	public void setOutputFileNames(ArrayList<String> outputFileNames) {
		this.outputFileNames = outputFileNames;
	}
	public boolean isPublic() {
		return isPublic;
	}
	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}

	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public boolean isSharedWithMe() {
		return sharedWithMe;
	}
	public void setSharedWithMe(boolean sharedWithMe) {
		this.sharedWithMe = sharedWithMe;
	}
	
}
