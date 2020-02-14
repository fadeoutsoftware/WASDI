package wasdi.shared.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;

import wasdi.shared.business.AppCategory;
import wasdi.shared.business.PublishedBand;
import wasdi.shared.business.Review;
import wasdi.shared.utils.Utils;

public class ReviewRepository extends MongoRepository {
	
    public List<Review> getReviews(String sProcessorId) {

        final ArrayList<Review> aoReturnList = new ArrayList<Review>();
        try {

            FindIterable<Document> oWSDocuments = getCollection("reviews").find(new Document("processorId", sProcessorId));
            fillList(aoReturnList, oWSDocuments, "ReviewRepository", Review.class);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    private <T> void fillList(final ArrayList<T> aoReturnList, FindIterable<Document> oWSDocuments, String sRepositoryName,Class<T> oClass) {
		oWSDocuments.forEach(new Block<Document>() {
		    public void apply(Document document) {
		        String sJSON = document.toJson();
		        T oAppCategory= null;
		        try {
		        	oAppCategory = s_oMapper.readValue(sJSON, oClass);
		            aoReturnList.add(oAppCategory);
		        } catch (IOException oEx) {
		        	Utils.debugLog(sRepositoryName + ".fillList: " + oEx);
		        }

		    }
		});
	}
    
}
