package wasdi.shared.viewmodels.processors;

import java.util.Date;

public class CommentViewModel {

	private String commentId;
	private String reviewId;
	private String userId;
	private Date date;
	private String text;

	public CommentViewModel() {
		super();
	}

	public CommentViewModel(String commentId, String reviewId, String userId, Date date, String text) {
		super();
		this.commentId = commentId;
		this.reviewId = reviewId;
		this.userId = userId;
		this.date = date;
		this.text = text;
	}

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

	@Override
	public String toString() {
		return "CommentViewModel [commentId=" + commentId + ", reviewId=" + reviewId + ", userId=" + userId + ", date=" + date + ", text=" + text + "]";
	}

}
