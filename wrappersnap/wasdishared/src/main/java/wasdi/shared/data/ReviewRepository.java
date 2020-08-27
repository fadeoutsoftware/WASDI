package wasdi.shared.data;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;

import wasdi.shared.business.Review;
import wasdi.shared.utils.Utils;

public class ReviewRepository extends MongoRepository {
	
	final String COLLECTION_NAME = "reviews";
	
    public List<Review> getReviews(String sProcessorId) {

        final ArrayList<Review> aoReturnList = new ArrayList<Review>();
        try {

            FindIterable<Document> oWSDocuments = getCollection(COLLECTION_NAME).find(new Document("processorId", sProcessorId)).sort(new Document("date", -1));
            fillList(aoReturnList, oWSDocuments, "ReviewRepository", Review.class);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        return aoReturnList;
    }
    
	
    
	public String addReview(Review oReview) {
		return add(oReview,COLLECTION_NAME,"ReviewRepository.InsertReview");
	}
	
	

    public int deleteReview(String sProcessorId, String sReviewId) {

		BasicDBObject oCriteria = new BasicDBObject();
		oCriteria.append("processorId", sProcessorId);
		oCriteria.append("id", sReviewId);

        return delete(oCriteria,COLLECTION_NAME);
    }
    
    
    
    
    //TODO REFACTORING
    public boolean updateReview(Review oReview) {
       
		BasicDBObject oCriteria = new BasicDBObject();
		oCriteria.append("processorId", oReview.getProcessorId());
		oCriteria.append("id", oReview.getId());

        return  update(oCriteria,oReview,COLLECTION_NAME);
    }
    
    //TODO FIX THIS FUNCTION 
	public boolean isTheOwnerOfTheReview(String sProcessorId, String sReviewId ,String sUserId) {
		boolean bIsTheOwner = false;

        final ArrayList<Review> aoReturnList = new ArrayList<Review>();
        try {
    		BasicDBObject oCriteria = new BasicDBObject();
    		oCriteria.append("processorId", sProcessorId);
    		oCriteria.append("id", sReviewId);
    		oCriteria.append("userId", sUserId);

            FindIterable<Document> oWSDocuments = getCollection(COLLECTION_NAME).find(oCriteria);
            fillList(aoReturnList, oWSDocuments, "ReviewRepository", Review.class);

        } catch (Exception oEx) {
            oEx.printStackTrace();
        }

        if(aoReturnList.size() > 0){
        	bIsTheOwner = true;
        }
		
		return bIsTheOwner;
	}
    
	public boolean alreadyVoted(String sProcessorId, String sUserId) {
		boolean bAlreadyVoted = false;
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
		return bAlreadyVoted;
	}
    
}
