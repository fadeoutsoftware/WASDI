package wasdi.shared.data.labelling;

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
}
