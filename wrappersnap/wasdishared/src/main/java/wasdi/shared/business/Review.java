package wasdi.shared.business;

public class Review {
	private String processorId;
	private Double date;
	private Float vote;
	private String comment;
	private String userId;
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
}
