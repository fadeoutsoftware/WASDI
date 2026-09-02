package wasdi.shared.data.labelling;

import java.util.List;

import wasdi.shared.business.labelling.ImageStyle;
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

	public boolean insertImageStyle(ImageStyle oImageStyle) {
		return m_oBackend.insertImageStyle(oImageStyle);
	}

	public ImageStyle getImageStyle(String sImageStyleId) {
		return m_oBackend.getImageStyle(sImageStyleId);
	}

	public boolean updateImageStyle(ImageStyle oImageStyle) {
		return m_oBackend.updateImageStyle(oImageStyle);
	}

	public boolean deleteImageStyle(String sImageStyleId) {
		return m_oBackend.deleteImageStyle(sImageStyleId);
	}

	public List<ImageStyle> getAll() {
		return m_oBackend.getAll();
	}
}
