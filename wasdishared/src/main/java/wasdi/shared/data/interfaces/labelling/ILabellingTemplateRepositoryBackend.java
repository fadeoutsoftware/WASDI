package wasdi.shared.data.interfaces.labelling;

import java.util.List;

import wasdi.shared.business.labelling.Template;

/**
 * Backend contract for labelling template repository.
 */
public interface ILabellingTemplateRepositoryBackend {

	boolean insertTemplate(Template oTemplate);

	Template getTemplate(String sTemplateId);

	boolean updateTemplate(Template oTemplate);

	boolean deleteTemplate(String sTemplateId);

	List<Template> getTemplatesByCreator(String sCreatorId);

	List<Template> getAll();
}
