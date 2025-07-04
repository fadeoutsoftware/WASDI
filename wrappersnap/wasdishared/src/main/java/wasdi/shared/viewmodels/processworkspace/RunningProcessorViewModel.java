package wasdi.shared.viewmodels.processworkspace;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Used by the processor resource to return info about the new started
 * application. It merges name and id of the processor with the process workspace id,
 *  the status and the result (payload) when availalbe.
 * 
 * @author p.campanella
 *
 */
@XmlRootElement
public class RunningProcessorViewModel {
	private String processorId;
	private String name;
	private String processingIdentifier;
	private String status;
	private String jsonEncodedResult;
	private String message;
	
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
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	
	
}
