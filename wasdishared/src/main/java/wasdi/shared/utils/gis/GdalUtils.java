package wasdi.shared.utils.gis;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import wasdi.shared.config.PathsConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.parameters.MosaicParameter;
import wasdi.shared.parameters.settings.MosaicSetting;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.ProcessWorkspaceLogger;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
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

	public static GdalInfoResult getGdalInfoResult(String sFilePath, boolean bCheckFileExists) {
		return getGdalInfoResult(new File(sFilePath), bCheckFileExists);
	}

	public static GdalInfoResult getGdalInfoResult(File oFile) { 
		return getGdalInfoResult(oFile,true);
	}

    
    /**
     * Get the output of GDALInfo on the file
     * @param oFile File to read with gdalinfo
     * @return GdalInfoResult filled or null in case of problems
     */
    @SuppressWarnings("unchecked")
	public static GdalInfoResult getGdalInfoResult(File oFile, boolean bCheckFileExists) {
    	try {
    		
    		// Domain check
    		if (oFile == null) {
    			WasdiLog.debugLog("GdalUtils.getGdalInfoResult: File is null, return null");
    			return null;
    		}

			if (bCheckFileExists) {
				if (oFile.exists()==false) {
					WasdiLog.debugLog("GdalUtils.getGdalInfoResult: File " + oFile.getPath() + " does not exists, return null");
					return null;
				}
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
							
							if (oBand.containsKey("description")) {
								oBandInfo.description = (String) oBand.get("description");
							}							
							
							if (oBand.containsKey("noDataValue")) {
								try {
									oBandInfo.noDataValue = (double) oBand.get("noDataValue");	
								}
								catch (Exception oEx) {
									WasdiLog.warnLog("GdalUtils.getGdalInfoResult: error converting noDataValue, assuming NaN");
									oBandInfo.noDataValue = Double.NaN;
								}
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
	public static Boolean runGDALMosaic(MosaicParameter oMosaicParameter, ProcessWorkspaceLogger oPWLogger) {
		
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
		
		String sOutputFile = oMosaicParameter.getDestinationProductName();
		String sOutputFileFormat = "GeoTIFF";
		
		if (!Utils.isNullOrEmpty(oMosaicSetting.getOutputFormat())) {
			sOutputFileFormat = oMosaicSetting.getOutputFormat();
		}
		
		// Get Base Path
		String sWorkspacePath = PathsConfig.getWorkspacePath(oMosaicParameter);
		// Output of the final gdal shell exec
		ShellExecReturn oShellExecReturn = null;
		
		
		try {
			
			ArrayList<String> asInputProducts = new ArrayList<String>();
			
			// for each product
			for (int iProducts = 0; iProducts<oMosaicSetting.getSources().size(); iProducts ++) {
				
				// Get full path
				String sProductFile = sWorkspacePath+oMosaicSetting.getSources().get(iProducts);
								
				// Check if the file exists
				File oFile = new File(sProductFile);
								
				// This is not promising
				if (oFile.exists()) {
					WasdiLog.debugLog("GdalUtils.runGDALMosaic: Adding input Product [" + iProducts +"] = " + sProductFile);
					asInputProducts.add(sProductFile);
				}
			}			
			
			
			// Get the output format
			String sOutputFormat = snapFormat2GDALFormat(sOutputFileFormat);
			
			if (sOutputFormat.equals(GdalFileFormats.DIMAP)) {
				// For DIMAP we use gdal merge
				
				String sGdalCommand = GdalUtils.adjustGdalFolder("gdal_merge.py");
				ArrayList<String> asArgs = new ArrayList<String>();
				asArgs.add(sGdalCommand);
				
				WasdiLog.debugLog("Mosaic.runGDALMosaic: " + GdalFileFormats.DIMAP + " - Set params for gdal_merge.py");
				
				// Output file
				asArgs.add("-o");
				asArgs.add(PathsConfig.getWorkspacePath(oMosaicParameter) + sOutputFile);
				
				// Output format
				asArgs.add("-of");
				asArgs.add(sOutputFormat);
				
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
				
				// Input Produts
				asArgs.addAll(asInputProducts);
				
				// Run the command
				oShellExecReturn = RunTimeUtils.shellExec(asArgs, true, true, true, true);
								
			}
			else if (sOutputFormat.equals(GdalFileFormats.VRT)) {
				
				String sGdalCommand = GdalUtils.adjustGdalFolder("gdalbuildvrt");
				ArrayList<String> asArgs = new ArrayList<String>();
				asArgs.add(sGdalCommand);
				
				WasdiLog.debugLog("Mosaic.runGDALMosaic: Virtual mosaic - set params for gdalbuildvrt");
				
				// Set No Data for input 
				if (oMosaicSetting.getInputIgnoreValue()!= null) {
					asArgs.add("-srcnodata");
					asArgs.add(""+oMosaicSetting.getInputIgnoreValue());				
				}
				
				// Set no data for mosaics 
				if (oMosaicSetting.getNoDataValue() != null) {
					asArgs.add("-vrtnodata");
					asArgs.add(""+oMosaicSetting.getNoDataValue());
				}
				
			
				asArgs.add(PathsConfig.getWorkspacePath(oMosaicParameter) + sOutputFile);
				
				// Add input products 
				asArgs.addAll(asInputProducts);
				
				// Run the command
				oShellExecReturn = RunTimeUtils.shellExec(asArgs, true, true, true, true);
								
			}
			else {
				WasdiLog.debugLog("Mosaic.runGDALMosaic:: " + GdalFileFormats.GTiff + " - Set params for COG GeoTiff");

	            String sTempVrtFile = sWorkspacePath + sOutputFile + ".tmp.vrt";
	            String sFinalOutputFile = sWorkspacePath + sOutputFile;

	            // Build a VRT mosaic
	            ArrayList<String> asVrtArgs = new ArrayList<>();
	            asVrtArgs.add(GdalUtils.adjustGdalFolder("gdalbuildvrt"));

	            if (oMosaicSetting.getInputIgnoreValue() != null) {
	                asVrtArgs.add("-srcnodata");
	                asVrtArgs.add("" + oMosaicSetting.getInputIgnoreValue());
	            }

	            if (oMosaicSetting.getNoDataValue() != null) {
	                asVrtArgs.add("-vrtnodata");
	                asVrtArgs.add("" + oMosaicSetting.getNoDataValue());
	            }

	            asVrtArgs.add(sTempVrtFile);

	            // Add source files
	            asVrtArgs.addAll(asInputProducts);

	            // Make the virtual mosaic
	            ShellExecReturn oVrtExecReturn = RunTimeUtils.shellExec(asVrtArgs, true, true, true, true);
	            
	            if (oVrtExecReturn!=null) {
		            WasdiLog.debugLog("Mosaic.runGDALMosaic: COG virtual mosaic output = " + oVrtExecReturn.getOperationLogs());
					if (oPWLogger!=null) {
						oPWLogger.log("Mosaic logs = " + oVrtExecReturn.getOperationLogs());
					}	            	            	
	            }

	            // Translate VRT to COG
	            ArrayList<String> asTranslateArgs = new ArrayList<>();
	            asTranslateArgs.add(GdalUtils.adjustGdalFolder("gdal_translate"));
	            asTranslateArgs.add("-of");
	            asTranslateArgs.add("COG");
	            
	            // COG Creation Options
	            asTranslateArgs.add("-co");
	            asTranslateArgs.add("COMPRESS=LZW");
	            asTranslateArgs.add("-co");
	            asTranslateArgs.add("BIGTIFF=YES");
	            asTranslateArgs.add("-co");
	            asTranslateArgs.add("NUM_THREADS=ALL_CPUS");

	            // Input VRT and Output COG
	            asTranslateArgs.add(sTempVrtFile);
	            asTranslateArgs.add(sFinalOutputFile);

	            oShellExecReturn = RunTimeUtils.shellExec(asTranslateArgs, true, true, true, true);
	            
	            // Clean up temporary VRT file
	            WasdiFileUtils.deleteFile(sTempVrtFile);
			}
			
			if (oShellExecReturn!=null) {
				// Is there an output to log?
				if (!Utils.isNullOrEmpty(oShellExecReturn.getOperationLogs())) {
					WasdiLog.debugLog("Mosaic.runGDALMosaic: logs = " + oShellExecReturn.getOperationLogs());
					if (oPWLogger!=null) {
						oPWLogger.log("Mosaic logs = " + oShellExecReturn.getOperationLogs());
					}
				}				
			}
			else {
				WasdiLog.errorLog("Mosaic.runGDALMosaic: oShellExecReturn is null");
			}
			
			File oOutputFile = new File(sWorkspacePath+sOutputFile); 
			
			if (oOutputFile.exists()) {
				// Done
				WasdiLog.infoLog("Mosaic.runGDALMosaic: created GDAL file = " + sOutputFile);				
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
				
    			
    			File oOutputFile2 = new File(sWorkspacePath+sOutputFile); 
    			
    			if (oOutputFile2.exists()) {
    				// Done
    				WasdiLog.infoLog("Mosaic.runGDALMosaic: created GDAL file = " + sOutputFile);				
    			}
    			else {
    				// Error
    				WasdiLog.errorLog("Mosaic.runGDALMosaic: error creating mosaic, the output file  = " + sOutputFile + " does not exists");
    				if (oPWLogger!=null) {
    					oPWLogger.log("Mosaic error creating mosaic, the output file does not exists ");
    				}    				
    				return false;
    			}
			}
			
		} 
        catch (Throwable e) {
			WasdiLog.errorLog("Mosaic.runGDALMosaic: Exception generating output Product " + PathsConfig.getWorkspacePath(oMosaicParameter) + sOutputFile);
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
                return GdalFileFormats.GTiff;
            case "BEAM-DIMAP":
                return GdalFileFormats.DIMAP;
            case "VRT":
                return GdalFileFormats.VRT;
            default:
                return GdalFileFormats.GTiff;
        }
    }	
}
