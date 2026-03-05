package wasdi.shared.data;

import java.util.List;

import wasdi.shared.business.processors.ProcessorParametersTemplate;
import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.IProcessorParametersTemplateRepositoryBackend;

/**
 * ProcessorParametersTemplate repository
 * @author PetruPetrescu
 *
 */
public class ProcessorParametersTemplateRepository {

	private final IProcessorParametersTemplateRepositoryBackend m_oBackend;

	public ProcessorParametersTemplateRepository() {
		m_oBackend = createBackend();
	}

	private IProcessorParametersTemplateRepositoryBackend createBackend() {
		// For now keep Mongo backend only. Next step will select by config.
		return DataRepositoryFactoryProvider.getFactory().createProcessorParametersTemplateRepository();
	}

	/**
	 * Get the list of ProcessorParametersTemplates by user and processor.
	 * 
	 * @param sUserId the id of the user
	 * @return the list of processorParameterTemplates
	 */
	public List<ProcessorParametersTemplate> getProcessorParametersTemplatesByUser(String sUserId) {
		return m_oBackend.getProcessorParametersTemplatesByUser(sUserId);
	}

	/**
	 * Get the list of ProcessorParametersTemplates by user and processor.
	 * 
	 * @param sUserId      the id of the user
	 * @param sProcessorId the id of the processor
	 * @return the list of processorParameterTemplates
	 */
	public List<ProcessorParametersTemplate> getProcessorParametersTemplatesByUserAndProcessor(String sUserId,
			String sProcessorId) {
		return m_oBackend.getProcessorParametersTemplatesByUserAndProcessor(sUserId, sProcessorId);
	}

	/**
	 * Get the list of ProcessorParametersTemplates by user and processor.
	 * 
	 * @param sProcessorId the id of the processor
	 * @return the list of processorParameterTemplates
	 */
	public List<ProcessorParametersTemplate> getProcessorParametersTemplatesByProcessor(String sProcessorId) {
		return m_oBackend.getProcessorParametersTemplatesByProcessor(sProcessorId);
	}

	/**
	 * Get a ProcessorParametersTemplate by templateId.
	 * 
	 * @param sTemplateId the id of the template
	 * @return Entity
	 */
	public ProcessorParametersTemplate getProcessorParametersTemplateByTemplateId(String sTemplateId) {
		return m_oBackend.getProcessorParametersTemplateByTemplateId(sTemplateId);
	}

	/**
	 * Get a ProcessorParametersTemplate by user, processor and name.
	 * 
	 * @param sUserId      the id of the user
	 * @param sProcessorId the id of the processor
	 * @param sName        the name of the parameters template
	 * @return Entity
	 */
	public ProcessorParametersTemplate getProcessorParametersTemplatesByUserAndProcessorAndName(String sUserId,
			String sProcessorId, String sName) {
		return m_oBackend.getProcessorParametersTemplatesByUserAndProcessorAndName(sUserId, sProcessorId, sName);
	}

	/**
	 * Create a new ProcessorParametersTemplate.
	 * 
	 * @param oProcessorParametersTemplate Entity
	 * @return Obj Id
	 */
	public String insertProcessorParametersTemplate(ProcessorParametersTemplate oProcessorParametersTemplate) {
		return m_oBackend.insertProcessorParametersTemplate(oProcessorParametersTemplate);
	}

	/**
	 * Delete the ProcessorParametersTemplate by templateId.
	 * 
	 * @param sTemplateId the id of the template
	 * @return 1 if it was deleted, 0 if it did not exists
	 */
	public int deleteByTemplateId(String sTemplateId) {
		return m_oBackend.deleteByTemplateId(sTemplateId);
	}

	/**
	 * Update an existing ProcessorParametersTemplate.
	 * 
	 * @param oProcessorParametersTemplate Entity
	 * @return true if the record was updated, false otherwise
	 */
	public boolean updateProcessorParametersTemplate(ProcessorParametersTemplate oProcessorParametersTemplate) {
		return m_oBackend.updateProcessorParametersTemplate(oProcessorParametersTemplate);

	}

	/**
	 * Check if the indicated user is the owner of the template.
	 * @param sTemplateId the template Id
	 * @param sUserId the user Id
	 * @return true if the user is the owner of the template, false otherwise
	 */
	public boolean isTheOwnerOfTheTemplate(String sTemplateId, String sUserId) {
		return m_oBackend.isTheOwnerOfTheTemplate(sTemplateId, sUserId);
	}
	
	/**
	 * Delete the ProcessorParametersTemplates by UserId.
	 * 
	 * @param sUSerId the id of the user
	 * @return number of elements deleted
	 */
	public int deleteByUserId(String sUserId) {
		return m_oBackend.deleteByUserId(sUserId);
	}	

}

