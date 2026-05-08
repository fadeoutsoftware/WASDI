package wasdi.shared.viewmodels.plan;

import java.util.ArrayList;

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
	private ArrayList<SensorViewModel> satelliteSensors;
	
	public ArrayList<SensorViewModel> getSatelliteSensors() {
		return satelliteSensors;
	}
	public void setSatelliteSensors(ArrayList<SensorViewModel> satelliteSensors) {
		this.satelliteSensors = satelliteSensors;
	}
	public String getSatelliteName() {
		return satelliteName;
	}
	public void setSatelliteName(String satelliteName) {
		this.satelliteName = satelliteName;
	}
}
