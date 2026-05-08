package wasdi.shared.data.sqlite;

import java.util.List;

import wasdi.shared.business.Review;
import wasdi.shared.data.interfaces.IReviewRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * SQLite backend implementation for review repository.
 */
public class SqliteReviewRepositoryBackend extends SqliteRepository implements IReviewRepositoryBackend {

	public SqliteReviewRepositoryBackend() {
		m_sThisCollection = "reviews";
		this.ensureTable(m_sThisCollection);
	}

	@Override
	public List<Review> getReviews(String sProcessorId) {
		try {
			return queryList(
					"SELECT data FROM " + m_sThisCollection +
					" WHERE json_extract(data,'$.processorId') = ?" +
					" ORDER BY json_extract(data,'$.date') DESC",
					new Object[]{sProcessorId}, Review.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ReviewRepository.getReviews: error ", oEx);
		}
		return new java.util.ArrayList<>();
	}

	@Override
	public Review getReview(String sReviewId) {
		try {
			return findOneWhere("id", sReviewId, Review.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ReviewRepository.getReview: error ", oEx);
		}
		return null;
	}

	@Override
	public String addReview(Review oReview) {
		try {
			insert(oReview.getId(), oReview);
			return oReview.getId();
		} catch (Exception oEx) {
			WasdiLog.errorLog("ReviewRepository.addReview: error ", oEx);
		}
		return null;
	}

	@Override
	public int deleteReview(String sProcessorId, String sReviewId) {
		if (Utils.isNullOrEmpty(sProcessorId)) return 0;
		if (Utils.isNullOrEmpty(sReviewId)) return 0;
		try {
			return execute(
					"DELETE FROM " + m_sThisCollection +
					" WHERE json_extract(data,'$.processorId') = ?" +
					" AND json_extract(data,'$.id') = ?",
					new Object[]{sProcessorId, sReviewId});
		} catch (Exception oEx) {
			WasdiLog.errorLog("ReviewRepository.deleteReview: ", oEx);
			return -1;
		}
	}

	@Override
	public int deleteReviewsByUser(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId)) return 0;
		try {
			return deleteWhere("userId", sUserId);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ReviewRepository.deleteReviewsByUser: ", oEx);
			return -1;
		}
	}

	@Override
	public boolean updateReview(Review oReview) {
		try {
			return updateById(oReview.getId(), oReview);
		} catch (Exception oEx) {
			WasdiLog.errorLog("ReviewRepository.updateReview: ", oEx);
			return false;
		}
	}

	@Override
	public boolean isTheOwnerOfTheReview(String sProcessorId, String sReviewId, String sUserId) {
		try {
			java.util.Map<String, Object> aoConditions = new java.util.LinkedHashMap<>();
			aoConditions.put("processorId", sProcessorId);
			aoConditions.put("id", sReviewId);
			aoConditions.put("userId", sUserId);
			return countWhere(aoConditions) > 0;
		} catch (Exception oEx) {
			WasdiLog.errorLog("ReviewRepository.isTheOwnerOfTheReview: error ", oEx);
		}
		return false;
	}

	@Override
	public boolean alreadyVoted(String sProcessorId, String sUserId) {
		try {
			Review oReview = queryOne(
					"SELECT data FROM " + m_sThisCollection +
					" WHERE json_extract(data,'$.processorId') = ?" +
					" AND json_extract(data,'$.userId') = ? LIMIT 1",
					new Object[]{sProcessorId, sUserId}, Review.class);
			return oReview != null;
		} catch (Exception oEx) {
			WasdiLog.errorLog("ReviewRepository.alreadyVoted: ", oEx);
		}
		return false;
	}
}
