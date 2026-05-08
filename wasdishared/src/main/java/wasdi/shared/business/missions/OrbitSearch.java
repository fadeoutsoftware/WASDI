package wasdi.shared.business.missions;

import java.util.ArrayList;

public class OrbitSearch {
	private ArrayList<String> sensortypes;
	private ArrayList<String> sensorresolutions;
	private ArrayList<String> satelliteNames;
	
	public ArrayList<String> getSensortypes() {
		return sensortypes;
	}
	public void setSensortypes(ArrayList<String> sensortypes) {
		this.sensortypes = sensortypes;
	}
	public ArrayList<String> getSensorresolutions() {
		return sensorresolutions;
	}
	public void setSensorresolutions(ArrayList<String> sensorresolutions) {
		this.sensorresolutions = sensorresolutions;
	}
	public ArrayList<String> getSatelliteNames() {
		return satelliteNames;
	}
	public void setSatelliteNames(ArrayList<String> satelliteNames) {
		this.satelliteNames = satelliteNames;
	}
}
