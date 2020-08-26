package wasdi.shared.viewmodels;

import java.util.ArrayList;

public class AppDetailViewModel {
	private String processorId;
	private String processorName;
	private String processorDescription;
	private String imgLink;
	private String publisher;
	private Float score;
	private String friendlyName;
	private String link;
	private String email;
	private Float ondemandPrice;
	private Float subscriptionPrice;
	private Double updateDate;
	private ArrayList<String> categories = new ArrayList<String>();
	private ArrayList<String> images = new ArrayList<String>();
	private Boolean isMine;
	private Boolean buyed;
	private String longDescription;
	private Boolean showInStore = false;
	
	public ArrayList<String> getCategories() {
		return categories;
	}
	public void setCategories(ArrayList<String> categories) {
		this.categories = categories;
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
	public String getLink() {
		return link;
	}
	public void setLink(String link) {
		this.link = link;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public Float getOndemandPrice() {
		return ondemandPrice;
	}
	public void setOndemandPrice(Float ondemandPrice) {
		this.ondemandPrice = ondemandPrice;
	}
	public Float getSubscriptionPrice() {
		return subscriptionPrice;
	}
	public void setSubscriptionPrice(Float subscriptionPrice) {
		this.subscriptionPrice = subscriptionPrice;
	}
	public Double getUpdateDate() {
		return updateDate;
	}
	public void setUpdateDate(Double updateDate) {
		this.updateDate = updateDate;
	}
	public Boolean getIsMine() {
		return isMine;
	}
	public void setIsMine(Boolean isMine) {
		this.isMine = isMine;
	}
	public Boolean getBuyed() {
		return buyed;
	}
	public void setBuyed(Boolean buyed) {
		this.buyed = buyed;
	}
	public ArrayList<String> getImages() {
		return images;
	}
	public void setImages(ArrayList<String> images) {
		this.images = images;
	}
	public String getLongDescription() {
		return longDescription;
	}
	public void setLongDescription(String longDescription) {
		this.longDescription = longDescription;
	}
	public Boolean getShowInStore() {
		return showInStore;
	}
	public void setShowInStore(Boolean showInStore) {
		this.showInStore = showInStore;
	}	
	
}
