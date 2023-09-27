package wasdi.shared.business.processors;

/**
 * Parameters template for processor by user Cross-table for users and
 * processors (applications). It enables a specific user to run a specific
 * processor with a specific set of parameters. The user can have more such
 * parameter templates for a specific processor.
 * 
 * @author PetruPetrescu
 *
 */
public class ProcessorParametersTemplate {
	
	public ProcessorParametersTemplate() {
		
	}

	/** Identifier of the template */
	private String templateId;

	/** User owner of the processor */
	private String userId;

	/** Identifier of the processor */
	private String processorId;

	/** Processor Name */
	private String name;

	/** Processor Description */
	private String description;

	/** Sample JSON Parameter */
	private String jsonParameters;

	/** Creation timestamp */
	private Double creationDate;

	/** Last update timestamp */
	private Double updateDate;

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

	public Double getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Double creationDate) {
		this.creationDate = creationDate;
	}

	public Double getUpdateDate() {
		return updateDate;
	}

	public void setUpdateDate(Double updateDate) {
		this.updateDate = updateDate;
	}

}
