package wasdi.shared.data.sqlite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import wasdi.shared.business.Comment;
import wasdi.shared.data.interfaces.ICommentRepositoryBackend;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * SQLite backend implementation for comment repository.
 */
public class SqliteCommentRepositoryBackend extends SqliteRepository implements ICommentRepositoryBackend {

	public SqliteCommentRepositoryBackend() {
		m_sThisCollection = "comments";
		ensureTable(m_sThisCollection);
	}

	@Override
	public List<Comment> getComments(String sReviewId) {
		try {
			String sSql = "SELECT data FROM " + m_sThisCollection
					+ " WHERE json_extract(data, '$.reviewId') = ?"
					+ " ORDER BY json_extract(data, '$.date') ASC";
			return queryList(sSql, Arrays.asList(sReviewId), Comment.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteCommentRepositoryBackend.getComments: error", oEx);
		}

		return new ArrayList<>();
	}

	@Override
	public Comment getComment(String sCommentId) {
		try {
			return findOneWhere(m_sThisCollection, "commentId", sCommentId, Comment.class);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteCommentRepositoryBackend.getComment: error", oEx);
		}

		return null;
	}

	@Override
	public String addComment(Comment oComment) {
		if (oComment == null) {
			return "";
		}

		try {
			insert(m_sThisCollection, oComment.getCommentId(), oComment);
			return oComment.getCommentId();
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteCommentRepositoryBackend.addComment: error", oEx);
		}

		return "";
	}

	@Override
	public int deleteComment(String sReviewId, String sCommentId) {
		if (Utils.isNullOrEmpty(sReviewId) || Utils.isNullOrEmpty(sCommentId)) {
			return 0;
		}

		try {
			Map<String, Object> aoFilter = new LinkedHashMap<>();
			aoFilter.put("reviewId", sReviewId);
			aoFilter.put("commentId", sCommentId);
			return deleteWhere(m_sThisCollection, aoFilter);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteCommentRepositoryBackend.deleteComment: error", oEx);
		}

		return 0;
	}

	@Override
	public int deleteComments(String sReviewId) {
		if (Utils.isNullOrEmpty(sReviewId)) {
			return 0;
		}

		try {
			return deleteWhere(m_sThisCollection, "reviewId", sReviewId);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteCommentRepositoryBackend.deleteComments: error", oEx);
		}

		return 0;
	}

	@Override
	public boolean updateComment(Comment oComment) {
		if (oComment == null) {
			return false;
		}

		try {
			Map<String, Object> aoFilter = new LinkedHashMap<>();
			aoFilter.put("commentId", oComment.getCommentId());
			aoFilter.put("reviewId", oComment.getReviewId());
			return updateWhere(m_sThisCollection, aoFilter, oComment);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteCommentRepositoryBackend.updateComment: error", oEx);
		}

		return false;
	}

	@Override
	public boolean isTheOwnerOfTheComment(String sReviewId, String sCommentId, String sUserId) {
		try {
			Map<String, Object> aoFilter = new LinkedHashMap<>();
			aoFilter.put("reviewId", sReviewId);
			aoFilter.put("commentId", sCommentId);
			aoFilter.put("userId", sUserId);
			return countWhere(m_sThisCollection, aoFilter) > 0;
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteCommentRepositoryBackend.isTheOwnerOfTheComment: error", oEx);
		}

		return false;
	}

	@Override
	public int deleteCommentsByUser(String sUserId) {
		if (Utils.isNullOrEmpty(sUserId)) {
			return 0;
		}

		try {
			return deleteWhere(m_sThisCollection, "userId", sUserId);
		} catch (Exception oEx) {
			WasdiLog.errorLog("SqliteCommentRepositoryBackend.deleteCommentsByUser: error", oEx);
		}

		return 0;
	}
}
