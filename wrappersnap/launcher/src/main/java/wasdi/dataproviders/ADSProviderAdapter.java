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

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.gis.BoundingBoxUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;

public class ADSProviderAdapter extends ProviderAdapter {

	private static final String ADS_URL_SEARCH = "https://ads.atmosphere.copernicus.eu/api/v2/resources";
	private static final String ADS_URL_GET_STATUS = "https://ads.atmosphere.copernicus.eu/broker/api/v1/0/requests";
	private static final String s_sQueuedStatus = "queued";
	
	/**
	 * Basic constructor
	 */
	public ADSProviderAdapter() {
	}

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		return 0;
	}

	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {

		WasdiLog.debugLog("ADSProviderAdapter.executeDownloadFile: try to get " + sFileURL);

		String sDesiredFileName = oProcessWorkspace.getProductName();

		Map<String, String> aoWasdiPayload = extractWasdiPayloadFromUrl(sFileURL);
		if (aoWasdiPayload == null) {
			return null;
		}

		Map<String, Object> aoAdsPayload = prepareAdsPayload(aoWasdiPayload);
		String sPayload = JsonUtils.stringify(aoAdsPayload);

		String sDataset = JsonUtils.getProperty(aoWasdiPayload, "dataset");

		String sAdsSearchRequestState = s_sQueuedStatus;
		String sUrl = ADS_URL_SEARCH + "/" + sDataset;
		
		String sAdsSearchRequestResult = performAdsSearchRequest(sUrl, sDownloadUser, sDownloadPassword, sPayload, iMaxRetry);

		Map<String, Object> oAdsSearchRequestResult = JsonUtils.jsonToMapOfObjects(sAdsSearchRequestResult);
		sAdsSearchRequestState = (String) JsonUtils.getProperty(oAdsSearchRequestResult, "state");
		String sAdsSearchRequestId = (String) JsonUtils.getProperty(oAdsSearchRequestResult, "request_id");

		String sUrlDownload = null;

		if ("completed".equalsIgnoreCase(sAdsSearchRequestState)) {
			sUrlDownload = (String) JsonUtils.getProperty(oAdsSearchRequestResult, "location");
		} else if (s_sQueuedStatus.equalsIgnoreCase(sAdsSearchRequestState)) {
			String sAdsGetStatusRequestResult;
			Map<String, Object> oAdsGetStatusRequestResult = new HashMap<>();
			String sAdsGetStatusRequestId;
			String sAdsGetStatusRequestState = "undefined";

			for (int i = 0; i < 1440; i++) {
				WasdiLog.debugLog("ADSProviderAdapter.performAdsGetStatusRequest: attemp #" + i);
				sAdsGetStatusRequestResult = performAdsGetStatusRequest(sDownloadUser, sDownloadPassword, sAdsSearchRequestId, iMaxRetry);

				List<Map<String, Object>> aoRequests = JsonUtils.jsonToListOfMapOfObjects(sAdsGetStatusRequestResult);

				for (Map<String, Object> aoRequest : aoRequests) {
					sAdsGetStatusRequestId = (String) JsonUtils.getProperty(aoRequest, "metadata.requestId");

					if (sAdsSearchRequestId.equalsIgnoreCase(sAdsGetStatusRequestId)) {
						oAdsGetStatusRequestResult = aoRequest;
						break;
					}
				}

				sAdsGetStatusRequestState = (String) JsonUtils.getProperty(oAdsGetStatusRequestResult, "status.state");

				if (s_sQueuedStatus.equalsIgnoreCase(sAdsGetStatusRequestState)) {
					try {
						TimeUnit.SECONDS.sleep(60);
					} catch (InterruptedException oEx) {
						Thread.currentThread().interrupt();
						WasdiLog.errorLog("ADSProviderAdapter.executeDownloadFile: current thread was interrupted ", oEx);
					}
				} else if ("running".equalsIgnoreCase(sAdsGetStatusRequestState)) {
					try {
						TimeUnit.SECONDS.sleep(6);
					} catch (InterruptedException oEx) {
						Thread.currentThread().interrupt();
						WasdiLog.errorLog("ADSProviderAdapter.executeDownloadFile: current thread was interrupted", oEx);
					}
				} else if ("completed".equalsIgnoreCase(sAdsGetStatusRequestState)) {
					sUrlDownload = (String) JsonUtils.getProperty(oAdsGetStatusRequestResult, "status.data.0.location");
					break;
				} else if ("failed".equalsIgnoreCase(sAdsGetStatusRequestState)) {
					String sReason = (String) JsonUtils.getProperty(oAdsGetStatusRequestResult, "status.data.reason");
					WasdiLog.debugLog("ADSProviderAdapter.executeDownloadFile: getStatus request failed: " + sReason);
					break;
				} else {
					WasdiLog.debugLog("ADSProviderAdapter.executeDownloadFile: getStatus request is in an unknown state: " + sAdsGetStatusRequestState);
					break;
				}
			}
		}

		if (!Utils.isNullOrEmpty(sUrlDownload)) {
			String sAdsDownloadRequestResult = performAdsDownloadRequest(sUrlDownload, sDownloadUser, sDownloadPassword, sSaveDirOnServer, iMaxRetry);

			return WasdiFileUtils.renameFile(sAdsDownloadRequestResult, sDesiredFileName);
		}

		return null;
	}

	private String performAdsSearchRequest(String sUrl, String sDownloadUser, String sDownloadPassword, String sPayload, int iMaxRetry) {
		String sResult = "";

		int iAttemp = 0;

		while (Utils.isNullOrEmpty(sResult) && iAttemp < iMaxRetry) {

			if (iAttemp > 0) {
				WasdiLog.debugLog("ADSProviderAdapter.performAdsSearchRequest.httpPost: attemp #" + iAttemp);
			}

			try {
				Map<String, String> asHeaders = null;
				String sAuth = sDownloadUser + ":" + sDownloadPassword;
				HttpCallResponse oHttpCallResponse = HttpUtils.httpPost(sUrl, sPayload, asHeaders, sAuth);  
				sResult = oHttpCallResponse.getResponseBody();
			} catch (Exception oEx) {
				WasdiLog.debugLog("ADSProviderAdapter.performAdsSearchRequest: exception in http get call: " + oEx.toString());
			}

			iAttemp ++;
		}

		return sResult;
	}

	private String performAdsGetStatusRequest(String sDownloadUser, String sDownloadPassword, String sRequestId, int iMaxRetry) {
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
				WasdiLog.debugLog("ADSProviderAdapter.performAdsGetStatusRequest.httpGet: attemp #" + iAttemp);
			}

			try {
				HttpCallResponse oHttpCallResponse = HttpUtils.httpGet(ADS_URL_GET_STATUS, asHeaders); 
				sResult = oHttpCallResponse.getResponseBody();
			} catch (Exception oEx) {
				WasdiLog.debugLog("ADSProviderAdapter.performAdsGetStatusRequest: exception in http get call: " + oEx.toString());
			}

			iAttemp ++;
		}

		return sResult;
	}

	private String performAdsDownloadRequest(String sUrl, String sDownloadUser, String sDownloadPassword, String sSaveDirOnServer, int iMaxRetry) {
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
				WasdiLog.debugLog("ADSProviderAdapter.performAdsDownloadRequest.downloadViaHttp: attemp #" + iAttemp);
			}
			
			try {
				sResult = downloadViaHttp(sUrl, sDownloadUser, sDownloadPassword, sSaveDirOnServer);
			}
			catch (Exception oEx) {
				WasdiLog.debugLog("ADSProviderAdapter.executeDownloadFile: exception in download via http call: " + oEx.toString());
			}
			
			iAttemp ++;
		}
		
		return sResult;
	}

	private Map<String, String> extractWasdiPayloadFromUrl(String sUrl) {
		String sDecodedUrl = deodeUrl(sUrl);

		String sPayload = null;
		if (sDecodedUrl != null && sDecodedUrl.startsWith(ADS_URL_SEARCH) && sDecodedUrl.contains("?payload=")) {
			sPayload = sDecodedUrl.substring(sDecodedUrl.indexOf("?payload=") + 9);

			return JsonUtils.jsonToMapOfStrings(sPayload);
		}

		return null;
	}
	
	private static Map<String, Object> prepareAdsPayload(Map<String, String> aoWasdiPayload) {
		String sType = JsonUtils.getProperty(aoWasdiPayload, "type");
		String sVariables = JsonUtils.getProperty(aoWasdiPayload, "variables");
		String sDate = JsonUtils.getProperty(aoWasdiPayload, "date");
		String sBoundingBox = JsonUtils.getProperty(aoWasdiPayload, "boundingBox");
		String sFormat = JsonUtils.getProperty(aoWasdiPayload, "format");

		List<String> oaLeadtimeHours = Arrays.asList("0", "12", "18", "24", "6");

		Map<String, Object> root = new HashMap<>();
		root.put("type", sType);
		root.put("variable", sVariables);

		root.put("date", sDate);

		root.put("time", "00:00");
		root.put("leadtime_hour", oaLeadtimeHours);
		root.put("format", sFormat);

		if (sBoundingBox != null && !sBoundingBox.contains("null")) {
			List<Double> adOriginalBbox = BoundingBoxUtils.parseBoundingBox(sBoundingBox);
			List<Integer> adExpandedBbox = BoundingBoxUtils.expandBoundingBoxUpToADegree(adOriginalBbox);

			if (adExpandedBbox != null) {
				root.put("area", adExpandedBbox);
			}
		}

		return root;
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
	public String getFileName(String sFileURL, String sDownloadPath) throws Exception {
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
		String sFileName = String.join("_", Platforms.CAMS, sDataset, sVariables, sDate).replaceAll("[\\W]", "_") + sExtension;

		return sFileName;
	}


	@Override
	protected void internalReadConfig() {
		
	}

	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
		
		if (sPlatformType.equals(Platforms.CAMS)) {
			return DataProviderScores.SLOW_DOWNLOAD.getValue();
		}
		
		return 0;
	}

}
