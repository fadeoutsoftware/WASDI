package wasdi.dataproviders;

import java.net.IDN;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.copernicusDataspace.ResponseTranslatorCopernicusDataspace;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;

public class CopernicusDataspaceProviderAdapter extends ProviderAdapter {
	
	private static final String sCOPERNICUS_DATASPACE_BASE_URL = "https://catalogue.dataspace.copernicus.eu/odata/v1/Products?";
	
	private static final String sODATA_FILE_URL_START = "https://download.dataspace.copernicus.eu/odata/v1/Products(";
	private static final String sODATA_FILE_URL_END = ")/$value";
	
	private static final String sAUTHENTICATION_URL = "https://identity.dataspace.copernicus.eu/auth/realms/CDSE/protocol/openid-connect/token";
	private static final String sDOWNLOAD_URL_END = "?token=";
	

	public CopernicusDataspaceProviderAdapter() {
		super();
	}

	@Override
	protected void internalReadConfig() {

	}


	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		WasdiLog.debugLog("CopernicusDataspaceProviderAdapter.getDownloadFileSize. Retrieving information for product: " + sFileURL);
		
		long lSizeInBytes = 0L;
		
		String sDownloadUrl = getODataDownloadUrl(sFileURL);
		String sProductId = getProductIdFromURL(sDownloadUrl);
		String sUrl = sCOPERNICUS_DATASPACE_BASE_URL + "$filter=Id eq '" + sProductId + "'&$expand=Attributes";
		URL oURL = new URL(sUrl);
		URI oURI = new URI(oURL.getProtocol(), oURL.getUserInfo(), IDN.toASCII(oURL.getHost()), oURL.getPort(), oURL.getPath(), oURL.getQuery(), oURL.getRef());
		
		HttpCallResponse oResponse = null;
		
		try {
			oResponse = HttpUtils.httpGet(oURI.toASCIIString());
			if (oResponse == null || oResponse.getResponseCode() < 200 || oResponse.getResponseCode() > 299 || Utils.isNullOrEmpty(oResponse.getResponseBody())) {
				WasdiLog.debugLog("CopernicusDataspaceProviderAdapter.getDownloadFileSize. Error retrieving the information about the product from the provider. " + sUrl);
				return -1L;
			}
		} catch (Exception oEx) {
			WasdiLog.debugLog("CopernicusDataspaceProviderAdapter.getDownloadFileSize. An exception occurred while retrieving the product information from the provider. " + oEx.getMessage());
			return -1L;
		}
		
		WasdiLog.debugLog("CopernicusDataspaceProviderAdapter.getDownloadFileSize. Answer well received from the provider");
			
		
		JSONObject oJsonBody = new JSONObject(oResponse.getResponseBody());
		JSONArray aoValues = oJsonBody.optJSONArray("value");
		if (aoValues == null || aoValues.length() != 1) {
			WasdiLog.debugLog("CopernicusDataspaceProviderAdapter.getDownloadFileSize. Array of attributes not found");
			return 0;
		}
		
		JSONObject oValue = (JSONObject) aoValues.get(0);
		String sFileSize = oValue.optString(ResponseTranslatorCopernicusDataspace.SODATA_SIZE, "0");
		
		
		if (Utils.isNullOrEmpty(sFileSize)) {
			WasdiLog.debugLog("CopernicusDataspaceProviderAdapter.getDownloadFileSize. Array of attributes not found for key " + ResponseTranslatorCopernicusDataspace.SODATA_SIZE);
		}
		
		lSizeInBytes = Long.parseLong(sFileSize);
		WasdiLog.debugLog("CopernicusDataspaceProviderAdapter.getDownloadFileSize. File size: " + lSizeInBytes);

		return lSizeInBytes;
	}
	
	
	private String getProductIdFromURL(String sFileUrl) {
		return sFileUrl.replace(sODATA_FILE_URL_START, "").replace(sODATA_FILE_URL_END, "");
	}
	
	
	private String getODataDownloadUrl(String sFileURL) {
		if (Utils.isNullOrEmpty(sFileURL)) {
			WasdiLog.errorLog("CopernicusDataspaceProviderAdapter..getODataDownloadUrl: Empty file URL");
			return "";
		}
		try {
			return sFileURL.split(ResponseTranslatorCopernicusDataspace.SLINK_SEPARATOR)[ResponseTranslatorCopernicusDataspace.IPOSITIONOF_LINK];
		} catch (Exception oE) {
			WasdiLog.errorLog("CopernicusDataspaceProviderAdapter..getODataDownloadUrl: " + oE);
		}
		return "";
	}

	
	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {
		int iAttemptCount = 0;
		int iWaitDelta = 100;
		long lLowerWatingTime = 1L;
		long lUpperWatingTime = 10L;
		
		String sDownloadedFilePath = null;
		
		while (iAttemptCount < iMaxRetry) {
			String sAccessToken = getAuthenticationToken(sDownloadUser, sDownloadPassword);
			
			if (Utils.isNullOrEmpty(sAccessToken)) {
				WasdiLog.warnLog("CopernicusDataspaceProviderAdapter.executeDownloadFile. Error retrieving the access token. Impossible to continue.");
				return null;
			}
			
			WasdiLog.debugLog("CopernicusDataspaceProviderAdapter.executeDownloadFile. Access token correctly received.");
			
			String sODataDownloadUrl = getODataDownloadUrl(sFileURL);
			
			Map<String, String> oHeader = new HashMap<>();
			oHeader.put("Authorization", "Bearer " + sAccessToken);
	        oHeader.put("Authorization", "Bearer " + sAccessToken);
	        oHeader.put("accept", "application/json, text/plain, */*");
	        oHeader.put("Accept-encoding", "gzip, deflate, br, zstd");
	        			
			WasdiLog.debugLog("CopernicusDataspaceProviderAdapter.executeDownloadFile. Access token correctly received. Download url (withouth accesso token): " + sODataDownloadUrl + sDOWNLOAD_URL_END);
			
			long lStartTime = System.currentTimeMillis();
			sDownloadedFilePath = downloadViaHttp(sODataDownloadUrl, oHeader, sSaveDirOnServer);
			
			if(Utils.isNullOrEmpty(sDownloadedFilePath)) {
				// we will try again
				iAttemptCount++;
				long lRandomWaitSeconds = new SecureRandom().longs(lLowerWatingTime, lUpperWatingTime).findFirst().getAsLong();
				//prepare to wait longer next time
				lLowerWatingTime = lRandomWaitSeconds;
				lUpperWatingTime += iWaitDelta;
				WasdiLog.warnLog("CopernicusDataspaceProviderAdapter.executeDownloadFile. Download failed. Trying again after waiting  " + lRandomWaitSeconds +" seconds.");
				TimeUnit.SECONDS.sleep(lRandomWaitSeconds);
			} else {
				// download completed
				long lEndTime = System.currentTimeMillis() - lStartTime;
				WasdiLog.debugLog("CopernicusDataspaceProviderAdapter.executeDownloadFile. Download completed in " + lEndTime + " milliseconds at path " + sDownloadedFilePath);
				return sDownloadedFilePath;
			}
		}
		
		return sDownloadedFilePath;

	}
	
	private String getAuthenticationToken(String sUsername, String sPassword) {
		Map<Object, Object> data = new HashMap<>();
        data.put("grant_type", "password");
        data.put("username", sUsername);
        data.put("password", sPassword);
        data.put("client_id", "cdse-public");
        
        String sEncodedFormData = getFormURLEncodedString(data);
        
		HttpCallResponse oResponse = null;
		try {
			oResponse = HttpUtils.httpPost(sAUTHENTICATION_URL, sEncodedFormData);
			if (oResponse == null ||  oResponse.getResponseCode() < 200 || oResponse.getResponseCode() > 299) {
				WasdiLog.debugLog("CopernicusDataspaceProviderAdapter.getAuthenticationToken. Error code while trying to retrieve the auth token.");
				return "";
			} 
		} catch (Exception oEx) {
			WasdiLog.debugLog("CopernicusDataspaceProviderAdapter.getAuthenticationToken. Error while trying to retrieve the authentication token." + oEx.getMessage());
			return "";
		}
		
		JSONObject oResponseBody = new JSONObject(oResponse.getResponseBody());
		String sAccessToken = oResponseBody.optString("access_token", "");
		
		if (Utils.isNullOrEmpty(sAccessToken)) {
			WasdiLog.debugLog("CopernicusDataspaceProviderAdapter.executeDownloadFile. No access token found.");
			return "";
		}
		
		return sAccessToken;
	}
	
    private String getFormURLEncodedString(Map<Object, Object> oData) {
        StringBuilder oResult = new StringBuilder();
        for (Map.Entry<Object, Object> oEntry : oData.entrySet()) {
            if (oResult.length() > 0) {
            	oResult.append("&");
            }
            oResult.append(URLEncoder.encode(oEntry.getKey().toString(), StandardCharsets.UTF_8));
            oResult.append("=");
            oResult.append(URLEncoder.encode(oEntry.getValue().toString(), StandardCharsets.UTF_8));
        }
        return oResult.toString();
    }
	

	@Override
	public String getFileName(String sFileURL, String sDownloadPath) throws Exception {
		String sResult = "";
		
		WasdiLog.debugLog("CopernicusDataspaceProviderAdapter.getFileName. Retrieving the product's file name from URL: " + sFileURL);
		
		if (Utils.isNullOrEmpty(sFileURL)) {
			WasdiLog.errorLog("CopernicusDataspaceProviderAdapter.getFileName: sFileURL is null or Empty");
			return sResult;
		}
		
		WasdiLog.debugLog("CopernicusDataspaceProviderAdapter.getFileName: http protocol - Retrieve file at local path: " + sFileURL);
		try {			
			String[] asTokens = sFileURL.split(ResponseTranslatorCopernicusDataspace.SLINK_SEPARATOR);
			sResult = asTokens[ResponseTranslatorCopernicusDataspace.IPOSITIONOF_FILENAME];
		} catch (Exception oEx) {
			WasdiLog.errorLog("CopernicusDataspaceProviderAdapter.getFileName: exception while trying to retrieve the file name." + oEx.getMessage());
			sResult = "";
		}

		return sResult;
	}
	

	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
		
		if (sPlatformType.equals(Platforms.SENTINEL1)
				|| sPlatformType.equals(Platforms.SENTINEL2)
				|| sPlatformType.equals(Platforms.SENTINEL3) 
				|| sPlatformType.equals(Platforms.SENTINEL5P)
				|| sPlatformType.equals(Platforms.SENTINEL6)) {
			return DataProviderScores.DOWNLOAD.getValue();
		}
		
		return 0;
	}
	

}
