package wasdi.shared.business.ecostress;

import java.util.List;

public class EcoStressLocation {
	
	public EcoStressLocation() {
		
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
