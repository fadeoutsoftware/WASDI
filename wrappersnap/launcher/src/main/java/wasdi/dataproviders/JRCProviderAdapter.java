package wasdi.dataproviders;

import java.util.concurrent.TimeUnit;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.jrc.ResponseTranslatorJRC;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class JRCProviderAdapter extends ProviderAdapter {
	
	public JRCProviderAdapter() {
	}

	@Override
	protected void internalReadConfig() {
		
	}

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		return 0;
	}

	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {
		
		String sResult = "";
		
		String sDownloadURL = sFileURL.split(ResponseTranslatorJRC.s_sLinkSeparator)[ResponseTranslatorJRC.s_iLinkIndex];
		
		WasdiLog.debugLog("JRCProviderAdapter.executeDownloadFile. Download url: " + sDownloadURL);
		
		int iAttempt = 0;
		
		while (Utils.isNullOrEmpty(sResult) && iAttempt < iMaxRetry) {

			WasdiLog.debugLog("JRCProviderAdapter.performCdsDownloadRequest.downloadViaHttp: attemp #" + (iAttempt + 1));
			
			try {
				sResult = downloadViaHttp(sDownloadURL, sDownloadUser, sDownloadPassword, sSaveDirOnServer);
			}
			catch (Exception oEx) {
				WasdiLog.debugLog("JRCProviderAdapter.executeDownloadFile: exception in download via http call: " + oEx.toString());
				WasdiLog.debugLog("JRCProviderAdapter.executeDownloadFile: waiting 5 seconds before retry");
				TimeUnit.SECONDS.sleep(5);
			}
			
			iAttempt++;
		}
		
		
		return sResult;
	}

	@Override
	public String getFileName(String sFileURL) throws Exception {
		String sFileName = "";
		try {
			sFileName = sFileURL.split(ResponseTranslatorJRC.s_sLinkSeparator)[ResponseTranslatorJRC.s_iFileNameIndex];
		} catch (Exception oEx) {
			WasdiLog.errorLog("JRCProviderAdapter.getFileName. Error retrieving the file name: " + oEx.getMessage());
		}
		WasdiLog.debugLog("JRCProviderAdapter.getFileName. File name retrieved: " + sFileName);
		return sFileName;
	}

	
	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
		
		if (sPlatformType.equals(Platforms.JRC_GHSL)) {
			return DataProviderScores.DOWNLOAD.getValue();
		}
		
		return 0;
	}
	
	

}
