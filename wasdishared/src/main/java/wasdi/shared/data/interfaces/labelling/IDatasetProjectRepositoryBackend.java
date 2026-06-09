package wasdi.shared.data.interfaces.labelling;

import java.util.List;

import wasdi.shared.business.labelling.DatasetProject;

public interface IDatasetProjectRepositoryBackend {
	
	public boolean insertDataset(DatasetProject oDataset);
	public DatasetProject getDataset(String sDatasetId);
	public boolean updateDataset(DatasetProject oDataset);
	public boolean deleteDataset(String sDatasetId);
	public List<DatasetProject> getDatasetsByOwner(String sOwnerId);
	public List<DatasetProject> getDatasetsForUser(String sUserId);
	public List<DatasetProject> getAll();

}
