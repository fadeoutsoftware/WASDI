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
	private ArrayList<String> reviewers = new ArrayList<>();
	private ArrayList<String> reviewNotes = new ArrayList<>();
	private ArrayList<Attribute> attributes = new ArrayList<>();
	
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
	public ArrayList<String> getReviewNotes() {
		return reviewNotes;
	}
	public void setReviewNotes(ArrayList<String> reviewNotes) {
		this.reviewNotes = reviewNotes;
	}
	public ArrayList<Attribute> getAttributes() {
		return attributes;
	}
	public void setAttributes(ArrayList<Attribute> attributes) {
		this.attributes = attributes;
	}
	

}
