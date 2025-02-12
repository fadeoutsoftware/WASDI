package wasdi.dataproviders;

import java.io.File;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.utils.MissionUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class EODCProviderAdapter extends ProviderAdapter{
	
	public EODCProviderAdapter() {
		super();
		m_sDataProviderCode = "EODC";
	}

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		WasdiLog.debugLog("EODCProviderAdapter.GetDownloadSize: start " + sFileURL);

		long lLenght = 0L;

		if (isFileProtocol(sFileURL)) {

			String sPath = removePrefixFile(sFileURL);
			File oSourceFile = new File(sPath);

			lLenght = getSourceFileLength(oSourceFile);
		} else if(isHttpsProtocol(sFileURL)) {
			lLenght = getDownloadFileSizeViaHttp(sFileURL);
		}

		return lLenght;
	}

	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {

		// Domain check
		if (Utils.isNullOrEmpty(sFileURL)) {
			WasdiLog.debugLog("EODCProviderAdapter.ExecuteDownloadFile: sFileURL is null");
			return "";
		}
		if (Utils.isNullOrEmpty(sSaveDirOnServer)) {
			WasdiLog.debugLog("EODCProviderAdapter.ExecuteDownloadFile: sSaveDirOnServer is null");
			return "";
		}
		
		WasdiLog.debugLog("EODCProviderAdapter.ExecuteDownloadFile: start");
		
		setProcessWorkspace(oProcessWorkspace);

		if (isFileProtocol(sFileURL)) {
			return localFileCopy(sFileURL, sSaveDirOnServer, iMaxRetry);
		}
		
		return "";
	}

	@Override
	public String getFileName(String sFileURL) throws Exception {
		
		//extract file name

		if (Utils.isNullOrEmpty(sFileURL)) {
			WasdiLog.errorLog("EODCProviderAdapter.GetFileName: sFileURL is null or Empty");
			return "";
		}

		if (isFileProtocol(sFileURL)) {
			
			// In Onda, the real file is .value but here we need the name of Satellite image that, in ONDA is the parent folder name

			String sPath = removePrefixFile(sFileURL);
			//sPath = removeSuffixValue(sPath);

			return extractDestinationFileName(sPath);

		} 		
		
		return "";
	}
	
	@Override
	protected void internalReadConfig() {
		
	}
	
	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
		
		if (isWorkspaceOnSameCloud()) {
			if (sPlatformType.equals(Platforms.SENTINEL1)) {
				
				String sProductType = MissionUtils.getProductTypeSatelliteImageFileName(sFileName, sPlatformType);
				
				if (sProductType.equals("GRD")) {
					return DataProviderScores.FILE_ACCESS.getValue();
				}
			}
			else if (sPlatformType.equals(Platforms.SENTINEL2)) {
				String sProductType = MissionUtils.getProductTypeSatelliteImageFileName(sFileName, sPlatformType);
				
				if (sProductType.equals("MSIL1C")) {
					return DataProviderScores.FILE_ACCESS.getValue();
				}				
			}
			else if (sPlatformType.equals(Platforms.SENTINEL3)) {
				String sProductType = MissionUtils.getProductTypeSatelliteImageFileName(sFileName, sPlatformType);
				
				if (sProductType.equals("EFR___") || sProductType.equals("ERR___")) {
					return DataProviderScores.FILE_ACCESS.getValue();
				}				
			}
		}
		return 0;
	}

}
