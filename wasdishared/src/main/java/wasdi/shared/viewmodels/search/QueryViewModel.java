package wasdi.shared.viewmodels.search;

import java.util.HashMap;
import java.util.Map;

/**
 * Represent a query to the data providers
 * 
 * @author p.campanella
 *
 */
public class QueryViewModel {
	/**
	 * Actual Offset
	 */
	public int offset = -1;
	/**
	 * Max elements
	 */
	public int limit = -1;
	/**
	 * North
	 */
	public Double north = null;
	/**
	 * South
	 */
	public Double south = null;
	/**
	 * East
	 */
	public Double east = null;
	/**
	 * West
	 */
	public Double west = null;
	/**
	 * Start Date from
	 */
	public String startFromDate;
	/**
	 * Start Date to
	 */
	public String startToDate;
	/**
	 * End Date from
	 */
	public String endFromDate;
	/**
	 * End Date to
	 */
	public String endToDate;

	/**
	 * Platform type
	 */
	public String platformName;
	/**
	 * Product type
	 */
	public String productType;
	/**
	 * Product Level
	 */
	public String productLevel;
	/**
	 * Relative Orbit
	 */
	public int relativeOrbit = -1;
	/**
	 * Absolute Orbit
	 */
	public int absoluteOrbit=-1;
	/**
	 * Cloud coverage from
	 */
	public Double cloudCoverageFrom = null;
	/**
	 * Cloud coverage to
	 */
	public Double cloudCoverageTo = null;
	/**
	 * Sensor mode
	 */
	public String sensorMode;

	/**
	 * Free text search, used when a specific product name is given
	 */
	public String productName = null;

	/**
	 * Timeliness (ie Sentinel 3 filter)
	 */
	public String timeliness = "";
	
	/**
	 * Polarisation
	 */
	public String polarisation;
	
	/**
	 * Platform identifier (eg. Sentinel-1 A or B)
	 */
	public String platformSerialIdentifier;
	
	/**
	 * Instrument
	 */
	public String instrument;
	
	/**
	 * Generic map with the different filters not parsed
	 */
	public Map<String, String> filters = new HashMap<>();

}
