package wasdi.shared.data.no2;

import static org.dizitart.no2.filters.FluentFilter.where;

import java.util.ArrayList;
import java.util.List;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.DocumentCursor;
import org.dizitart.no2.collection.NitriteCollection;

import wasdi.shared.business.AppCategory;
import wasdi.shared.data.interfaces.IAppsCategoriesRepositoryBackend;
import wasdi.shared.utils.log.WasdiLog;

/**
 * NO2 backend implementation for apps categories repository.
 */
public class No2AppsCategoriesRepositoryBackend extends No2Repository implements IAppsCategoriesRepositoryBackend {

	private static final String s_sCollectionName = "appscategories";

	@Override
	public List<AppCategory> getCategories() {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			DocumentCursor oCursor = oCollection != null ? oCollection.find() : null;
			return toList(oCursor, AppCategory.class);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2AppsCategoriesRepositoryBackend.getCategories: error", oEx);
		}

		return new ArrayList<>();
	}

	@Override
	public AppCategory getCategoryById(String sCategoryId) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return null;
			}

			for (Document oDocument : oCollection.find(where("id").eq(sCategoryId))) {
				return fromDocument(oDocument, AppCategory.class);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2AppsCategoriesRepositoryBackend.getCategoryById: error", oEx);
		}

		return null;
	}

	@Override
	public boolean insertCategory(AppCategory oCategory) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null || oCategory == null) {
				return false;
			}

			oCollection.insert(toDocument(oCategory));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2AppsCategoriesRepositoryBackend.insertCategory: error", oEx);
		}

		return false;
	}

	@Override
	public boolean deleteCategory(String sCategoryId) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return false;
			}

			oCollection.remove(where("id").eq(sCategoryId));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2AppsCategoriesRepositoryBackend.deleteCategory: error", oEx);
		}

		return false;
	}
}
