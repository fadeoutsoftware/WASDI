package wasdi.shared.data.sqlite.labelling;

import java.util.List;

import wasdi.shared.business.labelling.ImageStyle;
import wasdi.shared.data.interfaces.labelling.IImageStyleRepositoryBackend;
import wasdi.shared.data.sqlite.SqliteRepository;

public class SqliteImageStyleRepositoryBackend  extends SqliteRepository implements IImageStyleRepositoryBackend {

	@Override
	public boolean insertImageStyle(ImageStyle oImageStyle) {
		return false;
	}

	@Override
	public ImageStyle getImageStyle(String sImageStyleId) {
		return null;
	}

	@Override
	public boolean updateImageStyle(ImageStyle oImageStyle) {
		return false;
	}

	@Override
	public boolean deleteImageStyle(String sImageStyleId) {
		return false;
	}

	@Override
	public List<ImageStyle> getAll() {
		return null;
	}

}
