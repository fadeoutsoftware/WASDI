package wasdi.shared.data.interfaces;

import java.util.List;

import wasdi.shared.business.Comment;

/**
 * Backend contract for comment repository.
 */
public interface ICommentRepositoryBackend {

	List<Comment> getComments(String sReviewId);

	Comment getComment(String sCommentId);

	String addComment(Comment oComment);

	int deleteComment(String sReviewId, String sCommentId);

	int deleteComments(String sReviewId);

	boolean updateComment(Comment oComment);

	boolean isTheOwnerOfTheComment(String sReviewId, String sCommentId, String sUserId);

	int deleteCommentsByUser(String sUserId);
}
