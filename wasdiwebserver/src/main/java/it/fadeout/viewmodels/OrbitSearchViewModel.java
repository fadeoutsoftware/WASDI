package it.fadeout.viewmodels;

import java.util.ArrayList;
import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class OrbitSearchViewModel {

	private ArrayList<OrbitFilterViewModel> orbitFilters;
	private String polygon;
	private String acquisitionStartTime;
	private String acquisitionEndTime;
	private ArrayList<String> satelliteNames;
	
	public OrbitSearchViewModel()
	{}
	
	
	
	public String getPolygon() {
		return polygon;
	}
	public void setPolygon(String polygon) {
		this.polygon = polygon;
	}



	public ArrayList<OrbitFilterViewModel> getOrbitFilters() {
		return orbitFilters;
	}



	public void setOrbitFilters(ArrayList<OrbitFilterViewModel> orbitFilters) {
		this.orbitFilters = orbitFilters;
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



	public ArrayList<String> getSatelliteNames() {
		return satelliteNames;
	}



	public void setSatelliteNames(ArrayList<String> satelliteNames) {
		this.satelliteNames = satelliteNames;
	}
	
	
}
