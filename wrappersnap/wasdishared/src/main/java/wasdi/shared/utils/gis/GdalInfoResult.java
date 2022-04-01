package wasdi.shared.utils.gis;

import java.util.ArrayList;

/**
 * Representation of the gdalinfo output
 * @author p.campanella
 *
 */
public class GdalInfoResult {
	/**
	 * Decription of the file, usually the path
	 */
	public String description;
	/**
	 * Short name of the identified driver
	 */
	public String driverShortName;
	/**
	 * Long name of the identified driver 
	 */
	public String driverLongName;
	/**
	 * Image Size: 0 = # columns, 1 = # rows
	 */
	public ArrayList<Integer> size;
	/**
	 * WKT of the coordinate system
	 */
	public String coordinateSystemWKT;
	/**
	 * geoTransform array. Values are also copied in the explicit variables
	 */
	public ArrayList<Double> geoTransform;
	
	/**
	 * WGS84 North Coordinate if available
	 */
	public double wgs84North;
	/**
	 * WGS84 South Coordinate if available
	 */
	public double wgs84South;
	/**
	 * WGS84 East Coordinate if available
	 */
	public double wgs84East;
	/**
	 * WGS84 West Coordinate if available
	 */
	public double wgs84West;
	/**
	 * Top left X coordinate in the coordinate system of the file
	 */
	public double topLeftX;
	/**
	 * Horizontal Pixel size in the coordinate system of the file
	 */
	public double westEastPixelResolution;
	/**
	 * Top left Y coordinate in the coordinate system of the file
	 */
	public double topLeftY;
	/**
	 * Negative Vertical Pixel size in the coordinate system of the file
	 */
	public double northSouthPixelResolution;
	/**
	 * List of bands
	 */
	public ArrayList<GdalBandInfo> bands = new ArrayList<GdalBandInfo>();
}
