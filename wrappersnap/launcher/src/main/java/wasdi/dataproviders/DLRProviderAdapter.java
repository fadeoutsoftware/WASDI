package wasdi.dataproviders;

import java.util.concurrent.TimeUnit;

import java.util.HashMap;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;

public class DLRProviderAdapter extends ProviderAdapter {

	public DLRProviderAdapter() {
		m_sDataProviderCode = "DLR";
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
	public String getFileName(String sFileURL) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
		if (sPlatformType.equals(Platforms.WSF) ) {
			return DataProviderScores.DOWNLOAD.getValue();
		}
		return 0;
	}
	
	public static void main(String[]args) throws Exception {
		WasdiConfig.readConfig("C:/temp/wasdi/wasdiLocalTESTConfig.json");
		DLRProviderAdapter oAdapter = new DLRProviderAdapter();
		oAdapter.executeDownloadFile("https://download.geoservice.dlr.de/WSF2019/files//WSF2019_v1_-100_16.tif;1000", 
				null, null, "C:/Users/valentina.leone/Downloads", null, 2);
		
	}

}
