package wasdi.shared.data.no2;

import static org.dizitart.no2.filters.FluentFilter.where;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.NitriteCollection;

import wasdi.shared.business.Review;
import wasdi.shared.data.interfaces.IReviewRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * NO2 backend implementation for review repository.
 */
public class No2ReviewRepositoryBackend extends No2Repository implements IReviewRepositoryBackend {

	private static final String s_sCollectionName = "reviews";

	@Override
	public List<Review> getReviews(String sProcessorId) {
		List<Review> aoReturnList = new ArrayList<>();
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection != null) {
				aoReturnList = toList(oCollection.find(where("processorId").eq(sProcessorId)), Review.class);
				aoReturnList.sort(Comparator.comparing(Review::getDate, Comparator.nullsLast(Double::compareTo)).reversed());
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ReviewRepositoryBackend.getReviews", oEx);
		}
		return aoReturnList;
	}

	@Override
	public Review getReview(String sReviewId) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return null;
			}
			for (Document oDoc : oCollection.find(where("id").eq(sReviewId))) {
				return fromDocument(oDoc, Review.class);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ReviewRepositoryBackend.getReview", oEx);
		}
		return null;
	}

	@Override
	public String addReview(Review oReview) {
		if (oReview == null) {
			return "";
		}
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return "";
			}
			Document oDocument = toDocument(oReview);
			oCollection.insert(oDocument);
			Object oId = oDocument.get("_id");
			return oId != null ? oId.toString() : "";
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ReviewRepositoryBackend.addReview", oEx);
		}
		return "";
	}

	@Override
	public int deleteReview(String sProcessorId, String sReviewId) {
		if (Utils.isNullOrEmpty(sProcessorId) || Utils.isNullOrEmpty(sReviewId)) {
			return 0;
		}
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return -1;
			}
			for (Document oDoc : oCollection.find(where("processorId").eq(sProcessorId))) {
				Review oReview = fromDocument(oDoc, Review.class);
				if (oReview != null && equalsSafe(oReview.getId(), sReviewId)) {
					Object oId = oDoc.get("_id");
					if (oId != null) {
						oCollection.remove(where("_id").eq(oId));
					}
					return 1;
				}
			}
			return 0;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ReviewRepositoryBackend.deleteReview", oEx);
			return -1;
		}
	}

	@Override
	public int deleteReviewsByUser(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId)) {
			return 0;
		}
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return -1;
			}
			int iCount = 0;
			for (Document oDoc : oCollection.find(where("userId").eq(sUserId))) {
				if (oDoc != null) {
					iCount++;
				}
			}
			oCollection.remove(where("userId").eq(sUserId));
			return iCount;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ReviewRepositoryBackend.deleteReviewsByUser", oEx);
			return -1;
		}
	}

	@Override
	public boolean updateReview(Review oReview) {
		if (oReview == null) {
			return false;
		}
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return false;
			}
			for (Document oDoc : oCollection.find(where("processorId").eq(oReview.getProcessorId()))) {
				Review oStored = fromDocument(oDoc, Review.class);
				if (oStored != null && equalsSafe(oStored.getId(), oReview.getId())) {
					Object oId = oDoc.get("_id");
					if (oId != null) {
						oCollection.update(where("_id").eq(oId), toDocument(oReview));
						return true;
					}
				}
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2ReviewRepositoryBackend.updateReview", oEx);
		}
		return false;
	}

	@Override
	public boolean isTheOwnerOfTheReview(String sProcessorId, String sReviewId, String sUserId) {
		for (Review oReview : getReviews(sProcessorId)) {
			if (oReview != null && equalsSafe(oReview.getId(), sReviewId) && equalsSafe(oReview.getUserId(), sUserId)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean alreadyVoted(String sProcessorId, String sUserId) {
		for (Review oReview : getReviews(sProcessorId)) {
			if (oReview != null && equalsSafe(oReview.getUserId(), sUserId)) {
				return true;
			}
		}
		return false;
	}

	private boolean equalsSafe(String sA, String sB) {
		return sA == null ? sB == null : sA.equals(sB);
	}
}
