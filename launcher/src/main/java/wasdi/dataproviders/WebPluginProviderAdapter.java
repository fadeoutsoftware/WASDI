package wasdi.dataproviders;

import java.net.URLEncoder;
import java.util.Map;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;

public class WebPluginProviderAdapter extends ProviderAdapter {
	
	protected String m_sApiKey;
	protected String m_sWebServiceUrl;
	
	public WebPluginProviderAdapter() {
	}

	@Override
	protected void internalReadConfig() {
		try {
			m_sProviderUser = m_oDataProviderConfig.user;
			m_sApiKey = m_oDataProviderConfig.apiKey;
			m_sWebServiceUrl = m_oDataProviderConfig.link;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("WebPluginProviderAdapter.internalReadConfig: exception reading config files ", oEx);
		}	
		
	}

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		return 0;
	}
	
	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {
		
		try {
			
			String sUrl = m_sWebServiceUrl;
			if (!sUrl.endsWith("/")) sUrl += "/";
			sUrl += "download?fileName=";
			
			sUrl +=  URLEncoder.encode(sFileURL, java.nio.charset.StandardCharsets.UTF_8.toString()); 
			
			Map<String, String> asHeaders = HttpUtils.getBasicAuthorizationHeaders(m_sProviderUser, m_sApiKey);
			asHeaders.put("Content-Type", "application/json");
			
			if (!sSaveDirOnServer.endsWith("/")) sSaveDirOnServer += "/";
			
			String sFileName = getFileName(sFileURL);
			
			String sOutputFilePath = sSaveDirOnServer + sFileName;
			
			String sFilePath = HttpUtils.downloadFile(sUrl, asHeaders, sOutputFilePath);
			
			if (Utils.isNullOrEmpty(sFilePath)) {
				WasdiLog.errorLog("WebPluginProviderAdapter.executeDownloadFile: impossible to get a valid file Path");
				return "";
			}
			
			return sFilePath;
			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("WebPluginProviderAdapter.executeDownloadFile: exception ", oEx);
		}
		
		return "";
	}

	@Override
	public String getFileName(String sFileURL, String sDownloadPath) throws Exception {
		
		try {
			
			String sUrl = m_sWebServiceUrl;
			if (!sUrl.endsWith("/")) sUrl += "/";
			sUrl += "file";
			
			String sPayload =  URLEncoder.encode(sFileURL, java.nio.charset.StandardCharsets.UTF_8.toString()); 
			
			Map<String, String> asHeaders = HttpUtils.getBasicAuthorizationHeaders(m_sProviderUser, m_sApiKey);
			asHeaders.put("Content-Type", "application/json");
						
			HttpCallResponse oResponse = HttpUtils.httpPost(sUrl, sPayload, asHeaders);
			
			String sFileName = "";
			if (oResponse.getResponseCode()>=200 && oResponse.getResponseCode()<=299) {
				sFileName = oResponse.getResponseBody();
			}
			else {
				WasdiLog.warnLog("WebPluginProviderAdapter.getFileName: the web server returned code " + oResponse.getResponseCode() + "\nMessage: " + oResponse.getResponseBody());
			}
			
			return sFileName;
			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("WebPluginProviderAdapter.executeDownloadFile: exception ", oEx);
		}
		
		return "";
	}

	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
		return DataProviderScores.DOWNLOAD.getValue();
	}

}
