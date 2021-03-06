package wasdi.shared.opensearch;

/**
 * Public enum of the WASDI Supported Platforms.
 * Each Query can be referred to a platform: this info is set in the Query View Model by the DiasQueryTranslator.
 * Query executors, in the contructor, can set a list a supported platforms. 
 * @author p.campanella
 *
 */
public class Platforms {
	
	public static String SENTINEL1 = "Sentinel-1";
	public static String SENTINEL2 = "Sentinel-2";
	public static String SENTINEL3 = "Sentinel-3";
	
	public static String PROVAV = "ProvaV";
	public static String LANDSAT8 = "Landsat-8";
	public static String ENVISAT = "Envisat";
	
	public static String COPERNICUS_MARINE = "Copernicus-Marine";
	public static String VIIRS = "Viirs";
}
