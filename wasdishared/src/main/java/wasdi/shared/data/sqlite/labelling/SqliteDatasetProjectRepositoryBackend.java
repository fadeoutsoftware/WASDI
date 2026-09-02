package wasdi.shared.data.sqlite.labelling;

import java.util.List;

import wasdi.shared.business.labelling.DatasetProject;
import wasdi.shared.data.interfaces.labelling.IDatasetProjectRepositoryBackend;
import wasdi.shared.data.sqlite.SqliteRepository;

public class SqliteDatasetProjectRepositoryBackend  extends SqliteRepository  implements IDatasetProjectRepositoryBackend {

	@Override
	public boolean insertDataset(DatasetProject oDataset) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public DatasetProject getDataset(String sDatasetId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean updateDataset(DatasetProject oDataset) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteDataset(String sDatasetId) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<DatasetProject> getDatasetsByOwner(String sOwnerId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<DatasetProject> getDatasetsForUser(String sUserId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<DatasetProject> getAll() {
		// TODO Auto-generated method stub
		return null;
	}

}
