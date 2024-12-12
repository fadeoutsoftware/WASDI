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
	
	/**
	 * Get all the reviews of a processor
	 * @param sProcessorId
	 * @return
	 */
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
    
    /**
     * Create a new review
     * @param oReview
     * @return
     */
	public String addReview(Review oReview) {
		return add(oReview,m_sThisCollection,"ReviewRepository.InsertReview");
	}
	
	/**
	 * Delete a specific Review
	 * @param sProcessorId
	 * @param sReviewId
	 * @return
	 */
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
    
	/**
	 * Delete a specific Review
	 * @param sProcessorId
	 * @param sReviewId
	 * @return
	 */
    public int deleteReviewsByUser(String sUserId) {
    	
    	if (Utils.isNullOrEmpty(sUserId)) return 0;

    	try {
    		BasicDBObject oCriteria = new BasicDBObject();
    		oCriteria.append("userId", sUserId);

            return deleteMany(oCriteria,m_sThisCollection);
    	}
		catch (Exception oEx) {
			WasdiLog.errorLog("ReviewRepository.deleteReviewsByUser: ", oEx);
			return -1;
		}
    }
    /**
     * Update a review
     * @param oReview
     * @return
     */
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
    
    /**
     * Check if user id is the owner of a specific review
     * @param sProcessorId
     * @param sReviewId
     * @param sUserId
     * @return
     */
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
    
	/**
	 * Check if the user already left a review for this specific processor
	 * @param sProcessorId
	 * @param sUserId
	 * @return
	 */
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
