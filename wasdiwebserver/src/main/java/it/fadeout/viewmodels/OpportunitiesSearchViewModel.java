package it.fadeout.viewmodels;

import java.util.ArrayList;

public class OpportunitiesSearchViewModel {
	private ArrayList<SatelliteFilterViewModel> satelliteFilters;
	private String polygon;
	private String acquisitionStartTime;
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
