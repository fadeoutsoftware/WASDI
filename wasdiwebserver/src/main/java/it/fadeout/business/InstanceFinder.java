package it.fadeout.business;

import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.nfs.orbits.CoverageTool.CoverageRequest;
import org.nfs.orbits.CoverageTool.InterestArea;
import org.nfs.orbits.CoverageTool.Polygon;
import org.nfs.orbits.CoverageTool.apoint;
import org.nfs.orbits.sat.CoverageSwathResult;
import org.nfs.orbits.sat.ISatellite;
import org.nfs.orbits.sat.SatFactory;
import org.nfs.orbits.sat.SatSensor;
import org.nfs.orbits.sat.Satellite;
import org.nfs.orbits.sat.SensorMode;

import satLib.astro.time.Time;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.plan.OpportunitiesSearchViewModel;
import wasdi.shared.viewmodels.plan.SatelliteFilterViewModel;
import wasdi.shared.viewmodels.plan.SensorModeViewModel;
import wasdi.shared.viewmodels.plan.SensorViewModel;

/**
 * Static methods to make a query to the Plan Manager. The main method is 
 * findSwatsByFilters
 * that takes client filters and return a list of possibile acquisitions.
 * The plan engine is implemented in the org.nfs.orbits package that had been developed
 * by Acrotec Foundation in the context of the NFS project made for ASI in the past.
 * 
 * @author p.campanella
 *
 */
public class InstanceFinder {
	
	/**
	 * Available sat
	 */
	static ArrayList<ISatellite> m_aoSatellites = null;
	
	/**
	 * Random object 
	 */
	private static SecureRandom s_oRandom = new SecureRandom();	

	private InstanceFinder() {
		// private constructor to hide the public implicit one 
	}
	/**
	 * List of Orbit's satellites references
	 */
	protected static final String[] s_asOrbitSats = new String[] {
			"/org/nfs/orbits/sat/resource/cosmosky1.xml",
			"/org/nfs/orbits/sat/resource/cosmosky2.xml",
			"/org/nfs/orbits/sat/resource/cosmosky3.xml",
			"/org/nfs/orbits/sat/resource/cosmosky4.xml",
			"/org/nfs/orbits/sat/resource/sentinel_1a.xml",
			"/org/nfs/orbits/sat/resource/sentinel_1b.xml",
			"/org/nfs/orbits/sat/resource/landsat8.xml",
		    "/org/nfs/orbits/sat/resource/sentinel_2a.xml",
		    "/org/nfs/orbits/sat/resource/sentinel_2b.xml",
		    "/org/nfs/orbits/sat/resource/probav.xml",
		    "/org/nfs/orbits/sat/resource/geoeye.xml",
		    "/org/nfs/orbits/sat/resource/worldview2.xml"
	};

	protected static final Map<String, String> s_asOrbitSatsMap = new HashMap<String, String>();
	
	static {
		s_asOrbitSatsMap.put("COSMOSKY1", "/org/nfs/orbits/sat/resource/cosmosky1.xml");
		s_asOrbitSatsMap.put("COSMOSKY2", "/org/nfs/orbits/sat/resource/cosmosky2.xml");
		s_asOrbitSatsMap.put("COSMOSKY3", "/org/nfs/orbits/sat/resource/cosmosky3.xml");
		s_asOrbitSatsMap.put("COSMOSKY4", "/org/nfs/orbits/sat/resource/cosmosky4.xml");
		s_asOrbitSatsMap.put("SENTINEL1A", "/org/nfs/orbits/sat/resource/sentinel_1a.xml");
		s_asOrbitSatsMap.put("SENTINEL1B", "/org/nfs/orbits/sat/resource/sentinel_1b.xml");
		s_asOrbitSatsMap.put("LANDSAT8", "/org/nfs/orbits/sat/resource/landsat8.xml");
		s_asOrbitSatsMap.put("SENTINEL2A", "/org/nfs/orbits/sat/resource/sentinel_2a.xml");
		s_asOrbitSatsMap.put("SENTINEL2B", "/org/nfs/orbits/sat/resource/sentinel_2b.xml");			
		s_asOrbitSatsMap.put("PROBAV", "/org/nfs/orbits/sat/resource/probav.xml");
		s_asOrbitSatsMap.put("GEOEYE", "/org/nfs/orbits/sat/resource/geoeye.xml");
		s_asOrbitSatsMap.put("WORLDVIEW2", "/org/nfs/orbits/sat/resource/worldview2.xml");
		
		s_asOrbitSatsMap.put("COSMO-SKYMED 1", "/org/nfs/orbits/sat/resource/cosmosky1.xml");
		s_asOrbitSatsMap.put("COSMO-SKYMED 2", "/org/nfs/orbits/sat/resource/cosmosky2.xml");
		s_asOrbitSatsMap.put("COSMO-SKYMED 3", "/org/nfs/orbits/sat/resource/cosmosky3.xml");
		s_asOrbitSatsMap.put("COSMO-SKYMED 4", "/org/nfs/orbits/sat/resource/cosmosky4.xml");
		s_asOrbitSatsMap.put("SENTINEL-1A", "/org/nfs/orbits/sat/resource/sentinel_1a.xml");
		s_asOrbitSatsMap.put("SENTINEL-1B", "/org/nfs/orbits/sat/resource/sentinel_1b.xml");
		s_asOrbitSatsMap.put("LANDSAT 8", "/org/nfs/orbits/sat/resource/landsat8.xml");
		s_asOrbitSatsMap.put("SENTINEL-2A", "/org/nfs/orbits/sat/resource/sentinel_2a.xml");
		s_asOrbitSatsMap.put("SENTINEL-2B", "/org/nfs/orbits/sat/resource/sentinel_2b.xml");			
		s_asOrbitSatsMap.put("PROBA-V", "/org/nfs/orbits/sat/resource/probav.xml");
		s_asOrbitSatsMap.put("GEOEYE 1", "/org/nfs/orbits/sat/resource/geoeye.xml");
		s_asOrbitSatsMap.put("WORLDVIEW-2 (WV-2)", "/org/nfs/orbits/sat/resource/worldview2.xml");

	}
	
	public static Map<String, String> getOrbitSatsMap() {
		return s_asOrbitSatsMap;
	}
	
	/**
	 * Constant to convert from wgs84 to radiant.
	 */
	static final double s_dConversionFactor = Math.PI / 180.0;
	
	/**
	 * Finds the new possible acquisitions according to the input filters
	 * @param oOpportunitiesSearch Filters View Model
	 * @return Array of possible acquisitions
	 */
	public static  ArrayList<CoverageSwathResult> findSwatsByFilters(OpportunitiesSearchViewModel oOpportunitiesSearch)
	{
		WasdiLog.debugLog("findSwatsByFilters: start");
		
		// Create satellites
		m_aoSatellites = new ArrayList<ISatellite>();
		ArrayList<SatelliteFilterViewModel> aoSatelliteFilters;
		aoSatelliteFilters = oOpportunitiesSearch.getSatelliteFilters();
		
		for (int iIndexSatelliteFitler = 0; iIndexSatelliteFitler < aoSatelliteFilters.size() ; iIndexSatelliteFitler++) {
			
			String sSatelliteName = aoSatelliteFilters.get(iIndexSatelliteFitler).getSatelliteName();
			WasdiLog.debugLog("InstanceFinder::findSwatsByFilters: building : " + sSatelliteName);

			Satellite oSatellite;
			try {
				oSatellite=SatFactory.buildSat(s_asOrbitSatsMap.get(sSatelliteName));
				WasdiLog.debugLog("costruito");
			} 
			catch (Throwable oEx) {
				WasdiLog.errorLog("InstanceFinder::findSwatsByFilters: unable to instantiate satellite " + s_asOrbitSats[iIndexSatelliteFitler] + " - " + oEx);
				return null;
			}

			// add the current satellite to the find list
			m_aoSatellites.add(oSatellite);
		}

		if (!m_aoSatellites.isEmpty()) {
			WasdiLog.debugLog("InstanceFinder::findSwatsByFilters: Satellites Available: " + m_aoSatellites.size());
		}
		else {
			WasdiLog.debugLog("InstanceFinder::findSwatsByFilters: m_aoSatellites EMPTY ");
		}
		
		// Check selected sensors: for each satellite 
		for (ISatellite oSatellite : m_aoSatellites) 
		{
			// For each filter
			for(int iIndexSatelliteFilter = 0; iIndexSatelliteFilter < aoSatelliteFilters.size() ; iIndexSatelliteFilter++)
			{
				// If the fitler is associated to this satellite
				String sSatelliteName =  aoSatelliteFilters.get(iIndexSatelliteFilter).getSatelliteName();
				
				if(oSatellite.getName().equals(sSatelliteName))
				{
					// Get and enabled the list of sensors and sensor modes of this sat
					ArrayList<SensorViewModel> aoSatelliteSensorsEnabled = aoSatelliteFilters.get(iIndexSatelliteFilter).getSatelliteSensors();
					ArrayList<SatSensor> aoSatSensors = oSatellite.getSensors();
					setEnableSensorsAndSensorModes(aoSatSensors,aoSatelliteSensorsEnabled);
				}
				
			}
		}
		
		// Area of interest
		InterestArea oAreaOfInterest = new InterestArea("required area");
		String sArea = oOpportunitiesSearch.getPolygon();
		
		Polygon oPoligon = prepareAreaOfInterest(sArea);
		oAreaOfInterest.setArea(oPoligon);
		
		// Start and Dates
		String sAquisitionStartTime = oOpportunitiesSearch.getAcquisitionStartTime();
		String sAquisitionEndTime = oOpportunitiesSearch.getAcquisitionEndTime();
		sAquisitionStartTime = convertDateFormatsFromClientToPlanEngine(sAquisitionStartTime);
		sAquisitionEndTime = convertDateFormatsFromClientToPlanEngine(sAquisitionEndTime);
		
		// Set acquisition period
		Time oDateTimeStart = new Time(sAquisitionStartTime);
		Time oDateTimeEnd = new Time(sAquisitionEndTime);
		
		// Prepare coverage request
		CoverageRequest coverageRequest = new CoverageRequest();
		coverageRequest = getCoverageRequest(oAreaOfInterest,m_aoSatellites,oDateTimeStart,oDateTimeEnd);
		
		
		WasdiLog.debugLog("InstanceFinder::findSwatsByFilters: start solve request");
		
		// Start the search: tre means to find any potential coverage with all available resources
		ArrayList<CoverageSwathResult> oResults = coverageRequest.solveRequest(true);

		WasdiLog.debugLog("InstanceFinder::findSwatsByFilters: search done ");
		
		return oResults;
	}
	
	/**
	 * Utilty method to make an instance of CoverageRequest
	 * @param oAreaOfInterest Area of interest
	 * @param aoSatellites satellites 
	 * @param oDateTimeStart start date time 
	 * @param oDateTimeEnd end date time
	 * @return CoverageRequest created object
	 */
	private static CoverageRequest getCoverageRequest(InterestArea oAreaOfInterest,ArrayList<ISatellite> aoSatellites,Time oDateTimeStart,Time oDateTimeEnd)
	{
		CoverageRequest oCoverageRequest = new CoverageRequest();

		// aggiungo l'area di interesse (posso aggiungerne anche più di una)
		oCoverageRequest.addInterestArea(oAreaOfInterest);

		// imposto la lista di satelliti da utilizzare per la ricerca
		oCoverageRequest.setISatellite(aoSatellites);

		// imposto le date di inizio e fine osservazione
		oCoverageRequest.setFirstDate(oDateTimeStart);
		oCoverageRequest.setSecondDate(oDateTimeEnd);
		
		return oCoverageRequest;
	}
	
	/**
	 * Converts the client date format ( yyyy-MM-dd HH:mm:ss ) to the 
	 * format required by the plan engine ( yyyyMMddHHmmss )
	 * @param sDateTime Date time in format: yyyy-MM-dd HH:mm:ss
	 * @return Date time in format: yyyyMMddHHmmss
	 */
	private static String convertDateFormatsFromClientToPlanEngine(String sDateTime)
	{
		SimpleDateFormat oFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date oDateTime;
		try {
			oDateTime = oFormat.parse(sDateTime);
			SimpleDateFormat oFormat2 = new SimpleDateFormat("yyyyMMddHHmmss");
			sDateTime = oFormat2.format(oDateTime);
		} catch (ParseException oEx) {
			WasdiLog.errorLog("InstanceFinder.convertDateFormatsFromClientToPlanEngine:  error", oEx);
		}
		
		return sDateTime;
	}
	
	/**
	 * Converts the area string description in WKT to a Polygon geometry
	 * @param sArea WKT Polygon with the area of interest
	 * @return Polygon instance
	 */
	private static Polygon prepareAreaOfInterest(String sArea){
		
		// Create the polygon
		Polygon oPoligon = new Polygon();
		// clean the Area string
		String sCleanedArea = sArea.replaceAll("[POLYGN()]", "");
		// Split in points
		String[] asAreaPoints = sCleanedArea.split(",");
		int iPointsCount = asAreaPoints.length;
		apoint[] aoPoints = new apoint[iPointsCount];

		// process each polygon point
		if (asAreaPoints != null) 
		{
			// Little dispacememt due to a problem in the plan engine
			double dTempFixValue = 0.005; 
			
			// For all the poings
			for (int iCount = 0; iCount < iPointsCount; iCount++) {
				// Read the point
				String[] asPoint = asAreaPoints[iCount].split(" ");
				
				// Target coordinates
				double dX;
				double dY;
				
				// Update the random fix value
				dTempFixValue = s_oRandom.nextInt(9); 
				dTempFixValue = dTempFixValue/1000; 
				
				// Convert X
				try {
					dX = Double.valueOf(asPoint[0]);
					
				} catch (Exception oEx) {
					WasdiLog.debugLog("InstanceFinder.findSwats: eccezione nella conversione stringa double del punto x dell'area ");
					dX = 0;
				}
				
				// Convert Y
				try {
					dY = Double.valueOf(asPoint[1]);
				} catch (Exception oEx) {
					WasdiLog.debugLog("InstanceFinder.findSwats: eccezione nella conversione stringa double del punto y dell'area ");
					dY = 0;
				}
				
				// Adjust a little bit
				dX = dX + dTempFixValue; 
				dY = dY + dTempFixValue; 
				
				// add the point to the list
				aoPoints[iCount] = new apoint(dX * s_dConversionFactor, dY * s_dConversionFactor, 0);

			}
		}
		
		// Add the point list to the polygon
		oPoligon.setVertex(aoPoints);
		
		return oPoligon;
	}
	
	/**
	 * Given a list of sensors of a satellite, and a list of filters, it enables and disables single sensors
	 * according to the filters
	 * @param aoSatSensors List of satellite sensors to enable/disable
	 * @param aoSatelliteSensorsEnabled List of the input filters
	 */
	private static void setEnableSensorsAndSensorModes(ArrayList<SatSensor> aoSatSensors, ArrayList<SensorViewModel> aoSatelliteSensorsEnabled)
	{
		// For each sensor of the sat
		for (SatSensor oSensor : aoSatSensors)
		{
			// For each filter
			for(SensorViewModel oSensorEnabled : aoSatelliteSensorsEnabled)
			{
				// Does the filter include this sensor?
				if(oSensor.getDescription().equals(oSensorEnabled.getDescription()))
				{
					// Set the sensor as enabled 
					oSensor.setEnabled(true);
					
					// Get the list of  sat sensor modes
					ArrayList<SensorMode> oSensorModes = oSensor.getSensorModes();
					// And relative list in the filter
					ArrayList<SensorModeViewModel> oSensorModesEnabled = oSensorEnabled.getSensorModes();
					
					// Make the same job for sensor modes
					setEnableSensorModes(oSensorModes,oSensorModesEnabled);
				}
			}
		}
	}
	
	/**
	 * Given a list of sensors modes of a sensor of satellite, and a list of filters, it enables and disables single sensors modes
	 * @param oSensorModes  List of satellite sensor modes to enable/disable
	 * @param oSensorModesEnabled List of the input sensor modes filters
	 */
	private static void setEnableSensorModes(ArrayList<SensorMode> oSensorModes, ArrayList<SensorModeViewModel> oSensorModesEnabled)
	{
		// For each sensor mode
		for (SensorMode oSensorMode : oSensorModes) {
			// For each filter
			for(SensorModeViewModel oSensorModeEnabled:oSensorModesEnabled)
			{
				// Does the filter include this sensor mode?
				if(oSensorMode.getName().equals(oSensorModeEnabled.getName()))
				{
					// Enable it
					oSensorMode.setEnabled(true);
				}
			}
		}
	}
	

}
