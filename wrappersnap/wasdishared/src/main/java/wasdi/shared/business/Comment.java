package wasdi.shared.business;

public class Comment {

	private String commentId;
	private String reviewId;
	private String userId;
	private Double date;
	private String text;

	public Comment() {
		super();
	}

	public Comment(String commentId, String reviewId, String userId, Double date,
			String text) {
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

	public Double getDate() {
		return date;
	}

	public void setDate(Double date) {
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
		return "Comment [commentId=" + commentId + ", reviewId=" + reviewId + ", userId=" + userId + ", date=" + date + ", text=" + text + "]";
	}

}
