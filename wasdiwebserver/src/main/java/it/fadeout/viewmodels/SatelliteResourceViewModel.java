package it.fadeout.viewmodels;

import java.util.ArrayList;

import org.nfs.orbits.sat.SatSensor;

/**
 * View Model of a a Satellite Resource.
 * Used by plan manager of WASDI 
 * 
 * @author p.campanella
 *
 */
public class SatelliteResourceViewModel {
	
	/**
	 * Satellite/Mission name
	 */
	private String satelliteName;
	
	/**
	 * List of sensors on board
	 */
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
