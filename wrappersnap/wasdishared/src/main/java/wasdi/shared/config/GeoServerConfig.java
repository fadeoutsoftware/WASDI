package wasdi.shared.config;

/**
 * Geoserver configuration
 * @author p.campanella
 *
 */
public class GeoServerConfig {
	
	/**
	 * Geoserver address
	 */
	public String address;
	
	/**
	 * Geoserver User
	 */
	public String user;
	
	/**
	 * Geoserver Password
	 */
	public String password;
	
	/**
	 * Max dimension in mb to publis single images. Over this limit
	 * wasdi will make a pyramid of the image to be pubished
	 */
	public String maxGeotiffDimensionPyramid;
	
	/**
	 * Gdal retile command for pyramidation
	 */
	public String gdalRetileCommand = "gdal_retile.py -r bilinear -levels 4 -ps 2048 2048 -co TILED=YES";
	
	/**
	 * Flag to activate the special debug mode for PublishBand.
	 * If it is on, in the publish band operation, the input DonwloadedFile is forced to be gathered from the 
	 * database using /data/wasdi/ instead of the real local folder. After got the file from the db the 
	 * path is again set to the local one.
	 * This allows to debug a publish band from a parameter taken from the server
	 */
	public boolean localDebugPublisBand = false;
	
	public String defaultLayerToGetStyleImages = "wasdi:ESA_CCI_LAND_COVER_2015";
}
