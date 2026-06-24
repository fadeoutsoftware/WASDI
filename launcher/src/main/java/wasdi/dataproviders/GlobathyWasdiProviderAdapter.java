package wasdi.dataproviders;

import java.io.File;
import java.util.Map;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.wasdiAPI.AuthAPIClient;

public class GlobathyWasdiProviderAdapter extends ProviderAdapter {
	
	protected String m_sRemoteWasdiSessionId = "";
	

	public GlobathyWasdiProviderAdapter() {
		m_sDataProviderCode = "GLOBATHYWASDI";
	}

	@Override
	protected void internalReadConfig() {

	}

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		
		long lFileSize = 0L;
		
		try {
		
			if (sFileURL.contains(",")) {
				WasdiLog.debugLog("GlobathyWasdiProviderAdapter.getDownloadFileSize. File size for url " + sFileURL);
				return Long.parseLong(sFileURL.split(",")[1]);
			}
			
		} catch(Exception oE) {
			WasdiLog.errorLog("GlobathyWasdiProviderAdapter.getDownloadFileSize. Error", oE);
		}
				
		return lFileSize;
	}

	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {
		
		WasdiLog.debugLog("GlobathyWasdiProviderAdapter.executeDownloadFile. Copying bathymetry file to " + sSaveDirOnServer);
		
		String sDownloadedFilePath = null;
		
		try {
			String sFileName = this.getFileName(sFileURL);
			
			// get the subfolder in the 1000 repartition of files
			String sSubfolderName = getSubFolderName(sFileName);
			
			if (Utils.isNullOrEmpty(sSubfolderName)) {
				WasdiLog.warnLog("GlobathyWasdiProviderAdapter.executeDownloadFile. Could not determine subfolder name for url " + sFileURL);
				return null;
			}
			
			String sDestinationFilePath = sSaveDirOnServer;
			if (!sDestinationFilePath.endsWith(File.separator))
				sDestinationFilePath += File.separator;
			sDestinationFilePath += sFileName;
			
			String sHttpURL = this.m_oDataProviderConfig.link;
			if (!sHttpURL.endsWith("/"))
				sHttpURL += "/";
			sHttpURL += "images/get";
			//parameters
			sHttpURL += "?collection=globathy"
					+ "&folder=" + sSubfolderName 
					+ "&name=" + sFileName;
			
			m_sRemoteWasdiSessionId = AuthAPIClient.login(m_sProviderUser, m_sProviderPassword, this.m_oDataProviderConfig.link);
			
			if (Utils.isNullOrEmpty(m_sRemoteWasdiSessionId)) {
				WasdiLog.errorLog("QueryExecutorGlobathyWasdi.init: impossible to get the session id for url  " + this.m_oDataProviderConfig.link + " user " + m_sProviderUser);
			}
			
			Map<String, String> aoHeaders = Map.of("x-session-token", m_sRemoteWasdiSessionId);
				
			sDownloadedFilePath = HttpUtils.downloadFile(sHttpURL, aoHeaders, sDestinationFilePath);
			
			if (Utils.isNullOrEmpty(sDownloadedFilePath)) {
				WasdiLog.warnLog("GlobathyWasdiProviderAdapter.executeDownloadFile. Error in downloading the file");
				return null;
			}
			
			WasdiLog.debugLog("GlobathyWasdiProviderAdapter.executeDownloadFile. File saved at path " + sDownloadedFilePath);
			
			return sDownloadedFilePath;
			
		} catch (Exception oE) {
			WasdiLog.errorLog("GlobathyWasdiProviderAdapter.executeDownloadFile. Error copyting bathymetry file", oE);
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
		
		if (sPlatformType.equals(Platforms.GLOBATHYWASDI)) {
			return DataProviderScores.DOWNLOAD.getValue();
		}
		
		return 0;
		
	}
	
	
	/**
	 * Returns the name of the subfolder (e.g., "301001_302000") given the Lake ID.
	 * This logic handles the 1,000 elements partitioning.
	 * @param iHylakId The unique ID of the lake (e.g., 302000)
	 * @return String representing the subfolder name range
	 */
	public String getSubFolderName(String sFileName) {
		
		if (Utils.isNullOrEmpty(sFileName)) {
			return null;
		}
		
		try {
			String sLakeId = sFileName.replace("_bathymetry.tif", "");
			
			int iLakeId = Integer.parseInt(sLakeId);
	
		    // Calculate the upper bound of the 1,000-unit range
		    // Example: 302000 -> ((301999 / 1000) + 1) * 1000 = 302000
		    // Example: 301001 -> ((301000 / 1000) + 1) * 1000 = 302000
		    int iEndId = ((iLakeId - 1) / 1000 + 1) * 1000;
	
		    int iStartId = iEndId - 999;
	
		    String sSubFolderName = iStartId + "_" + iEndId;
	
		    return sSubFolderName;
		}
		catch (Exception oE) {
			WasdiLog.errorLog("GlobathyProvider.getSubFolderName. Error ", oE);
		}
		
		return null;
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
					WasdiLog.errorLog("GlobathyProvider.closeConnections. Error ", oEx);
				}
			}
			
		}
	}

}
