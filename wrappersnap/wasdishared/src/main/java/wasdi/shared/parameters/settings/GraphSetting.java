package wasdi.shared.parameters.settings;

import java.util.ArrayList;

/**
 * Created by s.adamo on 16/03/2017.
 */
public class GraphSetting implements ISetting{

	String workflowName;
	
	String graphXml;

	private ArrayList<String> inputNodeNames = new ArrayList<>();
	private ArrayList<String> inputFileNames = new ArrayList<>();
	
	private ArrayList<String> outputNodeNames = new ArrayList<>();
	private ArrayList<String> outputFileNames = new ArrayList<>();
	
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
	
	
}
