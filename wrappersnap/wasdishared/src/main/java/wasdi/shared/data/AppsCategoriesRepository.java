package wasdi.shared.data;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.result.DeleteResult;

import wasdi.shared.business.AppCategory;
import wasdi.shared.utils.log.WasdiLog;

/**
 * AppCategory Repository
 * 
 * @author p.campanella
 *
 */
public class AppsCategoriesRepository extends MongoRepository {
	
	public AppsCategoriesRepository() {
		m_sThisCollection = "appscategories";
	}

    /**
     * Get List of Process Workspaces in a Workspace
     * @param sWorkspaceId
     * @return
     */
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
    
    public AppCategory getCategoryById(String sCategoryId) {

        try {

        	Document oWSDocument = getCollection(m_sThisCollection).find(new Document("id", sCategoryId)).first();
        	String sJSON = oWSDocument.toJson();
        	
        	AppCategory oAppCategory = s_oMapper.readValue(sJSON, AppCategory.class);
        	
        	return oAppCategory;
        } catch (Exception oEx) {
        	WasdiLog.errorLog("AppsCategoriesRepository.getCategoryById: error", oEx);
        }

        return null;
    }
    
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
    
    public boolean deleteCategory(String sCategoryId) {

        try {

            DeleteResult oDeleteResult = getCollection(m_sThisCollection).deleteOne(new Document("id", sCategoryId));

            if (oDeleteResult != null)
            {
                if (oDeleteResult.getDeletedCount() == 1 )
                {
                    return  true;
                }
            }

        } catch (Exception oEx) {
        	WasdiLog.errorLog("AppsCategoriesRepository.deleteCategory: error", oEx);
        }

        return  false;
    }


}
