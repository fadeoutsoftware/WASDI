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
	// Creodias 2 new platforms
	public static String SENTINEL6 = "Sentinel-6";
	public static String SENTINEL_1_RTC = "Sentinel-1-RTC";
	
	public static String PROBAV = "Proba-V";
	public static String LANDSAT5 = "Landsat-5"; 
	public static String LANDSAT7 = "Landsat-7";
	public static String LANDSAT8 = "Landsat-*";
	public static String ENVISAT = "Envisat";
	
	public static String COPERNICUS_MARINE = "Copernicus-Marine";
	public static String VIIRS = "VIIRS";
	public static String ERA5 = "ERA5";
	public static String CAMS = "CAMS";
	
	public static String PLANET= "PLANET";

	public static String DEM = "DEM";
	public static String WORLD_COVER = "WorldCover";
	
	public static String STATICS = "StaticFiles";
	
	public static String JRC_GHSL = "StaticTiles";

	public static String IMERG = "IMERG";

	public static String CM = "CM";

	public static String ECOSTRESS = "ECOSTRESS";

	public static String EARTHCACHE = "Earthcache";
	
	public static String TERRA = "TERRA";
	
	public static String WSF = "WSF"; // World Settlement Footprint
	
	// Creodias 2 new platforms
	public static String SMOS = "SMOS";
	public static String TERRAAQUA = "Terraaqua";
	public static String COP_DEM = "COP_DEM";
	public static String S2GLC = "S2GLC";
	
	public static String ERS = "ERS";
	
	// Return Plaforms
	public static String BIGBANG = "BIGBANG";
	public static String RETURN_RASTER = "ReturnRaster";
	public static String METEOCEAN = "MeteOcean"; 
	public static String NATDHMS_INDEXES = "NATDHMSIndexes";
	public static String CIMA_VARIABLES = "CIMAVariables";
	public static String FOCA = "foca";

}
