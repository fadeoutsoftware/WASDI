package wasdi.shared.parameters;

public class DeployProcessorParameter extends BaseParameter {
	private String name;
	private String processorID;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getProcessorID() {
		return processorID;
	}
	public void setProcessorID(String processorID) {
		this.processorID = processorID;
	}
}
