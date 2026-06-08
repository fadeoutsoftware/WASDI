package wasdi.shared.business.labelling;

import java.util.ArrayList;

public class Attribute {
	private String name;
	private AttributeType type;
	private ArrayList<String> categories = new ArrayList<>();
	private ArrayList<Integer> colours = new ArrayList<>();
	private boolean isMandatory;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public AttributeType getType() {
		return type;
	}
	public void setType(AttributeType type) {
		this.type = type;
	}
	public ArrayList<String> getCategories() {
		return categories;
	}
	public void setCategories(ArrayList<String> categories) {
		this.categories = categories;
	}
	public ArrayList<Integer> getColours() {
		return colours;
	}
	public void setColours(ArrayList<Integer> colours) {
		this.colours = colours;
	}
	public boolean isMandatory() {
		return isMandatory;
	}
	public void setMandatory(boolean isMandatory) {
		this.isMandatory = isMandatory;
	}

}
