package wasdi.shared.payloads;

import wasdi.shared.LauncherOperations;

/**
 * Payload of the Delete Processor Operation
 * 
 * @author p.campanella
 *
 */
public class DeleteProcessorPayload extends OperationPayload {
	
	/**
	 * Name of the processor
	 */
	String processorName;
	
	/**
	 * Processor Id
	 */
	String processorId;
	
	public DeleteProcessorPayload() {
		this.operation = LauncherOperations.DELETEPROCESSOR.name();
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
