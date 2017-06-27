package it.fadeout.viewmodels;

import java.util.ArrayList;

public class SatelliteOrbitResultViewModel {
	public String satelliteName;
	public String currentPosition;
	public String code;
	public ArrayList<String> lastPositions=new ArrayList<String>();
	public ArrayList<String> nextPositions=new ArrayList<String>();
	public ArrayList<String> lastPositionsTime=new ArrayList<String>();
	public ArrayList<String> nextPositionsTime=new ArrayList<String>();
	
	
	public SatelliteOrbitResultViewModel(){
		
	}
	
}

