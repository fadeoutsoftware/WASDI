package wasdi.shared.viewmodels.processors;

/**
 * ProcessorParametersTemplate Detail View Model
 * 
 * Represents a ProcessorParametersTemplate
 * 
 * @author PetruPetrescu
 *
 */
public class ProcessorParametersTemplateDetailViewModel {

	private String templateId;
	private String userId;
	private String processorId;
	private String name;
	private String description;
	private String jsonParameters;
    private String creationDate;
	private String updateDate;
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
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getJsonParameters() {
		return jsonParameters;
	}
	public void setJsonParameters(String jsonParameters) {
		this.jsonParameters = jsonParameters;
	}
	public String getCreationDate() {
		return creationDate;
	}
	public void setCreationDate(String creationDate) {
		this.creationDate = creationDate;
	}
	public String getUpdateDate() {
		return updateDate;
	}
	public void setUpdateDate(String updateDate) {
		this.updateDate = updateDate;
	}

}
