package wasdi.io;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ucar.ma2.Array;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.gis.GdalUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.runtime.RunTimeUtils;
import wasdi.shared.utils.runtime.ShellExecReturn;
import wasdi.shared.viewmodels.products.BandViewModel;
import wasdi.shared.viewmodels.products.GeorefProductViewModel;
import wasdi.shared.viewmodels.products.MetadataViewModel;
import wasdi.shared.viewmodels.products.NodeGroupViewModel;
import wasdi.shared.viewmodels.products.ProductViewModel;

public class MeteOceanProductReader extends WasdiProductReader {

	private static final List<String> s_asExcludedVariables = Arrays.asList("longitude", "latitude", "quantile", "season", "surface", "month");
	
	public MeteOceanProductReader(File oProductFile) {
		super(oProductFile);
	}

	@Override
	public ProductViewModel getProductViewModel() {
		
		if (m_oProductFile == null) {
			WasdiLog.warnLog("MeteOceanProductReader.getProductViewModel: pointer to product file is null");
			return null;
		}
		
		GeorefProductViewModel oProductVM = new GeorefProductViewModel();
		
		try {
			
			// set names
			String sFileName = m_oProductFile.getName();
			String sFileNameNoExtension = WasdiFileUtils.getFileNameWithoutLastExtension(sFileName); 
			oProductVM.setFileName(sFileName);
			oProductVM.setName(sFileNameNoExtension);
			oProductVM.setProductFriendlyName(sFileNameNoExtension);
			
			// prepare bands 
			NodeGroupViewModel oNodeGroupVM = new NodeGroupViewModel();
			oNodeGroupVM.setNodeName("Bands");
			
			int iLongitudeLength = extractValueFromShape("longitude") ;
			int iLatitudeLenght = extractValueFromShape("latitude");
			
	    	List<BandViewModel> oBands = new ArrayList<>();
			
			List<Variable> aoVariables = getVariables();
			
			if (aoVariables != null) {
				
				for (Variable oVariable : aoVariables) {
					String sVariableName = oVariable.getShortName();
					if (s_asExcludedVariables.contains(sVariableName)) {
						continue;
					}
					BandViewModel oBandViewModel = new BandViewModel();
					oBandViewModel.setPublished(false);
					oBandViewModel.setGeoserverBoundingBox("");
					oBandViewModel.setHeight(iLatitudeLenght);
					oBandViewModel.setWidth(iLongitudeLength);
					oBandViewModel.setName(sVariableName);
					oBands.add(oBandViewModel);
				}
				
			}
			
			oNodeGroupVM.setBands(oBands);
			oProductVM.setBandsGroups(oNodeGroupVM);
		
		} catch(Exception oEx) {
			WasdiLog.errorLog("MeteOceanProductReader.getProductViewModel. Exception", oEx);
		} 
    	
		return oProductVM;
	}

	@Override
	public String getProductBoundingBox() {
		
		try {
			
			Variable oLatitudeVariable = getVariableByName("latitude");
			Variable oLongitudeVariable = getVariableByName("longitude");
			
			if (oLatitudeVariable == null || oLongitudeVariable == null) {
				return null;
			}
				
			Array oLatitudeArray = oLatitudeVariable.read();
			Array oLongitudeArray = oLongitudeVariable.read();
			
			if (oLatitudeArray == null || oLongitudeArray == null) {
				return null;
			}
		
			Object oLatitudeStorage = oLatitudeArray.getStorage();
			Object oLongitudeStorage = oLongitudeArray.getStorage();
			
			double[] adLatitudeCoordinates = extractExtremeCoordinatesFromStorage(oLatitudeStorage);
			double[] adLongitudeCoordinates = extractExtremeCoordinatesFromStorage(oLongitudeStorage);
			
			double dLatN = adLatitudeCoordinates[1];
			double dLonW = adLongitudeCoordinates[0];
			double dLatS = adLatitudeCoordinates[0];
			double dLonE = adLongitudeCoordinates[1];

			String sBBox = dLatN + "," + dLonW + "," + dLatS + "," + dLonE;

			return sBBox;
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("MeteOceanProductReader.getProductBoudningBox: excteption", oEx);
		}
		
		return null;
	}

	@Override
	public MetadataViewModel getProductMetadataViewModel() {
		return new MetadataViewModel("Metadata");
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
			
			String sGdalInput = "NETCDF:\"" + sInputFile+ "\":" + sBand;
			
			asArgs.add(sGdalInput);
			asArgs.add(sInputPath + sBand + ".vrt");

			// Execute the process
			ShellExecReturn oTranslateReturn = RunTimeUtils.shellExec(asArgs, true, true, true, true);
			WasdiLog.debugLog("MeteOceanProductReader.getFileForPublishBand [gdal]: " + oTranslateReturn.getOperationLogs());
			
			asArgs = new ArrayList<String>();
			sGdalCommand = "gdalwarp";
			sGdalCommand = GdalUtils.adjustGdalFolder(sGdalCommand);
			
			asArgs.add(sGdalCommand);
			// asArgs.add("-geoloc");
			asArgs.add("-t_srs");
			asArgs.add("EPSG:4326");
			asArgs.add("-overwrite");
			asArgs.add(sInputPath + sBand+ ".vrt");
			asArgs.add(sInputPath + sOutputFile);
			
			// Execute the process
			ShellExecReturn oWarpReturn = RunTimeUtils.shellExec(asArgs, true, true, true, true);
			WasdiLog.debugLog("MeteOceanProductReader.getFileForPublishBand [gdal]: " + oWarpReturn.getOperationLogs());
			
			return new File(sInputPath + sOutputFile);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("MeteOceanProductReader.getFileForPublishBand: Exception ", oEx);
			return null;
		}
	}
	
	
	public List<Variable> getVariables() {
		NetcdfFile oFile;
		
		try {
			oFile = NetcdfFiles.open(m_oProductFile.getAbsolutePath());
			Group oRootGroup = oFile.getRootGroup();
			return oRootGroup.getVariables();
		} catch (Exception oEx) {
			WasdiLog.errorLog("MeteOcean.getVariables. Exception getting the variable", oEx);
		}
		
		return null;

	}

	/**
	 * Get the specified variable from the Netcdf file.
	 * @param oFile the Netcdf file
	 * @param sVariableName the name of the variable to be found
	 * @return the variable with the specified name
	 */
	private Variable getVariableByName(String sVariableName) {
		List<Variable> aoVariables = getVariables();
		
		if (aoVariables != null) {			
			for (Variable oVariable : aoVariables) {
				String sVariableShortName = oVariable.getShortName();
				if (sVariableShortName.equals(sVariableName)) {
					return oVariable;
				}
			}
		}
		
		return null;
	}
	
	private double[] extractExtremeCoordinatesFromStorage(Object oStorage) {
		double[] adExtremeCoordinates = new double[] {0F, 0F};

		if (oStorage != null && oStorage instanceof double[]) {
			double[] adFloatArray = (double[]) oStorage;

			adExtremeCoordinates = extractExtremeValuesFromArray(adFloatArray);
		}

		return adExtremeCoordinates;
	}
	
	private double[] extractExtremeValuesFromArray(double[] adDoubleArray) {
		double dMin = 181d;
		double dMax = - 181d;

		if (adDoubleArray != null) {
			for (double dNumber : adDoubleArray) {
				if (dNumber > dMax) {
					dMax = dNumber;
				}

				if (dNumber < dMin) {
					dMin = dNumber;
				}
			}
		}

		return new double[] {dMin, dMax};
	}
	
	private int extractValueFromShape(String sVariableName) {
		
		int iValue = 0;

		Variable oVariable = getVariableByName(sVariableName);
		
		if (oVariable == null) {
			return iValue;
		}

		int[] aiShapeArray = oVariable.getShape();

		if (aiShapeArray != null && aiShapeArray.length > 0) {
			iValue = aiShapeArray[0];
		}

		return iValue;
	}
	
	
	public static void main(String[] args) throws Exception {
		
		WasdiConfig.readConfig("C:/temp/wasdi/wasdiLocalTESTConfig.json");
		
		String sPath = "C:/Users/valentina.leone/Desktop/WORK/Return/test/hindcast_hs_1979_2005__nseastates_over_p95__seasonalmean.nc";
		// NetcdfFile oFile = NetcdfFiles.open("C:/Users/valentina.leone/Desktop/WORK/Return/104435/wave_dataset/hindcast_hs_1979_2005__nseastates_over_p95__seasonalmean.nc");
		MeteOceanProductReader oReader = new MeteOceanProductReader(new File(sPath));
		System.out.println(oReader.getProductBoundingBox());
		
		
		oReader.getFileForPublishBand("hs", "hs_layer");
		
	}
	

}
