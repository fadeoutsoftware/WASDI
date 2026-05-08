package wasdi.shared.data.interfaces;

import java.util.List;

import wasdi.shared.business.processors.ProcessorParametersTemplate;

/**
 * Backend contract for processor parameters template repository.
 */
public interface IProcessorParametersTemplateRepositoryBackend {

	List<ProcessorParametersTemplate> getProcessorParametersTemplatesByUser(String sUserId);

	List<ProcessorParametersTemplate> getProcessorParametersTemplatesByUserAndProcessor(String sUserId, String sProcessorId);

	List<ProcessorParametersTemplate> getProcessorParametersTemplatesByProcessor(String sProcessorId);

	ProcessorParametersTemplate getProcessorParametersTemplateByTemplateId(String sTemplateId);

	ProcessorParametersTemplate getProcessorParametersTemplatesByUserAndProcessorAndName(String sUserId, String sProcessorId, String sName);

	String insertProcessorParametersTemplate(ProcessorParametersTemplate oProcessorParametersTemplate);

	int deleteByTemplateId(String sTemplateId);

	boolean updateProcessorParametersTemplate(ProcessorParametersTemplate oProcessorParametersTemplate);

	boolean isTheOwnerOfTheTemplate(String sTemplateId, String sUserId);

	int deleteByUserId(String sUserId);
}
