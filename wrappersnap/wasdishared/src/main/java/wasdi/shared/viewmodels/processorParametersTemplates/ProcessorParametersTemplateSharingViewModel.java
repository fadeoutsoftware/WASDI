package wasdi.shared.viewmodels.processorParametersTemplates;

public class ProcessorParametersTemplateSharingViewModel {

	private  String processorParametersTemplateId;
	private  String userId;
	private  String ownerId;
	private  String permissions;
	
	public String getProcessorParametersTemplateId() {
		return processorParametersTemplateId;
	}
	public void setProcessorParametersTemplateId(String processorParametersTemplateId) {
		this.processorParametersTemplateId = processorParametersTemplateId;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getOwnerId() {
		return ownerId;
	}
	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}
	public String getPermissions() {
		return permissions;
	}
	public void setPermissions(String permissions) {
		this.permissions = permissions;
	}

}
