package wasdi.shared.data;

import static com.mongodb.client.model.Filters.eq;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import wasdi.shared.business.AppCategory;
import wasdi.shared.business.Counter;
import wasdi.shared.business.Processor;
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
    
	
    
	public String addReview(Review oReview) {
		return add(oReview,COLLECTION_NAME,"ReviewRepository.InsertReview");
	}
	
	

    public int deleteReview(String sProcessorId, String sReviewId) {

		BasicDBObject oCriteria = new BasicDBObject();
		oCriteria.append("processorId", sProcessorId);
		oCriteria.append("id", sReviewId);

        return delete(oCriteria,COLLECTION_NAME);
    }
    
    
    
//    //TODO REFACTORING
//    public boolean updateReview(Review oReview) {
//        try {
//            String sJSON = s_oMapper.writeValueAsString(oReview);
//            
//			BasicDBObject oCriteria = new BasicDBObject();
//			oCriteria.append("processorId", oReview.getProcessorId());
//			oCriteria.append("id", oReview.getId());
////            Bson oFilter = new Document("id", oReview.getId());
//            Bson oUpdateOperationDocument = new Document("$set", new Document(Document.parse(sJSON)));
//            
//            UpdateResult oResult = getCollection(COLLECTION_NAME).updateOne(oCriteria, oUpdateOperationDocument);
//
//            
//            if (oResult.getModifiedCount()==1) return true;
//        }
//        catch (Exception oEx) {
//            oEx.printStackTrace();
//        }
//
//        return  update;
//    }
    
    //TODO REFACTORING
    public boolean updateReview(Review oReview) {
       
		BasicDBObject oCriteria = new BasicDBObject();
		oCriteria.append("processorId", oReview.getProcessorId());
		oCriteria.append("id", oReview.getId());

        return  update(oCriteria,oReview,COLLECTION_NAME);
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
