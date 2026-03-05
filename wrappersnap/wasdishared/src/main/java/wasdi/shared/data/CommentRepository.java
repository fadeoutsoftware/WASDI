package wasdi.shared.data;

import java.util.List;

import wasdi.shared.business.Comment;
import wasdi.shared.data.factories.DataRepositoryFactoryProvider;
import wasdi.shared.data.interfaces.ICommentRepositoryBackend;

public class CommentRepository {

    private final ICommentRepositoryBackend m_oBackend;

	public CommentRepository() {
        m_oBackend = createBackend();
	}

    private ICommentRepositoryBackend createBackend() {
        // For now keep Mongo backend only. Next step will select by config.
        return DataRepositoryFactoryProvider.getFactory().createCommentRepository();
    }

    public List<Comment> getComments(String sReviewId) {
        return m_oBackend.getComments(sReviewId);
    }

    /**
     * Get a comment by Id
     * @param sCommentId id of the review
     * @return Entity
     */
    public Comment getComment(String sCommentId) {
        return m_oBackend.getComment(sCommentId);
    }

	public String addComment(Comment oComment) {
        return m_oBackend.addComment(oComment);
	}

    public int deleteComment(String sReviewId, String sCommentId) {
        return m_oBackend.deleteComment(sReviewId, sCommentId);
    }

    public int deleteComments(String sReviewId) {
        return m_oBackend.deleteComments(sReviewId);
    }

    public boolean updateComment(Comment oComment) {
        return m_oBackend.updateComment(oComment);
    }
    
	public boolean isTheOwnerOfTheComment(String sReviewId, String sCommentId, String sUserId) {
        return m_oBackend.isTheOwnerOfTheComment(sReviewId, sCommentId, sUserId);
	}

	/**
	 * Delete all the comments of a specific user
	 * @param sUserId
	 * @return
	 */
    public int deleteCommentsByUser(String sUserId) {
        return m_oBackend.deleteCommentsByUser(sUserId);
    }
}

