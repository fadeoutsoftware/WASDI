package wasdi.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ucar.ma2.Array;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.ZipFileUtils;
import wasdi.shared.utils.gis.GdalUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.runtime.RunTimeUtils;
import wasdi.shared.utils.runtime.ShellExecReturn;
import wasdi.shared.viewmodels.products.BandViewModel;
import wasdi.shared.viewmodels.products.GeorefProductViewModel;
import wasdi.shared.viewmodels.products.MetadataViewModel;
import wasdi.shared.viewmodels.products.NodeGroupViewModel;
import wasdi.shared.viewmodels.products.ProductViewModel;

public class Sentinel5ProductReader extends WasdiProductReader {

	public Sentinel5ProductReader(File oProductFile) {
		super(oProductFile);
	}

	@Override
	public ProductViewModel getProductViewModel() {

		if (m_oProductFile == null) return null;

    	// Create the return value
    	GeorefProductViewModel oRetViewModel = null;
    	NetcdfFile oFile = null;

		try {
			 oFile = NetcdfFiles.open(m_oProductFile.getAbsolutePath());

	    	// Create the Product View Model
	    	oRetViewModel = new GeorefProductViewModel();

        	// Set name values
        	oRetViewModel.setFileName(m_oProductFile.getName());
        	oRetViewModel.setName(WasdiFileUtils.getFileNameWithoutLastExtension(m_oProductFile.getName()));
        	oRetViewModel.setProductFriendlyName(oRetViewModel.getName());

	    	NodeGroupViewModel oNodeGroupViewModel = new NodeGroupViewModel();
        	oNodeGroupViewModel.setNodeName("Bands");

    		Set<String> excludedVariableSet = new HashSet<>(Arrays.asList("scanline", "ground_pixel", "time", "corner",
    				"latitude", "longitude", "delta_time", "time_utc", "qa_value", "layer"));

    		Group rootGroup = oFile.getRootGroup();
    		List<Group> rootGroupGroups = rootGroup.getGroups();

        	List<BandViewModel> oBands = new ArrayList<>();

    		for (Group g : rootGroupGroups) {
    			if (g.getShortName().equalsIgnoreCase("PRODUCT")) {

    				List<Variable> variableList = g.getVariables();
    				for (Variable v : variableList) {
    					String variableShortName = v.getShortName();
    					if (!excludedVariableSet.contains(variableShortName)) {
    						int iWidth = 0;
    						int iHeight = 0;
    						// [time = 1;, scanline = 358;, ground_pixel = 450;]
    						int[] shapeArray = v.getShape();

    						if (shapeArray == null) {
    							continue;
    						}

							if (shapeArray.length < 3) {
								continue;
							}

							iWidth = shapeArray[1];
							iHeight = shapeArray[2];
    			        	
    			        	// Create the single band representing the shape
    			        	BandViewModel oBandViewModel = new BandViewModel();
    			        	oBandViewModel.setPublished(false);
    			        	oBandViewModel.setGeoserverBoundingBox("");
    			        	oBandViewModel.setHeight(iHeight);
    			        	oBandViewModel.setWidth(iWidth);
    			        	oBandViewModel.setPublished(false);
    			        	oBandViewModel.setName(variableShortName);

    			        	oBands.add(oBandViewModel);
    					}
    				}
    			}	
    		}

        	oNodeGroupViewModel.setBands(oBands);
	    	oRetViewModel.setBandsGroups(oNodeGroupViewModel);
		} catch (Exception e) {
    		WasdiLog.debugLog("Sentinel5ProductReader.getProductViewModel: exception reading the shape file: " + e.toString());
		} finally {
			if (oFile != null) {
				try {
					oFile.close();
				} catch (IOException oEx) {
		    		WasdiLog.errorLog("Sentinel5ProductReader.getProductViewModel: exception reading the shape file: ", oEx);
				}
			}
		}
		
    	return oRetViewModel;
	}

	@Override
	public String getProductBoundingBox() {

		if (m_oProductFile == null) return null;

		try {
			return extractBboxFromFile(m_oProductFile.getAbsolutePath());
		} catch (Exception e) {
    		WasdiLog.debugLog("Sentinel5ProductReader.getProductBoundingBox: exception reading the shape file: " + e.toString());

    		return null;
		}
	}

	@Override
	public MetadataViewModel getProductMetadataViewModel() {
		
		return new MetadataViewModel("Metadata");
	}

	/**
	 * Extract the bounding box of a Netcdf file (i.e. 45.9,8.5,45.7,8.7).
	 * @param fileName the absolute filename of the Netcdf file 
	 * @return the bounding box
	 * @throws IOException in case of issues reading the file
	 */
	private static String extractBboxFromFile(String fileName) throws IOException {
		NetcdfFile oFile = NetcdfFiles.open(fileName);

		Variable latitudeVariable = getVariableByName(oFile, "latitude");
		Variable longitudeVariable = getVariableByName(oFile, "longitude");

		Array latArray = getArrayFromVariable(latitudeVariable);
		Object latStorage = getStorageFromArray(latArray);
		float[] latitudeCoordinates = extractExtremeCoordinatesFromStorage(latStorage);

		Array lonArray = getArrayFromVariable(longitudeVariable);
		Object lonStorage = getStorageFromArray(lonArray);
		float[] longitudeCoordinates = extractExtremeCoordinatesFromStorage(lonStorage);

		float fLatN = latitudeCoordinates[1];
		float fLonW = longitudeCoordinates[0];
		float fLatS = latitudeCoordinates[0];
		float fLonE = longitudeCoordinates[1];

		String sBBox = fLatN + "," + fLonW + "," + fLatS + "," + fLonE;

		return sBBox;
	}

	/**
	 * Get the specified variable from the Netcdf file.
	 * @param oFile the Netcdf file
	 * @param variableName the name of the variable to be found
	 * @return the variable with the specified name
	 */
	private static Variable getVariableByName(NetcdfFile oFile, String variableName) {
		Group rootGroup = oFile.getRootGroup();
		List<Group> rootGroupGroups = rootGroup.getGroups();

		for (Group g : rootGroupGroups) {
			if (g.getShortName().equalsIgnoreCase("PRODUCT")) {

				List<Variable> variableList = g.getVariables();
				for (Variable v : variableList) {
					String variableShortName = v.getShortName();
					if (variableShortName.equalsIgnoreCase(variableName)) {
						return v;
					}
				}
			}
		}

		return null;
	}

	/**
	 * Get the inner array of the variable.
	 * @param variable the variable containing the data-set
	 * @return the array containing the data-set
	 * @throws IOException in case of any issues reading the data-set
	 */
	private static Array getArrayFromVariable(Variable variable) throws IOException {
		Array array = null;

		if (variable != null) {
			array = variable.read();
		}

		return array;
	}

	/**
	 * Get the storage object from an array.
	 * @param array the array
	 * @return the storage object
	 */
	private static Object getStorageFromArray(Array array) {
		Object o = null;

		if (array != null) {
			o = array.getStorage();
		}

		return o;
	}

	/**
	 * Get the extreme coordinates (min & max) from the storage object.
	 * @param storage the storage object
	 * @return the extreme coordinates of the storage object
	 */
	private static float[] extractExtremeCoordinatesFromStorage(Object storage) {
		float[] extremeCoordinates = new float[] {0F, 0F};

		if (storage != null && storage instanceof float[]) {
			float[] floatArray = (float[]) storage;

			extremeCoordinates = extractExtremeValuesFromArray(floatArray);
		}

		return extremeCoordinates;
	}

	/**
	 * Get the extreme values (min & max) of a float array.
	 * @param floatArray the array
	 * @return a float array containing the min & max values of the initial array
	 */
	private static float[] extractExtremeValuesFromArray(float[] floatArray) {
		float minf = 181F;
		float maxf = - 181F;

		if (floatArray != null) {
			for (float f : floatArray) {
				if (f > maxf) {
					maxf = f;
				}

				if (f < minf) {
					minf = f;
				}
			}
		}

		return new float[] {minf, maxf};
	}

	@Override
	public String adjustFileAfterDownload(String sDownloadedFileFullPath, String sFileNameFromProvider) {
		
		String sFileName = sDownloadedFileFullPath;
		
		try {
			if (sFileNameFromProvider.startsWith("S5P") && sFileNameFromProvider.toLowerCase().endsWith(".zip")) {
				WasdiLog.debugLog("Sentinel5ProductReader.adjustFileAfterDownload: File is a Sentinel 5P image, start unzip");
				String sDownloadPath = new File(sDownloadedFileFullPath).getParentFile().getPath();
				
				String sTargetDirectoryPath = sDownloadPath;

				File oSourceFile = new File(sDownloadedFileFullPath);
				File oTargetDirectory = new File(sTargetDirectoryPath);
				ZipFileUtils.cleanUnzipFile(oSourceFile, oTargetDirectory);

				//String sFolderName = sDownloadPath + File.separator + sFileNameFromProvider.replace(".zip", "");
				//WasdiLog.debugLog("Sentinel5ProductReader.adjustFileAfterDownload: Unzip done, folder name: " + sFolderName);
				
				sFileName = sDownloadPath + File.separator + sFileNameFromProvider.replace(".zip", "");
				
				if (!sFileName.toUpperCase().endsWith(".NC")) {
					WasdiLog.debugLog("Sentinel5ProductReader.adjustFileAfterDownload: missing .nc extension at the end of the file, add it");
					sFileName = sFileName + ".nc";
				}
				
				WasdiLog.debugLog("Sentinel5ProductReader.adjustFileAfterDownload: File Name: " + sFileName);
				
				String sCdlFileName = sFileName.replace(".nc", ".cdl");
					
				File oCdlFile = new File(sCdlFileName);
				
				if (oCdlFile.exists()) {
					boolean bIsFileDeleted = oCdlFile.delete();
					WasdiLog.debugLog("Sentinel5ProductReader.adjustFileAfterDownload. Result of the deletion of the cdl file " + sCdlFileName + ": " + bIsFileDeleted);
				}
				else {
					WasdiLog.debugLog("Sentinel5ProductReader.adjustFileAfterDownload: impossible to find cdl file " + oCdlFile.getPath());
				}

				m_oProductFile = new File(sFileName);
			}			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("Sentinel5ProductReader.adjustFileAfterDownload: error ", oEx);
		}
		
		
		return sFileName;
	}

	@Override
	public File getFileForPublishBand(String sBand, String sLayerId) {
		try {
			
			String sInputFile = m_oProductFile.getAbsolutePath();
			String sOutputFile = sLayerId + ".tif";			
			
			String sInputPath = "";
			File oFile = new File(sInputFile);
			sInputPath = oFile.getParentFile().getPath();
			if (!sInputPath.endsWith("/")) sInputPath += "/";
			
			String sGdalCommand = "gdal_translate";
			sGdalCommand = GdalUtils.adjustGdalFolder(sGdalCommand);
			
			ArrayList<String> asArgs = new ArrayList<String>();
			asArgs.add(sGdalCommand);
			
			asArgs.add("-co");
			asArgs.add("WRITE_BOTTOMUP=NO");
			
			asArgs.add("-of");
			asArgs.add("VRT");
			
			String sGdalInput = "NETCDF:\""+sInputFile+"\":/PRODUCT/"+sBand;
			
			asArgs.add(sGdalInput);
			asArgs.add(sInputPath + sBand + ".vrt");

			// Execute the process
			ShellExecReturn oTranslateReturn = RunTimeUtils.shellExec(asArgs, true, true, true, true);
			WasdiLog.debugLog("Publishband.convertS5PtoGeotiff [gdal]: " + oTranslateReturn.getOperationLogs());
			
			asArgs = new ArrayList<String>();
			sGdalCommand = "gdalwarp";
			sGdalCommand = GdalUtils.adjustGdalFolder(sGdalCommand);
			
			asArgs.add(sGdalCommand);
			asArgs.add("-geoloc");
			asArgs.add("-t_srs");
			asArgs.add("EPSG:4326");
			asArgs.add("-overwrite");
			asArgs.add(sInputPath + sBand+ ".vrt");
			asArgs.add(sInputPath + sOutputFile);
			
			// Execute the process
			ShellExecReturn oWarpReturn = RunTimeUtils.shellExec(asArgs, true, true, true, true);
			WasdiLog.debugLog("Publishband.convertSentine5PtoGeotiff [gdal]: " + oWarpReturn.getOperationLogs());
			
			return new File(sInputPath + sOutputFile);
		}
		catch (Exception oEx) {
			
			WasdiLog.debugLog("Publishband.convertSentinel5PtoGeotiff: Exception = " + oEx.toString());
			
			return null;
		}
	}

}
