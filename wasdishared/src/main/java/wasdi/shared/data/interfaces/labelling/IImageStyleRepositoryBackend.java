package wasdi.shared.data.interfaces.labelling;

import java.util.List;

import wasdi.shared.business.labelling.ImageStyle;

public interface IImageStyleRepositoryBackend {
	boolean insertImageStyle(ImageStyle oImageStyle);

	ImageStyle getImageStyle(String sImageStyleId);

	boolean updateImageStyle(ImageStyle oImageStyle);

	boolean deleteImageStyle(String sImageStyleId);

	List<ImageStyle> getAll();

}
