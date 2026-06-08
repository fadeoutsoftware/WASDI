package wasdi.shared.data.labelling;

import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.labelling.IImageStyleRepositoryBackend;

public class ImageStyleRepository {
	private final IImageStyleRepositoryBackend m_oBackend;

	public ImageStyleRepository() {
		m_oBackend = createBackend();
	}

	private IImageStyleRepositoryBackend createBackend() {
		// For now keep Mongo backend only. Next step will select by config.
		return DataRepositoryFactoryProvider.getFactory().createImageStyleRepository();
	}
}
