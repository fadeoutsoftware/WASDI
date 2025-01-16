package wasdi.shared.utils;

import java.io.File;
import java.util.Date;

import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.utils.log.WasdiLog;

public class MissionUtils {
	public static boolean isSentinel5PFile(File oFile) {
		try {
			if(null==oFile) {
				return false;
			}
			if (oFile.getName().toLowerCase().startsWith("s5p") && ! (oFile.getName().toLowerCase().endsWith(".tif")|| oFile.getName().toLowerCase().endsWith(".tiff"))) {
				return true;
			}
			else {
				return false;
			}
			
		} catch (Exception oE) {
			WasdiLog.debugLog("WasdiFileUtils.isSentinel5PFile( File ): " + oE);
		}
		return false;
	}
	
	public static boolean isSentinel6File(File oFile) {
		if (oFile == null) {
			return false;
		}
		
		String sFileName = oFile.getName();
		if (sFileName.toUpperCase().startsWith("S6A_") || sFileName.toUpperCase().startsWith("S6B_") || sFileName.toUpperCase().startsWith("S6_")) {
			return true;
		}
		
		return false;
	}
	
	public static boolean isLandsat5File(File oFile) {
		if (oFile == null) {
			return false;
		}
		
		String sFileName = oFile.getName();
		if (sFileName.toUpperCase().startsWith("LS05_")) {
			return true;
		}
		
		return false;
	}
	public static boolean isAsciiFile(File oFile) {
		try {
			if (oFile == null) {
				return false;
			}
			
			String sFileName = oFile.getName();
			
			return sFileName.endsWith(".asc");
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("WasdiFileUtils.isAsciiFile: exception ", oEx);
		}
		return false;
	}
	

	
	public static boolean isLandsat7File(File oFile) {
		if (oFile == null) {
			return false;
		}
		
		String sFileName = oFile.getName();
		if (sFileName.toUpperCase().startsWith("LS07_")) {
			return true;
		}
		
		return false;
	}

	public static boolean isGpmZipFile(File oFile) {
		try {
			if (null == oFile) {
				return false;
			}

			if ((oFile.getName().toUpperCase().startsWith("3B-") || oFile.getName().toUpperCase().contains("IMERG"))
					&& oFile.getName().toLowerCase().endsWith(".zip")) {
				return true;
			} else {
				return false;
			}
			
		} catch (Exception oE) {
			WasdiLog.debugLog("WasdiFileUtils.isGpmZipFile( File ): " + oE);
		}

		return false;
	}

	private static boolean isSentinel3ZippedFile(String sName) {
		try {
			if(Utils.isNullOrEmpty(sName)) {
				return false;
			}
			if(sName.toLowerCase().startsWith("s3") && sName.toLowerCase().endsWith(".zip")){
				return true;
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("WasdiFileUtils.isSentinel3File( String): " + oE);
		}
		return false;
	}
	
	public static boolean isSentinel3ZippedFile(File oFile) {
		try {
			if(null==oFile) {
				return false;
			}
			return isSentinel3ZippedFile(oFile.getName());
		} catch (Exception oE) {
			WasdiLog.debugLog("WasdiFileUtils.isSentinel3File( File ): " + oE);
		}
		return false;
	}

	private static boolean isSentinel3Name(String sName) {
		try {
			if(Utils.isNullOrEmpty(sName)) {
				return false;
			}
			if(sName.toLowerCase().startsWith("s3") && ! (sName.toLowerCase().endsWith(".tif") || sName.toLowerCase().endsWith(".tiff") || sName.toLowerCase().endsWith(".shp"))  ){
				return true;
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("WasdiFileUtils.isSentinel3File( String): " + oE);
		}
		return false;
	}
	
	public static boolean isSentinel3Name(File oFile) {
		try {
			if(null==oFile) {
				return false;
			}
			return isSentinel3Name(oFile.getName());
		} catch (Exception oE) {
			WasdiLog.debugLog("WasdiFileUtils.isSentinel3File( File ): " + oE);
		}
		return false;
	}
	
	private static boolean isSentinel3Directory(String sName) {
		try {
			if(Utils.isNullOrEmpty(sName)) {
				return false;
			}
			if(sName.toLowerCase().startsWith("s3") && sName.toLowerCase().endsWith(".sen3")){
				return true;
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("WasdiFileUtils.isSentinel3File( String): " + oE);
		}
		return false;
	}
	
	public static boolean isSentinel3Directory(File oFile) {
		try {
			if(null==oFile) {
				return false;
			}
			return isSentinel3Directory(oFile.getName());
		} catch (Exception oE) {
			WasdiLog.debugLog("WasdiFileUtils.isSentinel3File( File ): " + oE);
		}
		return false;
	}
	
	private static boolean isSentinel6Directory(String sName) {
		try {
			if(Utils.isNullOrEmpty(sName)) {
				return false;
			}
			if(sName.toLowerCase().startsWith("s6") && sName.toLowerCase().endsWith(".sen6")){
				return true;
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("WasdiFileUtils.isSentinel6Directory( String): " + oE);
		}
		return false;
	}
	
	public static boolean isSentinel6Directory(File oFile) {
		try {
			if(oFile == null) {
				return false;
			}
			return isSentinel6Directory(oFile.getName());
		} catch (Exception oE) {
			WasdiLog.errorLog("WasdiFileUtils.isSentinel6Directory( File ): ", oE);
		}
		return false;
	}
	
	
	/**
	 * Get the Platform code of the mission starting from the file Name
	 * @param sFileName File Name to investigate
	 * @return Code of the Platform as definied in the Platforms class. Null if not recognized
	 */
	public static String getPlatformFromSatelliteImageFileName(String sFileName) {
		try {
			if (Utils.isNullOrEmpty(sFileName)) return null;

			if (sFileName.toUpperCase().startsWith("S1A_") || sFileName.toUpperCase().startsWith("S1B_")) {
				return Platforms.SENTINEL1;
			}
			else if (sFileName.toUpperCase().startsWith("S2A_") || sFileName.toUpperCase().startsWith("S2B_")) {
				return Platforms.SENTINEL2;
			}
			else if (sFileName.toUpperCase().startsWith("S3A_") || sFileName.toUpperCase().startsWith("S3B_") || sFileName.toUpperCase().startsWith("S3__")) {
				return Platforms.SENTINEL3;
			}
			else if (sFileName.toUpperCase().startsWith("S5P_")) {
				return Platforms.SENTINEL5P;
			} 
			else if (sFileName.toUpperCase().startsWith("S6_") || sFileName.toUpperCase().startsWith("S6A_") || sFileName.toUpperCase().startsWith("S6B_")) {
				return Platforms.SENTINEL6;
			}
			else if (sFileName.toUpperCase().startsWith("LS05_")) {
				return Platforms.LANDSAT5;
			}
			else if (sFileName.toUpperCase().startsWith("LS07_")) {
				return Platforms.LANDSAT7;
			}
			else if (sFileName.toUpperCase().startsWith("LC08") || sFileName.toUpperCase().startsWith("LC8")) {
				return Platforms.LANDSAT8;
			}
			else if (sFileName.toUpperCase().startsWith("MER_") || sFileName.toUpperCase().startsWith("ASA_")) {
				return Platforms.ENVISAT;
			}
			else if (sFileName.toUpperCase().startsWith("RIVER-FLD") 
					|| sFileName.toUpperCase().startsWith("VNP21A1D.")
					|| sFileName.toUpperCase().startsWith("VNP21A1N.")) {
				return Platforms.VIIRS;
			}
			else if (sFileName.toUpperCase().startsWith("PROBAV_")) {
				return Platforms.PROBAV;
			}
			else if (sFileName.toUpperCase().startsWith("ERA5_")) {
				return Platforms.ERA5;
			}
			else if (sFileName.toUpperCase().startsWith("CAMS_")) {
				return Platforms.CAMS;
			}
			else if (sFileName.toUpperCase().startsWith("PLANET_")) {
				return Platforms.PLANET;
			}
			else if (sFileName.toUpperCase().startsWith("COPERNICUS_DSM_COG_")) {
				return Platforms.DEM;
			}
			else if (sFileName.toUpperCase().startsWith("ESA_WORLDCOVER")) {
				return Platforms.WORLD_COVER;
			}
			else if (sFileName.toUpperCase().startsWith("WASDI_STATIC_")) {
				return Platforms.STATICS;
			}
			else if (sFileName.toUpperCase().startsWith("3B-") || sFileName.toUpperCase().contains("IMERG")) {
				return Platforms.IMERG;
			}
			else if (sFileName.toUpperCase().startsWith("EEHCM")
					|| sFileName.toUpperCase().startsWith("EEHSEBS")
					|| sFileName.toUpperCase().startsWith("EEHSTIC")
					|| sFileName.toUpperCase().startsWith("EEHSW")
					|| sFileName.toUpperCase().startsWith("EEHTES")
					|| sFileName.toUpperCase().startsWith("EEHTSEB")
					|| sFileName.toUpperCase().startsWith("ECOSTRESS")) {
				return Platforms.ECOSTRESS;
			}
			else if (sFileName.toUpperCase().startsWith("SKYWATCH_")) {
				return Platforms.EARTHCACHE;
			} 
			else if (sFileName.toUpperCase().startsWith("MOD11A2")
					|| sFileName.toUpperCase().startsWith("MCD43A3")
					|| sFileName.toUpperCase().startsWith("MCD43A4")
					|| sFileName.toUpperCase().startsWith("MCD43D16")) {
				return Platforms.TERRA;
			}
			else if (sFileName.toUpperCase().startsWith("GHS_BUILT_S_E2018_GLOBE_R2023A_54009_10_V1_0_")) {
				return Platforms.JRC_GHSL;
			} else if (sFileName.toUpperCase().startsWith("WSF2019_V1_")) {
				return Platforms.WSF;
			}
			else if (sFileName.toUpperCase().startsWith("SPEI01_") 
					|| sFileName.toUpperCase().startsWith("SPEI03_") 
					|| sFileName.toUpperCase().startsWith("SPEI06_")
					|| sFileName.toUpperCase().startsWith("SPEI12_")) {
				return Platforms.BIGBANG;
			}
			else if (sFileName.toUpperCase().startsWith("RETRAS_") && sFileName.toUpperCase().endsWith(".TIF")) {
				return Platforms.RETURN_RASTER;
			} 
			else if (sFileName.toUpperCase().startsWith("SAR_IMP_1P")
					|| sFileName.toUpperCase().startsWith("SAR_IMS_1P")
					|| sFileName.toUpperCase().startsWith("SAR_IMM_1P")) {
				return Platforms.ERS;
			}
			else if (sFileName.toLowerCase().startsWith("meteocean_") && sFileName.toLowerCase().endsWith(".nc")) {
				return Platforms.METEOCEAN;
			}
			else if (sFileName.toLowerCase().endsWith(".nc")) {
				return Platforms.CM;
			}
			return null;
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("WasdiFileUtils.getPlatformFromFileName: exception " + oEx.toString());
		}
		
		return null;
	}
	
	/**
	 * Get the reference date of a Satellite Image from the file Name
	 * If not available, not relevant or in case of error returns "now".
	 * @param sFileName Name of the Satellite Image File
	 * @return Reference Date 
	 */
	public static Date getDateFromSatelliteImageFileName(String sFileName) {
		
		try {
			String sPlatform = getPlatformFromSatelliteImageFileName(sFileName);
			if (Utils.isNullOrEmpty(sPlatform)) return new Date();
			
			if (sPlatform.equals(Platforms.SENTINEL1)) {
				sFileName = sFileName.replace("__", "_");
				String [] asS1Parts = sFileName.split("_");
				String sDate = asS1Parts[4];
				Long lTime = TimeEpochUtils.fromDateStringToEpoch(sDate, "yyyyMMdd'T'HHmmss");
				return new Date(lTime);
			}
			else if (sPlatform.equals(Platforms.SENTINEL2)) {
				String [] asS2Parts = sFileName.split("_");
				String sDate = asS2Parts[2];
				Long lTime = TimeEpochUtils.fromDateStringToEpoch(sDate, "yyyyMMdd'T'HHmmss");
				return new Date(lTime);				
			}
			else if (sPlatform.equals(Platforms.SENTINEL3)) {
				String sDate = sFileName.substring(16,31);
				Long lTime = TimeEpochUtils.fromDateStringToEpoch(sDate, "yyyyMMdd'T'HHmmss");
				return new Date(lTime);
			}
			else if (sPlatform.equals(Platforms.SENTINEL5P)) {
				String sDate = sFileName.substring(20, 20+15);
				Long lTime = TimeEpochUtils.fromDateStringToEpoch(sDate, "yyyyMMdd'T'HHmmss");
				return new Date(lTime);
			}
			else if(sPlatform.equals(Platforms.SENTINEL6)) {
				String sDate = sFileName.substring(18, 18+15);
				Long lTime = TimeEpochUtils.fromDateStringToEpoch(sDate, "yyyyMMdd'T'HHmmss");
				return new Date(lTime);
			}
			else if (sPlatform.equals(Platforms.ENVISAT)) {
				String sDate = sFileName.substring(14, 14+8);
				Long lTime = TimeEpochUtils.fromDateStringToEpoch(sDate, "yyyyMMdd");
				return new Date(lTime);
			}
			else if (sPlatform.equals(Platforms.LANDSAT5)
					|| sPlatform.equals(Platforms.LANDSAT7)) {
				String sDate = sFileName.substring(21, 21+15);
				Long lTime = TimeEpochUtils.fromDateStringToEpoch(sDate, "yyyyMMdd");
				return new Date(lTime);
			}
			else if (sPlatform.equals(Platforms.LANDSAT8)) {
				String [] asL8Parts = sFileName.split("_");
				String sDate = asL8Parts[3];
				Long lTime = TimeEpochUtils.fromDateStringToEpoch(sDate, "yyyyMMdd");
				return new Date(lTime);				
			}
			else if (sPlatform.equals(Platforms.VIIRS)) {
				String [] asViirsParts = sFileName.split("_");
				String sDate = asViirsParts[1];
				Long lTime = TimeEpochUtils.fromDateStringToEpoch(sDate, "yyyyMMdd");
				return new Date(lTime);				
			}			
			
			// For CMEMS, ERA5 are Not relevant 
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("WasdiFileUtils.getDateFromFileName: exception " + oEx.toString());
			return null;
		}
		
		return new Date();
	}
	
	
	
	/**
	 * Get the Product Type of a Satellite Image from the file Name
	 * If not available, not relevant or in case of error returns "".
	 * @param sFileName Name of the Satellite Image File
	 * @return Product Type, or ""  
	 */
	public static String getProductTypeSatelliteImageFileName(String sFileName) {
		
		try {
			String sPlatform = getPlatformFromSatelliteImageFileName(sFileName);
			if (Utils.isNullOrEmpty(sPlatform)) return "";
			
			if (sPlatform.equals(Platforms.SENTINEL1)) {
				String [] asS1Parts = sFileName.split("_");
				String sType = asS1Parts[2];
				return sType.substring(0,3);
			}
			else if (sPlatform.equals(Platforms.SENTINEL2)) {
				String [] asS2Parts = sFileName.split("_");
				String sType = asS2Parts[1];
				return sType;				
			}
			else if (sPlatform.equals(Platforms.SENTINEL3)) {
				String sType = sFileName.substring(9,9+6);
				return sType;
			}
			else if (sPlatform.equals(Platforms.SENTINEL5P)) {
				String sType = sFileName.substring(9, 9+10);
				return sType;
			}

			// For Others are Not relevant 
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("WasdiFileUtils.getProductTypeSatelliteImageFileName: exception " + oEx.toString());
		}
		
		return "";
	}
}
