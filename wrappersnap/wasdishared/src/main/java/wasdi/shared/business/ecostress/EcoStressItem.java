package wasdi.shared.business.ecostress;

import com.mongodb.client.model.geojson.Polygon;

public class EcoStressItem {
	
	private String fileName;
	
	private int orbit;
	
	private String dayNight;
	
	private Double date;
	
	private Polygon location;

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public int getOrbit() {
		return orbit;
	}

	public void setOrbit(int orbit) {
		this.orbit = orbit;
	}

	public String getDayNight() {
		return dayNight;
	}

	public void setDayNight(String dayNight) {
		this.dayNight = dayNight;
	}

	public Double getDate() {
		return date;
	}

	public void setDate(Double date) {
		this.date = date;
	}

	public Polygon getLocation() {
		return location;
	}

	public void setLocation(Polygon location) {
		this.location = location;
	}
	
}
