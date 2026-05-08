package wasdi.shared.data.sqlite;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import wasdi.shared.business.Style;
import wasdi.shared.data.interfaces.IStyleRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * SQLite backend implementation for style repository.
 */
public class SqliteStyleRepositoryBackend extends SqliteRepository implements IStyleRepositoryBackend {

	public SqliteStyleRepositoryBackend() {
		m_sThisCollection = "styles";
		this.ensureTable(m_sThisCollection);
	}

	@Override
	public boolean insertStyle(Style oStyle) {

		try {
			if (oStyle == null || Utils.isNullOrEmpty(oStyle.getStyleId())) {
				return false;
			}
			return insert(oStyle.getStyleId(), oStyle);
		} catch (Exception oEx) {
			WasdiLog.errorLog("StyleRepository.insertStyle : error ", oEx);
		}

		return false;
	}

	@Override
	public Style getStyle(String sStyleId) {

		try {
			return findOneWhere("styleId", sStyleId, Style.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("StyleRepository.getStyle : error ", oEx);
		}

		return null;
	}

	@Override
	public Style getStyleByName(String sName) {

		try {
			return findOneWhere("name", sName, Style.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("StyleRepository.getStyleByName : error ", oEx);
		}

		return null;
	}

	@Override
	public boolean isStyleNameTaken(String sName) {

		try {
			return countWhere("name", sName) > 0;
		} catch (Exception oEx) {
			WasdiLog.errorLog("StyleRepository.isStyleNameTaken : error ", oEx);
		}

		return true;
	}

	@Override
	public List<Style> getStylePublicAndByUser(String sUserId) {
		try {
			return queryList(
					"SELECT data FROM " + m_sThisCollection +
					" WHERE json_extract(data,'$.userId') = ? OR json_extract(data,'$.isPublic') = 1" +
					" ORDER BY json_extract(data,'$.name') ASC",
					new Object[]{sUserId}, Style.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("StyleRepository.getStylePublicAndByUser : error ", oEx);
		}

		return new ArrayList<>();
	}

	@Override
	public List<Style> getList() {
		try {
			return queryList(
					"SELECT data FROM " + m_sThisCollection + " ORDER BY json_extract(data,'$.name') ASC",
					new Object[]{}, Style.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("StyleRepository.getList : error ", oEx);
		}

		return new ArrayList<>();
	}

	@Override
	public boolean updateStyle(Style oStyle) {

		try {
			if (oStyle == null || Utils.isNullOrEmpty(oStyle.getStyleId())) {
				return false;
			}
			return updateById(oStyle.getStyleId(), oStyle);
		} catch (Exception oEx) {
			WasdiLog.errorLog("StyleRepository.updateStyle : error ", oEx);
		}

		return false;
	}

	@Override
	public boolean deleteStyle(String sStyleId) {
		if (Utils.isNullOrEmpty(sStyleId)) {
			return false;
		}

		try {
			return deleteById(sStyleId) > 0;
		} catch (Exception oEx) {
			WasdiLog.errorLog("StyleRepository.deleteStyle : error ", oEx);
		}

		return false;
	}

	@Override
	public int deleteStyleByUser(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId)) {
			return 0;
		}

		try {
			return deleteWhere("userId", sUserId);
		} catch (Exception oEx) {
			WasdiLog.errorLog("StyleRepository.deleteStyleByUser : error ", oEx);
		}

		return 0;
	}

	@Override
	public boolean isOwnedByUser(String sUserId, String sStyleId) {
		try {
			Map<String, Object> oFilter = new HashMap<>();
			oFilter.put("userId", sUserId);
			oFilter.put("styleId", sStyleId);
			return countWhere(oFilter) > 0;
		} catch (Exception oE) {
			WasdiLog.errorLog("StyleRepository.isStyleOwnedByUser( " + sUserId + ", " + sStyleId + " ): ", oE);
		}

		return false;
	}

	@Override
	public List<Style> findStylesByPartialName(String sPartialName) {

		if (Utils.isNullOrEmpty(sPartialName) || sPartialName.length() < 3) {
			return null;
		}

		try {
			String sLike = "%" + sPartialName + "%";
			return queryList(
					"SELECT data FROM " + m_sThisCollection +
					" WHERE LOWER(json_extract(data,'$.styleId')) LIKE LOWER(?)" +
					" OR LOWER(json_extract(data,'$.name')) LIKE LOWER(?)" +
					" OR LOWER(json_extract(data,'$.description')) LIKE LOWER(?)" +
					" ORDER BY json_extract(data,'$.name') ASC",
					new Object[]{sLike, sLike, sLike},
					Style.class);

		} catch (Exception oEx) {
			WasdiLog.errorLog("StyleWorkspaceRepository.findStylesByPartialName : error ", oEx);
		}

		return new ArrayList<>();
	}
}
