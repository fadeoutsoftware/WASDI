package wasdi.shared.payloads;

import wasdi.shared.LauncherOperations;

/**
 * Payload of the Deploy Processor Operation
 * 
 * @author p.campanella
 *
 */
public class DeployProcessorPayload extends OperationPayload {
	
	/**
	 * Processor Name
	 */
	private String processorName;
	
	/**
	 * Processor Type
	 */
	private String type;
	
	public DeployProcessorPayload() {
		this.operation = LauncherOperations.DEPLOYPROCESSOR.name();
	}	
	
	public String getProcessorName() {
		return processorName;
	}

	public void setProcessorName(String processorName) {
		this.processorName = processorName;
	}
	
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
