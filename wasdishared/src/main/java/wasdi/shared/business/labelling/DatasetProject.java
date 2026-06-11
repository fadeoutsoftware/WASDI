package wasdi.shared.business.labelling;

import java.util.ArrayList;

public class DatasetProject {
	private String id;
	private String name;
	private String description;
	private boolean isGlobal;
	private String bbox;
	private boolean isPublic;
	private long creationDate;
	private String link;
	private long startDate;
	private long endDate;
	private boolean annotatorSeeAllLabels;
	private boolean reviewRequired;
	private int minReviewCount;
	private String missions;
	private ArrayList<String> tasks = new ArrayList<>();
	private ArrayList<String> imagesIds = new ArrayList<>();
	private String templateId;
	private String owner;
	private ArrayList<String> annotators = new ArrayList<>();
	private ArrayList<String> reviewers = new ArrayList<>();
	private String imageStyleId;
	private String workspaceId;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public boolean isGlobal() {
		return isGlobal;
	}
	public void setGlobal(boolean isGlobal) {
		this.isGlobal = isGlobal;
	}
	public String getBbox() {
		return bbox;
	}
	public void setBbox(String bbox) {
		this.bbox = bbox;
	}
	public boolean isPublic() {
		return isPublic;
	}
	public void setPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}
	public long getCreationDate() {
		return creationDate;
	}
	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}
	public String getLink() {
		return link;
	}
	public void setLink(String link) {
		this.link = link;
	}
	public long getStartDate() {
		return startDate;
	}
	public void setStartDate(long startDate) {
		this.startDate = startDate;
	}
	public long getEndDate() {
		return endDate;
	}
	public void setEndDate(long endDate) {
		this.endDate = endDate;
	}
	public boolean isAnnotatorSeeAllLabels() {
		return annotatorSeeAllLabels;
	}
	public void setAnnotatorSeeAllLabels(boolean annotatorSeeAllLabels) {
		this.annotatorSeeAllLabels = annotatorSeeAllLabels;
	}
	public boolean isReviewRequired() {
		return reviewRequired;
	}
	public void setReviewRequired(boolean reviewRequired) {
		this.reviewRequired = reviewRequired;
	}
	public int getMinReviewCount() {
		return minReviewCount;
	}
	public void setMinReviewCount(int minReviewCount) {
		this.minReviewCount = minReviewCount;
	}
	public String getMissions() {
		return missions;
	}
	public void setMissions(String missions) {
		this.missions = missions;
	}
	public ArrayList<String> getTasks() {
		return tasks;
	}
	public void setTasks(ArrayList<String> tasks) {
		this.tasks = tasks;
	}
	public String getTemplateId() {
		return templateId;
	}
	public void setTemplateId(String templateId) {
		this.templateId = templateId;
	}
	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}
	public String getImageStyleId() {
		return imageStyleId;
	}
	public void setImageStyleId(String imageStyleId) {
		this.imageStyleId = imageStyleId;
	}
	public ArrayList<String> getImagesIds() {
		return imagesIds;
	}
	public void setImagesIds(ArrayList<String> imagesIds) {
		this.imagesIds = imagesIds;
	}
	public ArrayList<String> getAnnotators() {
		return annotators;
	}
	public void setAnnotators(ArrayList<String> annotators) {
		this.annotators = annotators;
	}
	public ArrayList<String> getReviewers() {
		return reviewers;
	}
	public void setReviewers(ArrayList<String> reviewers) {
		this.reviewers = reviewers;
	}
	public String getWorkspaceId() {
		return workspaceId;
	}
	public void setWorkspaceId(String workspaceId) {
		this.workspaceId = workspaceId;
	}

}
