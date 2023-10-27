package wasdi.shared.data;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;

import wasdi.shared.business.Review;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class ReviewRepository extends MongoRepository {
	
	public ReviewRepository() {
		m_sThisCollection = "reviews";
	}
	
    public List<Review> getReviews(String sProcessorId) {

        final ArrayList<Review> aoReturnList = new ArrayList<Review>();
        try {

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(new Document("processorId", sProcessorId)).sort(new Document("date", -1));
            fillList(aoReturnList, oWSDocuments, Review.class);

        } catch (Exception oEx) {
        	WasdiLog.errorLog("ReviewRepository.getReviews :error ", oEx);
        }

        return aoReturnList;
    }
    
    /**
     * Get a review by Id
     * @param sReviewId id of the review
     * @return Entity
     */
    public Review getReview(String sReviewId) {

        try {
            Document oWSDocument = getCollection(m_sThisCollection).find(new Document("id", sReviewId)).first();

            if (null != oWSDocument) {
            	String sJSON = oWSDocument.toJson();
            	return s_oMapper.readValue(sJSON, Review.class);
            }
        } catch (Exception oEx) {
        	WasdiLog.errorLog("ReviewRepository.getReview :error ", oEx);
        }

        return  null;
    }
    
	public String addReview(Review oReview) {
		return add(oReview,m_sThisCollection,"ReviewRepository.InsertReview");
	}
	
	

    public int deleteReview(String sProcessorId, String sReviewId) {
    	
    	if (Utils.isNullOrEmpty(sProcessorId)) return 0;
    	if (Utils.isNullOrEmpty(sReviewId)) return 0;

    	try {
    		BasicDBObject oCriteria = new BasicDBObject();
    		oCriteria.append("processorId", sProcessorId);
    		oCriteria.append("id", sReviewId);

            return delete(oCriteria,m_sThisCollection);
    	}
		catch (Exception oEx) {
			WasdiLog.errorLog("ReviewRepository.deleteReview: ", oEx);
			return -1;
		}
    	
    }
    
    public boolean updateReview(Review oReview) {
    	
    	try {
    		BasicDBObject oCriteria = new BasicDBObject();
    		oCriteria.append("processorId", oReview.getProcessorId());
    		oCriteria.append("id", oReview.getId());

            return  update(oCriteria,oReview,m_sThisCollection);    		
    	}
		catch (Exception oEx) {
			WasdiLog.errorLog("ReviewRepository.updateReview: ", oEx);
			return false;
		}

       
    }
    
	public boolean isTheOwnerOfTheReview(String sProcessorId, String sReviewId ,String sUserId) {
		
		boolean bIsTheOwner = false;

        final ArrayList<Review> aoReturnList = new ArrayList<Review>();
        try {
    		BasicDBObject oCriteria = new BasicDBObject();
    		oCriteria.append("processorId", sProcessorId);
    		oCriteria.append("id", sReviewId);
    		oCriteria.append("userId", sUserId);

            FindIterable<Document> oWSDocuments = getCollection(m_sThisCollection).find(oCriteria);
            fillList(aoReturnList, oWSDocuments, Review.class);

        } catch (Exception oEx) {
        	WasdiLog.errorLog("ReviewRepository.isTheOwnerOfTheReview :error ", oEx);
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
			Document oWSDocument = getCollection(m_sThisCollection).find(oCriteria).first();
			if(oWSDocument != null ){
				bAlreadyVoted = true;
			}
			
		} 
		catch (Exception oEx) {
			WasdiLog.errorLog("ReviewRepository.alreadyVoted: ", oEx);
		}
		return bAlreadyVoted;
	}
    
}
