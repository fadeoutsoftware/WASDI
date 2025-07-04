package wasdi.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;

import ucar.ma2.Array;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.products.BandViewModel;
import wasdi.shared.viewmodels.products.GeorefProductViewModel;
import wasdi.shared.viewmodels.products.MetadataViewModel;
import wasdi.shared.viewmodels.products.NodeGroupViewModel;
import wasdi.shared.viewmodels.products.ProductViewModel;

public class CdsNetcdfProductReader extends WasdiProductReader {
	
	private static final String s_sLongitude = "longitude";
	private static final String s_sLatitude = "latitude";
	private static final String s_sTime = "valid_time";
	private static final String s_sExpver = "expver";
	private static final String s_sNumber = "number";
	
	public CdsNetcdfProductReader(File oProductFile) {
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

			Set<String> asExcludedVariableSet = new HashSet<>(Arrays.asList(s_sLongitude, s_sLatitude, s_sTime, s_sExpver, s_sNumber));

			List<Variable> aoVariablesList = oFile.getVariables();

			List<BandViewModel> oBands = new ArrayList<>();
			int iLatitudeLength = 0;
			int iLongitudeLength = 0;
			List<Integer> aiTimeHoursList = new ArrayList<Integer>();

			for (Variable oVariable : aoVariablesList) {
				String sVariableShortName = oVariable.getShortName();

				if (sVariableShortName.equalsIgnoreCase(s_sLongitude)) {
					iLongitudeLength = extractValueFromShape(oVariable);
				}

				if (sVariableShortName.equalsIgnoreCase(s_sLatitude)) {
					iLatitudeLength = extractValueFromShape(oVariable);
				}

				if (sVariableShortName.equalsIgnoreCase(s_sTime)) {
					long[] alHoursArray = (long[]) (oVariable.read().getStorage());
					for (long lTimeStamp : alHoursArray) {
						LocalDateTime oUTCDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(lTimeStamp), ZoneId.of("UTC"));
						aiTimeHoursList.add(oUTCDateTime.getHour());
					}
				}
			}

			for (Variable oVariable : aoVariablesList) {
				String sVariableShortName = oVariable.getShortName();
				String sDescription = oVariable.getDescription();
				if (!asExcludedVariableSet.contains(sVariableShortName)) {
					for (Integer iTimeHour : aiTimeHoursList) {
						// Create the single band representing the shape
						BandViewModel oBandViewModel = new BandViewModel();
						oBandViewModel.setPublished(false);
						oBandViewModel.setGeoserverBoundingBox("");
						oBandViewModel.setHeight(iLatitudeLength);
						oBandViewModel.setWidth(iLongitudeLength);
						oBandViewModel.setPublished(false);
						
						if (!Utils.isNullOrEmpty(sDescription))
							oBandViewModel.setName(sDescription.replaceAll("[\\W]", "_") + "_" + String.format("%02d" , iTimeHour) + "hh");
						else if (!Utils.isNullOrEmpty(sVariableShortName))
							oBandViewModel.setName(sVariableShortName.replaceAll("[\\W]", "_") + "_" + String.format("%02d" , iTimeHour) + "hh");
						else
							oBandViewModel.setName("null_" + String.format("%02d" , iTimeHour) + "hh");
						oBands.add(oBandViewModel);
					}
				}
			}

			oNodeGroupViewModel.setBands(oBands);
			oRetViewModel.setBandsGroups(oNodeGroupViewModel);
		} catch (IOException oEx) {
			WasdiLog.debugLog("CdsNetcdfProductReader.getProductViewModel: exception reading the shape file: " + oEx.toString());
		} finally {
			if (oFile != null) {
				try {
					oFile.close();
				} catch (IOException oEx) {
					WasdiLog.errorLog("CdsNetcdfProductReader.getProductViewModel: exception closing the shape file: ", oEx);
				}
			}
		}

		return oRetViewModel;
	}

	private static int extractValueFromShape(Variable oVariable) {
		int iValue = 0;

		int[] aiShapeArray = oVariable.getShape();

		if (aiShapeArray != null && aiShapeArray.length > 0) {
			iValue = aiShapeArray[0];
		}

		return iValue;
	}

	@Override
	public String getProductBoundingBox() {
		if (m_oProductFile == null) return null;

		try {
			return extractBboxFromFile(m_oProductFile.getAbsolutePath());
		} catch (IOException e) {
			WasdiLog.debugLog("CdsNetcdfProductReader.getProductBoundingBox: exception reading the shape file: " + e.toString());
		}

		try {
			return extractBboxFromFile(m_oProductFile.getAbsolutePath());
		} catch (IOException e) {
			WasdiLog.debugLog("CdsNetcdfProductReader.getProductBoundingBox: exception reading the shape file: " + e.toString());
		}

		return null;
	}

	private static String extractBboxFromFile(String sFileName) throws IOException {
		NetcdfFile oFile = NetcdfFiles.open(sFileName);

		Variable oLatitudeVariable = getVariableByName(oFile, s_sLatitude);
		Variable oLongitudeVariable = getVariableByName(oFile, s_sLongitude);

		Array oLatArray = getArrayFromVariable(oLatitudeVariable);
		Object oLatStorage = getStorageFromArray(oLatArray);
		float[] afLatitudeCoordinates = extractExtremeCoordinatesFromStorage(oLatStorage);

		Array oLonArray = getArrayFromVariable(oLongitudeVariable);
		Object oLonStorage = getStorageFromArray(oLonArray);
		float[] afLongitudeCoordinates = extractExtremeCoordinatesFromStorage(oLonStorage);

		float fLatN = afLatitudeCoordinates[1];
		float fLonW = afLongitudeCoordinates[0];
		float fLatS = afLatitudeCoordinates[0];
		float fLonE = afLongitudeCoordinates[1];

		String sBBox = fLatN + "," + fLonW + "," + fLatS + "," + fLonE;

		return sBBox;
	}

	private static float[] extractExtremeCoordinatesFromStorage(Object oStorage) {
		float[] afExtremeCoordinates = new float[] {0F, 0F};

		if (oStorage != null && oStorage instanceof float[]) {
			float[] afFloatArray = (float[]) oStorage;

			afExtremeCoordinates = extractExtremeValuesFromArray(afFloatArray);
		}

		return afExtremeCoordinates;
	}

	private static float[] extractExtremeValuesFromArray(float[] afFloatArray) {
		float fMinf = 181F;
		float fMaxf = - 181F;

		if (afFloatArray != null) {
			for (float fNumber : afFloatArray) {
				if (fNumber > fMaxf) {
					fMaxf = fNumber;
				}

				if (fNumber < fMinf) {
					fMinf = fNumber;
				}
			}
		}

		return new float[] {fMinf, fMaxf};
	}

	/**
	 * Get the specified variable from the Netcdf file.
	 * @param oFile the Netcdf file
	 * @param sVariableName the name of the variable to be found
	 * @return the variable with the specified name
	 */
	private static Variable getVariableByName(NetcdfFile oFile, String sVariableName) {
		Group oRootGroup = oFile.getRootGroup();
		List<Group> aoRootGroupGroups = oRootGroup.getGroups();

		for (Group oGroup : aoRootGroupGroups) {
			if (oGroup.getShortName().equalsIgnoreCase("PRODUCT")) {

				List<Variable> aoVariableList = oGroup.getVariables();
				for (Variable oVariable : aoVariableList) {
					String oVariableShortName = oVariable.getShortName();
					if (oVariableShortName.equalsIgnoreCase(sVariableName)) {
						return oVariable;
					}
				}
			}
		}

		return null;
	}

	private static Array getArrayFromVariable(Variable oVariable) throws IOException {
		Array oArray = null;

		if (oVariable != null) {
			oArray = oVariable.read();
		}

		return oArray;
	}

	private static Object getStorageFromArray(Array oArray) {
		Object oObject = null;

		if (oArray != null) {
			oObject = oArray.getStorage();
		}

		return oObject;
	}

	@Override
	public MetadataViewModel getProductMetadataViewModel() {
		return new MetadataViewModel("Metadata");
	}

	@Override
	public Product getSnapProduct() {
		Product oProduct = super.getSnapProduct();

		if (oProduct != null && oProduct.getStartTime() == null) {
			oProduct.setStartTime(ProductData.UTC.create(new java.util.Date(), 0));
		}

		return oProduct;
	}
	
	@Override
	public File getFileForPublishBand(String sBand, String sLayerId, String sPlatform) {
		return null;
	}


}

