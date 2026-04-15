package wasdi.shared.data;

import java.util.List;

import wasdi.shared.business.Style;
import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.IStyleRepositoryBackend;

public class StyleRepository {
	private final IStyleRepositoryBackend m_oBackend;

	public StyleRepository() {
		m_oBackend = createBackend();
	}

	private IStyleRepositoryBackend createBackend() {
		return DataRepositoryFactoryProvider.getFactory().createStyleRepository();
	}

	/**
	 * Insert a new style
	 *
	 * @param oStyle
	 * @return
	 */
	public boolean insertStyle(Style oStyle) {
		return m_oBackend.insertStyle(oStyle);
	}

	/**
	 * Get a style by Id
	 *
	 * @param sStyleId
	 * @return
	 */
	public Style getStyle(String sStyleId) {
		return m_oBackend.getStyle(sStyleId);
	}

	/**
	 * Get a style by name.
	 *
	 * @param sName style name
	 * @return the style corresponding to the name
	 */
	public Style getStyleByName(String sName) {
		return m_oBackend.getStyleByName(sName);
	}

	/**
	 * Check if the name is already taken by other style.
	 *
	 * @param sName style name
	 * @return true if there is already a style with the same style, false otherwise
	 */
	public boolean isStyleNameTaken(String sName) {
		return m_oBackend.isStyleNameTaken(sName);
	}

	/**
	 * Get all the style that can be accessed by UserId
	 *
	 * @param sUserId
	 * @return List of private style of users plus all the public ones
	 */
	public List<Style> getStylePublicAndByUser(String sUserId) {
		return m_oBackend.getStylePublicAndByUser(sUserId);
	}

	/**
	 * Get the list of all styles
	 *
	 * @return
	 */
	public List<Style> getList() {
		return m_oBackend.getList();
	}

	/**
	 * Update a Style
	 *
	 * @param oStyle
	 * @return
	 */
	public boolean updateStyle(Style oStyle) {
		return m_oBackend.updateStyle(oStyle);
	}

	/**
	 * Deletes a style
	 */
	public boolean deleteStyle(String sStyleId) {
		return m_oBackend.deleteStyle(sStyleId);
	}

	/**
	 * Delete all the styles of User
	 *
	 * @param sUserId
	 * @return
	 */
	public int deleteStyleByUser(String sUserId) {
		return m_oBackend.deleteStyleByUser(sUserId);
	}

	/**
	 * Check if the user is the owner of the style workspace
	 * @param sUserId a valid user id
	 * @param sStyleId a valid style id
	 * @return true if the user launched the style, false otherwise
	 */
	public boolean isOwnedByUser(String sUserId, String sStyleId) {
		return m_oBackend.isOwnedByUser(sUserId, sStyleId);
	}
	
    /**
     * Find a style by partial name, by partial description or by partial id
     * @return the list of styles that partially match the name, the description or the id
     */
    public List<Style> findStylesByPartialName(String sPartialName) {
		return m_oBackend.findStylesByPartialName(sPartialName);
    }

}

