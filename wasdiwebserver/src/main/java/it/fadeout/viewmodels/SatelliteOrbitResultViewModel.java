package it.fadeout.viewmodels;

import java.util.ArrayList;

public class SatelliteOrbitResultViewModel {
	private final static double k=180.0/Math.PI;
	public String satelliteName;
	public String currentPosition;
	public String currentTime;
	public String code;
	public ArrayList<String> lastPositions=new ArrayList<String>();
	public ArrayList<String> nextPositions=new ArrayList<String>();
	public ArrayList<String> lastPositionsTime=new ArrayList<String>();
	public ArrayList<String> nextPositionsTime=new ArrayList<String>();
	
	
	public SatelliteOrbitResultViewModel(){
		
	}
	
	public void setCurrentPosition(double[] lLa){
		currentPosition=lLa[0]*k+";"+lLa[1]*k+";"+lLa[2];
	}
	
	public void addPosition(double[] lLa,String time){
		if (time.compareTo(currentTime)<=0){
				lastPositions.add(lLa[0]*k+";"+lLa[1]*k+";"+lLa[2]);
				lastPositionsTime.add(time);
		} else{
			nextPositions.add(lLa[0]*k+";"+lLa[1]*k+";"+lLa[2]);
			nextPositionsTime.add(time);
		}
	}
	
	
	
}

