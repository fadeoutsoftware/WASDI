package wasdi.shared.data;

import java.util.List;

import wasdi.shared.business.Review;
import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.IReviewRepositoryBackend;

public class ReviewRepository {

	private final IReviewRepositoryBackend m_oBackend;
	
	public ReviewRepository() {
		m_oBackend = createBackend();
	}

	private IReviewRepositoryBackend createBackend() {
		// For now keep Mongo backend only. Next step will select by config.
		return DataRepositoryFactoryProvider.getFactory().createReviewRepository();
	}
	
	/**
	 * Get all the reviews of a processor
	 * @param sProcessorId
	 * @return
	 */
    public List<Review> getReviews(String sProcessorId) {
		return m_oBackend.getReviews(sProcessorId);
    }
    
    /**
     * Get a review by Id
     * @param sReviewId id of the review
     * @return Entity
     */
    public Review getReview(String sReviewId) {
		return m_oBackend.getReview(sReviewId);
    }
    
    /**
     * Create a new review
     * @param oReview
     * @return
     */
	public String addReview(Review oReview) {
		return m_oBackend.addReview(oReview);
	}
	
	/**
	 * Delete a specific Review
	 * @param sProcessorId
	 * @param sReviewId
	 * @return
	 */
    public int deleteReview(String sProcessorId, String sReviewId) {
		return m_oBackend.deleteReview(sProcessorId, sReviewId);
    }
    
	/**
	 * Delete a specific Review
	 * @param sProcessorId
	 * @param sReviewId
	 * @return
	 */
    public int deleteReviewsByUser(String sUserId) {
		return m_oBackend.deleteReviewsByUser(sUserId);
    }
    /**
     * Update a review
     * @param oReview
     * @return
     */
    public boolean updateReview(Review oReview) {
		return m_oBackend.updateReview(oReview);
    }
    
    /**
     * Check if user id is the owner of a specific review
     * @param sProcessorId
     * @param sReviewId
     * @param sUserId
     * @return
     */
	public boolean isTheOwnerOfTheReview(String sProcessorId, String sReviewId ,String sUserId) {
		return m_oBackend.isTheOwnerOfTheReview(sProcessorId, sReviewId, sUserId);
	}
    
	/**
	 * Check if the user already left a review for this specific processor
	 * @param sProcessorId
	 * @param sUserId
	 * @return
	 */
	public boolean alreadyVoted(String sProcessorId, String sUserId) {
		return m_oBackend.alreadyVoted(sProcessorId, sUserId);
	}
    
}

