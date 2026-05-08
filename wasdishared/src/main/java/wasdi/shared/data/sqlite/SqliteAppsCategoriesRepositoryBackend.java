package wasdi.shared.data.sqlite;

import java.util.List;

import wasdi.shared.business.AppCategory;
import wasdi.shared.data.interfaces.IAppsCategoriesRepositoryBackend;
import wasdi.shared.utils.log.WasdiLog;

/**
 * SQLite backend implementation for apps categories repository.
 */
public class SqliteAppsCategoriesRepositoryBackend extends SqliteRepository implements IAppsCategoriesRepositoryBackend {

	public SqliteAppsCategoriesRepositoryBackend() {
		m_sThisCollection = "appscategories";
		ensureTable(m_sThisCollection);
	}

	@Override
	public List<AppCategory> getCategories() {
		try {
			return findAll(m_sThisCollection, AppCategory.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteAppsCategoriesRepositoryBackend.getCategories: error", oEx);
		}

		return new java.util.ArrayList<>();
	}

	@Override
	public AppCategory getCategoryById(String sCategoryId) {
		try {
			return findOneWhere(m_sThisCollection, "id", sCategoryId, AppCategory.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteAppsCategoriesRepositoryBackend.getCategoryById: error", oEx);
		}

		return null;
	}

	@Override
	public boolean insertCategory(AppCategory oCategory) {
		if (oCategory == null) {
			return false;
		}

		try {
			return insert(m_sThisCollection, oCategory.getId(), oCategory);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteAppsCategoriesRepositoryBackend.insertCategory: error", oEx);
		}

		return false;
	}

	@Override
	public boolean deleteCategory(String sCategoryId) {
		try {
			return deleteWhere(m_sThisCollection, "id", sCategoryId) > 0;
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteAppsCategoriesRepositoryBackend.deleteCategory: error", oEx);
		}

		return false;
	}
}
