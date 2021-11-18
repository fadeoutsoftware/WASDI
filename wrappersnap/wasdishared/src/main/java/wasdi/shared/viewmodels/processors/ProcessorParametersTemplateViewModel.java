package wasdi.shared.viewmodels.processors;

/**
 * ProcessorParametersTemplate View Model
 * 
 * Represents a ProcessorParametersTemplate
 * 
 * @author PetruPetrescu
 *
 */
public class ProcessorParametersTemplateViewModel {

	private String templateId;
	private String userId;
	private String processorId;
	private String name;
	private String description;
	private String jsonParameters;

	public ProcessorParametersTemplateViewModel() {
		super();
	}

	public ProcessorParametersTemplateViewModel(String templateId, String userId, String processorId, String name, String description, String jsonParameters) {
		super();
		this.templateId = templateId;
		this.userId = userId;
		this.processorId = processorId;
		this.name = name;
		this.description = description;
		this.jsonParameters = jsonParameters;
	}

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

	@Override
	public String toString() {
		return "ProcessorParametersTemplateViewModel [templateId=" + templateId + ", userId=" + userId
				+ ", processorId=" + processorId + ", name=" + name + ", description=" + description
				+ ", jsonParameters=" + jsonParameters + "]";
	}

}
