package wasdi.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;

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

public class CmNcProductReader extends WasdiProductReader {

	public CmNcProductReader(File oProductFile) {
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

			Set<String> asExcludedVariables = new HashSet<>(Arrays.asList("longitude", "lon", "latitude", "lat", "time"));

			List<Variable> aoVariables = oFile.getVariables();

			List<BandViewModel> oBands = new ArrayList<>();
			int iLatitudeLength = 0;
			int iLongitudeLength = 0;
			List<Integer> aiTimeHours = Collections.emptyList();

			for (Variable oVariable : aoVariables) {
				String sVariableShortName = oVariable.getShortName();

				if (sVariableShortName.equalsIgnoreCase("lon") || sVariableShortName.equalsIgnoreCase("longitude")) {
					iLongitudeLength = extractValueFromShape(oVariable);
				}

				if (sVariableShortName.equalsIgnoreCase("lat") || sVariableShortName.equalsIgnoreCase("latitude")) {
					iLatitudeLength = extractValueFromShape(oVariable);
				}

				if (sVariableShortName.equalsIgnoreCase("time")) {
					Object aoHours = (oVariable.read().getStorage());
					int[] aiIntHours = new int[0];

					if (aoHours instanceof int[]) {
						aiIntHours = (int[]) aoHours;
					} else if (aoHours instanceof double[]) {
						double[] adDoubleHours = (double[]) aoHours;

						aiIntHours = new int[adDoubleHours.length];
						for (int i = 0 ; i < adDoubleHours.length; i++) {
							aiIntHours[i] = (int) adDoubleHours[i];
						}
					} else if (aoHours instanceof float[]) {
						float[] afFloatHours = (float[]) aoHours;

						aiIntHours = new int[afFloatHours.length];
						for (int i = 0 ; i < afFloatHours.length; i++) {
							aiIntHours[i] = (int) afFloatHours[i];
						}
					}

					aiTimeHours = IntStream.of(aiIntHours).map(i -> i % 24).boxed().collect(Collectors.toList());
				}
			}

			for (Variable oVariable : aoVariables) {
				String sVariableShortName = oVariable.getShortName();
				if (!asExcludedVariables.contains(sVariableShortName)) {
					for (Integer iTimeHour : aiTimeHours) {
						// Create the single band representing the shape
						BandViewModel oBandViewModel = new BandViewModel();
						oBandViewModel.setPublished(false);
						oBandViewModel.setGeoserverBoundingBox("");
						oBandViewModel.setHeight(iLatitudeLength);
						oBandViewModel.setWidth(iLongitudeLength);
						oBandViewModel.setPublished(false);

						if (aiTimeHours.size() == 1) {
							oBandViewModel.setName(sVariableShortName);
						} else {
							oBandViewModel.setName(sVariableShortName + "_" + String.format("%02d" , iTimeHour) + "hh");
						}

						oBands.add(oBandViewModel);
					}
				}
			}

			oNodeGroupViewModel.setBands(oBands);
			oRetViewModel.setBandsGroups(oNodeGroupViewModel);
		} catch (IOException e) {
			WasdiLog.debugLog("CmNcProductReader.getProductViewModel: exception reading the shape file: " + e.toString());
		} finally {
			if (oFile != null)
				try {
					oFile.close();
				} catch (IOException oEx) {
					WasdiLog.errorLog("CmNcProductReader.getProductViewModel: exception closing the shape file: ", oEx);
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
			WasdiLog.debugLog("CmNcProductReader.getProductBoundingBox: exception reading the shape file: " + e.toString());
		}

		try {
			return extractBboxFromFile(m_oProductFile.getAbsolutePath());
		} catch (IOException e) {
			WasdiLog.debugLog("CmNcProductReader.getProductBoundingBox: exception reading the shape file: " + e.toString());
		}

		return null;
	}

	private static String extractBboxFromFile(String oFileName) throws IOException {
		NetcdfFile oFile = NetcdfFiles.open(oFileName);

		Variable oLatitudeVariable = getVariableByName(oFile, "latitude");
		Variable oLongitudeVariable = getVariableByName(oFile, "longitude");

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
		float fMin = 181F;
		float fMax = - 181F;

		if (afFloatArray != null) {
			for (float fValue : afFloatArray) {
				if (fValue > fMax) {
					fMax = fValue;
				}

				if (fValue < fMin) {
					fMin = fValue;
				}
			}
		}

		return new float[] {fMin, fMax};
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

				List<Variable> aoVariables = oGroup.getVariables();
				for (Variable oVariable : aoVariables) {
					String sVariableShortName = oVariable.getShortName();
					if (sVariableShortName.equalsIgnoreCase(sVariableName)) {
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
		Object oStorage = null;

		if (oArray != null) {
			oStorage = oArray.getStorage();
		}

		return oStorage;
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
	public File getFileForPublishBand(String sBand, String sLayerId) {
		return null;
	}

}
