package wasdi.shared.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.Block;
import com.mongodb.client.FindIterable;

import wasdi.shared.business.AppCategory;
import wasdi.shared.utils.Utils;

public class AppsCategoriesRepository extends MongoRepository {

    /**
     * Get List of Process Workspaces in a Workspace
     * @param sWorkspaceId
     * @return
     */
    public List<AppCategory> getCategories() {

        final ArrayList<AppCategory> aoReturnList = new ArrayList<AppCategory>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("appscategories").find();
            fillList(aoReturnList, oWSDocuments);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
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
}
