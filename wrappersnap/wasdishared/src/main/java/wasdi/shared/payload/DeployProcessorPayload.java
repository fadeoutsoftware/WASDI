package wasdi.shared.payload;

/**
 * Payload of the Deploy Processor Operation
 * 
 * @author p.campanella
 *
 */
public class DeployProcessorPayload extends OperationPayload {
	
	private String processorName;
	private String type;
	
	public DeployProcessorPayload() {
		this.operation = "DEPLOYPROCESSOR";
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
