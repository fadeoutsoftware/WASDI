package wasdi.jwasdilib;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;

@RunWith(MockitoJUnitRunner.class)
public class WasdiLibTest {

	private static String getWorkflowsResponse;
	private static String searchEOImagesResponse;

	private static Map<String, String> asHeaders;

	@BeforeClass
	public static void loadKey() throws IOException {
		Properties properties = new Properties();
		properties.load(WasdiLibTest.class.getClassLoader().getResourceAsStream("test.properties"));

		getWorkflowsResponse = properties.getProperty("test.executeWorkflow.getWorkflows");
		searchEOImagesResponse = properties.getProperty("test.searchEOImages");


		//{x-session-token=, Content-Type=application/json}
		asHeaders = new HashMap<>();
		asHeaders.put("x-session-token", "");
		asHeaders.put("Content-Type", "application/json");
	}

	@Test
	public void asynchMultiSubsetTest() {
		WasdiLib wasdiLib = spy(WasdiLib.class);

		String sInputFile = "compressedBaghdad.tif";
		List<String> asOutputFiles = Arrays.asList("left.tif", "right.tif");
		List<Double> adLatN = Arrays.asList(0.7091242500000039, 0.2363747500000013);
		List<Double> adLonW = Arrays.asList(0.27378950000000124, 0.27378950000000124);
		List<Double> adLatS = Arrays.asList(0.2363747500000013, 0.2363747500000013);
		List<Double> adLonE = Arrays.asList(0.27378950000000124, 0.8213685000000037);
		boolean bBigTiff = true;

		String sUrl = "https://www.wasdi.net/wasdiwebserver/rest/processing/multisubset?source=compressedBaghdad.tif&name=compressedBaghdad.tif&workspace=";
		String sPayload = "{\"lonEList\":[0.27378950000000124,0.8213685000000037],\"latNList\":[0.7091242500000039,0.2363747500000013],\"latSList\":[0.2363747500000013,0.2363747500000013],\"outputNames\":[\"left.tif\",\"right.tif\"],\"bigTiff\":true,\"lonWList\":[0.27378950000000124,0.27378950000000124]}";


		String fixedPostResponse = "{\"boolValue\":true,\"doubleValue\":null,\"intValue\":200,\"stringValue\":\"bfe82d4e-703c-4fc7-8e40-90bb1fb1f8bd\"}";
		String expectedResponse = "bfe82d4e-703c-4fc7-8e40-90bb1fb1f8bd";

		doReturn(fixedPostResponse).when(wasdiLib).httpPost(sUrl, sPayload, asHeaders);

		String actualResponse = wasdiLib.asynchMultiSubset(sInputFile, asOutputFiles, adLatN, adLonW, adLatS, adLonE, bBigTiff);

		Assert.assertEquals(expectedResponse, actualResponse);
		verify(wasdiLib, times(1)).httpPost(sUrl, sPayload, asHeaders);
		verify(wasdiLib, times(0)).httpPost("", "", new HashMap<>());
	}

	@Test
	public void asynchMultiSubsetAnyValuesTest() {
		WasdiLib wasdiLib = spy(WasdiLib.class);

		String sInputFile = "compressedBaghdad.tif";
		List<String> asOutputFiles = Arrays.asList("left.tif", "right.tif");
		List<Double> adLatN = Arrays.asList(0.7091242500000039, 0.2363747500000013);
		List<Double> adLonW = Arrays.asList(0.27378950000000124, 0.27378950000000124);
		List<Double> adLatS = Arrays.asList(0.2363747500000013, 0.2363747500000013);
		List<Double> adLonE = Arrays.asList(0.27378950000000124, 0.8213685000000037);
		boolean bBigTiff = true;

		String fixedPostResponse = "{\"boolValue\":true,\"doubleValue\":null,\"intValue\":200,\"stringValue\":\"bfe82d4e-703c-4fc7-8e40-90bb1fb1f8bd\"}";
		String expectedResponse = "bfe82d4e-703c-4fc7-8e40-90bb1fb1f8bd";
		
		doReturn(fixedPostResponse).when(wasdiLib).httpPost(any(String.class), any(String.class), anyMap());

		String actualResponse = wasdiLib.asynchMultiSubset(sInputFile, asOutputFiles, adLatN, adLonW, adLatS, adLonE, bBigTiff);
		
		Assert.assertEquals(expectedResponse, actualResponse);
	}

	@Test
	public void executeWorkflowTest() {
		WasdiLib wasdiLib = spy(WasdiLib.class);

		String sGetUrl = "https://www.wasdi.net/wasdiwebserver/rest/workflows/getbyuser";
		String fixedGetResponse = getWorkflowsResponse;

		doReturn(fixedGetResponse).when(wasdiLib).httpGet(sGetUrl, asHeaders);


		String sPostUrl = "https://www.wasdi.net/wasdiwebserver/rest/workflows/run?workspace=";
		String sPayload = "{\"name\":\"LISTSinglePreproc2\", \"description\":\"\",\"workflowId\":\"a93af948-36d6-4a55-aed7-7a084e4f8b4d\", \"inputNodeNames\": [], \"inputFileNames\": [\"S1A_IW_GRDH_1SDV_20211022T172457_20211022T172522_040235_04C439_5855.zip\"], \"outputNodeNames\":[], \"outputFileNames\":[\"S1A_IW_GRDH_1SDV_20211022T172457_20211022T172522_040235_04C439_5855.zip_preproc.tif\"]}";


//		String dynamicProcessObjId = "a93af948-36d6-4a55-aed7-7a084e4f8b4d";
		String dynamicProcessObjId = "UUID_corresponding_to_the_process_workspace_ID";
		String fixedPostResponse = "{\"boolValue\":true,\"doubleValue\":null,\"intValue\":200,\"stringValue\":\"" + dynamicProcessObjId + "\"}";

		doReturn(fixedPostResponse).when(wasdiLib).httpPost(sPostUrl, sPayload, asHeaders);

		String sInput = "S1A_IW_GRDH_1SDV_20211022T172457_20211022T172522_040235_04C439_5855.zip";
		String []  asInputFileName = new String[]{sInput};
		String [] asOutputFileName = new String[]{sInput + "_preproc.tif"};
		String sWorkflowName = "LISTSinglePreproc2";

		String sGetStatusUrl = wasdiLib.getWorkspaceBaseUrl() + "/process/byid?procws=" + dynamicProcessObjId;

		String fixedGetStatusRunningResponse = "{\"boolValue\":true,\"doubleValue\":null,\"intValue\":200,\"status\":\"RUNNING\"}";
		String fixedGetStatusWaitingResponse = "{\"boolValue\":true,\"doubleValue\":null,\"intValue\":200,\"status\":\"WAITING\"}";
		String fixedGetStatusReadyResponse = "{\"boolValue\":true,\"doubleValue\":null,\"intValue\":200,\"status\":\"READY\"}";
		String fixedGetStatusDoneResponse = "{\"boolValue\":true,\"doubleValue\":null,\"intValue\":200,\"status\":\"DONE\"}";

		doReturn(fixedGetStatusRunningResponse)
		.doReturn(fixedGetStatusWaitingResponse)
		.doReturn(fixedGetStatusReadyResponse)
		.doReturn(fixedGetStatusRunningResponse)
		.doReturn(fixedGetStatusDoneResponse)
		.when(wasdiLib).httpGet(sGetStatusUrl, asHeaders);

		String expectedResponse = "DONE";
		String actualResponse = wasdiLib.executeWorkflow(asInputFileName, asOutputFileName, sWorkflowName);

		Assert.assertEquals(expectedResponse, actualResponse);

		verify(wasdiLib, times(1)).httpGet(sGetUrl, asHeaders);
		verify(wasdiLib, times(0)).httpGet("", new HashMap<>());

		verify(wasdiLib, times(1)).httpPost(sPostUrl, sPayload, asHeaders);
		verify(wasdiLib, times(0)).httpPost("", "", new HashMap<>());

		verify(wasdiLib, times(5)).httpGet(sGetStatusUrl, asHeaders);
		verify(wasdiLib, times(0)).httpGet("", new HashMap<>());
	}

	@Test
	public void searchEOImagesTest() throws JsonMappingException, JsonProcessingException {
		WasdiLib wasdiLib = spy(WasdiLib.class);

		String sPlatform = "S2";
		String sDateFrom = "2020-10-05";
		String sDateTo = "2020-10-25";
		Double dULLat = 45.9;
		Double dULLon = 8.5;
		Double dLRLat = 45.7;
		Double dLRLon = 8.7;
		String sProductType = "S2MSI1C";
		Integer iOrbitNumber = null;
		String sSensorOperationalMode = null;
		String sCloudCoverage = "[0 TO 30]";

		String sUrl = "https://www.wasdi.net/wasdiwebserver/rest/search/querylist?providers=LSA";
		String sPayload = "[\"( footprint:\\\"intersects(POLYGON(( 8.5 45.7,8.5 45.9,8.7 45.9,8.7 45.7,8.5 45.7)))\\\") AND ( platformname:Sentinel-2  AND producttype:S2MSI1C) AND ( beginPosition:[2020-10-05T00:00:00.000Z TO 2020-10-25T23:59:59.999Z]AND ( endPosition:[2020-10-05T00:00:00.000Z TO 2020-10-25T23:59:59.999Z]) \"]";

		String fixedPostResponse = searchEOImagesResponse;


		doReturn(fixedPostResponse).when(wasdiLib).httpPost(sUrl, sPayload, asHeaders);


		List<Map<String, Object>> expectedResponse = wasdiLib.s_oMapper.readValue(searchEOImagesResponse, new TypeReference<List<Map<String,Object>>>(){});

		List<Map<String, Object>> actualResponse = wasdiLib.searchEOImages(sPlatform, sDateFrom, sDateTo, dULLat, dULLon, dLRLat, dLRLon,
				sProductType, iOrbitNumber, sSensorOperationalMode, sCloudCoverage);

		Assert.assertNotNull(actualResponse);
		Assert.assertEquals(expectedResponse, actualResponse);

		verify(wasdiLib, times(1)).httpPost(sUrl, sPayload, asHeaders);
		verify(wasdiLib, times(0)).httpPost("", "", new HashMap<>());
	}

}
