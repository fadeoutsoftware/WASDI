package wasdi.shared.business.labelling;

import java.util.ArrayList;

public class Template {
	private String id;
	private String name;
	private String description;
	private boolean hasPolygons;
	private boolean hasLines;
	private boolean hasPoints;
	private ArrayList<Attribute> attributes = new ArrayList<>();
	private boolean isFixedColour;
	private Integer fixedColour;
	private String colourAttributeName;
	private String creator;
	private long creationDate;
	
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
	public boolean isHasPolygons() {
		return hasPolygons;
	}
	public void setHasPolygons(boolean hasPolygons) {
		this.hasPolygons = hasPolygons;
	}
	public boolean isHasLines() {
		return hasLines;
	}
	public void setHasLines(boolean hasLines) {
		this.hasLines = hasLines;
	}
	public boolean isHasPoints() {
		return hasPoints;
	}
	public void setHasPoints(boolean hasPoints) {
		this.hasPoints = hasPoints;
	}
	public ArrayList<Attribute> getAttributes() {
		return attributes;
	}
	public void setAttributes(ArrayList<Attribute> attributes) {
		this.attributes = attributes;
	}
	public boolean isFixedColour() {
		return isFixedColour;
	}
	public void setFixedColour(boolean isFixedColour) {
		this.isFixedColour = isFixedColour;
	}
	public Integer getFixedColour() {
		return fixedColour;
	}
	public void setFixedColour(Integer fixedColour) {
		this.fixedColour = fixedColour;
	}
	public String getColourAttributeName() {
		return colourAttributeName;
	}
	public void setColourAttributeName(String colourAttributeName) {
		this.colourAttributeName = colourAttributeName;
	}
	public String getCreator() {
		return creator;
	}
	public void setCreator(String creator) {
		this.creator = creator;
	}
	public long getCreationDate() {
		return creationDate;
	}
	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}

}
