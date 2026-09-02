package wasdi.shared.data.labelling;

import java.util.List;

import wasdi.shared.business.labelling.Template;
import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.labelling.ILabellingTemplateRepositoryBackend;

public class LabellingTemplateRepository {
	private final ILabellingTemplateRepositoryBackend m_oBackend;

	public LabellingTemplateRepository() {
		m_oBackend = createBackend();
	}

	private ILabellingTemplateRepositoryBackend createBackend() {
		// For now keep Mongo backend only. Next step will select by config.
		return DataRepositoryFactoryProvider.getFactory().createLabellingTemplateRepository();
	}

	/**
	 * Insert a new Template
	 *
	 * @param oTemplate
	 * @return true if successful, false otherwise
	 */
	public boolean insertTemplate(Template oTemplate) {
		return m_oBackend.insertTemplate(oTemplate);
	}

	/**
	 * Get a Template by ID
	 *
	 * @param sTemplateId Template ID (GUID string)
	 * @return Template object or null if not found
	 */
	public Template getTemplate(String sTemplateId) {
		return m_oBackend.getTemplate(sTemplateId);
	}

	/**
	 * Update an existing Template
	 *
	 * @param oTemplate
	 * @return true if successful, false otherwise
	 */
	public boolean updateTemplate(Template oTemplate) {
		return m_oBackend.updateTemplate(oTemplate);
	}

	/**
	 * Delete a Template by ID
	 *
	 * @param sTemplateId Template ID (GUID string)
	 * @return true if successful, false otherwise
	 */
	public boolean deleteTemplate(String sTemplateId) {
		return m_oBackend.deleteTemplate(sTemplateId);
	}

	/**
	 * Get all Templates created by a specific user
	 *
	 * @param sCreatorId WASDI UserId (GUID string)
	 * @return List of Templates created by the user
	 */
	public List<Template> getTemplatesByCreator(String sCreatorId) {
		return m_oBackend.getTemplatesByCreator(sCreatorId);
	}

	/**
	 * Get all Templates
	 *
	 * @return List of all Templates
	 */
	public List<Template> getAll() {
		return m_oBackend.getAll();
	}
}
