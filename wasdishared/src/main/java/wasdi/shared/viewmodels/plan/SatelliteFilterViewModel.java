package wasdi.shared.viewmodels.plan;

import java.util.ArrayList;

/**
 * View Model of the filters for a single satellite used to query the 
 * WASDI plan manager
 * @author p.campanella
 *
 */
public class SatelliteFilterViewModel {
	/**
	 * True if enabled, false otherwise
	 */
	private boolean enable;
	/**
	 * Name of the satellite
	 */
	private String satelliteName;
	/**
	 * List of sensors
	 */
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
