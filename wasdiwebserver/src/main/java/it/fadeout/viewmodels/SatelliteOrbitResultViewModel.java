package it.fadeout.viewmodels;

import java.util.ArrayList;

/**
 * View Model of a a Satellite Orbit Result.
 * Return of the plan manager engine of WASDI.
 * 
 * @author p.campanella
 *
 */
public class SatelliteOrbitResultViewModel {
	
	/**
	 * Constant to convert from wgs84 to radiant.
	 */
	private final static double k=180.0/Math.PI;
	
	/**
	 * Long description (Name) of the satellite 
	 */
	public String satelliteName;
	
	/**
	 * Current Position: string with lat;lon;altitude
	 */
	public String currentPosition;
	/**
	 * String representing the current time
	 */
	public String currentTime;
	/**
	 * Short satellite name (unique code)
	 */
	public String code;
	
	/**
	 * List of last postions
	 */
	public ArrayList<String> lastPositions=new ArrayList<String>();
	/**
	 * List of next postions
	 */
	public ArrayList<String> nextPositions=new ArrayList<String>();
	/**
	 * List of the times associated to the last positions
	 */
	public ArrayList<String> lastPositionsTime=new ArrayList<String>();
	/**
	 * List of the times associated to the next positions
	 */
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

