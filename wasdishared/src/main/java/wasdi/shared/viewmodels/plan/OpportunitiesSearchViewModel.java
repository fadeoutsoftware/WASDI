package wasdi.shared.viewmodels.plan;

import java.util.ArrayList;

/**
 * View Model of the whole Orbit Search client
 * Sent by the client to the server to query the Plan Manager
 * @author p.campanella
 *
 */
public class OpportunitiesSearchViewModel {
	
	/**
	 * List of satellites filters
	 */
	private ArrayList<SatelliteFilterViewModel> satelliteFilters;
	/**
	 * Area of interest
	 */
	private String polygon;
	/**
	 * Start time of search
	 */
	private String acquisitionStartTime;
	/**
	 * End time of search
	 */
	private String acquisitionEndTime;
	
	public ArrayList<SatelliteFilterViewModel> getSatelliteFilters() {
		return satelliteFilters;
	}
	public void setSatelliteFilters(ArrayList<SatelliteFilterViewModel> satelliteFilters) {
		this.satelliteFilters = satelliteFilters;
	}
	public String getPolygon() {
		return polygon;
	}
	public void setPolygon(String polygon) {
		this.polygon = polygon;
	}
	public String getAcquisitionStartTime() {
		return acquisitionStartTime;
	}
	public void setAcquisitionStartTime(String acquisitionStartTime) {
		this.acquisitionStartTime = acquisitionStartTime;
	}
	public String getAcquisitionEndTime() {
		return acquisitionEndTime;
	}
	public void setAcquisitionEndTime(String acquisitionEndTime) {
		this.acquisitionEndTime = acquisitionEndTime;
	}

	
}
