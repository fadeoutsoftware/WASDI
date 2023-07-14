package wasdi.dataproviders;

import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.queryexecutors.creodias.ResponseTranslatorCREODIAS;
import wasdi.shared.queryexecutors.creodias2.ResponseTranslatorCreoDias2;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;

public class CreoDias2ProviderAdapter extends ProviderAdapter {
	
	private static final String SODATA_ATTRIBUTES = "Attributes";
	private static final String SODATA_NAME = "Name";
	private static final String SODATA_VALUE = "Value";
	private static final String SODATA_SIZE = "ContentLength";
	
	private static final String SAUTHENTICATION_URL = "https://identity.cloudferro.com/auth/realms/wekeo-elasticity/protocol/openid-connect/token";
	private static final String SDOWNLOAD_URL_START = "https://zipper.dataspace.copernicus.eu/odata/v1/Products(";
	private static final String SDOWNLOAD_URL_END = ")/$value?token=";
	
	private static final String SODATA_FILE_URL_START = "https://datahub.creodias.eu/odata/v1/Products(";
	private static final String SODATA_FILE_URL_END = ")/$value";
	
	private static final String SCREODIAS_BASE_URL = "https://datahub.creodias.eu/odata/v1/Products?";
	
	public CreoDias2ProviderAdapter() {
		super();
		m_sDataProviderCode = "CREODIAS2";
	}
	

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		// receives the file URI and must return the size of the file. Useful to give progress to the user
		// TODO: are we are talking about the overall file size? or just the size has been downloaded until a certain point?
		
		WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize. Retrieving information for product: " + sFileURL);
		
		// create a creodias query like: https://datahub.creodias.eu/odata/v1/Products?$filter=Id eq '79d7807b-66e5-41d2-a586-1628eb82c3e0'&$expand=Attributes
		String sProductId = getProductIdFromURL(sFileURL);
		String sUrl = SCREODIAS_BASE_URL + "$filter=Id eq '" + sProductId + "'&$expand=Attributes";
		HttpCallResponse sResponse = null;
		
		try {
			sResponse = HttpUtils.httpGet(sUrl);
			if (sResponse == null || sResponse.getResponseCode() < 200 || sResponse.getResponseCode() > 299 || Utils.isNullOrEmpty(sResponse.getResponseBody())) {
				WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize. Error retrieving the information about the product from the provider. " + sUrl);
				return -1L;
			}
		} catch (Exception oEx) {
			WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize. An exception occurred while retrieving the product information from the provider. " + oEx.getMessage());
			return -1L;
		}
		
		WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize. Anwer well received from the provider");
		
		JSONObject oJsonBody = new JSONObject(sResponse.getResponseBody());
		JSONArray aoJsonAttributes = oJsonBody.optJSONArray(SODATA_ATTRIBUTES);
		
		if (aoJsonAttributes == null) 
			WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize. Array of attributes not found for key " + SODATA_ATTRIBUTES);
		
		String sFileSize = getAttribute(aoJsonAttributes, SODATA_SIZE);
		
		if (Utils.isNullOrEmpty(sFileSize)) {
			WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize. Value not found for attribute name: " + SODATA_SIZE);
			return -1L;
		}
		
		WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize. File size: " + sFileSize);

		
		return Long.parseLong(sFileSize);
	}
	
	@Override
	protected void internalReadConfig() {
		try {
			m_sDefaultProtocol = m_oDataProviderConfig.defaultProtocol; 
		} catch (Exception e) {
			WasdiLog.errorLog("CreoDias2ProvierAdapter: Config reader is null");
		}
	}
	
	
	private String getAttribute(JSONArray aoAttributes, String sAttributeName) {
		for (Object oAtt : aoAttributes) {
			JSONObject oJsonAtt = (JSONObject) oAtt;
			if (oJsonAtt.get(SODATA_NAME).equals(sAttributeName))
				return oJsonAtt.optString(SODATA_VALUE);
		}
		return "";
	}
	
	
	private String getProductIdFromURL(String sFileUrl) {
		// url in input is something like: https://datahub.creodias.eu/odata/v1/Products(a6212de3-f2e4-58c2-840b-7f42c3c8c612)/$value
		return sFileUrl.replace(SODATA_FILE_URL_START, "").replace(SODATA_FILE_URL_END, "");
	}
	

	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {
		// main method. sFileUrl is the url received by the lined data provider. sSaveDirOnServer is the local folder. 
		// must return the valid file full path or "" if the download was not possible. 
		
		WasdiLog.debugLog("CreoDias2ProviderAdaper.executeDownloadFile. Execute download for file: " + sFileURL);

		
		int iAttemptCount = 0;
		int iWaitDelta = 100;
		long lLowerWatingTime = 1L;
		long lUpperWatingTime = 10L;
		
		while (iAttemptCount < iMaxRetry) {
			String sAccessToken = getAuthenticationToken(sDownloadUser, sDownloadPassword);
			
			if (Utils.isNullOrEmpty(sAccessToken)) {
				WasdiLog.debugLog("CreoDias2ProviderAdaper.executeDownloadFile. Error retrieving the access token. Impossible to continue.");
				// TODO: better to return an empty string or null?
				return "";
			}
			
			WasdiLog.debugLog("CreoDias2ProviderAdaper.executeDownloadFile. Access token correctly received.");

			String sProductId = getProductIdFromURL(sFileURL);
			// with the auth token, we can send the download request
			String sDownloadUrl = SDOWNLOAD_URL_START + sProductId + SDOWNLOAD_URL_END + sAccessToken;
			
			WasdiLog.debugLog("CreoDias2ProviderAdaper.executeDownloadFile. Access token correctly received. Download url (withouth accesso token: " + SDOWNLOAD_URL_START + sProductId + SDOWNLOAD_URL_END);

			
			// TODO: understand if I should also pass the name of the file - I think no. The method already parses the "File-disposition" attribute in the header. Should also work for Creodias2
			String sDownloadedFilePath = downloadViaHttp(sDownloadUrl, null, sSaveDirOnServer);
			
			if(Utils.isNullOrEmpty(sDownloadedFilePath)) {
				// we will try again
				iAttemptCount++;
				long lRandomWaitSeconds = new SecureRandom().longs(lLowerWatingTime, lUpperWatingTime).findFirst().getAsLong();
				//prepare to wait longer next time
				lLowerWatingTime = lRandomWaitSeconds;
				lUpperWatingTime += iWaitDelta;
				WasdiLog.warnLog("CreoDias2ProviderAdapter.executeDownloadFile. Download failed. Trying again after waiting  " + lRandomWaitSeconds +" seconds.");
				TimeUnit.SECONDS.sleep(lRandomWaitSeconds);
			} else {
				// download completed
				WasdiLog.debugLog("CreoDias2ProviderAdapter.executeDownloadFile. Download completed: " + sDownloadedFilePath);
				return sDownloadedFilePath;
			}
		}
		// TODO: better to return an empty string or null? 
		return "";
	}
	
	private String getAuthenticationToken(String sUsername, String sPassword) {
		String sPayload = "client_id=CLOUDFERRO_PUBLIC&password=" + sPassword + "&username=" + sUsername + "&grant_type=password";
		HttpCallResponse oResponse = null;
		try {
			oResponse = HttpUtils.httpPost(SAUTHENTICATION_URL, sPayload);
			if (oResponse == null ||  oResponse.getResponseCode() < 200 || oResponse.getResponseCode() > 299) {
				WasdiLog.debugLog("CreoDias2ProviderAdaper.getAuthenticationToken. Error code while trying to retrieve the auth token.");
				return "";
			} 
		} catch (Exception oEx) {
			WasdiLog.debugLog("CreoDias2ProviderAdaper.getAuthenticationToken. Error while trying to retrieve the authentication token." + oEx.getMessage());
			return "";
		}
		
		JSONObject oResponseBody = new JSONObject(oResponse.getResponseBody());
		String sAccessToken = oResponseBody.optString("access_token", "");
		
		if (Utils.isNullOrEmpty(sAccessToken)) {
			WasdiLog.debugLog("CreoDias2ProviderAdaper.executeDownloadFile. No access token found.");
			return "";
		}
		
		return sAccessToken;
	}

	@Override
	public String getFileName(String sFileURL) throws Exception {
		// extracts the file name from the URL
		WasdiLog.debugLog("CreoDias2ProviderAdaper.getFileName. Retrieving the product's file name from URL: " + sFileURL);
		
		if (Utils.isNullOrEmpty(sFileURL)) {
			WasdiLog.errorLog("CreoDias2ProviderAdaper.getFileName: sFileURL is null or Empty");
			return "";
		}

		String sResult = "";
		if (isHttpsProtocol(sFileURL)) {
			try {
				String sProductId = getProductIdFromURL(sFileURL);
				String sUrl = SCREODIAS_BASE_URL + "$filter=Id eq '" + sProductId;
				HttpCallResponse sResponse = null;
				
				WasdiLog.debugLog("CreoDias2ProviderAdaper.getFileName. Retrieve file at http URL: " + sUrl);

				try {
					sResponse = HttpUtils.httpGet(sUrl);
					if (sResponse == null || sResponse.getResponseCode() < 200 || sResponse.getResponseCode() > 299 || Utils.isNullOrEmpty(sResponse.getResponseBody())) {
						WasdiLog.debugLog("CreoDias2ProviderAdaper.getFileName. Error retrieving the information about the product from the provider. " + sUrl);
						return "";
					}
				} catch (Exception oEx) {
					WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize. An exception occurred while retrieving the product information from the provider. " + oEx.getMessage());
					return "";
				}
				
				WasdiLog.debugLog("CreoDias2ProviderAdaper.getFileName. Response correctly received from the data provider.");

				
				JSONObject oJsonBody = new JSONObject(sResponse.getResponseBody());
				
				String sFileName = oJsonBody.optString(SODATA_NAME);
				
				if (Utils.isNullOrEmpty(sFileName)) 
					WasdiLog.debugLog("CreoDias2ProviderAdaper.getFileName. File name was not found under the key " + SODATA_NAME);
				
				sResult = ResponseTranslatorCreoDias2.removeExtensionFromProductTitle(sFileName);
				
				WasdiLog.debugLog("CreoDias2ProviderAdaper.getFileName. File name without extension: " + sResult);
			
			} catch (Exception oEx) {
				WasdiLog.debugLog("CreoDias2ProviderAdaper.getFileName. Exception during the retrieval of the file name from http url " + oEx.getMessage());
				return "";
			}
		} else if (isFileProtocol(sFileURL)) {
			
			WasdiLog.debugLog("CreoDias2ProviderAdaper.getFileName. Retrieve file at local path: " + sFileURL);

			String[] asParts = sFileURL.split("/");

			if (asParts != null && asParts.length > 1) {
				sResult = asParts[asParts.length-1];
			}
			
			WasdiLog.debugLog("CreoDias2ProviderAdaper.getFileName. File name without extension: " + sResult);
		}

		return sResult;
	}

	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
		// TODO Auto-generated method stub
		// returns the score auto-evaluated by the Provider Adapter to download sFileName of sPlatformType.
		return 0;
	}
	
	public static void main(String[]args) throws Exception {
		String sUrl = "https://identity.cloudferro.com/auth/realms/wekeo-elasticity/protocol/openid-connect/token";
		URL oURL = new URL(sUrl);

		HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();
		oConnection.setRequestMethod("POST");
		oConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		oConnection.setDoOutput(true);
		String sDownloadPassword = "******";
		String sDownloadUser = "******";
		String sPayload = "client_id=CLOUDFERRO_PUBLIC&password=" + sDownloadPassword + "&username=" + sDownloadUser + "&grant_type=password";

		HttpCallResponse oResponse = HttpUtils.httpPost(sUrl, sPayload);
		System.out.println(oResponse.getResponseBody());
		JSONObject oResponseBody = new JSONObject(oResponse.getResponseBody());
		String sAccessToken = oResponseBody.optString("access_token");
		
		String otherUrl = "https://zipper.dataspace.copernicus.eu/odata/v1/Products(702b4faf-16d5-4450-9f61-4d0a13f96794)/$value?token=" + sAccessToken;
		
		HttpCallResponse oResponse2 = HttpUtils.httpGet(otherUrl);
		
		System.out.println(oResponse2);


	}
	
}
