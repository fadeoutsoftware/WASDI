package wasdi.shared.viewmodels.processors;

/**
 * ProcessorParametersTemplate List View Model
 * 
 * Wraps a list of comments view models
 * 
 * @author PetruPetrescu
 *
 */

public class ProcessorParametersTemplateListViewModel {

	private String templateId;
	private String userId;
	private String processorId;
	private String name;
	private String updateDate;
	private boolean readOnly;
	
	public String getTemplateId() {
		return templateId;
	}
	public void setTemplateId(String templateId) {
		this.templateId = templateId;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
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
	public String getUpdateDate() {
		return updateDate;
	}
	public void setUpdateDate(String updateDate) {
		this.updateDate = updateDate;
	}
	public boolean isReadOnly() {
		return readOnly;
	}
	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

}
