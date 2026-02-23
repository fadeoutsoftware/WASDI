package wasdi.dataproviders;

import java.util.Map;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class GlobathyProviderAdapter extends ProviderAdapter {
	

	public GlobathyProviderAdapter() {
		m_sDataProviderCode = "GLOBATHY";
	}

	@Override
	protected void internalReadConfig() {

	}

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		
		long lFileSize = 0L;
		
		try {
		
			if (sFileURL.contains(",")) {
				WasdiLog.debugLog("GlobathyProviderAdapter.getDownloadFileSize. File size for url " + sFileURL);
				return Long.parseLong(sFileURL.split(",")[1]);
			}
			
		} catch(Exception oE) {
			WasdiLog.errorLog("GlobathyProviderAdapter.getDownloadFileSize. Error", oE);
		}
				
		return lFileSize;
	}

	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {
		
		WasdiLog.debugLog("GlobathyProviderAdapter.executeDownloadFile. Copying bathymetry file to " + sSaveDirOnServer);
		
		String sDownloadedFilePath = null;
		
		try {
			String sFileName = this.getFileName(sFileURL);
			
			// get the subfolder in the 1000 repartition of files
			String sSubfolderName = getSubFolderName(sFileName);
			
			if (Utils.isNullOrEmpty(sSubfolderName)) {
				WasdiLog.warnLog("GlobathyProviderAdapter.executeDownloadFile. Could not determine subfolder name for url " + sFileURL);
				return null;
			}
			
			String sSessionId = this.m_sSession;
			
			if (Utils.isNullOrEmpty(sSessionId)) {
				WasdiLog.warnLog("GlobathyProviderAdapter.executeDownloadFile. No session id. Impossible to call APIs");
				return null;
			}
			
			String sDestinationFilePath = sSaveDirOnServer;
			if (!sDestinationFilePath.endsWith(File.separator))
				sDestinationFilePath += File.separator;
			sDestinationFilePath += sFileName;
			
			String sHttpURL = WasdiConfig.Current.baseUrl;
			if (!sHttpURL.endsWith(File.separator))
				sHttpURL += File.separator;
			sHttpURL += "images/get";
			//parameters
			sHttpURL += "?collection=globathy"
					+ "&folder=" + sSubfolderName 
					+ "&name=" + sFileName;
			
			Map<String, String> aoHeaders = Map.of("x-session-token", sSessionId);
				
			sDownloadedFilePath = HttpUtils.downloadFile(sHttpURL, aoHeaders, sDestinationFilePath);
			
			if (Utils.isNullOrEmpty(sDownloadedFilePath)) {
				WasdiLog.warnLog("GlobathyProviderAdapter.executeDownloadFile. Error in downloading the file");
				return null;
			}
			
			WasdiLog.debugLog("GlobathyProviderAdapter.executeDownloadFile. File saved at path " + sDownloadedFilePath);
			
			return sDownloadedFilePath;
			
		} catch (Exception oE) {
			WasdiLog.errorLog("GlobathyProviderAdapter.executeDownloadFile. Error copyting bathymetry file", oE);
		}

		return sDownloadedFilePath;
	}

	@Override
	public String getFileName(String sFileURL, String sDownloadPath) throws Exception {
		if (sFileURL.startsWith("https://")) {
			return sFileURL.replace("https://", "");
		}
		return sFileURL;
	}

	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
		
		if (sPlatformType.equals(Platforms.GLOBATHY)) {
			return DataProviderScores.FILE_ACCESS.getValue();
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

}
