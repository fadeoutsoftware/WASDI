package wasdi.shared.utils.gis;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Map;

import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.Utils;

/**
 * Wrapper of GDAL utils
 * 
 * The class uses static methods to call command line gdal and get the results.
 * 
 * @author p.campanella
 *
 */
public class GdalUtils {
	
	/**
	 * Adjust the folder of the gdal commands according to the local WASDI Configuration
	 * @param sGdalCommand Name of the command
	 * @return Full path to use to call the command in the system
	 */
    public static String adjustGdalFolder(String sGdalCommand) {
        try {
            String sGdalPath = WasdiConfig.Current.paths.gdalPath;

            if (!Utils.isNullOrEmpty(sGdalPath)) {
                File oGdalFolder = new File(sGdalPath);
                if (oGdalFolder.exists()) {
                    if (oGdalFolder.isDirectory()) {
                        if (!sGdalPath.endsWith("" + File.separatorChar)) sGdalPath = sGdalPath + File.separatorChar;
                        sGdalCommand = sGdalPath + sGdalCommand;
                    }
                }
            }
        } catch (Exception oEx) {
            oEx.printStackTrace();
        }


        return sGdalCommand;

    }
    
    /**
     * Get the output of GDALInfo on the file
     * @param sFilePath full path of the file 
     * @return GdalInfoResult filled or null in case of problems
     */
    public static GdalInfoResult getGdalInfoResult(String sFilePath) {
    	return getGdalInfoResult(new File(sFilePath));
    }
    
    /**
     * Get the output of GDALInfo on the file
     * @param oFile File to read with gdalinfo
     * @return GdalInfoResult filled or null in case of problems
     */
    @SuppressWarnings("unchecked")
	public static GdalInfoResult getGdalInfoResult(File oFile) {
    	try {
    		
    		// Domain check
    		if (oFile == null) {
    			Utils.debugLog("GdalUtils.getGdalInfoResult: File is null, return null");
    			return null;
    		}

    		if (oFile.exists()==false) {
    			Utils.debugLog("GdalUtils.getGdalInfoResult: File " + oFile.getPath() + " does not exists, return null");
    			return null;
    		}
    		
    		// We need to call gdalinfo
			String sGdalCommand = "gdalinfo";
			sGdalCommand = GdalUtils.adjustGdalFolder(sGdalCommand);
			
			ArrayList<String> asArgs = new ArrayList<String>();
			asArgs.add(sGdalCommand);
			
			asArgs.add("-json");
			asArgs.add(oFile.getPath());
			asArgs.add("-wkt_format");
			asArgs.add("WKT1");
			
			// Execute the process
			ProcessBuilder oProcessBuidler = new ProcessBuilder(asArgs.toArray(new String[0]));
			Process oProcess;
			
			oProcessBuidler.redirectErrorStream(true);
			oProcess = oProcessBuidler.start();
			
			// Get the result
			BufferedReader oReader = new BufferedReader(new InputStreamReader(oProcess.getInputStream()));
			String sOutput = "";
			String sLine;
			while ((sLine = oReader.readLine()) != null)
				sOutput += sLine + "\n";
			
			// Wait for the process to finish
			oProcess.waitFor();
			
			try {
				// Create the return object
				GdalInfoResult oGdalInfoResult = new GdalInfoResult();
				
				Map<String, Object> aoInfoJson = JsonUtils.jsonToMapOfObjects(sOutput);
				
				if (aoInfoJson == null) {
	    			Utils.debugLog("GdalUtils.getGdalInfoResult: aoInfoJson is null, return null");
	    			return null;					
				}
				
				// Ok now we parse all the json elements
				if (aoInfoJson.containsKey("description")) {
					oGdalInfoResult.description = (String) aoInfoJson.get("description");
				}
				
				if (aoInfoJson.containsKey("driverShortName")) {
					oGdalInfoResult.driverShortName = (String) aoInfoJson.get("driverShortName");
				}

				if (aoInfoJson.containsKey("driverLongName")) {
					oGdalInfoResult.driverLongName = (String) aoInfoJson.get("driverLongName");
				}

				if (aoInfoJson.containsKey("size")) {
					oGdalInfoResult.size = (ArrayList<Integer>) aoInfoJson.get("size");
				}
				
				if (aoInfoJson.containsKey("coordinateSystem")) {
					Map<String, Object> oCoordinateSystem = (Map<String, Object>) aoInfoJson.get("coordinateSystem");
					if (oCoordinateSystem.containsKey("wkt")) {
						oGdalInfoResult.coordinateSystemWKT = (String) oCoordinateSystem.get("wkt");
					}
				}
				
				if (aoInfoJson.containsKey("geoTransform")) {
					oGdalInfoResult.geoTransform = (ArrayList<Double>) aoInfoJson.get("geoTransform");
					
					if (oGdalInfoResult.geoTransform!=null) {
						if (oGdalInfoResult.geoTransform.size()>0) {
							oGdalInfoResult.topLeftX = oGdalInfoResult.geoTransform.get(0);
						}
						if (oGdalInfoResult.geoTransform.size()>1) {
							oGdalInfoResult.westEastPixelResolution = oGdalInfoResult.geoTransform.get(1);
						}
						if (oGdalInfoResult.geoTransform.size()>3) {
							oGdalInfoResult.topLeftY = oGdalInfoResult.geoTransform.get(3);
						}
						if (oGdalInfoResult.geoTransform.size()>5) {
							oGdalInfoResult.northSouthPixelResolution = oGdalInfoResult.geoTransform.get(5);
						}
					}
				}
				
				if (aoInfoJson.containsKey("wgs84Extent")) {
					Map<String, Object> oWgs84Extent = (Map<String, Object>) aoInfoJson.get("wgs84Extent");
					if (oWgs84Extent.containsKey("coordinates")) {
						
						try {
							ArrayList<ArrayList<ArrayList<Double>>> aoCoordinates = (ArrayList<ArrayList<ArrayList<Double>>>) oWgs84Extent.get("coordinates");
							
							if (aoCoordinates != null) {
								if (aoCoordinates.get(0) != null) {
									for (int i=0; i<aoCoordinates.get(0).size(); i++) {
										ArrayList<Double> aoPoint = aoCoordinates.get(0).get(i);
										
										if (aoPoint != null) {
											if (aoPoint.size()>=2) {
												if (i==0) {
													oGdalInfoResult.wgs84North = aoPoint.get(1);
													oGdalInfoResult.wgs84South = aoPoint.get(1);
													oGdalInfoResult.wgs84East = aoPoint.get(0);
													oGdalInfoResult.wgs84West = aoPoint.get(0);
												}
												else {
													double dLat = aoPoint.get(1);
													double dLon = aoPoint.get(0);
													
													if (dLat > oGdalInfoResult.wgs84North) oGdalInfoResult.wgs84North = dLat;
													if (dLat < oGdalInfoResult.wgs84South) oGdalInfoResult.wgs84South = dLat;
													
													if (dLon > oGdalInfoResult.wgs84East) oGdalInfoResult.wgs84East = dLon;
													if (dLon < oGdalInfoResult.wgs84West) oGdalInfoResult.wgs84West = dLon;
												}
											}
										}
									}								
								}
							}							
						}
						catch (Exception oEx) {
							
							Utils.debugLog("GdalUtils.getGdalInfoResult: exception getting wgs84 extent: " + oEx.toString());
						}
					}
				}	
				
				
				if (aoInfoJson.containsKey("bands")) {
					ArrayList<Map<String, Object>> aoBands = (ArrayList<Map<String, Object>>) aoInfoJson.get("bands");
					
					if (aoBands!=null) {
						for (int iBands = 0; iBands<aoBands.size(); iBands++) {
							Map<String, Object> oBand = aoBands.get(iBands);
							GdalBandInfo oBandInfo = new GdalBandInfo();
							
							if (oBand.containsKey("band")) {
								oBandInfo.band = (int) oBand.get("band");
							}
							
							if (oBand.containsKey("type")) {
								oBandInfo.type = (String) oBand.get("type");
							}

							if (oBand.containsKey("colorInterpretation")) {
								oBandInfo.colorInterpretation = (String) oBand.get("colorInterpretation");
							}
							
							if (oBand.containsKey("noDataValue")) {
								oBandInfo.noDataValue = (double) oBand.get("noDataValue");
							}
							
							oGdalInfoResult.bands.add(oBandInfo);
						}
						
					}
				}
				
				return oGdalInfoResult;
			}
	    	catch (Exception oEx) {
	    		Utils.debugLog("GdalUtils.getGdalInfoResult: exception converting the result: " + oEx.toString());
			}
			
			return null;
			
    	}
    	catch (Exception oEx) {
    		Utils.debugLog("GdalUtils.getGdalInfoResult: exception " + oEx.toString());
		}
    	
    	return null;
    }
    
    public static void convertToWGS84(String sInputFile, String sOutputFile) {
    	convertToWGS84(sInputFile, sOutputFile, null);
    }
    
    /**
     * Converts an input file in the output file with WGS84 Projection
     * @param sInputFile
     * @param sOutputFile
     * @param sInputSrs Input source spatial reference. If not specified the SRS found in the input dataset will be used
     */
    public static void convertToWGS84(String sInputFile, String sOutputFile, String sInputSrs) {
    	
    	try {
    		ArrayList<String> asArgs = new ArrayList<String>();
    		String sGdalCommand = "gdalwarp";
    		sGdalCommand = GdalUtils.adjustGdalFolder(sGdalCommand);
    		
    		asArgs.add(sGdalCommand);
    		
    		if (!Utils.isNullOrEmpty(sInputSrs)) {
    			asArgs.add("-s_srs");
    			asArgs.add(sInputSrs);
    		}
    		
    		asArgs.add("-geoloc");
    		asArgs.add("-t_srs");
    		asArgs.add("EPSG:4326");
    		asArgs.add("-overwrite");
    		asArgs.add(sInputFile);
    		asArgs.add(sOutputFile);
    		
    		ProcessBuilder oProcessBuidler = new ProcessBuilder(asArgs.toArray(new String[0]));
    		Process oProcess;
    		
    		oProcessBuidler.redirectErrorStream(true);
    		oProcess = oProcessBuidler.start();
    		String sLine = "";
    		
    		BufferedReader oReader = new BufferedReader(new InputStreamReader(oProcess.getInputStream()));
    		while ((sLine = oReader.readLine()) != null)
    			Utils.debugLog("GdalUtils.convertToWGS84 [gdal]: " + sLine);
    		
    		oProcess.waitFor();    	    		
    	}
    	catch (Exception oEx) {
    		Utils.debugLog("GdalUtils.convertToWGS84: exception " + oEx.toString());
		}
    }
    
    public static String getMollweideProjectionDescription() {
    	return "PROJCS[\"World_Mollweide\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"WGS_1984\",SPHEROID[\"WGS_1984\",6378137,298.257223563]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],PROJECTION[\"Mollweide\"],PARAMETER[\"False_Easting\",0],PARAMETER[\"False_Northing\",0],PARAMETER[\"Central_Meridian\",0],UNIT[\"Meter\",1],AUTHORITY[\"EPSG\",\"54009\"]]";
    }
}
