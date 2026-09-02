package wasdi.shared.data.sqlite.labelling;

import java.util.List;

import wasdi.shared.business.labelling.Template;
import wasdi.shared.data.interfaces.labelling.ILabellingTemplateRepositoryBackend;
import wasdi.shared.data.sqlite.SqliteRepository;

public class SqliteLabellingTemplatesRepositoryBackend  extends SqliteRepository implements ILabellingTemplateRepositoryBackend{

	@Override
	public boolean insertTemplate(Template oTemplate) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Template getTemplate(String sTemplateId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean updateTemplate(Template oTemplate) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteTemplate(String sTemplateId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<Template> getTemplatesByCreator(String sCreatorId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Template> getAll() {
		// TODO Auto-generated method stub
		return null;
	}

}
