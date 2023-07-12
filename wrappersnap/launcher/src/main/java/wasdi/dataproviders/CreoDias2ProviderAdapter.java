package wasdi.dataproviders;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.net.io.Util;
import org.json.JSONObject;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.utils.log.WasdiLog;

public class CreoDias2ProviderAdapter extends ProviderAdapter {
	
	private static final String SAUTHENTICATION_URL = "https://identity.cloudferro.com/auth/realms/wekeo-elasticity/protocol/openid-connect/token";
	
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
		// TODO Auto-generated method stub
		// receives the file URI and must return the size of the file. Useful to give progress to the user
		return 0;
	}

	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {
		// TODO Auto-generated method stub
		// main method. sFileUrl is the url received by the lined data provider. sSaveDirOnSerer is the local folder. 
		// must return the valid file full path or "" if the download was not possible. 
		
		
		
		return null;
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
		URL oURL = new URL("https://identity.cloudferro.com/auth/realms/wekeo-elasticity/protocol/openid-connect/token");

		HttpURLConnection oConnection = (HttpURLConnection) oURL.openConnection();
		oConnection.setRequestMethod("POST");
		oConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		oConnection.setDoOutput(true);
		String sDownloadPassword = "**********";
		String sDownloadUser = "**********";
		String totp = "140043";
		oConnection.getOutputStream().write(("client_id=CLOUDFERRO_PUBLIC&password=" + sDownloadPassword + "&username=" + sDownloadUser + "&grant_type=password").getBytes());
		int iStatus = oConnection.getResponseCode();
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


	}
	
}
