package wasdi.shared.data.labelling;

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
}
