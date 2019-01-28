package it.fadeout.viewmodels;

import java.util.ArrayList;

public class SatelliteFilterViewModel {
	private boolean enable;
	private String satelliteName;
	private ArrayList<SensorViewModel> satelliteSensors;
	public boolean isEnable() {
		return enable;
	}
	public void setEnable(boolean enable) {
		this.enable = enable;
	}
	public String getSatelliteName() {
		return satelliteName;
	}
	public void setSatelliteName(String satelliteName) {
		this.satelliteName = satelliteName;
	}
	public ArrayList<SensorViewModel> getSatelliteSensors() {
		return satelliteSensors;
	}
	public void setSatelliteSensors(ArrayList<SensorViewModel> satelliteSensors) {
		this.satelliteSensors = satelliteSensors;
	}
	
}
