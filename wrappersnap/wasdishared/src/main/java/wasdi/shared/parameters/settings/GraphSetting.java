package wasdi.shared.parameters.settings;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by s.adamo on 16/03/2017.
 */
public class GraphSetting implements ISetting{

	/**
	 * Workflow Name
	 */
	String workflowName;
	
	/**
	 * Effective XML graph
	 */
	String graphXml;

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

	public String getGraphXml() {
		return graphXml;
	}

	public void setGraphXml(String graphXml) {
		this.graphXml = graphXml;
	}

	public String getWorkflowName() {
		return workflowName;
	}

	public void setWorkflowName(String workflowName) {
		this.workflowName = workflowName;
	}

	public HashMap<String, String> getTemplateParams() {
		return templateParams;
	}

	public void setTemplateParams(HashMap<String, String> templateParams) {
		this.templateParams = templateParams;
	}
	
	
}
