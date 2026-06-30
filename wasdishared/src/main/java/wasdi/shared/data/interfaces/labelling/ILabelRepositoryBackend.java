package wasdi.shared.data.interfaces.labelling;

import java.util.List;

import wasdi.shared.business.labelling.Label;

public interface ILabelRepositoryBackend {
	boolean insertLabel(Label oLabel);

	Label getLabel(String sLabelId);

	boolean updateLabel(Label oLabel);

	boolean deleteLabel(String sLabelId);

	List<Label> getLabelsByImage(String sDatasetId, String sImage);

	List<Label> getAll();
	
	List<Label> getLabelsByDataset(String sDatasetId);

}
