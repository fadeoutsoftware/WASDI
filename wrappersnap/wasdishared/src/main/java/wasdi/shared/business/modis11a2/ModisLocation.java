package wasdi.shared.business.modis11a2;

import java.util.List;

public class ModisLocation {
	
	public ModisLocation() {
		
	}
	
	private String type;
	private List<List<List<Double>>> coordinates;
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public List<List<List<Double>>> getCoordinates() {
		return coordinates;
	}
	public void setCoordinates(List<List<List<Double>>> coordinates) {
		this.coordinates = coordinates;
	}

}
