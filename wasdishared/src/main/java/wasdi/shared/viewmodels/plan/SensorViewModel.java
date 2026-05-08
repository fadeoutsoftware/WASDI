package wasdi.shared.viewmodels.plan;

import java.util.ArrayList;

/**
 * View Model of a sensor on board to a satellite.
 * Used by plan manager of WASDI
 * @author p.campanella
 *
 */
public class SensorViewModel {
	/**
	 * Sensor description
	 */
	private String description;
	
	/**
	 * True if enabled, false if not	
	 */
	private boolean enable;
	
	/**
	 * List of available sensor modes
	 */
	private ArrayList<SensorModeViewModel> sensorModes = new ArrayList<SensorModeViewModel>();
	
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
