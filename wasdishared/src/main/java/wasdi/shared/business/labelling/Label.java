package wasdi.shared.business.labelling;

import java.util.ArrayList;

public class Label {
	private String id;
	private String geometry;
	private boolean isPoint;
	private boolean isLine;
	private boolean isPolygon;
	private boolean isMultiPolygon;
	private String annotator;
	private String image;
	private ArrayList<String> reviewers = new ArrayList<>();
	private ArrayList<ReviewNote> reviewNotes = new ArrayList<>();
	private ArrayList<Attribute> attributes = new ArrayList<>();
	private int reviewCount;
	private boolean isValidated;
	private String creatorId;
	private String datasetId;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getGeometry() {
		return geometry;
	}
	public void setGeometry(String geometry) {
		this.geometry = geometry;
	}
	public boolean isPoint() {
		return isPoint;
	}
	public void setPoint(boolean isPoint) {
		this.isPoint = isPoint;
	}
	public boolean isLine() {
		return isLine;
	}
	public void setLine(boolean isLine) {
		this.isLine = isLine;
	}
	public boolean isPolygon() {
		return isPolygon;
	}
	public void setPolygon(boolean isPolygon) {
		this.isPolygon = isPolygon;
	}
	public boolean isMultiPolygon() {
		return isMultiPolygon;
	}
	public void setMultiPolygon(boolean isMultiPolygon) {
		this.isMultiPolygon = isMultiPolygon;
	}
	public String getAnnotator() {
		return annotator;
	}
	public void setAnnotator(String annotator) {
		this.annotator = annotator;
	}
	public ArrayList<String> getReviewers() {
		return reviewers;
	}
	public void setReviewers(ArrayList<String> reviewers) {
		this.reviewers = reviewers;
	}
	public ArrayList<ReviewNote> getReviewNotes() {
		return reviewNotes;
	}
	public void setReviewNotes(ArrayList<ReviewNote> reviewNotes) {
		this.reviewNotes = reviewNotes;
	}
	public ArrayList<Attribute> getAttributes() {
		return attributes;
	}
	public void setAttributes(ArrayList<Attribute> attributes) {
		this.attributes = attributes;
	}
	public String getImage() {
		return image;
	}
	public void setImage(String image) {
		this.image = image;
	}
	public int getReviewCount() {
		return reviewCount;
	}
	public void setReviewCount(int reviewCount) {
		this.reviewCount = reviewCount;
	}
	public boolean isValidated() {
		return isValidated;
	}
	public void setValidated(boolean isValidated) {
		this.isValidated = isValidated;
	}
	public String getCreatorId() {
		return creatorId;
	}
	public void setCreatorId(String creatorId) {
		this.creatorId = creatorId;
	}
	public String getDatasetId() {
		return datasetId;
	}
	public void setDatasetId(String datasetId) {
		this.datasetId = datasetId;
	}
}
