package wasdi.shared.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;

import wasdi.shared.business.AppCategory;
import wasdi.shared.business.Counter;
import wasdi.shared.business.PublishedBand;
import wasdi.shared.business.Review;
import wasdi.shared.utils.Utils;

public class ReviewRepository extends MongoRepository {
	
	final String COLLECTION_NAME = "reviews";
	
    public List<Review> getReviews(String sProcessorId) {

        final ArrayList<Review> aoReturnList = new ArrayList<Review>();
        try {

            FindIterable<Document> oWSDocuments = getCollection(COLLECTION_NAME).find(new Document("processorId", sProcessorId));
            fillList(aoReturnList, oWSDocuments, "ReviewRepository", Review.class);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
    //TODO REFACTOR IT 
//	public String addReview(Review oReview) {
//		String sResult = "";
//		if(oReview != null) {
//			try {
//				String sJSON = s_oMapper.writeValueAsString(oReview);
//				Document oDocument = Document.parse(sJSON);
//				getCollection(COLLECTION_NAME).insertOne(oDocument);
//				sResult = oDocument.getObjectId("_id").toHexString();
//	
//			} catch (Exception oEx) {
//				Utils.debugLog("ReviewRepository.InsertReview: " + oEx);
//			}
//		}
//		return sResult;
//	}
	
	public String addReview(Review oReview) {
		return add(oReview,COLLECTION_NAME,"ReviewRepository.InsertReview");
	}
	
	public boolean alreadyVoted(Review oReview) {
		boolean bAlreadyVoted = false;
		if(oReview != null) {
			String sProcessorId = oReview.getProcessorId();
			String sUserId = oReview.getUserId();
			try {
				
				BasicDBObject oCriteria = new BasicDBObject();
				oCriteria.append("processorId", sProcessorId);
				oCriteria.append("userId", sUserId);
				Document oWSDocument = getCollection(COLLECTION_NAME).find(oCriteria).first();
				if(oWSDocument != null ){
					bAlreadyVoted = true;
				}
				
			} catch (Exception oEx) {
				Utils.debugLog("ReviewRepository.InsertReview: " + oEx);
			}
		}
		return bAlreadyVoted;
	}
    
}
