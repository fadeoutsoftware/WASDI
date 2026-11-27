package wasdi.dataproviders;

import java.io.File;
import java.util.Map;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.wasdiAPI.AuthAPIClient;

public class Lake3dWasdiProviderAdapter extends ProviderAdapter {
	
	protected String m_sRemoteWasdiSessionId = "";
	

	public Lake3dWasdiProviderAdapter() {
		m_sDataProviderCode = "LAKE3DWASDI";
	}

	@Override
	protected void internalReadConfig() {

	}

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		
		long lFileSize = 0L;
		
		try {
		
			if (sFileURL.contains(",")) {
				WasdiLog.debugLog("Lake3dWasdiProviderAdapter.getDownloadFileSize. File size for url " + sFileURL);
				return Long.parseLong(sFileURL.split(",")[1]);
			}
			
		} catch(Exception oE) {
			WasdiLog.errorLog("Lake3dWasdiProviderAdapter.getDownloadFileSize. Error", oE);
		}
				
		return lFileSize;
	}

	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {
		
		WasdiLog.debugLog("Lake3dWasdiProviderAdapter.executeDownloadFile. Copying lake 3d file to " + sSaveDirOnServer);
		
		String sDownloadedFilePath = null;
		
		try {
			String sFileName = this.getFileName(sFileURL);
			
			String sDestinationFilePath = sSaveDirOnServer;
			if (!sDestinationFilePath.endsWith(File.separator)) sDestinationFilePath += File.separator;
			
			sDestinationFilePath += sFileName;
			
			String sHttpURL = this.m_oDataProviderConfig.link;
			
			if (!sHttpURL.endsWith("/")) sHttpURL += "/";
			
			sHttpURL += "images/get";
			//parameters
			sHttpURL += "?collection=lake3d&name=" + sFileName;
			
			m_sRemoteWasdiSessionId = AuthAPIClient.login(m_sProviderUser, m_sProviderPassword, this.m_oDataProviderConfig.link);
			
			if (Utils.isNullOrEmpty(m_sRemoteWasdiSessionId)) {
				WasdiLog.errorLog("Lake3dWasdiProviderAdapter.init: impossible to get the session id for url  " + this.m_oDataProviderConfig.link + " user " + m_sProviderUser);
			}
			
			Map<String, String> aoHeaders = Map.of("x-session-token", m_sRemoteWasdiSessionId);
				
			sDownloadedFilePath = HttpUtils.downloadFile(sHttpURL, aoHeaders, sDestinationFilePath);
			
			if (Utils.isNullOrEmpty(sDownloadedFilePath)) {
				WasdiLog.warnLog("Lake3dWasdiProviderAdapter.executeDownloadFile. Error in downloading the file");
				return null;
			}
			
			WasdiLog.debugLog("Lake3dWasdiProviderAdapter.executeDownloadFile. File saved at path " + sDownloadedFilePath);
			
			return sDownloadedFilePath;
			
		} catch (Exception oE) {
			WasdiLog.errorLog("Lake3dWasdiProviderAdapter.executeDownloadFile. Error copyting bathymetry file", oE);
		}

		return sDownloadedFilePath;
	}

	@Override
	public String getFileName(String sFileURL, String sDownloadPath) throws Exception {
		if (sFileURL.startsWith("https://") && sFileURL.contains(",")) {
			return sFileURL.replace("https://", "").split(",")[0];
		}
		return sFileURL;
	}

	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
		
		if (sPlatformType.equals(Platforms.LAKE_3DWASDI)) {
			return DataProviderScores.DOWNLOAD.getValue();
		}
		
		return 0;
		
	}
	
	
	@Override
	public void closeConnections() {
		super.closeConnections();
		if (!Utils.isNullOrEmpty(m_sRemoteWasdiSessionId)) {
			if (m_oDataProviderConfig!=null) {
				try {
					AuthAPIClient.logout(m_sRemoteWasdiSessionId, this.m_oDataProviderConfig.link);		
				}
				catch (Exception oEx) {
					WasdiLog.errorLog("Lake3dWasdiProviderAdapter.closeConnections. Error ", oEx);
				}
			}
			
		}
	}

}
