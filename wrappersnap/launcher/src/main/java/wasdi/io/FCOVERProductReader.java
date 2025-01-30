package wasdi.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ucar.ma2.Array;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.products.BandViewModel;
import wasdi.shared.viewmodels.products.GeorefProductViewModel;
import wasdi.shared.viewmodels.products.MetadataViewModel;
import wasdi.shared.viewmodels.products.NodeGroupViewModel;
import wasdi.shared.viewmodels.products.ProductViewModel;

public class FCOVERProductReader extends WasdiProductReader {
	
	private static final List<String> s_asExcludedVariables = Arrays.asList("lon", "lat", "crs");

	
	public FCOVERProductReader(File oProductFile) {
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
			
			int iLongitudeLength = extractValueFromShape("lon") ;
			int iLatitudeLenght = extractValueFromShape("lat");
			
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
			Variable oLatitudeVariable = getVariableByName("lat");
			Variable oLongitudeVariable = getVariableByName("lon");
			
			if (oLatitudeVariable == null || oLongitudeVariable == null) {
				return "";
			}
				
			Array oLatitudeArray;
			
				oLatitudeArray = oLatitudeVariable.read();
			
			Array oLongitudeArray = oLongitudeVariable.read();
			
			if (oLatitudeArray == null || oLongitudeArray == null) {
				return "";
			}
		
			Object oLatitudeStorage = oLatitudeArray.getStorage();
			Object oLongitudeStorage = oLongitudeArray.getStorage();
			
			double[] adLatitudeCoordinates = extractExtremeCoordinatesFromStorage(oLatitudeStorage);
			double[] adLongitudeCoordinates = extractExtremeCoordinatesFromStorage(oLongitudeStorage);
			
			double dLatN = adLatitudeCoordinates[1];
			double dLonW = adLongitudeCoordinates[0];
			double dLatS = adLatitudeCoordinates[0];
			double dLonE = adLongitudeCoordinates[1];
	
			return dLatN + "," + dLonW + "," + dLatS + "," + dLonE;	
			
		} catch (IOException oEx) {
			WasdiLog.errorLog("FCOVER.getProductBoudingBox. Error ", oEx);
		}
		
		return "";
	}

	@Override
	public MetadataViewModel getProductMetadataViewModel() {
		return new MetadataViewModel("Metadata");
	}

	@Override
	public String adjustFileAfterDownload(String sDownloadedFileFullPath, String sFileNameFromProvider) {
		return sDownloadedFileFullPath;
		
	}

	@Override
	public File getFileForPublishBand(String sBand, String sLayerId) {
		return null;
	}
	
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
	
	private List<Variable> getVariables() {
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
	
	private  double[] extractExtremeCoordinatesFromStorage(Object oStorage) {
		double[] afExtremeCoordinates = new double[] {0F, 0F};

		if (oStorage != null && oStorage instanceof double[]) {
			double[] afFloatArray = (double[]) oStorage;

			afExtremeCoordinates = extractExtremeValuesFromArray(afFloatArray);
		}

		return afExtremeCoordinates;
	}

	private static double[] extractExtremeValuesFromArray(double[] afFloatArray) {
		double fMinf = 181d;
		double fMaxf = - 181d;

		if (afFloatArray != null) {
			for (double fNumber : afFloatArray) {
				if (fNumber > fMaxf) {
					fMaxf = fNumber;
				}

				if (fNumber < fMinf) {
					fMinf = fNumber;
				}
			}
		}
		return new double[] {fMinf, fMaxf};
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

}
