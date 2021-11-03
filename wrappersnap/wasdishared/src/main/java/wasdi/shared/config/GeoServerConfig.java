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
}
