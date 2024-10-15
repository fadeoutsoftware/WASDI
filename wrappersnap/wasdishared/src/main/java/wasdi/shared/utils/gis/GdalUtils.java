package wasdi.shared.utils.gis;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import wasdi.shared.config.PathsConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.parameters.MosaicParameter;
import wasdi.shared.parameters.settings.MosaicSetting;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.runtime.RunTimeUtils;
import wasdi.shared.utils.runtime.ShellExecReturn;

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
        	WasdiLog.errorLog("GdalUtils.adjustGdalFolder: error", oEx);
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
    			WasdiLog.debugLog("GdalUtils.getGdalInfoResult: File is null, return null");
    			return null;
    		}

    		if (oFile.exists()==false) {
    			WasdiLog.debugLog("GdalUtils.getGdalInfoResult: File " + oFile.getPath() + " does not exists, return null");
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
			
			ShellExecReturn oShellExecReturn = RunTimeUtils.shellExec(asArgs, true, true, true, true);
			String sOutput = oShellExecReturn.getOperationLogs();
			
			try {
				// Create the return object
				GdalInfoResult oGdalInfoResult = new GdalInfoResult();
				
				if (!sOutput.startsWith("{")) {
					sOutput = sOutput.substring(sOutput.indexOf("{"));
				}
				
				Map<String, Object> aoInfoJson = JsonUtils.jsonToMapOfObjects(sOutput);
				
				if (aoInfoJson == null) {
	    			WasdiLog.debugLog("GdalUtils.getGdalInfoResult: aoInfoJson is null, return null");
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
							
							WasdiLog.debugLog("GdalUtils.getGdalInfoResult: exception getting wgs84 extent: " + oEx.toString());
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
	    		WasdiLog.debugLog("GdalUtils.getGdalInfoResult: exception converting the result: " + oEx.toString());
			}
			
			return null;
			
    	}
    	catch (Exception oEx) {
    		WasdiLog.debugLog("GdalUtils.getGdalInfoResult: exception " + oEx.toString());
		}
    	
    	return null;
    }
    
    /**
     * Converts an input file in the output file with WGS84 Projection
     * @param sInputFile File to convert
     * @param sOutputFile Converted file
     */
    public static void convertToWGS84(String sInputFile, String sOutputFile) {
    	convertToWGS84(sInputFile, sOutputFile, null);
    }
    
    /**
     * Converts an input file in the output file with WGS84 Projection
     * @param sInputFile File to convert
     * @param sOutputFile Converted file
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
    		
    		ShellExecReturn oReturn = RunTimeUtils.shellExec(asArgs, true, true, true, true);

    		WasdiLog.debugLog("GdalUtils.convertToWGS84 [gdal]: " + oReturn.getOperationLogs());
    	}
    	catch (Exception oEx) {
    		WasdiLog.debugLog("GdalUtils.convertToWGS84: exception " + oEx.toString());
		}
    }
    
    /**
     * Get the (here static) WKT description of the Molleweide projection
     * @return
     */
    public static String getMollweideProjectionDescription() {
    	return "PROJCS[\"World_Mollweide\",GEOGCS[\"GCS_WGS_1984\",DATUM[\"WGS_1984\",SPHEROID[\"WGS_1984\",6378137,298.257223563]],PRIMEM[\"Greenwich\",0],UNIT[\"Degree\",0.017453292519943295]],PROJECTION[\"Mollweide\"],PARAMETER[\"False_Easting\",0],PARAMETER[\"False_Northing\",0],PARAMETER[\"Central_Meridian\",0],UNIT[\"Meter\",1],AUTHORITY[\"EPSG\",\"54009\"]]";
    }
    
    /**
     * Run a GDAL Parameter
     * @param oMosaicParameter
     * @return
     */
	public static Boolean runGDALMosaic(MosaicParameter oMosaicParameter) {
		
		MosaicSetting oMosaicSetting = (MosaicSetting) oMosaicParameter.getSettings();
		
		// Check parameter
		if (oMosaicSetting == null) {
			WasdiLog.errorLog("Mosaic.runGDALMosaic: parameter is null, return false");
			return false;
		}
		
		if (oMosaicSetting.getSources() == null) {
			WasdiLog.errorLog("Mosaic.runGDALMosaic: sources are null, return false");
			return false;
		}
		
		if (oMosaicSetting.getSources().size() <= 0) {
			WasdiLog.errorLog("Mosaic.runGDALMosaic: sources are empty, return false");
			return false;
		}
		
		String sOuptutFile = oMosaicParameter.getDestinationProductName();
		String sOutputFileFormat = "GeoTIFF";
		
		if (!Utils.isNullOrEmpty(oMosaicSetting.getOutputFormat())) {
			sOutputFileFormat = oMosaicSetting.getOutputFormat();
		}
		
		try {
			String sGdalCommand = "gdal_merge.py";
			
			String sOutputFormat = snapFormat2GDALFormat(sOutputFileFormat);
			Boolean bVrt = false;
			
			if (sOutputFormat.equals("VRT")) {
				sGdalCommand = "gdalbuildvrt";
				bVrt = true;
			}
			
			sGdalCommand = GdalUtils.adjustGdalFolder(sGdalCommand);
			
			ArrayList<String> asArgs = new ArrayList<String>();
			asArgs.add(sGdalCommand);
			
			if (!bVrt) {
				
				WasdiLog.debugLog("NOT Virtual mosaic - set params for gdal_merge.py");
				
				// Output file
				asArgs.add("-o");
				asArgs.add(PathsConfig.getWorkspacePath(oMosaicParameter) + sOuptutFile);
				
				// Output format
				asArgs.add("-of");
				asArgs.add(sOutputFormat);
				
				if (sOutputFormat.equals("GTiff")) {
					asArgs.add("-co");
					asArgs.add("COMPRESS=LZW");
					
					asArgs.add("-co");
					asArgs.add("BIGTIFF=YES");
				}
				
				// Set No Data for input 
				if (oMosaicSetting.getInputIgnoreValue()!= null) {
					asArgs.add("-n");
					asArgs.add(""+oMosaicSetting.getInputIgnoreValue());				
				}

				if (oMosaicSetting.getNoDataValue() != null) {
					asArgs.add("-a_nodata");
					asArgs.add(""+oMosaicSetting.getNoDataValue());				

					asArgs.add("-init");
					asArgs.add(""+oMosaicSetting.getNoDataValue());				

				}
				
				// Pixel Size
				if (oMosaicSetting.getPixelSizeX()>0.0 && oMosaicSetting.getPixelSizeY()>0.0) {
					asArgs.add("-ps");
					asArgs.add(""+ oMosaicSetting.getPixelSizeX());
					asArgs.add("" + oMosaicSetting.getPixelSizeY());
				}				
			}
			else {
				
				WasdiLog.debugLog("Virtual mosaic - set params for gdalbuildvrt");
				
				// Set No Data for input 
				if (oMosaicSetting.getInputIgnoreValue()!= null) {
					asArgs.add("-srcnodata");
					asArgs.add(""+oMosaicSetting.getInputIgnoreValue());				
				}
				
				// Set no data for mosaics 
				if (oMosaicSetting.getNoDataValue() != null) {
					asArgs.add("-vrtnodata");
					asArgs.add(""+oMosaicSetting.getNoDataValue());				
					
					// Could not find this param for vrt..
					//asArgs.add("-init");
					//asArgs.add(""+m_oMosaicSetting.getNoDataValue());
				}
				
			
				asArgs.add(PathsConfig.getWorkspacePath(oMosaicParameter) + sOuptutFile);
			}
						
			// Get Base Path
			String sWorkspacePath = PathsConfig.getWorkspacePath(oMosaicParameter);
			
			// for each product
			for (int iProducts = 0; iProducts<oMosaicSetting.getSources().size(); iProducts ++) {
				
				// Get full path
				String sProductFile = sWorkspacePath+oMosaicSetting.getSources().get(iProducts);
				WasdiLog.debugLog("Mosaic.runGDALMosaic: Adding input Product [" + iProducts +"] = " + sProductFile);
				
				asArgs.add(sProductFile);
			}
			
			// Run the command
			ShellExecReturn oShellExecReturn = RunTimeUtils.shellExec(asArgs, true, true, true, true);
			
			// Is there an output to log?
			if (!Utils.isNullOrEmpty(oShellExecReturn.getOperationLogs())) {
				WasdiLog.debugLog("Mosaic.runGDALMosaic: logs = " + oShellExecReturn.getOperationLogs());
			}
			
			File oOutputFile = new File(sWorkspacePath+sOuptutFile); 
			
			if (oOutputFile.exists()) {
				// Done
				WasdiLog.infoLog("Mosaic.runGDALMosaic: created GDAL file = " + sOuptutFile);				
			}
			else {
				
				WasdiLog.warnLog("Mosaic.runGDALMosaic: output file not found. Retry in a while");
				
    			try {
    				Thread.sleep(WasdiConfig.Current.msWaitAfterChmod);
    			}
    			catch (InterruptedException oEx) {
					Thread.currentThread().interrupt();
					WasdiLog.errorLog("Mosaic.runGDALMosaic: Current thread was interrupted", oEx);
				}				
				
    			
    			File oOutputFile2 = new File(sWorkspacePath+sOuptutFile); 
    			
    			if (oOutputFile2.exists()) {
    				// Done
    				WasdiLog.infoLog("Mosaic.runGDALMosaic: created GDAL file = " + sOuptutFile);				
    			}
    			else {
    				// Error
    				WasdiLog.errorLog("Mosaic.runGDALMosaic: error creating mosaic, the output file  = " + sOuptutFile + " does not exists");
    				return false;
    			}
			}
			
		} 
        catch (Throwable e) {
			WasdiLog.errorLog("Mosaic.runGDALMosaic: Exception generating output Product " + PathsConfig.getWorkspacePath(oMosaicParameter) + sOuptutFile);
			WasdiLog.errorLog("Mosaic.runGDALMosaic: " + e.toString());
			return false;
		}

		return true;
	}
	
	/**
	 * Converts the names used by SNAP to define a file format to the 
	 * equivalent name in GDAL
	 * @param sFormatName Snap Format Name
	 * @return GDAL Format Name
	 */
    public static String snapFormat2GDALFormat(String sFormatName) {

        if (Utils.isNullOrEmpty(sFormatName)) {
            return "";
        }

        switch (sFormatName) {
            case "GeoTIFF":
                return "GTiff";
            case "BEAM-DIMAP":
                return "DIMAP";
            case "VRT":
                return "VRT";
            default:
                return "GTiff";
        }
    }	
}
