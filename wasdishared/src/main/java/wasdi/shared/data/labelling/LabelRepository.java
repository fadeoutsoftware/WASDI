package wasdi.shared.data.labelling;

import java.util.List;

import wasdi.shared.business.labelling.Label;
import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.labelling.ILabelRepositoryBackend;

public class LabelRepository {
	private final ILabelRepositoryBackend m_oBackend;

	public LabelRepository() {
		m_oBackend = createBackend();
	}

	private ILabelRepositoryBackend createBackend() {
		// For now keep Mongo backend only. Next step will select by config.
		return DataRepositoryFactoryProvider.getFactory().createLabelRepository();
	}

	public boolean insertLabel(Label oLabel) {
		return m_oBackend.insertLabel(oLabel);
	}

	public Label getLabel(String sLabelId) {
		return m_oBackend.getLabel(sLabelId);
	}

	public boolean updateLabel(Label oLabel) {
		return m_oBackend.updateLabel(oLabel);
	}

	public boolean deleteLabel(String sLabelId) {
		return m_oBackend.deleteLabel(sLabelId);
	}

	public List<Label> getLabelsByImage(String sDatasetId, String sImage) {
		return m_oBackend.getLabelsByImage(sDatasetId, sImage);
	}

	public List<Label> getAll() {
		return m_oBackend.getAll();
	}
	
	public List<Label> getLabelsByDataset(String sDatasetId){
		return m_oBackend.getLabelsByDataset(sDatasetId);
	}
}
