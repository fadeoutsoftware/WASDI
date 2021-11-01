package wasdi.shared.payloads;

import wasdi.shared.LauncherOperations;

/**
 * Payload of the GRAPH Operation
 * 
 * @author p.campanella
 *
 */
public class ExecuteGraphPayload extends OperationPayload {
	
	/**
	 * Name of the workflow
	 */
	private String workflowName;
	
	/**
	 * List of input files
	 */
	private String [] inputFiles;
	
	/**
	 * Name of ouptut files
	 */
	private String [] outputFiles;
	
	public ExecuteGraphPayload() {
		operation = LauncherOperations.GRAPH.name();
	}
		
	public String getWorkflowName() {
		return workflowName;
	}
	public void setWorkflowName(String workflowName) {
		this.workflowName = workflowName;
	}
	public String[] getInputFiles() {
		return inputFiles;
	}
	public void setInputFiles(String[] inputFiles) {
		this.inputFiles = inputFiles;
	}
	public String[] getOutputFiles() {
		return outputFiles;
	}
	public void setOutputFiles(String[] outputFiles) {
		this.outputFiles = outputFiles;
	}
}
