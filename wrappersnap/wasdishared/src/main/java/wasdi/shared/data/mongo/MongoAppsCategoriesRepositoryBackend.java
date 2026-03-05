package wasdi.shared.data.mongo;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.result.DeleteResult;

import wasdi.shared.business.AppCategory;
import wasdi.shared.data.interfaces.IAppsCategoriesRepositoryBackend;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Mongo backend implementation for apps categories repository.
 */
public class MongoAppsCategoriesRepositoryBackend extends MongoRepository implements IAppsCategoriesRepositoryBackend {

	public MongoAppsCategoriesRepositoryBackend() {
		m_sThisCollection = "appscategories";
	}

	@Override
	public List<AppCategory> getCategories() {
		final ArrayList<AppCategory> aoReturnList = new ArrayList<AppCategory>();
		try {

			FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find();
			fillList(aoReturnList, oWSDocuments, AppCategory.class);

		} catch (Exception oEx) {
			WasdiLog.errorLog("AppsCategoriesRepository.getCategories: error", oEx);
		}

		return aoReturnList;
	}

	@Override
	public AppCategory getCategoryById(String sCategoryId) {

		try {

			Document oWSDocument = getCollection(m_sThisCollection).find(new Document("id", sCategoryId)).first();
			if (oWSDocument == null) {
				return null;
			}

			String sJSON = oWSDocument.toJson();
			return s_oMapper.readValue(sJSON, AppCategory.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("AppsCategoriesRepository.getCategoryById: error", oEx);
		}

		return null;
	}

	@Override
	public boolean insertCategory(AppCategory oCategory) {

		try {
			String sJSON = s_oMapper.writeValueAsString(oCategory);
			getCollection(m_sThisCollection).insertOne(Document.parse(sJSON));

			return true;

		} catch (Exception oEx) {
			WasdiLog.errorLog("AppsCategoriesRepository.insertCategory: error", oEx);
		}

		return false;
	}

	@Override
	public boolean deleteCategory(String sCategoryId) {

		try {

			DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteOne(new Document("id", sCategoryId));

			if (oDeleteResult != null) {
				if (oDeleteResult.getDeletedCount() == 1) {
					return true;
				}
			}

		} catch (Exception oEx) {
			WasdiLog.errorLog("AppsCategoriesRepository.deleteCategory: error", oEx);
		}

		return false;
	}
}
