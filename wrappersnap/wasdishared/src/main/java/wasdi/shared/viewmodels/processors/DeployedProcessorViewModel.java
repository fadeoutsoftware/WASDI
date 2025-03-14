package wasdi.shared.viewmodels.processors;

/**
 * Deployed Processor View Model
 * 
 * Used by the technical view of the applications.
 * It can be used also to update the processor entity on the server.
 * 
 * @author p.campanella
 *
 */
public class DeployedProcessorViewModel {
	private String processorId;
	private String processorName;
	private String processorVersion;
	private String processorDescription;
	private String imgLink;
	private String logo;
	private String publisher;
	private String publisherNickName;
	private String paramsSample = "";
	private int isPublic = 0;
	private int minuteTimeout = 180;
	private String type = "";
	private Boolean sharedWithMe = false;
	private boolean readOnly = false;
	private boolean isDeploymentOngoing = false;
	
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
	public int getIsPublic() {
		return isPublic;
	}
	public void setIsPublic(int isPublic) {
		this.isPublic = isPublic;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Boolean getSharedWithMe() {
		return sharedWithMe;
	}
	public void setSharedWithMe(Boolean sharedWithMe) {
		this.sharedWithMe = sharedWithMe;
	}
	public int getMinuteTimeout() {
		return minuteTimeout;
	}
	public void setMinuteTimeout(int minuteTimeout) {
		this.minuteTimeout = minuteTimeout;
	}
	public boolean isReadOnly() {
		return readOnly;
	}
	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}
	public String getLogo() {
		return logo;
	}
	public void setLogo(String logo) {
		this.logo = logo;
	}
	public boolean isDeploymentOngoing() {
		return isDeploymentOngoing;
	}
	public void setDeploymentOngoing(boolean isDeploymentOngoing) {
		this.isDeploymentOngoing = isDeploymentOngoing;
	}
	public String getPublisherNickName() {
		return publisherNickName;
	}
	public void setPublisherNickName(String publisherNickName) {
		this.publisherNickName = publisherNickName;
	}
}
