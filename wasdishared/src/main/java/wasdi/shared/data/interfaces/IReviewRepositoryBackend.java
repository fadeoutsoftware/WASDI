package wasdi.shared.data.interfaces;

import java.util.List;

import wasdi.shared.business.Review;

/**
 * Backend contract for review repository.
 */
public interface IReviewRepositoryBackend {

	List<Review> getReviews(String sProcessorId);

	Review getReview(String sReviewId);

	String addReview(Review oReview);

	int deleteReview(String sProcessorId, String sReviewId);

	int deleteReviewsByUser(String sUserId);

	boolean updateReview(Review oReview);

	boolean isTheOwnerOfTheReview(String sProcessorId, String sReviewId, String sUserId);

	boolean alreadyVoted(String sProcessorId, String sUserId);
}
