package wasdi.shared.viewmodels.workflows;

import java.util.ArrayList;
import java.util.HashMap;

import wasdi.shared.business.SnapWorkflow;
/**
 * View model class to pass data from SnapWorkflow to UI 
 * aka Graph, Workflows
 * @author marco
 *
 */
public class SnapWorkflowViewModel {
	
	/**
	 * Workflow Unique Id
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
	 * Is public flag
	 */
	private boolean isPublic;
	/**
	 * User Owner of the workflow
	 */
	private String userId;
	/**
	 * Url of the node where was uploaded
	 */
	private String nodeUrl;
	
	/**
	 * Flag to know if this is shared with the requesting user
	 * This field should be initialized before return the view model checking in the workflow sharing through the repositories
	 * default value to false
	 */
	private boolean sharedWithMe = false;
	
	/**
	 * Read only flag in case is shared read only or public
	 */
	private boolean readOnly = false;

	/**
	 * List of the input node names
	 */
	private ArrayList<String> inputNodeNames = new ArrayList<>();
	/**
	 * List of the files to associate to the input nodes
	 */
	private ArrayList<String> inputFileNames = new ArrayList<>();
	
	/**
	 * List of the output node names
	 */
	private ArrayList<String> outputNodeNames = new ArrayList<>();
	/**
	 * List of the files to associate to the output nodes
	 */
	private ArrayList<String> outputFileNames = new ArrayList<>();
	
	/**
	 * Map of parameters: KEY-VALUE that will be used to fill potential parameters in the Workflow XML 
	 */
	private HashMap<String, String> templateParams = new HashMap<>();
 
	/**
	 * Default constructor
	 */
	public SnapWorkflowViewModel() {
		
	};
	
	/**
	 * Parameterized constructor with all fields except sharing  
	 * @param workflowId
	 * @param name
	 * @param description
	 * @param isPublic
	 * @param userId
	 * @param nodeUrl
	 * @param inputNodeNames
	 * @param inputFileNames
	 * @param outputNodeNames
	 * @param outputFileNames
	 */
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
	
	static public SnapWorkflowViewModel getFromWorkflow(SnapWorkflow oWorkflow) {
        SnapWorkflowViewModel oVM = new SnapWorkflowViewModel();
        oVM.setName(oWorkflow.getName());
        oVM.setDescription(oWorkflow.getDescription());
        oVM.setWorkflowId(oWorkflow.getWorkflowId());
        oVM.setOutputNodeNames(oWorkflow.getOutputNodeNames());
        oVM.setInputNodeNames(oWorkflow.getInputNodeNames());
        oVM.setPublic(oWorkflow.getIsPublic());
        oVM.setUserId(oWorkflow.getUserId());
        oVM.setNodeUrl(oWorkflow.getNodeUrl());
        return oVM;
	}
	public boolean isReadOnly() {
		return readOnly;
	}
	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}
	public HashMap<String, String> getTemplateParams() {
		return templateParams;
	}
	public void setTemplateParams(HashMap<String, String> templateParams) {
		this.templateParams = templateParams;
	}
	
}
