package wasdi.shared.business;

/**
 * Comment that users can make to an existing review.
 * Reviews and comment are available in the space market
 * 
 * @author p.campanella
 *
 */
public class Comment {
	
	/**
	 * Comment Id: guid
	 */
	private String commentId;
	
	/**
	 * Id of the parent review
	 */
	private String reviewId;
	
	/**
	 * User that made the review
	 */
	private String userId;
	
	/**
	 * Date of the review (timestamp)
	 */
	private Double date;
	
	/**
	 * Text of the comment
	 */
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
