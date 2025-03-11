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
import java.util.stream.DoubleStream;

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

public class CdsGribProductReader extends WasdiProductReader {

	public CdsGribProductReader(File oProductFile) {
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

			Set<String> asExcludedVariableSet = new HashSet<>(Arrays.asList("LatLon_Projection", "lat", "lon", "reftime", "reftime1", "time", "time1", "time_bounds", "time1_bounds", "isobaric"));

			List<Variable> aoVariablesList = oFile.getVariables();

			List<BandViewModel> oBands = new ArrayList<>();
			int iLatitudeLength = 0;
			int iLongitudeLength = 0;
			List<Integer> aoTimeHoursList = Collections.emptyList();
			List<Integer> aoIsobaricLevelsList = new ArrayList<>();

			for (Variable oVariable : aoVariablesList) {
				String sVariableShortName = oVariable.getShortName();oVariable.getName();

				if (sVariableShortName.equalsIgnoreCase("lon")) {
					iLongitudeLength = extractValueFromShape(oVariable);
				}

				if (sVariableShortName.equalsIgnoreCase("lat")) {
					iLatitudeLength = extractValueFromShape(oVariable);
				}

				if (sVariableShortName.equalsIgnoreCase("time")) {
					double[] adHoursArray = (double[]) (oVariable.read().getStorage());
					aoTimeHoursList = DoubleStream.of(adHoursArray).boxed().map(Double::intValue).collect(Collectors.toList());
				}

				if (sVariableShortName.equalsIgnoreCase("isobaric")) {
					float[] afIsobaricLevelsArray = (float[]) (oVariable.read().getStorage());
					for (float fLevel : afIsobaricLevelsArray) {
						aoIsobaricLevelsList.add((int) fLevel);
					}
				}
			}

			// for datasets without presure levels
			if (aoIsobaricLevelsList.isEmpty()) {
				aoIsobaricLevelsList.add(null);
			}

			for (Variable oVariable : aoVariablesList) {
				String sVariableShortName = oVariable.getShortName();
				if (!asExcludedVariableSet.contains(sVariableShortName)) {
					for (Integer iIsobaricLevel : aoIsobaricLevelsList) {
						for (Integer iTimeHour : aoTimeHoursList) {
							// Create the single band representing the shape
							BandViewModel oBandViewModel = new BandViewModel();
							oBandViewModel.setPublished(false);
							oBandViewModel.setGeoserverBoundingBox("");
							oBandViewModel.setHeight(iLatitudeLength);
							oBandViewModel.setWidth(iLongitudeLength);
							oBandViewModel.setPublished(false);

							String sBandName;
							if (iIsobaricLevel == null) {
								sBandName = sVariableShortName + "_" + String.format("%02d" , iTimeHour) + "hh";
							} else {
								sBandName = sVariableShortName + "_" + iIsobaricLevel + "hPa" + "_" + String.format("%02d" , iTimeHour) + "hh";
							}

							oBandViewModel.setName(sBandName);

							oBands.add(oBandViewModel);
						}
					}
				}
			}

			oNodeGroupViewModel.setBands(oBands);
			oRetViewModel.setBandsGroups(oNodeGroupViewModel);
		} catch (IOException e) {
			WasdiLog.debugLog("CdsGribProductReader.getProductViewModel: exception reading the shape file: " + e.toString());
		} finally {
			if (oFile != null)
				try {
					oFile.close();
				} catch (IOException oEx) {
					WasdiLog.errorLog("CdsGribProductReader.getProductViewModel: exception while closing the shape file", oEx);
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
			WasdiLog.debugLog("CdsGribProductReader.getProductBoundingBox: exception reading the shape file: " + e.toString());
		}

		try {
			return extractBboxFromFile2(m_oProductFile.getAbsolutePath());
		} catch (IOException e) {
			WasdiLog.debugLog("CdsGribProductReader.getProductBoundingBox: exception reading the shape file: " + e.toString());
		}

		return null;
	}

	private static String extractBboxFromFile2(String sFileName) throws IOException {
		NetcdfFile oFile = NetcdfFiles.open(sFileName);

		List<ucar.nc2.Variable> aoVariablesList = oFile.getVariables();
		if (aoVariablesList != null) {
			Variable oVariable = aoVariablesList.get(aoVariablesList.size() - 1);
			Object oObject = oVariable.getSPobject();

			if (oObject instanceof ucar.nc2.grib.collection.GribCollectionImmutable.VariableIndex) {
				ucar.nc2.grib.collection.GribCollectionImmutable.VariableIndex oVariableIndex = (ucar.nc2.grib.collection.GribCollectionImmutable.VariableIndex) oObject;
				ucar.nc2.grib.collection.GribCollectionImmutable.GroupGC oGroupGC = oVariableIndex.getGroup();

				ucar.nc2.grib.collection.GribHorizCoordSystem horizCoordSys = oGroupGC.horizCoordSys;

				Object oObject2 = horizCoordSys.getGdsHash();
				if (oObject2 instanceof ucar.nc2.grib.grib1.Grib1Gds.LatLon) {
//					ucar.nc2.grib.grib1.Grib1Gds.LatLon latLon = (ucar.nc2.grib.grib1.Grib1Gds.LatLon) oObject2;

//					ucar.nc2.grib.GdsHorizCoordSys makeHorizCoordSys = latLon.makeHorizCoordSys();
//					double startx = makeHorizCoordSys.startx;
//					double starty = makeHorizCoordSys.starty;
//
//					double endx = makeHorizCoordSys.getEndX();
//					double endy = makeHorizCoordSys.getEndX();
				}
			}
		}

		float fLatN = 0;
		float fLonW = 0;
		float fLatS = 0;
		float fLonE = 0;
		String sBBox = fLatN + "," + fLonW + "," + fLatS + "," + fLonE;
		
		oFile.close();

		return sBBox;
	}

	private static String extractBboxFromFile(String sFileName) throws IOException {
		NetcdfFile oFile = NetcdfFiles.open(sFileName);

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
		float adMinf = 181F;
		float afMaxf = - 181F;

		if (afFloatArray != null) {
			for (float f : afFloatArray) {
				if (f > afMaxf) {
					afMaxf = f;
				}

				if (f < adMinf) {
					adMinf = f;
				}
			}
		}

		return new float[] {adMinf, afMaxf};
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
	public File getFileForPublishBand(String sBand, String sLayerId) {
		return null;
	}

}
