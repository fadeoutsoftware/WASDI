package wasdi.shared.data.interfaces;

import java.util.List;

import wasdi.shared.business.Style;

/**
 * Backend contract for style repository.
 */
public interface IStyleRepositoryBackend {

	boolean insertStyle(Style oStyle);

	Style getStyle(String sStyleId);

	Style getStyleByName(String sName);

	boolean isStyleNameTaken(String sName);

	List<Style> getStylePublicAndByUser(String sUserId);

	List<Style> getList();

	boolean updateStyle(Style oStyle);

	boolean deleteStyle(String sStyleId);

	int deleteStyleByUser(String sUserId);

	boolean isOwnedByUser(String sUserId, String sStyleId);

	List<Style> findStylesByPartialName(String sPartialName);
}
