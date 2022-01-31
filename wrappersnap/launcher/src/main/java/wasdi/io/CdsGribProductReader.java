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
import wasdi.LauncherMain;
import wasdi.shared.utils.Utils;
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

		try {
			NetcdfFile oFile = NetcdfFiles.open(m_oProductFile.getAbsolutePath());

			// Create the Product View Model
			oRetViewModel = new GeorefProductViewModel();

			// Set name values
			oRetViewModel.setFileName(m_oProductFile.getName());
			oRetViewModel.setName(Utils.getFileNameWithoutLastExtension(m_oProductFile.getName()));
			oRetViewModel.setProductFriendlyName(oRetViewModel.getName());

			NodeGroupViewModel oNodeGroupViewModel = new NodeGroupViewModel();
			oNodeGroupViewModel.setNodeName("Bands");

			Set<String> excludedVariableSet = new HashSet<>(Arrays.asList("LatLon_Projection", "lat", "lon", "reftime", "reftime1", "time", "time1", "time_bounds", "time1_bounds", "isobaric"));

			List<Variable> variablesList = oFile.getVariables();

			List<BandViewModel> oBands = new ArrayList<>();
			int latitudeLength = 0;
			int longitudeLength = 0;
			List<Integer> timeHoursList = Collections.emptyList();
			List<Integer> isobaricLevelsList = new ArrayList<>();

			for (Variable v : variablesList) {
				String variableShortName = v.getShortName();v.getName();

				if (variableShortName.equalsIgnoreCase("lon")) {
					longitudeLength = extractValueFromShape(v);
				}

				if (variableShortName.equalsIgnoreCase("lat")) {
					latitudeLength = extractValueFromShape(v);
				}

				if (variableShortName.equalsIgnoreCase("time")) {
					double[] hoursArray = (double[]) (v.read().getStorage());
					timeHoursList = DoubleStream.of(hoursArray).boxed().map(Double::intValue).collect(Collectors.toList());
				}

				if (variableShortName.equalsIgnoreCase("isobaric")) {
					float[] isobaricLevelsArray = (float[]) (v.read().getStorage());
					for (float f : isobaricLevelsArray) {
						isobaricLevelsList.add((int) f);
					}
				}
			}

			// for datasets without presure levels
			if (isobaricLevelsList.isEmpty()) {
				isobaricLevelsList.add(null);
			}

			for (Variable v : variablesList) {
				String variableShortName = v.getShortName();
				if (!excludedVariableSet.contains(variableShortName)) {
					for (Integer isobaricLevel : isobaricLevelsList) {
						for (Integer timeHour : timeHoursList) {
							// Create the single band representing the shape
							BandViewModel oBandViewModel = new BandViewModel();
							oBandViewModel.setPublished(false);
							oBandViewModel.setGeoserverBoundingBox("");
							oBandViewModel.setHeight(latitudeLength);
							oBandViewModel.setWidth(longitudeLength);
							oBandViewModel.setPublished(false);

							String bandName;
							if (isobaricLevel == null) {
								bandName = variableShortName + "_" + String.format("%02d" , timeHour) + "hh";
							} else {
								bandName = variableShortName + "_" + isobaricLevel + "hPa" + "_" + String.format("%02d" , timeHour) + "hh";
							}

							oBandViewModel.setName(bandName);

							oBands.add(oBandViewModel);
						}
					}
				}
			}

			oNodeGroupViewModel.setBands(oBands);
			oRetViewModel.setBandsGroups(oNodeGroupViewModel);
		} catch (IOException e) {
			LauncherMain.s_oLogger.debug("CdsGribProductReader.getProductViewModel: exception reading the shape file: " + e.toString());
		}

		return oRetViewModel;
	}

	private static int extractValueFromShape(Variable v) {
		int value = 0;

		int[] shapeArray = v.getShape();

		if (shapeArray != null && shapeArray.length > 0) {
			value = shapeArray[0];
		}

		return value;
	}

	@Override
	public String getProductBoundingBox() {
		if (m_oProductFile == null) return null;

		try {
			return extractBboxFromFile(m_oProductFile.getAbsolutePath());
		} catch (IOException e) {
			LauncherMain.s_oLogger.debug("CdsGribProductReader.getProductBoundingBox: exception reading the shape file: " + e.toString());
		}

		try {
			return extractBboxFromFile2(m_oProductFile.getAbsolutePath());
		} catch (IOException e) {
			LauncherMain.s_oLogger.debug("CdsGribProductReader.getProductBoundingBox: exception reading the shape file: " + e.toString());
		}

		return null;
	}

	private static String extractBboxFromFile2(String fileName) throws IOException {
		NetcdfFile oFile = NetcdfFiles.open(fileName);

		List<ucar.nc2.Variable> variablesList = oFile.getVariables();
		if (variablesList != null) {
			Variable variable = variablesList.get(variablesList.size() - 1);
			Object object = variable.getSPobject();

			if (object instanceof ucar.nc2.grib.collection.GribCollectionImmutable.VariableIndex) {
				ucar.nc2.grib.collection.GribCollectionImmutable.VariableIndex variableIndex = (ucar.nc2.grib.collection.GribCollectionImmutable.VariableIndex) object;
				ucar.nc2.grib.collection.GribCollectionImmutable.GroupGC groupGC = variableIndex.getGroup();

				ucar.nc2.grib.collection.GribHorizCoordSystem horizCoordSys = groupGC.horizCoordSys;

				Object object2 = horizCoordSys.getGdsHash();
				if (object2 instanceof ucar.nc2.grib.grib1.Grib1Gds.LatLon) {
					ucar.nc2.grib.grib1.Grib1Gds.LatLon latLon = (ucar.nc2.grib.grib1.Grib1Gds.LatLon) object2;

					ucar.nc2.grib.GdsHorizCoordSys makeHorizCoordSys = latLon.makeHorizCoordSys();
					double startx = makeHorizCoordSys.startx;
					double starty = makeHorizCoordSys.starty;

					double endx = makeHorizCoordSys.getEndX();
					double endy = makeHorizCoordSys.getEndX();
				}
			}
		}

		float fLatN = 0;
		float fLonW = 0;
		float fLatS = 0;
		float fLonE = 0;
		String sBBox = fLatN + "," + fLonW + "," + fLatS + "," + fLonE;

		return sBBox;
	}

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

	private static float[] extractExtremeCoordinatesFromStorage(Object storage) {
		float[] extremeCoordinates = new float[] {0F, 0F};

		if (storage != null && storage instanceof float[]) {
			float[] floatArray = (float[]) storage;

			extremeCoordinates = extractExtremeValuesFromArray(floatArray);
		}

		return extremeCoordinates;
	}

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

	private static Array getArrayFromVariable(Variable variable) throws IOException {
		Array array = null;

		if (variable != null) {
			array = variable.read();
		}

		return array;
	}

	private static Object getStorageFromArray(Array array) {
		Object o = null;

		if (array != null) {
			o = array.getStorage();
		}

		return o;
	}

	@Override
	public MetadataViewModel getProductMetadataViewModel() {
		return new MetadataViewModel("Metadata");
	}

	@Override
	public String adjustFileAfterDownload(String sDownloadedFileFullPath, String sFileNameFromProvider) {
		String sFileName = sDownloadedFileFullPath;

		return sFileName;
	}

}
