package wasdi.shared.data.labelling;

import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.labelling.IDatasetImageRepositoryBackend;

public class DatasetImageRepository {
	private final IDatasetImageRepositoryBackend m_oBackend;

	public DatasetImageRepository() {
		m_oBackend = createBackend();
	}

	private IDatasetImageRepositoryBackend createBackend() {
		// For now keep Mongo backend only. Next step will select by config.
		return DataRepositoryFactoryProvider.getFactory().createDatasetImageRepository();
	}
}
