package it.fadeout.viewmodels;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class OrbitFilterViewModel {
	
	private String sensorType;
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
