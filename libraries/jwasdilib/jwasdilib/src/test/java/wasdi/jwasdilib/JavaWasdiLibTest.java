package wasdi.jwasdilib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import wasdi.jwasdilib.utils.DateUtils;
import wasdi.jwasdilib.utils.FileUtils;
import wasdi.jwasdilib.utils.StringUtils;

@RunWith(MockitoJUnitRunner.class)
@Ignore
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JavaWasdiLibTest {

	private final static Logger LOGGER = Logger.getLogger(JavaWasdiLibTest.class);

	private static WasdiLib wasdi;

	private static String sDefaultProvider;
	private static String sNodeCode;
	private static String sPlatform;

	private static String sWorkspaceName;
	private static String sTestFile1Name;
	private static String sTestFile2Name;

	private static String sDateFrom;
	private static String sDateTo;
	private static Double dULLat;
	private static Double dULLon;
	private static Double dLRLat;
	private static Double dLRLon;
	private static String sProductType;
	private static Integer iOrbitNumber;
	private static String sSensorOperationalMode;
	private static String sCloudCoverage;

	@BeforeClass
	public static void setup() throws IOException {
		LOGGER.info("setup");

		Properties configProperties = new Properties();
		configProperties.load(JavaWasdiLibTest.class.getClassLoader().getResourceAsStream("config.properties"));

		assertNotNull("Missing valid user in the config.properties file", configProperties.getProperty("USER"));
		assertNotEquals("Missing valid user in the config.properties file", "", configProperties.getProperty("USER"));
		assertNotNull("Missing valid password in the config.properties file", configProperties.getProperty("PASSWORD"));
		assertNotEquals("Missing valid password in the config.properties file", "", configProperties.getProperty("PASSWORD"));

		sWorkspaceName = configProperties.getProperty("WORKSPACE");

		Properties parametersProperties = new Properties();
		parametersProperties.load(JavaWasdiLibTest.class.getClassLoader().getResourceAsStream("parameters.properties"));

		sDefaultProvider = parametersProperties.getProperty("test.default.provider");
		sNodeCode = parametersProperties.getProperty("test.node.code");
		sPlatform = parametersProperties.getProperty("test.platform");
		sProductType = parametersProperties.getProperty("test.product.type");

		sTestFile1Name = parametersProperties.getProperty("test.file1.name");
		sTestFile2Name = parametersProperties.getProperty("test.file2.name");

		String bbox = parametersProperties.getProperty("test.bounding.box");
		double[] boundingBox = StringUtils.convertBoundingBox(bbox);
		dULLat = boundingBox[0];
		dULLon = boundingBox[1];
		dLRLat = boundingBox[2];
		dLRLon = boundingBox[3];

		Date endDay = DateUtils.convert(parametersProperties.getProperty("test.date"));
		Date startDay = DateUtils.getEarlierDate(endDay, StringUtils.stringToInteger(parametersProperties.getProperty("test.search.days")));

		sDateFrom = DateUtils.convert(startDay);
		sDateTo = DateUtils.convert(endDay);

		sCloudCoverage = "[0 TO " + parametersProperties.getProperty("test.max.cloud") + "]";

		
		wasdi = new WasdiLib();
		String absoluteFilePath = FileUtils.getAbsoluteFilePath("config.properties");
		wasdi.init(absoluteFilePath);
		wasdi.setDefaultProvider(sDefaultProvider);
	}

	@Test
	public void test_00_validateParameters() {
		LOGGER.info("validateParameters");

		assertEquals("LSA", sDefaultProvider);
		assertEquals("WASDI-ONDA-1", sNodeCode);
		assertEquals("S2", sPlatform);

		assertEquals("TestWorkspace", sWorkspaceName);
		assertEquals("S2A_MSIL1C_20201008T102031_N0209_R065_T32TMR_20201008T123525", sTestFile1Name);
		assertEquals("S2B_MSIL1C_20201013T101909_N0209_R065_T32TMR_20201018T165151", sTestFile2Name);

		assertEquals("2020-10-05", sDateFrom);
		assertEquals("2020-10-25", sDateTo);
		assertEquals(Double.valueOf(45.9D), dULLat);
		assertEquals(Double.valueOf(08.5D), dULLon);
		assertEquals(Double.valueOf(45.7D), dLRLat);
		assertEquals(Double.valueOf(08.7D), dLRLon);
		assertEquals("S2MSI1C", sProductType);
		assertNull(iOrbitNumber);
		assertNull(sSensorOperationalMode);
		assertEquals("[0 TO 30]", sCloudCoverage);
	}

	@Test
	public void test_01_createWorkspace() {
		LOGGER.info("createWorkspace");

		String createdWorkspaceId = wasdi.createWorkspace(sWorkspaceName, sNodeCode);

		String foundWorkspaceId = wasdi.getWorkspaceIdByName(sWorkspaceName);

		assertEquals(createdWorkspaceId, foundWorkspaceId);
	}

	@Test
	public void test_02_openWorkspace() {
		LOGGER.info("openWorkspace");

		String activeWorkspaceId = wasdi.openWorkspace(sWorkspaceName);

		String foundWorkspaceId = wasdi.getWorkspaceIdByName(sWorkspaceName);

		assertEquals(activeWorkspaceId, wasdi.getActiveWorkspace());

		assertEquals(activeWorkspaceId, foundWorkspaceId);
	}

	@Test
	public void test_03_searchEOImages() {
		LOGGER.info("searchEOImages");

		List<Map<String, Object>> images = wasdi.searchEOImages(sPlatform, sDateFrom, sDateTo, dULLat, dULLon, dLRLat,
				dLRLon, sProductType, iOrbitNumber, sSensorOperationalMode, sCloudCoverage);

		List<String> imageNames = images.stream().map(t -> (String) t.get("title")).collect(Collectors.toList());

		assertTrue(imageNames.contains(sTestFile1Name));
		assertTrue(imageNames.contains(sTestFile2Name));
	}

	@Test
	public void test_04_importProductListWithMaps() {
		LOGGER.info("importProductListWithMaps");

		List<Map<String, Object>> images = wasdi.searchEOImages(sPlatform, sDateFrom, sDateTo, dULLat, dULLon, dLRLat,
				dLRLon, sProductType, iOrbitNumber, sSensorOperationalMode, sCloudCoverage);

		List<String> alreadyExistingImages = wasdi.getProductsByActiveWorkspace();

		List<Map<String, Object>> imagesToImport = images.stream()
				.filter(t -> ((String) t.get("title")).equals(sTestFile1Name) || ((String) t.get("title")).equals(sTestFile2Name))
				.filter(t -> !alreadyExistingImages.contains(t + ".zip"))
				.collect(Collectors.toList());

		if (!imagesToImport.isEmpty()) {
			List<String> statusList = wasdi.importProductListWithMaps(imagesToImport);

			assertEquals(2, statusList.size());
			assertEquals(Arrays.asList("DONE", "DONE"), statusList);

			List<String> importedImages = wasdi.getProductsByActiveWorkspace();

			assertTrue(importedImages.contains(sTestFile1Name + ".zip"));
			assertTrue(importedImages.contains(sTestFile2Name + ".zip"));
		}

	}

	@Test
	public void test_05_deleteImage() throws JsonMappingException, JsonProcessingException {
		LOGGER.info("deleteImage");

		String response = wasdi.deleteProduct(sTestFile1Name + ".zip");

		assertTrue(StringUtils.getResponseBooleanValue(response));

		List<String> availableImages = wasdi.getProductsByActiveWorkspace();

		assertTrue(availableImages.contains(sTestFile2Name + ".zip"));
	}

	@Test
	public void test_06_executeWorkflow() {
		LOGGER.info("executeWorkflow");

		List<String> availableImages = wasdi.getProductsByActiveWorkspace();

		assertTrue(availableImages.contains(sTestFile2Name + ".zip"));
		String availableImageName = sTestFile2Name + ".zip";

		List<Map<String, Object>> workflows = wasdi.getWorkflows();

		Map<String, Object> workflow = workflows.stream()
			.filter(t -> ((String) t.get("name")).equalsIgnoreCase("ndvi"))
			.findFirst()
            .orElse(null);

		String actualResponse = wasdi.executeWorkflow(new String[] {availableImageName}, new String[] {availableImageName + "_preproc.tif"}, (String) workflow.get("name"));
		assertEquals("DONE", actualResponse);

		availableImages = wasdi.getProductsByActiveWorkspace();
		assertTrue(availableImages.contains(availableImageName));
		assertTrue(availableImages.contains(availableImageName + "_preproc.tif"));

		wasdi.deleteProduct(availableImageName);
		wasdi.deleteProduct(availableImageName + "_preproc.tif");

		availableImages = wasdi.getProductsByActiveWorkspace();
		assertFalse(availableImages.contains(availableImageName));
		assertFalse(availableImages.contains(availableImageName + "_preproc.tif"));
	}

	@Test
	public void test_07_addFileToWASDI() throws IOException {
		LOGGER.info("addFileToWASDI");

		FileUtils.copyResourceFileToLocalBasePath("lux1.tif", wasdi.getPath("lux1.tif"));
		String status = wasdi.addFileToWASDI("lux1.tif");
		assertEquals("DONE", status);

		FileUtils.copyResourceFileToLocalBasePath("lux2.tif", wasdi.getPath("lux2.tif"));
		status = wasdi.addFileToWASDI("lux2.tif");
		assertEquals("DONE", status);

		List<String> availableImages = wasdi.getProductsByActiveWorkspace();
		assertTrue(availableImages.contains("lux1.tif"));
		assertTrue(availableImages.contains("lux2.tif"));
	}

	@Test
	public void test_08_mosaic() throws IOException {
		LOGGER.info("mosaic");

		List<String> asInputs = Arrays.asList("lux1.tif", "lux2.tif");
		String sOutputFile = "mosaic.tif";

		String status = wasdi.mosaic(asInputs, sOutputFile);
		assertEquals("DONE", status);

		List<String> availableImages = wasdi.getProductsByActiveWorkspace();
		assertTrue(availableImages.contains("lux1.tif"));
		assertTrue(availableImages.contains("lux2.tif"));
		assertTrue(availableImages.contains("mosaic.tif"));
	}

	@Test
	public void test_09_multiSubset() throws IOException {
		LOGGER.info("multiSubset");

		String sInputFile = "mosaic.tif";
		List<String> asOutputFiles = Arrays.asList("subset1.tif", "subset2.tif");

		List<Double> adLatN = Arrays.asList(48.9922701083264869, 48.9863412274512982);
		List<Double> adLonW = Arrays.asList(5.9689794485811358, 6.0399463560265785);
		List<Double> adLatS = Arrays.asList(48.9182489289150411, 48.9256151142448203);
		List<Double> adLonE = Arrays.asList(6.0406650082538738, 6.1136082093243793);

		String status = wasdi.multiSubset(sInputFile, asOutputFiles, adLatN, adLonW, adLatS, adLonE);
		assertEquals("DONE", status);

		List<String> availableImages = wasdi.getProductsByActiveWorkspace();
		assertTrue(availableImages.contains("lux1.tif"));
		assertTrue(availableImages.contains("lux2.tif"));
		assertTrue(availableImages.contains("mosaic.tif"));
		assertTrue(availableImages.contains("subset1.tif"));
		assertTrue(availableImages.contains("subset2.tif"));
	}

	@Test
	@Ignore
	public void test_10_executeProcessor() throws IOException {
		LOGGER.info("executeProcessor");

		String sProcName = "hellowasdiworld";
		Map<String, Object> asParams = new HashMap<>();
		asParams.put("NAME", "Tester");

		String status = wasdi.executeProcessor(sProcName, asParams);
		assertEquals("DONE", status);
	}

	@Test
//	@Ignore
	public void test_99_deleteWorkspace() {
		LOGGER.info("deleteWorkspace");

		String sWorkspaceId = wasdi.getWorkspaceIdByName(sWorkspaceName);
		String actualResponse = wasdi.deleteWorkspace(sWorkspaceId);

		assertEquals("", actualResponse);

		assertTrue(true);
	}

}
