package it.fadeout.viewmodels;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class OrbitSearchViewModel {

	private ArrayList<OrbitFilterViewModel> orbitFilters;
	private String polygon;
	
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
	
	
}
