package wasdi.shared.viewmodels.processors;

import java.util.Date;

/**
 * Comment ListViewModel
 * 
 * Represents a comment to a review
 * 
 * @author PetruPetrescu
 *
 */
public class CommentListViewModel {

	private String commentId;
	private String reviewId;
	private String userId;
	private Date date;
	private String text;
	public String getCommentId() {
		return commentId;
	}
	public void setCommentId(String commentId) {
		this.commentId = commentId;
	}
	public String getReviewId() {
		return reviewId;
	}
	public void setReviewId(String reviewId) {
		this.reviewId = reviewId;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}

}
