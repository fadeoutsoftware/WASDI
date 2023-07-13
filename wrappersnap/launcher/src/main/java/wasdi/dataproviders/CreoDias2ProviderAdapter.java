package wasdi.dataproviders;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.net.io.Util;
import org.checkerframework.checker.units.qual.A;
import org.json.JSONObject;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.LoggerWrapper;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;

public class CreoDias2ProviderAdapter extends ProviderAdapter {
	
	private static final String SAUTHENTICATION_URL = "https://identity.cloudferro.com/auth/realms/wekeo-elasticity/protocol/openid-connect/token";
	private static final String SDOWNLOAD_URL = "https://zipper.dataspace.copernicus.eu/odata/v1/Products(702b4faf-16d5-4450-9f61-4d0a13f96794)/$value?token=";
	
	public CreoDias2ProviderAdapter() {
		super();
		m_sDataProviderCode = "CREODIAS2";
	}
	

	@Override
	protected void internalReadConfig() {
		// TODO Auto-generated method srub. 
		// read from WasdiConfig specific configurations
	}

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		// receives the file URI and must return the size of the file. Useful to give progress to the user
		// TODO: are we are talking about the overall file size? or just the size has been downloaded until a certain point?
		return 0;
	}

	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {
		// TODO Auto-generated method stub
		// main method. sFileUrl is the url received by the lined data provider. sSaveDirOnServer is the local folder. 
		// must return the valid file full path or "" if the download was not possible. 
		
		String sAccessToken = getAuthenticationToken(sDownloadUser, sDownloadPassword);
		
		if (Utils.isNullOrEmpty(sAccessToken)) {
			WasdiLog.debugLog("CreoDias2ProviderAdaper.executeDownloadFile. Error retrieving the access token. Impossible to continue.");
			return "";
		}
		
		// with the auth token, we can send the download request
		String sDownloadUrl = SDOWNLOAD_URL + sAccessToken;
		
		// TODO: understand if I can should also pass the name of the file 
		String sDownloadedFilePath = downloadViaHttp(sFileURL, null, sSaveDirOnServer);
		if(Utils.isNullOrEmpty(sDownloadedFilePath)) {
			//try again
//			++iAttempt;
//			long lRandomWaitSeconds = new SecureRandom().longs(lLo, lUp).findFirst().getAsLong();
			//prepare to wait longer next time
//			lLo = lRandomWaitSeconds;
//			lUp += lWaitStep;
//			WasdiLog.warnLog("CreoDias2ProviderAdapter.executeDownloadFile. Download failed. Trying again after waoting  " + lRandomWaitSeconds +" seconds...");
//			TimeUnit.SECONDS.sleep(lRandomWaitSeconds);
		} else {
			//we're done
			WasdiLog.debugLog("CreoDias2ProviderAdapter.executeDownloadFile. Download completed: " + sDownloadedFilePath);
//			break;
		}
		
		return null;
	}
	
	private String getAuthenticationToken(String sUsername, String sPassword) {
		String sPayload = "client_id=CLOUDFERRO_PUBLIC&password=" + sPassword + "&username=" + sUsername + "&grant_type=password";
		HttpCallResponse oResponse = null;
		try {
			oResponse = HttpUtils.httpPost(SAUTHENTICATION_URL, sPayload);
			int iResponseCode = oResponse.getResponseCode();
			if (oResponse == null ||  oResponse.getResponseCode() < 200 || oResponse.getResponseCode() > 299) {
				WasdiLog.debugLog("CreoDias2ProviderAdaper.getAuthenticationToken. Error code " + iResponseCode + " while trying to retrieve the auth token.");
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
		// TODO Auto-generated method stub
		// extracts the file name from the URL
		return null;
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
		oConnection.getOutputStream().write(("client_id=CLOUDFERRO_PUBLIC&password=" + sDownloadPassword + "&username=" + sDownloadUser + "&grant_type=password").getBytes());
/*		int iStatus = oConnection.getResponseCode();
		WasdiLog.debugLog("CREODIASProviderAdapter.obtainKeycloakToken: Response status: " + iStatus);
		if( iStatus == 200) {
			InputStream oInputStream = oConnection.getInputStream();
			ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
			if(null!=oInputStream) {
				Util.copyStream(oInputStream, oBytearrayOutputStream);
				String sResult = oBytearrayOutputStream.toString();
				//WasdiLog.debugLog("CREODIASProviderAdapter.obtainKeycloakToken: json: " + sResult);
				JSONObject oJson = new JSONObject(sResult);
				String sToken = oJson.optString("access_token", null);
				System.out.println(sToken);
			}
		} else {
			ByteArrayOutputStream oBytearrayOutputStream = new ByteArrayOutputStream();
			InputStream oErrorStream = oConnection.getErrorStream();
			Util.copyStream(oErrorStream, oBytearrayOutputStream);
			String sMessage = oBytearrayOutputStream.toString();
			WasdiLog.debugLog("CREODIASProviderAdapter.obtainKeycloakToken:" + sMessage);

		}
		*/
		HttpCallResponse oResponse = HttpUtils.httpPost(sUrl, sPayload);
		System.out.println(oResponse.getResponseBody());
		JSONObject oResponseBody = new JSONObject(oResponse.getResponseBody());
		String sAccessToken = oResponseBody.optString("access_token");
		
		String otherUrl = "https://zipper.dataspace.copernicus.eu/odata/v1/Products(702b4faf-16d5-4450-9f61-4d0a13f96794)/$value?token=" + sAccessToken;
		
		HttpCallResponse oResponse2 = HttpUtils.httpGet(otherUrl);
		
		System.out.println(oResponse2);
		
		
		


	}
	
}
