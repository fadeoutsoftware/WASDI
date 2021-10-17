package wasdi.shared.payload;

/**
 * Payload of the GRAPH Operation
 * 
 * @author p.campanella
 *
 */
public class ExecuteGraphPayload extends OperationPayload {
	public ExecuteGraphPayload() {
		operation = "GRAPH";
	}
	
	private String workflowName;
	private String [] inputFiles;
	private String [] outputFiles;
	
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
