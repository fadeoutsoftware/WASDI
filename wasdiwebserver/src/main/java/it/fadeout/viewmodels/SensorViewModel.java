package it.fadeout.viewmodels;

import java.util.ArrayList;

public class SensorViewModel {
	private String description;
	private boolean enable;
	private ArrayList<SensorModeViewModel> sensorModes;
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public boolean isEnable() {
		return enable;
	}
	public void setEnable(boolean enabled) {
		this.enable = enabled;
	}
	public ArrayList<SensorModeViewModel> getSensorModes() {
		return sensorModes;
	}
	public void setSensorModes(ArrayList<SensorModeViewModel> sensorModes) {
		this.sensorModes = sensorModes;
	}
	
}
