package wasdi.shared.viewmodels.plan;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * View Model of a single Orbit Filter
 * @author p.campanella
 *
 */
@XmlRootElement
public class OrbitFilterViewModel {
	
	/**
	 * Type of the sensor
	 */
	private String sensorType;
	/**
	 * Required resolution
	 */
    private String sensorResolution;
    

    public String getSensorType() {
        return sensorType;
    }

    public void setSensorType(String sensorType) {
        this.sensorType = sensorType;
    }
    
    public String getSensorResolution() {
        return sensorResolution;
    }

    public void setSensorResolution(String sensorResolution) {
        this.sensorResolution = sensorResolution;
    }


}
