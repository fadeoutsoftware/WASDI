package wasdi.shared.payload;

public class DeleteProcessorPayload extends OperationPayload {
	
	String processorName;
	String processorId;
	
	public DeleteProcessorPayload() {
		this.operation = "DELETEPROCESSOR";
	}	
	
	public String getProcessorName() {
		return processorName;
	}

	public void setProcessorName(String processorName) {
		this.processorName = processorName;
	}

	public String getProcessorId() {
		return processorId;
	}

	public void setProcessorId(String processorId) {
		this.processorId = processorId;
	}
}
