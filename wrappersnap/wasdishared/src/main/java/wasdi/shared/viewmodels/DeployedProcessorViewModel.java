package wasdi.shared.viewmodels;

public class DeployedProcessorViewModel {
	private String processorId;
	private String processorName;
	private String processorVersion;
	private String processorDescription;
	private String imgLink;
	private String publisher;
	private String paramsSample = "";
	
	public String getParamsSample() {
		return paramsSample;
	}
	public void setParamsSample(String paramsSample) {
		this.paramsSample = paramsSample;
	}
	public String getProcessorId() {
		return processorId;
	}
	public void setProcessorId(String processorId) {
		this.processorId = processorId;
	}
	public String getProcessorName() {
		return processorName;
	}
	public void setProcessorName(String processorName) {
		this.processorName = processorName;
	}
	public String getProcessorVersion() {
		return processorVersion;
	}
	public String getImgLink() {
		return imgLink;
	}
	public void setImgLink(String imgLink) {
		this.imgLink = imgLink;
	}
	public String getPublisher() {
		return publisher;
	}
	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}
	public void setProcessorVersion(String processorVersion) {
		this.processorVersion = processorVersion;
	}
	public String getProcessorDescription() {
		return processorDescription;
	}
	public void setProcessorDescription(String processorDescription) {
		this.processorDescription = processorDescription;
	}
}
