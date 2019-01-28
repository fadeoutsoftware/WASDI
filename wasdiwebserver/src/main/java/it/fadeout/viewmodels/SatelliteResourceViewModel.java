package it.fadeout.viewmodels;

import java.util.ArrayList;

import org.nfs.orbits.sat.SatSensor;

public class SatelliteResourceViewModel {
	private String satelliteName;
	private ArrayList<SatSensor> satelliteSensors;
	
	public ArrayList<SatSensor> getSatelliteSensors() {
		return satelliteSensors;
	}
	public void setSatelliteSensors(ArrayList<SatSensor> satelliteSensors) {
		this.satelliteSensors = satelliteSensors;
	}
	public String getSatelliteName() {
		return satelliteName;
	}
	public void setSatelliteName(String satelliteName) {
		this.satelliteName = satelliteName;
	}
}
