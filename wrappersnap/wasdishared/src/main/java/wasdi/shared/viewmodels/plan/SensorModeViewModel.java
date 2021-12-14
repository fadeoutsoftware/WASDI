package wasdi.shared.viewmodels.plan;

/**
 * View Model of a differeny sensor modes that of a sensor on board to a satellite.
 * Used by plan manager of WASDI
 * @author p.campanella
 *
 */
public class SensorModeViewModel {
	
	/**
	 * Name Mode
	 */
	private String name;
	
	/**
	 * true if enabled, false otherwise
	 */
	private boolean enable;
	
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public boolean isEnable() {
		return enable;
	}
	public void setEnable(boolean enabled) {
		this.enable = enabled;
	}
	
}
