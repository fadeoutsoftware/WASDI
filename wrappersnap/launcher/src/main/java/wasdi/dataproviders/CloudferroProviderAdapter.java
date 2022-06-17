package wasdi.dataproviders;

import wasdi.dbutils.helpers.S3BucketHelper;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.utils.LoggerWrapper;
import wasdi.shared.utils.Utils;

public class CloudferroProviderAdapter extends ProviderAdapter {

	/**
	 * Flag to know if we already authenticated to the Cloudferro Data Center or no
	 */
	boolean m_bAuthenticated = false;

	/**
	 * Basic constructor
	 */
	public CloudferroProviderAdapter() {
		m_sDataProviderCode = "CLOUDFERRO";
	}

	/**
	 * @param logger
	 */
	public CloudferroProviderAdapter(LoggerWrapper logger) {
		super(logger);
		m_sDataProviderCode = "CLOUDFERRO";
	}

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		//todo fail instead
		if (Utils.isNullOrEmpty(sFileURL)) {
			m_oLogger.error("CloudferroProviderAdapter.GetDownloadFileSize: sFileURL is null or Empty");
			return 0l;
		}

		long lSizeInBytes = 0L;

		if (sFileURL.contains(",")) {
			String sEndpoint = WasdiConfig.Current.s3Bucket.endpoint;
			String sBucketName = WasdiConfig.Current.s3Bucket.bucketName;
			String sFilePath = sFileURL.substring(0, sFileURL.indexOf(",")).replace(sEndpoint + sBucketName + "/", "");

			lSizeInBytes = S3BucketHelper.getFileSize(sBucketName, sFilePath);
		}

		return lSizeInBytes;
	}

	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {

		Utils.debugLog("CloudferroProviderAdapter.executeDownloadFile: try to get " + sFileURL);

		String sResult = "";
		
		if (sFileURL.contains(",")) {
			String sEndpoint = WasdiConfig.Current.s3Bucket.endpoint;
			String sBucketName = WasdiConfig.Current.s3Bucket.bucketName;
			String sFilePath = sFileURL.substring(0, sFileURL.indexOf(",")).replace(sEndpoint + sBucketName + "/", "");

			sResult = S3BucketHelper.downloadFile(sBucketName, sFilePath, sSaveDirOnServer);
		}

		return sResult;
	}

	@Override
	public String getFileName(String sFileURL) throws Exception {
		if (Utils.isNullOrEmpty(sFileURL)) return "";

		String sFileName = "";

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
