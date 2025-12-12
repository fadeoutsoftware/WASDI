package wasdi.dataproviders;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.utils.S3BucketUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class CloudferroProviderAdapter extends ProviderAdapter {

	/**
	 * Flag to know if we already authenticated to the Cloudferro Data Center or no
	 */
	boolean m_bAuthenticated = false;

	/**
	 * Basic constructor
	 */
	public CloudferroProviderAdapter() {
	}

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		//todo fail instead
		if (Utils.isNullOrEmpty(sFileURL)) {
			WasdiLog.errorLog("CloudferroProviderAdapter.GetDownloadFileSize: sFileURL is null or Empty");
			return 0l;
		}

		long lSizeInBytes = 0L;

		if (sFileURL.contains(",")) {
			String sEndpoint = WasdiConfig.Current.s3Bucket.endpoint;
			String sBucketName = WasdiConfig.Current.s3Bucket.bucketName;
			String sFilePath = sFileURL.substring(0, sFileURL.indexOf(",")).replace(sEndpoint + sBucketName + "/", "");

			lSizeInBytes = S3BucketUtils.getFileSize(sBucketName, sFilePath);
		}

		return lSizeInBytes;
	}

	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {

		WasdiLog.debugLog("CloudferroProviderAdapter.executeDownloadFile: try to get " + sFileURL);

		String sResult = null;
		
		String sFileName = getFileName(sFileURL, null);
		
		if (Utils.isNullOrEmpty(sFileName) ||
				!(sFileName.toUpperCase().startsWith("EEH2TES")
						|| sFileName.toUpperCase().startsWith("EEH2STIC")
						|| sFileName.toUpperCase().startsWith("ECOV002_L2_CLOUD")
						|| sFileName.toUpperCase().startsWith("ECOV002_L1B_GEO")
						|| sFileName.toUpperCase().startsWith("ECOV002_L1B_RAD"))) {
			return sResult;
		}
		
		if (sFileURL.contains(",")) {
			String sEndpoint = WasdiConfig.Current.s3Bucket.endpoint;
			String sBucketName = WasdiConfig.Current.s3Bucket.bucketName;
			String sFilePath = sFileURL.substring(0, sFileURL.indexOf(",")).replace(sEndpoint + sBucketName + "/", "");
			
			sResult = S3BucketUtils.downloadFile(sBucketName, sFilePath, sSaveDirOnServer);
		}

		return sResult;
	}

	@Override
	public String getFileName(String sFileURL, String sDownloadPath) throws Exception {
		if (Utils.isNullOrEmpty(sFileURL)) return "";

		String sFileName = null;

		if (sFileURL.contains(",")) {
			sFileName = sFileURL.substring(sFileURL.indexOf(",") + 1, sFileURL.indexOf(",", sFileURL.indexOf(",") + 1));
		}

		return sFileName;
	}

	@Override
	protected void internalReadConfig() {
	}

	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {

		if (sPlatformType.equals(Platforms.ECOSTRESS) ) {
			if (isWorkspaceOnSameCloud()) {
				return DataProviderScores.SAME_CLOUD_DOWNLOAD.getValue();
			}
			else {
				return DataProviderScores.DOWNLOAD.getValue();
			}
		}

		return 0;
	}

}
