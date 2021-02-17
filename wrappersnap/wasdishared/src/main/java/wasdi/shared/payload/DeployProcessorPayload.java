package wasdi.shared.payload;

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
