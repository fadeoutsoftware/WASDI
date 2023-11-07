package wasdi.dataproviders;

import java.io.File;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class VIIRSProviderAdapter extends ProviderAdapter {
	
	public VIIRSProviderAdapter() {
		super();
		m_sDataProviderCode = "VIIRS";
	}

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		return getDownloadFileSizeViaHttp(sFileURL.replace("_part", ".part"));
	}

	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {

		WasdiLog.debugLog("VIIRSProviderAdapter.executeDownloadFile: try to get " + sFileURL);
				
		String sResult = "";
		
		int iAttemp = 0;
		
		while (Utils.isNullOrEmpty(sResult) && iAttemp<iMaxRetry) {
			
			WasdiLog.debugLog("VIIRSProviderAdapter.executeDownloadFile: attemp #" + iAttemp);
			
			try {
				sResult = downloadViaHttp(sFileURL.replace("_part", ".part"), "", "", sSaveDirOnServer);
				
				File oOriginalFile = new File(sResult);
				File oRenamedFile = new File(sResult.replace(".part", "_part"));
				boolean bIsFileRenamed = oOriginalFile.renameTo(oRenamedFile);
				
				if (!bIsFileRenamed)
					WasdiLog.debugLog("VIIRSProviderAdapter.executeDownloadFile. File was not renamed.");
				
				sResult = sResult.replace(".part", "_part");
				
			}
			catch (Exception oEx) {
				WasdiLog.debugLog("VIIRSProviderAdapter.executeDownloadFile: exception in download via http call: " + oEx.toString());
			}
			
			iAttemp ++;
		}
		
		return sResult;
	}

	@Override
	public String getFileName(String sFileURL) throws Exception {
		if (Utils.isNullOrEmpty(sFileURL)) return "";
		
		String sFileName = "";
		
		String [] asParts = sFileURL.split("/");
		
		if (asParts != null) {
			sFileName = asParts[asParts.length-1];
			
			sFileName = sFileName.replace(".part", "_part");
		}
		
		return sFileName;
	}

	@Override
	protected void internalReadConfig() {
		
	}

	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
		
		if (sPlatformType.equals(Platforms.VIIRS)) {
			return DataProviderScores.DOWNLOAD.getValue();
		}
		
		return 0;
	}

}
