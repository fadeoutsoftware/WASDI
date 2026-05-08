package wasdi.dataproviders;

import java.util.concurrent.TimeUnit;

import java.util.HashMap;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class DLRProviderAdapter extends ProviderAdapter {

	public DLRProviderAdapter() {
	}

	@Override
	protected void internalReadConfig() {
	}

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		long lFileSize = 0L;
		
		if (Utils.isNullOrEmpty(sFileURL)) {
			WasdiLog.warnLog("DLRProviderAdapter.getDownloadFileSize. The file URL is null or empty");
			return lFileSize;
		}
		
		try {
			
			String[] asLinkInfos = sFileURL.split(";");
			String sFileSize = asLinkInfos[1];
			lFileSize = Long.parseLong(sFileSize);
			
			if (Utils.isNullOrEmpty(sFileSize)) {
				WasdiLog.warnLog("DLRProviderAdapter.getDownloadFileSize. The link does not contain information about the size " + sFileURL);
				return lFileSize;
			}
			
			WasdiLog.debugLog("DLRProviderAdapter.getDownloadFileSize. File size in bytes " + sFileSize);
			return lFileSize;
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("DLRProviderAdapter.getDownloadFileSize. ", oEx);
			return lFileSize;
		}
	}

	
	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {
		
		String sResult = null;
		
		if (Utils.isNullOrEmpty(sFileURL)) {
			WasdiLog.warnLog("DLRProviderAdapter.executeDownloadFile. The file URL is null or empty");
			return sResult;
		}
		
		try {
			String[] asLinkInfos = sFileURL.split(";");
			String sHttpUrl = asLinkInfos[0];
			
			if (Utils.isNullOrEmpty(sHttpUrl)) {
				WasdiLog.warnLog("DLRProviderAdapter.executeDownloadFile. The link does not contain information about the download URL " + sHttpUrl);
				return sResult;
			}
			
			WasdiLog.debugLog("DLRProviderAdapter.executeDownloadFile. Download url " + sHttpUrl);
			
			int iAttempt = 0;
			while (iAttempt < iMaxRetry) {
				
				sResult = downloadViaHttp(sHttpUrl, new HashMap<String, String>(), sSaveDirOnServer);
				
				if (Utils.isNullOrEmpty(sResult)) {
					WasdiLog.debugLog("DLRProviderAdapter.executeDownloadFile. Download failed. Waiting 5 seconds before retrying");
					TimeUnit.SECONDS.sleep(5);
				} else {
					WasdiLog.debugLog("DLRProviderAdapter.executeDownloadFile. File downloaded at path " + sResult);
					return sResult;
				}
				
				iAttempt++;
			}
			
			return sResult;
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("DLRProviderAdapter.executeDownloadFile. Error downloading the file" + sFileURL);
			return sResult;
		}
	}

	@Override
	public String getFileName(String sFileURL, String sDownloadPath) throws Exception {
		String sResult = null;
		
		if (Utils.isNullOrEmpty(sFileURL)) {
			WasdiLog.warnLog("DLRProviderAdapter.getFileName. The file URL is null or empty");
			return sResult;
		}
		
		try {
			String[] asLinkInfos = sFileURL.split(";");
			String sHttpUrl = asLinkInfos[0];
			
			String[] asUrlTokens = sHttpUrl.split("/");
			
			sResult = asUrlTokens[asUrlTokens.length - 1];
			WasdiLog.debugLog("DLRProviderAdapter.getFileName. File name extracted from URL: " + sResult);
			return sResult;
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("DLRProviderAdapter.getFileName. Error extracting file name from URL " + sFileURL);
			return sResult;
		}
			
	}

	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
		if (sPlatformType.equals(Platforms.WSF) ) {
			return DataProviderScores.DOWNLOAD.getValue();
		}
		return 0;
	}

}
