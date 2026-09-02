package wasdi.shared.data.no2.labelling;

import java.util.List;

import wasdi.shared.business.labelling.Label;
import wasdi.shared.data.interfaces.labelling.ILabelRepositoryBackend;
import wasdi.shared.data.no2.No2Repository;

public class No2LabelRepositoryBackend  extends No2Repository implements ILabelRepositoryBackend {

	@Override
	public boolean insertLabel(Label oLabel) {
		return false;
	}

	@Override
	public Label getLabel(String sLabelId) {
		return null;
	}

	@Override
	public boolean updateLabel(Label oLabel) {
		return false;
	}

	@Override
	public boolean deleteLabel(String sLabelId) {
		return false;
	}

	@Override
	public List<Label> getLabelsByImage(String sDatasetId, String sImage) {
		return null;
	}

	@Override
	public List<Label> getAll() {
		return null;
	}

	@Override
	public List<Label> getLabelsByDataset(String sDatasetId) {
		// TODO Auto-generated method stub
		return null;
	}

}
