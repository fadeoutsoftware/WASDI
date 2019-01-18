package it.fadeout.viewmodels;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class OrbitSearchViewModel {

	private ArrayList<OrbitFilterViewModel> orbitFilters;
	private String polygon;
	private String acquisitionStartTime;
	private String acquisitionEndTime;
	private ArrayList<String> satelliteNames;
	//private LookingType lookingType;
	private String lookingType;
	//private ViewAngle viewAngle;
	//private swathSize swathSize;
	private String viewAngle;
	private String swathSize;	
	
	/*public LookingType getLookingType() {
		return lookingType;
	}



	public void setLookingType(LookingType lookingType) {
		this.lookingType = lookingType;
	}



	public ViewAngle getViewAngle() {
		return viewAngle;
	}



	public void setViewAngle(ViewAngle viewAngle) {
		this.viewAngle = viewAngle;
	}



	public org.nfs.orbits.sat.swathSize getSwathSize() {
		return swathSize;
	}



	public void setSwathSize(org.nfs.orbits.sat.swathSize swathSize) {
		this.swathSize = swathSize;
	}

*/
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



	public String getLookingType() {
		return lookingType;
	}



	public void setLookingType(String lookingType) {
		this.lookingType = lookingType;
	}



	public String getSwathSize() {
		return swathSize;
	}



	public void setSwathSize(String swathSize) {
		this.swathSize = swathSize;
	}



	public String getViewAngle() {
		return viewAngle;
	}



	public void setViewAngle(String viewAngle) {
		this.viewAngle = viewAngle;
	}
	
	
}
