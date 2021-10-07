package wasdi.shared.viewmodels.processworkspace;

public class RunningProcessorViewModel {
	private String processorId;
	private String name;
	private String processingIdentifier;
	private String status;
	private String jsonEncodedResult;
	public String getProcessorId() {
		return processorId;
	}
	public void setProcessorId(String processorId) {
		this.processorId = processorId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getProcessingIdentifier() {
		return processingIdentifier;
	}
	public void setProcessingIdentifier(String processingIdentifier) {
		this.processingIdentifier = processingIdentifier;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getJsonEncodedResult() {
		return jsonEncodedResult;
	}
	public void setJsonEncodedResult(String jsonEncodedResult) {
		this.jsonEncodedResult = jsonEncodedResult;
	}
	
	
}
