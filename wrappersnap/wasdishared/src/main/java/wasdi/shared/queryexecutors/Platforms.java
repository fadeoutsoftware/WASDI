package wasdi.shared.queryexecutors;

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
	public static String SENTINEL5P = "Sentinel-5P";
	
	public static String PROBAV = "Proba-V";
	public static String LANDSAT8 = "Landsat-8";
	public static String ENVISAT = "Envisat";
	
	public static String COPERNICUS_MARINE = "Copernicus-Marine";
	public static String VIIRS = "VIIRS";
	public static String ERA5 = "ERA5";
	
	public static String PLANET= "PLANET";

	public static String DEM = "DEM";
	public static String WORLD_COVER = "WorldCover";

}
