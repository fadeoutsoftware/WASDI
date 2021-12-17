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
import wasdi.LauncherMain;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.products.BandViewModel;
import wasdi.shared.viewmodels.products.GeorefProductViewModel;
import wasdi.shared.viewmodels.products.MetadataViewModel;
import wasdi.shared.viewmodels.products.NodeGroupViewModel;
import wasdi.shared.viewmodels.products.ProductViewModel;

public class CdsNetcdfProductReader extends WasdiProductReader {

	public CdsNetcdfProductReader(File oProductFile) {
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

			Set<String> excludedVariableSet = new HashSet<>(Arrays.asList("longitude", "latitude", "time"));

			List<Variable> variablesList = oFile.getVariables();

			List<BandViewModel> oBands = new ArrayList<>();
			int latitudeLength = 0;
			int longitudeLength = 0;
			List<Integer> timeHoursList = Collections.emptyList();

			for (Variable v : variablesList) {
				String variableShortName = v.getShortName();v.getName();
				System.out.println("variableShortName: " + variableShortName);

				if (variableShortName.equalsIgnoreCase("longitude")) {
					longitudeLength = extractValueFromShape(v);
				}

				if (variableShortName.equalsIgnoreCase("latitude")) {
					latitudeLength = extractValueFromShape(v);
				}

				if (variableShortName.equalsIgnoreCase("time")) {
					int[] hoursArray = (int[]) (v.read().getStorage());
					timeHoursList = IntStream.of(hoursArray).map(i -> i % 24).boxed().collect(Collectors.toList());
				}
			}

			for (Variable v : variablesList) {
				String variableShortName = v.getShortName();
				String description = v.getDescription();
				System.out.println("variableShortName: " + variableShortName + "; " + "description: " + description);
				if (!excludedVariableSet.contains(variableShortName)) {
					for (Integer timeHour : timeHoursList) {
						// Create the single band representing the shape
						BandViewModel oBandViewModel = new BandViewModel();
						oBandViewModel.setPublished(false);
						oBandViewModel.setGeoserverBoundingBox("");
						oBandViewModel.setHeight(latitudeLength);
						oBandViewModel.setWidth(longitudeLength);
						oBandViewModel.setPublished(false);
						oBandViewModel.setName(description.replaceAll("[\\W]", "_") + "_" + String.format("%02d" , timeHour) + "hh");

						oBands.add(oBandViewModel);
					}
				}
			}

			oNodeGroupViewModel.setBands(oBands);
			oRetViewModel.setBandsGroups(oNodeGroupViewModel);
		} catch (IOException e) {
			LauncherMain.s_oLogger.debug("CdsNetcdfProductReader.getProductViewModel: exception reading the shape file: " + e.toString());
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
			LauncherMain.s_oLogger.debug("CdsNetcdfProductReader.getProductBoundingBox: exception reading the shape file: " + e.toString());
		}

		try {
			return extractBboxFromFile(m_oProductFile.getAbsolutePath());
		} catch (IOException e) {
			LauncherMain.s_oLogger.debug("CdsNetcdfProductReader.getProductBoundingBox: exception reading the shape file: " + e.toString());
		}

		return null;
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

	@Override
	public Product getSnapProduct() {
		Product oProduct = super.getSnapProduct();

		if (oProduct != null && oProduct.getStartTime() == null) {
			oProduct.setStartTime(ProductData.UTC.create(new java.util.Date(), 0));
		}

		return oProduct;
	}

}
