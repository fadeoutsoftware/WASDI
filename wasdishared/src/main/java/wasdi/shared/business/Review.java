package wasdi.shared.business;

/**
 * Review of an application made by a user
 * 
 * @author p.campanella
 *
 */
public class Review {
	/**
	 * Processor Id
	 */
	private String processorId;
	/**
	 * Date of the review (timestamp)
	 */
	private Double date;
	/**
	 * Vote (1-5)
	 */
	private Float vote;
	/**
	 * Review Title
	 */
	private String title;
	/**
	 * Review comment
	 */
	private String comment;
	/**
	 * User that made the review
	 */
	private String userId;
	/**
	 * Unique review id (guid)
	 */
	private String id;
	
	public String getProcessorId() {
		return processorId;
	}
	public void setProcessorId(String processorId) {
		this.processorId = processorId;
	}
	public Double getDate() {
		return date;
	}
	public void setDate(Double date) {
		this.date = date;
	}
	public Float getVote() {
		return vote;
	}
	public void setVote(Float vote) {
		this.vote = vote;
	}
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
}
