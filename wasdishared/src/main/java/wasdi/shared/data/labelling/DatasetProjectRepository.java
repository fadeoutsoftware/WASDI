package wasdi.shared.data.labelling;

import java.util.List;

import wasdi.shared.business.labelling.DatasetProject;
import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.labelling.IDatasetProjectRepositoryBackend;

public class DatasetProjectRepository {
	private final IDatasetProjectRepositoryBackend m_oBackend;

	public DatasetProjectRepository() {
		m_oBackend = createBackend();
	}

	private IDatasetProjectRepositoryBackend createBackend() {
		// For now keep Mongo backend only. Next step will select by config.
		return DataRepositoryFactoryProvider.getFactory().createDatasetProjectRepository();
	}
	
	public boolean insertDataset(DatasetProject oDataset) {
		return m_oBackend.insertDataset(oDataset);
	}
	
	public DatasetProject getDataset(String sDatasetId) {
		return m_oBackend.getDataset(sDatasetId);
	}
	
	public boolean updateDataset(DatasetProject oDataset) {
		return m_oBackend.updateDataset(oDataset);
	}
	
	public boolean deleteDataset(String sDatasetId) {
		return m_oBackend.deleteDataset(sDatasetId);
	}
	
	public List<DatasetProject> getDatasetsByOwner(String sOwnerId) {
		return m_oBackend.getDatasetsByOwner(sOwnerId);
	}
	
	public List<DatasetProject> getDatasetsForUser(String sUserId) {
		return m_oBackend.getDatasetsForUser(sUserId);
	}
	public List<DatasetProject> getAll() {
		return m_oBackend.getAll();
	}
}
