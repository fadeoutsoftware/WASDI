package wasdi.shared.viewmodels;

import java.util.Date;

public class ReviewViewModel {
	private String processorId;
	private Date date;
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
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
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
