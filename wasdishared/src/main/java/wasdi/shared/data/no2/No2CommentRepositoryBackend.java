package wasdi.shared.data.no2;

import static org.dizitart.no2.filters.Filter.and;
import static org.dizitart.no2.filters.FluentFilter.where;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.DocumentCursor;
import org.dizitart.no2.collection.NitriteCollection;

import wasdi.shared.business.Comment;
import wasdi.shared.data.interfaces.ICommentRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * NO2 backend implementation for comment repository.
 */
public class No2CommentRepositoryBackend extends No2Repository implements ICommentRepositoryBackend {

	private static final String s_sCollectionName = "comments";

	@Override
	public List<Comment> getComments(String sReviewId) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return new ArrayList<>();
			}

			DocumentCursor oCursor = oCollection.find(where("reviewId").eq(sReviewId));
			List<Comment> aoComments = toList(oCursor, Comment.class);
			aoComments.sort(Comparator.comparing(Comment::getDate, Comparator.nullsLast(Double::compareTo)));
			return aoComments;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2CommentRepositoryBackend.getComments: error", oEx);
		}

		return new ArrayList<>();
	}

	@Override
	public Comment getComment(String sCommentId) {
		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return null;
			}

			for (Document oDocument : oCollection.find(where("commentId").eq(sCommentId))) {
				return fromDocument(oDocument, Comment.class);
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2CommentRepositoryBackend.getComment: error", oEx);
		}

		return null;
	}

	@Override
	public String addComment(Comment oComment) {
		try {
			if (oComment == null) {
				return "";
			}

			if (Utils.isNullOrEmpty(oComment.getCommentId())) {
				oComment.setCommentId(Utils.getRandomName());
			}

			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return "";
			}

			oCollection.insert(toDocument(oComment));
			return oComment.getCommentId();
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2CommentRepositoryBackend.addComment: error", oEx);
		}

		return "";
	}

	@Override
	public int deleteComment(String sReviewId, String sCommentId) {
		if (Utils.isNullOrEmpty(sReviewId) || Utils.isNullOrEmpty(sCommentId)) {
			return 0;
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return 0;
			}

			int iBefore = getComments(sReviewId).size();
			oCollection.remove(and(where("reviewId").eq(sReviewId), where("commentId").eq(sCommentId)));
			int iAfter = getComments(sReviewId).size();
			return iBefore > iAfter ? 1 : 0;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2CommentRepositoryBackend.deleteComment: error", oEx);
		}

		return 0;
	}

	@Override
	public int deleteComments(String sReviewId) {
		if (Utils.isNullOrEmpty(sReviewId)) {
			return 0;
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return 0;
			}

			int iCount = getComments(sReviewId).size();
			oCollection.remove(where("reviewId").eq(sReviewId));
			return iCount;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2CommentRepositoryBackend.deleteComments: error", oEx);
		}

		return 0;
	}

	@Override
	public boolean updateComment(Comment oComment) {
		try {
			if (oComment == null) {
				return false;
			}

			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return false;
			}

			oCollection.update(
					and(where("commentId").eq(oComment.getCommentId()), where("reviewId").eq(oComment.getReviewId())),
					toDocument(oComment));
			return true;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2CommentRepositoryBackend.updateComment: error", oEx);
		}

		return false;
	}

	@Override
	public boolean isTheOwnerOfTheComment(String sReviewId, String sCommentId, String sUserId) {
		if (Utils.isNullOrEmpty(sReviewId) || Utils.isNullOrEmpty(sCommentId) || Utils.isNullOrEmpty(sUserId)) {
			return false;
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return false;
			}

			for (Document oDocument : oCollection.find(
					and(where("reviewId").eq(sReviewId), where("commentId").eq(sCommentId), where("userId").eq(sUserId)))) {
				return true;
			}
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2CommentRepositoryBackend.isTheOwnerOfTheComment: error", oEx);
		}

		return false;
	}

	@Override
	public int deleteCommentsByUser(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId)) {
			return 0;
		}

		try {
			NitriteCollection oCollection = getCollection(s_sCollectionName);
			if (oCollection == null) {
				return 0;
			}

			DocumentCursor oCursor = oCollection.find(where("userId").eq(sUserId));
			int iCount = toList(oCursor, Comment.class).size();
			oCollection.remove(where("userId").eq(sUserId));
			return iCount;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("No2CommentRepositoryBackend.deleteCommentsByUser: error", oEx);
		}

		return 0;
	}
}
