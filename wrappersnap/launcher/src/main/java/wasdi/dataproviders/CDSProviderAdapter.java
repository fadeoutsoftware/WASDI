package wasdi.dataproviders;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.cds.QueryExecutorCDS;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.gis.BoundingBoxUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;

public class CDSProviderAdapter extends ProviderAdapter {

	private static final String CDS_URL_SEARCH = "https://cds.climate.copernicus.eu/api/v2/resources";
	private static final String CDS_URL_GET_STATUS = "https://cds.climate.copernicus.eu/broker/api/v1/0/requests";
	
	/**
	 * Basic constructor
	 */
	public CDSProviderAdapter() {
		m_sDataProviderCode = "CDS";
	}

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		return 0;
	}

	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {

		WasdiLog.debugLog("CDSProviderAdapter.executeDownloadFile: try to get " + sFileURL);

		String sDesiredFileName = oProcessWorkspace.getProductName();

		Map<String, String> aoWasdiPayload = extractWasdiPayloadFromUrl(sFileURL);
		if (aoWasdiPayload == null) {
			return null;
		}

		Map<String, Object> aoCdsPayload = prepareCdsPayload(aoWasdiPayload);
		String sPayload = JsonUtils.stringify(aoCdsPayload);

		String sDataset = JsonUtils.getProperty(aoWasdiPayload, "dataset");

		String sCdsSearchRequestState = "queued";
		String sUrl = CDS_URL_SEARCH + "/" + sDataset;
		
		String sCdsSearchRequestResult = performCdsSearchRequest(sUrl, sDownloadUser, sDownloadPassword, sPayload, iMaxRetry);

		Map<String, Object> oCdsSearchRequestResult = JsonUtils.jsonToMapOfObjects(sCdsSearchRequestResult);
		sCdsSearchRequestState = (String) JsonUtils.getProperty(oCdsSearchRequestResult, "state");
		String sCdsSearchRequestId = (String) JsonUtils.getProperty(oCdsSearchRequestResult, "request_id");

		String sUrlDownload = null;

		if ("completed".equalsIgnoreCase(sCdsSearchRequestState)) {
			sUrlDownload = (String) JsonUtils.getProperty(oCdsSearchRequestResult, "location");
		} else if ("queued".equalsIgnoreCase(sCdsSearchRequestState)) {
			String sCdsGetStatusRequestResult;
			Map<String, Object> oCdsGetStatusRequestResult = new HashMap<>();
			String sCdsGetStatusRequestId;
			String sCdsGetStatusRequestState = "undefined";

			for (int i = 0; i < 120; i++) {
				WasdiLog.debugLog("CDSProviderAdapter.performCdsGetStatusRequest: attemp #" + i);
				sCdsGetStatusRequestResult = performCdsGetStatusRequest(sDownloadUser, sDownloadPassword, sCdsSearchRequestId, iMaxRetry);

				List<Map<String, Object>> aoRequests = JsonUtils.jsonToListOfMapOfObjects(sCdsGetStatusRequestResult);

				for (Map<String, Object> aoRequest : aoRequests) {
					sCdsGetStatusRequestId = (String) JsonUtils.getProperty(aoRequest, "metadata.requestId");

					if (sCdsSearchRequestId.equalsIgnoreCase(sCdsGetStatusRequestId)) {
						oCdsGetStatusRequestResult = aoRequest;
						break;
					}
				}

				sCdsGetStatusRequestState = (String) JsonUtils.getProperty(oCdsGetStatusRequestResult, "status.state");

				if ("queued".equalsIgnoreCase(sCdsGetStatusRequestState)) {
					try {
						TimeUnit.SECONDS.sleep(60);
					} catch (InterruptedException oEx) {
						Thread.currentThread().interrupt();
						WasdiLog.errorLog("CDSProviderAdapter.executeDownloadFile: current thread was interrupted ", oEx);
					}
				} else if ("running".equalsIgnoreCase(sCdsGetStatusRequestState)) {
					try {
						TimeUnit.SECONDS.sleep(6);
					} catch (InterruptedException oEx) {
						Thread.currentThread().interrupt();
						WasdiLog.errorLog("CDSProviderAdapter.executeDownloadFile: current thread was interrupted ", oEx);
					}
				} else if ("completed".equalsIgnoreCase(sCdsGetStatusRequestState)) {
					sUrlDownload = (String) JsonUtils.getProperty(oCdsGetStatusRequestResult, "status.data.0.location");
					break;
				} else if ("failed".equalsIgnoreCase(sCdsGetStatusRequestState)) {
					String sReason = (String) JsonUtils.getProperty(oCdsGetStatusRequestResult, "status.data.reason");
					WasdiLog.debugLog("CDSProviderAdapter.executeDownloadFile: getStatus request failed: " + sReason);
					break;
				} else {
					WasdiLog.debugLog("CDSProviderAdapter.executeDownloadFile: getStatus request is in an unknown state: " + sCdsGetStatusRequestState);
					break;
				}
			}
		}

		if (!Utils.isNullOrEmpty(sUrlDownload)) {
			String sCdsDownloadRequestResult = performCdsDownloadRequest(sUrlDownload, sDownloadUser, sDownloadPassword, sSaveDirOnServer, iMaxRetry);

			return WasdiFileUtils.renameFile(sCdsDownloadRequestResult, sDesiredFileName);
		}

		return null;
	}

	private String performCdsSearchRequest(String sUrl, String sDownloadUser, String sDownloadPassword, String sPayload, int iMaxRetry) {
		String sResult = "";

		int iAttemp = 0;

		while (Utils.isNullOrEmpty(sResult) && iAttemp < iMaxRetry) {

			if (iAttemp > 0) {
				WasdiLog.debugLog("CDSProviderAdapter.performCdsSearchRequest.httpPost: attemp #" + iAttemp);
			}

			try {
				Map<String, String> asHeaders = null;
				String sAuth = sDownloadUser + ":" + sDownloadPassword;
				HttpCallResponse oHttpCallResponse = HttpUtils.httpPost(sUrl, sPayload, asHeaders, sAuth);
				sResult = oHttpCallResponse.getResponseBody();
			} catch (Exception oEx) {
				WasdiLog.debugLog("CDSProviderAdapter.performCdsSearchRequest: exception in http get call: " + oEx.toString());
			}

			iAttemp ++;
		}

		return sResult;
	}

	private String performCdsGetStatusRequest(String sDownloadUser, String sDownloadPassword, String sRequestId, int iMaxRetry) {
		String sResult = "";

		// Add the auth header
		String sAuth = sDownloadUser + ":" + sDownloadPassword;
		String sEncodedAuth = Base64.getEncoder().encodeToString(sAuth.getBytes(StandardCharsets.UTF_8));
		String sAuthHeaderValue = "Basic " + sEncodedAuth;

		Map<String, String> asHeaders = new HashMap<>();
		asHeaders.put("Authorization", sAuthHeaderValue);

		int iAttemp = 0;

		while (Utils.isNullOrEmpty(sResult) && iAttemp < iMaxRetry) {

			if (iAttemp > 0) {
				WasdiLog.debugLog("CDSProviderAdapter.performCdsGetStatusRequest.httpGet: attemp #" + iAttemp);
			}

			try {
				HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(CDS_URL_GET_STATUS, asHeaders); 
				sResult = oHttpCallResponse.getResponseBody();
			} catch (Exception oEx) {
				WasdiLog.debugLog("CDSProviderAdapter.performCdsGetStatusRequest: exception in http get call: " + oEx.toString());
			}

			iAttemp ++;
		}

		return sResult;
	}

	private String performCdsDownloadRequest(String sUrl, String sDownloadUser, String sDownloadPassword, String sSaveDirOnServer, int iMaxRetry) {
		String sResult = "";

		// Add the auth header
		String sAuth = sDownloadUser + ":" + sDownloadPassword;
		String sEncodedAuth = Base64.getEncoder().encodeToString(sAuth.getBytes(StandardCharsets.UTF_8));
		String sAuthHeaderValue = "Basic " + sEncodedAuth;

		Map<String, String> asHeaders = new HashMap<>();
		asHeaders.put("Authorization", sAuthHeaderValue);
		
		int iAttemp = 0;
		
		while (Utils.isNullOrEmpty(sResult) && iAttemp<iMaxRetry) {

			if (iAttemp > 0) {
				WasdiLog.debugLog("CDSProviderAdapter.performCdsDownloadRequest.downloadViaHttp: attemp #" + iAttemp);
			}
			
			try {
				sResult = downloadViaHttp(sUrl, sDownloadUser, sDownloadPassword, sSaveDirOnServer);
			}
			catch (Exception oEx) {
				WasdiLog.debugLog("CDSProviderAdapter.executeDownloadFile: exception in download via http call: " + oEx.toString());
			}
			
			iAttemp ++;
		}
		
		return sResult;
	}

	private Map<String, String> extractWasdiPayloadFromUrl(String sUrl) {
		String sDecodedUrl = deodeUrl(sUrl);

		String sPayload = null;
		if (sDecodedUrl != null && sDecodedUrl.startsWith(CDS_URL_SEARCH) && sDecodedUrl.contains("?payload=")) {
			sPayload = sDecodedUrl.substring(sDecodedUrl.indexOf("?payload=") + 9);

			return JsonUtils.jsonToMapOfStrings(sPayload);
		}

		return null;
	}
	
	private static Map<String, Object> prepareCdsPayload(Map<String, String> aoWasdiPayload) {
		String sDataset = JsonUtils.getProperty(aoWasdiPayload, "dataset");
		String sProductType = JsonUtils.getProperty(aoWasdiPayload, "productType");
		String sVariables = JsonUtils.getProperty(aoWasdiPayload, "variables");
		String sMonthlyAggregation = JsonUtils.getProperty(aoWasdiPayload, "monthlyAggregation");
		String sStartDate = JsonUtils.getProperty(aoWasdiPayload, "startDate");
		String sEndDate = JsonUtils.getProperty(aoWasdiPayload, "endDate");
		String sDate = JsonUtils.getProperty(aoWasdiPayload, "date");
		String sBoundingBox = JsonUtils.getProperty(aoWasdiPayload, "boundingBox");
		String sFormat = JsonUtils.getProperty(aoWasdiPayload, "format");

		List<String> asVariables = null;
		if (!Utils.isNullOrEmpty(sVariables)) {
			asVariables = Arrays.stream(sVariables.split(" "))
					.map(CDSProviderAdapter::inflateVariable)
					.collect(Collectors.toList());
		}

		String sYear = "";
		String sMonth = "";
		String sDay = "";
		List<String> asDays = Collections.emptyList();
		
		if (sMonthlyAggregation.equalsIgnoreCase("true")) {
			sYear = sStartDate.substring(0, 4);
			sMonth = sStartDate.substring(4, 6);
			int iStartDay = Integer.parseInt(sStartDate.substring(6,  8));
			int iEndDay = Integer.parseInt(sEndDate.substring(6,  8));
			asDays = IntStream.rangeClosed(iStartDay, iEndDay)
					.mapToObj(String::valueOf)
					.collect(Collectors.toList());
		} else {
			sYear = sDate.substring(0, 4);
			sMonth = sDate.substring(4, 6);
			sDay = sDate.substring(6, 8);
		}
		

		List<String> oaTimeHours = Arrays.asList("00:00", "01:00", "02:00", "03:00", "04:00", "05:00", "06:00", "07:00", "08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00", "19:00", "20:00", "21:00", "22:00", "23:00");

		Map<String, Object> aoHashMap = new HashMap<>();
		if (sProductType != null) aoHashMap.put("product_type", sProductType);
		if (asVariables != null) {
			aoHashMap.put("variable", asVariables);
		}
		aoHashMap.put("year", sYear);
		aoHashMap.put("month", sMonth);
		aoHashMap.put("time", oaTimeHours);
		aoHashMap.put("format", sFormat);
		if (sMonthlyAggregation.equalsIgnoreCase("true")) {
			aoHashMap.put("day", asDays);
		} else {
			aoHashMap.put("day", sDay);
		}

		if (sBoundingBox != null && !sBoundingBox.contains("null")) {
			List<Double> originalBbox = BoundingBoxUtils.parseBoundingBox(sBoundingBox);
			List<Double> expandedBbox = BoundingBoxUtils.expandBoundingBoxUpToAQuarterDegree(originalBbox);

			if (expandedBbox != null) {
				aoHashMap.put("area", expandedBbox);
			}
		}

		if (sDataset.equalsIgnoreCase("reanalysis-era5-pressure-levels")) {
			String sPresureLevels = JsonUtils.getProperty(aoWasdiPayload, "presureLevels");
			List<String> oaPressureLevels = Arrays.asList(sPresureLevels.split(" "));

			aoHashMap.put("pressure_level", oaPressureLevels);
		}
		
        if (sDataset.equalsIgnoreCase("satellite-sea-surface-temperature")) {
            aoHashMap.put("sensor_on_satellite", "combined_product");
            aoHashMap.put("version", "2_1");
            aoHashMap.put("processinglevel", "level_4");
            aoHashMap.remove("time");
            aoHashMap.remove("area");
        }

		return aoHashMap;
	}

	private static String inflateVariable(String sVariable) {
		switch (sVariable) {
		case "RH":
			return "relative_humidity";
		case "U":
			return "u_component_of_wind";
		case "V":
			return "v_component_of_wind";
		case "SST":
			return "sea_surface_temperature";
		case "SP":
			return "surface_pressure";
		case "TP":
			return "total_precipitation";
		case "10U":
			return "10m_u_component_of_wind";
		case "10V":
			return "10m_v_component_of_wind";
		case "2DT":
			return "2m_dewpoint_temperature";
		case "2T":
			return "2m_temperature";
		default:
			WasdiLog.debugLog("CDSProviderAdapter.inflateVariable: unexpected variable: " + sVariable + ".");
			return sVariable;
		}
	}

	private static String deodeUrl(String sUrlEncoded) {
		try {
			return URLDecoder.decode(sUrlEncoded, java.nio.charset.StandardCharsets.UTF_8.toString());
		} catch (UnsupportedEncodingException oE) {
			WasdiLog.debugLog("Wasdi.deodeUrl: could not decode URL due to " + oE + ".");
		}

		return sUrlEncoded;
	}

	@Override
	public String getFileName(String sFileURL) throws Exception {
		if (Utils.isNullOrEmpty(sFileURL)) return "";

		Map<String, String> aoWasdiPayload = extractWasdiPayloadFromUrl(sFileURL);
		if (aoWasdiPayload == null) {
			return null;
		}
		
		String sDataset = JsonUtils.getProperty(aoWasdiPayload, "dataset");
		String sVariables = JsonUtils.getProperty(aoWasdiPayload, "variables");
		String sDate =  JsonUtils.getProperty(aoWasdiPayload, "date");
		String sStartDate = JsonUtils.getProperty(aoWasdiPayload, "startDate");
		String sEndDate = JsonUtils.getProperty(aoWasdiPayload, "endDate");
		String sFormat = JsonUtils.getProperty(aoWasdiPayload, "format");
		String sFootprint = JsonUtils.getProperty(aoWasdiPayload, "boundingBox");
		String sFootprintForFileName = getFootprintForFileName(sFootprint, sDataset);
		String sExtension = "." + sFormat;

		// filename: reanalysis-era5-pressure-levels_UV_20211201
		String sFileName = getFileName(sDataset, sVariables, sDate, sStartDate, sEndDate, sExtension, sFootprintForFileName);
			
		return sFileName;
	}
	
	private String getFootprintForFileName(String sFootprint, String sDatasetName) {
		String sFootprintForFileName = "";
		
		if (sFootprint == null || sFootprint.contains("null")) {
			sFootprintForFileName = QueryExecutorCDS.getFootprintForFileName(null, null, null, null, sDatasetName);
		} else {
			String[] asFootprint = sFootprint.split(", ");
			if (asFootprint.length == 4) {
				double dNorth = Double.parseDouble(asFootprint[0]);
				double dWest = Double.parseDouble(asFootprint[1]);
				double dSouth = Double.parseDouble(asFootprint[2]);
				double dEast = Double.parseDouble(asFootprint[3]);
				sFootprintForFileName = QueryExecutorCDS.getFootprintForFileName(dNorth, dWest, dSouth, dEast, sDatasetName);
			}
		}
		return sFootprintForFileName;
	}
	
	private String getFileName(String sDataset, String sVariables, String sDailyDate, String sStartDate, String sEndDate, String sExtension, String sFootprint) {
	return Utils.isNullOrEmpty(sStartDate) || Utils.isNullOrEmpty(sEndDate) 
			? String.join("_", Platforms.ERA5, sDataset, sVariables, sDailyDate, sFootprint).replaceAll("[\\W]", "_") + sExtension
			: String.join("_", Platforms.ERA5, sDataset, sVariables, sStartDate, sEndDate, sFootprint).replaceAll("[\\W]", "_") + sExtension;
	}


	@Override
	protected void internalReadConfig() {
		
	}

	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
		
		if (sPlatformType.equals(Platforms.ERA5)) {
			return DataProviderScores.SLOW_DOWNLOAD.getValue();
		}
		
		return 0;
	}

}
