package wasdi.dataproviders;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.utils.BoundingBoxUtils;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;

public class CDSProviderAdapter extends ProviderAdapter {

	private static final String CDS_URL_SEARCH = "https://cds.climate.copernicus.eu/api/v2/resources";
	private static final String CDS_URL_GET_STATUS = "https://cds.climate.copernicus.eu/broker/api/v1/0/request/";

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		return 0;
	}

	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {

		Utils.debugLog("CDSProviderAdapter.executeDownloadFile: try to get " + sFileURL);

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


		if ("queued".equalsIgnoreCase(sCdsSearchRequestState)) {
			String sUrlRequestStatus = CDS_URL_GET_STATUS + sCdsSearchRequestId;

			String sCdsGetStatusRequestResult;
			Map<String, Object> oCdsGetStatusRequestResult;
			String sCdsGetStatusRequestState;
			
			String sUrlDownload = null;

			for (int i = 0; i < 120; i++) {
				Utils.debugLog("CDSProviderAdapter.performCdsGetStatusRequest: attemp #" + i);
				sCdsGetStatusRequestResult = performCdsGetStatusRequest(sUrlRequestStatus, sDownloadUser, sDownloadPassword, sCdsSearchRequestId, iMaxRetry);

				oCdsGetStatusRequestResult = JsonUtils.jsonToMapOfObjects(sCdsGetStatusRequestResult);
				sCdsGetStatusRequestState = (String) JsonUtils.getProperty(oCdsGetStatusRequestResult, "status.state");
				
				if ("queued".equalsIgnoreCase(sCdsGetStatusRequestState)) {
					try {
						TimeUnit.SECONDS.sleep(60);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} else if ("running".equalsIgnoreCase(sCdsGetStatusRequestState)) {
					try {
						TimeUnit.SECONDS.sleep(6);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} else if ("completed".equalsIgnoreCase(sCdsGetStatusRequestState)) {
					sUrlDownload = (String) JsonUtils.getProperty(oCdsGetStatusRequestResult, "status.data.0.location");
					break;
				} else if ("failed".equalsIgnoreCase(sCdsGetStatusRequestState)) {
					String sReason = (String) JsonUtils.getProperty(oCdsGetStatusRequestResult, "status.data.reason");
					Utils.debugLog("CDSProviderAdapter.executeDownloadFile: getStatus request failed: " + sReason);
					break;
				} else {
					Utils.debugLog("CDSProviderAdapter.executeDownloadFile: getStatus request is in an unknown state: " + sCdsGetStatusRequestState);
					break;
				}
			}

			if (!Utils.isNullOrEmpty(sUrlDownload)) {
				String sCdsDownloadRequestResult = performCdsDownloadRequest(sUrlDownload, sDownloadUser, sDownloadPassword, sSaveDirOnServer, iMaxRetry);

				return WasdiFileUtils.renameFile(sCdsDownloadRequestResult, sDesiredFileName);
			}

		}

		return null;
	}

	private String performCdsSearchRequest(String sUrl, String sDownloadUser, String sDownloadPassword, String sPayload, int iMaxRetry) {
		String sResult = "";

		int iAttemp = 0;

		while (Utils.isNullOrEmpty(sResult) && iAttemp < iMaxRetry) {

			if (iAttemp > 0) {
				Utils.debugLog("CDSProviderAdapter.performCdsSearchRequest.httpPost: attemp #" + iAttemp);
			}

			try {
				Map<String, String> asHeaders = null;
				String sAuth = sDownloadUser + ":" + sDownloadPassword;
				sResult = HttpUtils.httpPost(sUrl, sPayload, asHeaders, sAuth);
			} catch (Exception oEx) {
				Utils.debugLog("CDSProviderAdapter.performCdsSearchRequest: exception in http get call: " + oEx.toString());
			}

			iAttemp ++;
		}

		return sResult;
	}

	private String performCdsGetStatusRequest(String sUrl, String sDownloadUser, String sDownloadPassword, String sRequestId, int iMaxRetry) {
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
				Utils.debugLog("CDSProviderAdapter.performCdsGetStatusRequest.httpGet: attemp #" + iAttemp);
			}

			try {
				sResult = HttpUtils.httpGet(sUrl, asHeaders);
			} catch (Exception oEx) {
				Utils.debugLog("CDSProviderAdapter.performCdsGetStatusRequest: exception in http get call: " + oEx.toString());
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
				Utils.debugLog("CDSProviderAdapter.performCdsDownloadRequest.downloadViaHttp: attemp #" + iAttemp);
			}
			
			try {
				sResult = downloadViaHttp(sUrl, sDownloadUser, sDownloadPassword, sSaveDirOnServer);
			}
			catch (Exception oEx) {
				Utils.debugLog("LSAProviderAdapter.executeDownloadFile: exception in download via http call: " + oEx.toString());
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
		String sDate = JsonUtils.getProperty(aoWasdiPayload, "date");
		String sBoundingBox = JsonUtils.getProperty(aoWasdiPayload, "boundingBox");
		String sFormat = JsonUtils.getProperty(aoWasdiPayload, "format");

		List<String> oaVariables = Arrays.stream(sVariables.split(" "))
				.map(CDSProviderAdapter::inflateVariable)
				.collect(Collectors.toList());

		String sYear = sDate.substring(0, 4);
		String sMonth = sDate.substring(4, 6);
		String sDay = sDate.substring(6, 8);

		List<String> oaTimeHours = Arrays.asList("00:00", "01:00", "02:00", "03:00", "04:00", "05:00", "06:00", "07:00", "08:00", "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00", "19:00", "20:00", "21:00", "22:00", "23:00");

		List<Double> grid = Arrays.asList(0.25, 0.25);

		Map<String, Object> root = new HashMap<>();
		root.put("product_type", sProductType);
		root.put("variable", oaVariables);
		root.put("year", sYear);
		root.put("month", sMonth);
		root.put("day", sDay);
		root.put("time", oaTimeHours);
		root.put("grid", grid);
		root.put("format", sFormat);

		if (sBoundingBox != null && !sBoundingBox.contains("null")) {
			List<Double> originalBbox = BoundingBoxUtils.parseBoundingBox(sBoundingBox);
			List<Double> expandedBbox = BoundingBoxUtils.expandBoundingBoxUpToAQuarterDegree(originalBbox);

			if (expandedBbox != null) {
				root.put("area", expandedBbox);
			}
		}

		if (sDataset.equalsIgnoreCase("reanalysis-era5-pressure-levels")) {
			String sPresureLevels = JsonUtils.getProperty(aoWasdiPayload, "presureLevels");
			List<String> oaPressureLevels = Arrays.asList(sPresureLevels.split(" "));

			root.put("pressure_level", oaPressureLevels);
		}

		return root;
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
			Utils.debugLog("CDSProviderAdapter.inflateVariable: unexpected variable: " + sVariable + ".");
			return sVariable;
		}
	}

	private static String deodeUrl(String sUrlEncoded) {
		try {
			return URLDecoder.decode(sUrlEncoded, java.nio.charset.StandardCharsets.UTF_8.toString());
		} catch (UnsupportedEncodingException oE) {
			Utils.debugLog("Wasdi.deodeUrl: could not decode URL due to " + oE + ".");
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
		String sDate = JsonUtils.getProperty(aoWasdiPayload, "date");
		String sFormat = JsonUtils.getProperty(aoWasdiPayload, "format");

		String sExtension = "." + sFormat;

		// filename: reanalysis-era5-pressure-levels_UV_20211201
		String sFileName = String.join("_", sDataset, sVariables, sDate).replaceAll("[\\W]", "_") + sExtension;

		return sFileName;
	}

	@Override
	public void readConfig() {
		
	}

}
