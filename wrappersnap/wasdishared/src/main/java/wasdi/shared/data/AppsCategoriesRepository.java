package wasdi.shared.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.result.DeleteResult;

import wasdi.shared.business.AppCategory;
import wasdi.shared.utils.Utils;

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
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
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
            oEx.printStackTrace();
        }

        return null;
    }
    
    
	private void fillList(final ArrayList<AppCategory> aoReturnList, FindIterable<Document> oWSDocuments) {
		oWSDocuments.forEach(new Block<Document>() {
		    public void apply(Document document) {
		        String sJSON = document.toJson();
		        AppCategory oAppCategory= null;
		        try {
		        	oAppCategory = s_oMapper.readValue(sJSON,AppCategory.class);
		            aoReturnList.add(oAppCategory);
		        } catch (IOException oEx) {
		        	Utils.debugLog("AppCategoriesLogRepository.fillList: "+oEx);
		        }

		    }
		});
	}
	
    public boolean insertCategory(AppCategory oCategory) {

        try {
            String sJSON = s_oMapper.writeValueAsString(oCategory);
            getCollection(m_sThisCollection).insertOne(Document.parse(sJSON));

            return true;

        } catch (Exception oEx) {
            oEx.printStackTrace();
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
            oEx.printStackTrace();
        }

        return  false;
    }


}
