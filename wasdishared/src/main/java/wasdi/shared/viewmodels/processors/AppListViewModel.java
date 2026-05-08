package wasdi.shared.viewmodels.processors;

/**
 * App List View Model
 * 
 * Ligth view of an application, used for the marketplace landing page
 * 
 * @author p.campanella
 *
 */
public class AppListViewModel {
	private String processorId;
	private String processorName;
	private String processorDescription;
	private String imgLink;
	private String publisher;
	private String publisherNickName;
	private Float score;
	private Integer votes;
	private String friendlyName;
	private Float price;
	private Float squareKilometerPrice;
	private boolean isMine;
	private boolean buyed;
	private String logo;
	private boolean readOnly;
	
	public Float getPrice() {
		return price;
	}
	
	public void setPrice(Float price) {
		this.price = price;
	}
	
	public Float getSquareKilometerPrice() {
		return squareKilometerPrice;
	}
	
	public void setSquareKilometerPrice(Float squareKilometerPrice) {
		this.squareKilometerPrice = squareKilometerPrice;
	}
	
	public boolean getIsMine() {
		return isMine;
	}
	
	public void setIsMine(boolean isMine) {
		this.isMine = isMine;
	}
	
	public boolean isBuyed() {
		return buyed;
	}
	
	public void setBuyed(boolean buyed) {
		this.buyed = buyed;
	}
	
	public String getProcessorId() {
		return processorId;
	}
	
	public void setProcessorId(String processorId) {
		this.processorId = processorId;
	}
	
	public String getProcessorName() {
		return processorName;
	}
	
	public void setProcessorName(String processorName) {
		this.processorName = processorName;
	}
	
	public String getProcessorDescription() {
		return processorDescription;
	}
	
	public void setProcessorDescription(String processorDescription) {
		this.processorDescription = processorDescription;
	}
	
	public String getImgLink() {
		return imgLink;
	}
	
	public void setImgLink(String imgLink) {
		this.imgLink = imgLink;
	}
	
	public String getPublisher() {
		return publisher;
	}
	
	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}
	
	public Float getScore() {
		return score;
	}
	
	public void setScore(Float score) {
		this.score = score;
	}
	
	public String getFriendlyName() {
		return friendlyName;
	}
	
	public void setFriendlyName(String friendlyName) {
		this.friendlyName = friendlyName;
	}
	
	public Integer getVotes() {
		return votes;
	}
	
	public void setVotes(Integer votes) {
		this.votes = votes;
	}
	
	public String getLogo() {
		return logo;
	}
	
	public void setLogo(String logo) {
		this.logo = logo;
	}
	
	public boolean isReadOnly() {
		return readOnly;
	}
	
	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public String getPublisherNickName() {
		return publisherNickName;
	}

	public void setPublisherNickName(String publisherNickName) {
		this.publisherNickName = publisherNickName;
	}

}
