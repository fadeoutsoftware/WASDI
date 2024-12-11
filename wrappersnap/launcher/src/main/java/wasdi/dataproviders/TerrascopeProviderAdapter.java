package wasdi.dataproviders;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.StringUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.ZipFileUtils;
import wasdi.shared.utils.log.WasdiLog;

public class TerrascopeProviderAdapter extends ProviderAdapter {

	/**
	 * Flag to know if we already authenticated to the Terrascope Data Center or no
	 */
	boolean m_bAuthenticated = false;

	private static final String TERRASCOPE_URL_ZIPPER = "https://services.terrascope.be/package/zip";
	private static final String s_sSizeParam = "&size=";
	private static final String s_sCompressedPayload = "?compressed_payload=";


	/**
	 * Basic constructor
	 */
	public TerrascopeProviderAdapter() {
		m_sDataProviderCode = "TERRASCOPE";
	}
	
	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		//todo fail instead
		if (Utils.isNullOrEmpty(sFileURL)) {
			WasdiLog.errorLog("TerrascopeProviderAdapter.GetDownloadFileSize: sFileURL is null or Empty");
			return 0l;
		}

		long lSizeInBytes = 0L;

		if (isHttpsProtocol(sFileURL)) {
			String sResult = "";

			if (sFileURL.contains(s_sSizeParam)) {
				return Long.valueOf(sFileURL.substring(sFileURL.indexOf(s_sSizeParam) + 6, sFileURL.indexOf(",", sFileURL.indexOf(s_sSizeParam))));
			}

			try {
				//extract appropriate url
				StringBuilder oUrl = new StringBuilder(getZipperUrl(sFileURL));
				sFileURL = oUrl.toString();

				String sOpenidConnectToken = obtainTerrascopeOpenidConnectToken();
				Map<String, String> asHeaders = HttpUtils.getOpenIdConnectHeaders(sOpenidConnectToken);

				lSizeInBytes = HttpUtils.getDownloadFileSizeViaHttp(sFileURL, asHeaders);

				WasdiLog.debugLog("TerrascopeProviderAdapter.getDownloadFileSize: file size is: " + sResult);
			} catch (Exception oE) {
				WasdiLog.debugLog("TerrascopeProviderAdapter.getDownloadFileSize: could not extract file size due to " + oE);
			}
		}

		return lSizeInBytes;
	}

	private String obtainTerrascopeOpenidConnectToken() {
		String sDownloadUser = m_sProviderUser;
		String sDownloadPassword = m_sProviderPassword;
		String sUrl = "https://sso.terrascope.be/auth/realms/terrascope/protocol/openid-connect/token";
		String sClientId = "public";

		return HttpUtils.obtainOpenidConnectToken(sUrl, sDownloadUser, sDownloadPassword, sClientId);
	}

	private String getZipperUrl(String sFileURL) {
		String sResult = "";

		try {
			sResult = sFileURL.split(",")[0];
		} catch (Exception oE) {
			WasdiLog.errorLog("TerrascopeProviderAdapter.getZipperUrl: " + oE);
		}

		return sResult;
	}

	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {

		WasdiLog.debugLog("TerrascopeProviderAdapter.executeDownloadFile: try to get " + sFileURL);

		String sResult = "";

//		if (isFileProtocol(m_sDefaultProtocol)) {
//
//			String sPathLinux = null;
//			if (isFileProtocol(sFileURL)) {
//				sPathLinux = removePrefixFile(sFileURL);
//			} else if (isHttpsProtocol(sFileURL)) {
//				sPathLinux = extractFilePathFromHttpsUrl(sFileURL);
//			} else {
//				WasdiLog.debugLog("TerrascopeProviderAdapter.executeDownloadFile: unknown protocol " + sFileURL);
//			}
//
//			if (sPathLinux != null) {
//				File oSourceFile = new File(sPathLinux);
//
//				if (oSourceFile != null && oSourceFile.exists()) {
//					sResult = copyFile("file:" + sPathLinux, sDownloadUser, sDownloadPassword, sSaveDirOnServer, oProcessWorkspace, iMaxRetry);
//
//					return sResult;
//				}
//			}
//		}

		if (isHttpsProtocol(sFileURL)) {
			if (sFileURL.startsWith(TERRASCOPE_URL_ZIPPER) && sFileURL.contains("compressed_payload")) {

				List<String> aoUrls = extractPayloadFromUrl(sFileURL);
				if (aoUrls != null) {

					String sPayload = JsonUtils.stringify(aoUrls);
					sResult = downloadHttpsPost(sFileURL, sPayload, sSaveDirOnServer, iMaxRetry, sResult);

					if (!Utils.isNullOrEmpty(sResult)) {
						String sDesiredFileName = this.getFileName(sFileURL);

						String sRenamedFile = WasdiFileUtils.renameFile(sResult, sDesiredFileName);
						ZipFileUtils.fixZipFileInnerSafePath(sRenamedFile);

						return sRenamedFile;
					}

				}
			} else {
				sResult = downloadHttps(sFileURL, sSaveDirOnServer, iMaxRetry, sResult);
			}
		}

		return sResult;
	}

	private List<String> extractPayloadFromUrl(String sUrl) {
		String sDecodedUrl = sUrl;

		String sCompressedPayload = null;
		String sPayload = null;
		if (sDecodedUrl != null && sDecodedUrl.startsWith(TERRASCOPE_URL_ZIPPER) && sDecodedUrl.contains(s_sCompressedPayload)) {
			sCompressedPayload = sDecodedUrl.substring(sDecodedUrl.indexOf(s_sCompressedPayload) + s_sCompressedPayload.length(), sDecodedUrl.indexOf(s_sSizeParam));
			sPayload = uncompressPayload(sCompressedPayload);
			if (sPayload == null) {
				return null;
			}

			return JsonUtils.jsonToListOfStrings(sPayload);
		}

		return null;
	}

	private static String uncompressPayload(String sCompressedPayload) {
		String sPayload = null;

		try {
			sPayload = StringUtils.uncompressString(sCompressedPayload);
		} catch (IOException e) {
			WasdiLog.debugLog("TerrascopeProviderAdapter.uncompressPayload: the payload cannot be uncompressed: " + e.getMessage());
		}
		
		return sPayload;
	}

	private String downloadHttps(String sFileURL, String sSaveDirOnServer, int iMaxRetry, String sResult) {

		StringBuilder oUrl = new StringBuilder(getZipperUrl(sFileURL));
		sFileURL = oUrl.toString();

		String sOpenidConnectToken = obtainTerrascopeOpenidConnectToken();
		Map<String, String> asHeaders = HttpUtils.getOpenIdConnectHeaders(sOpenidConnectToken);

		for (int iAttempt = 0; iAttempt < iMaxRetry; iAttempt ++) {

			WasdiLog.debugLog("TerrascopeProviderAdapter.downloadHttps: attemp #" + iAttempt);

			try {
				sResult = downloadViaHttp(sFileURL, asHeaders, sSaveDirOnServer);
			} catch (Exception oEx) {
				WasdiLog.debugLog("TerrascopeProviderAdapter.downloadHttps: exception in download via http call: " + oEx.toString());
			}

			if (!Utils.isNullOrEmpty(sResult)) {
				return sResult;
			}

			try {
				int iMsSleep = (int) ((Math.random() * 15_000) + 10_000);
				Thread.sleep(iMsSleep);
			} catch (InterruptedException oEx) {
				Thread.currentThread().interrupt();
				WasdiLog.errorLog("TerrascopeProviderAdapter.executeDownloadFile: exception in sleep for retry: ", oEx);
			}
		}

		return sResult;
	}

	private String downloadHttpsPost(String sFileURL, String sPayload, String sSaveDirOnServer, int iMaxRetry, String sResult) {

		String sOpenidConnectToken = obtainTerrascopeOpenidConnectToken();
		Map<String, String> asHeaders = HttpUtils.getOpenIdConnectHeaders(sOpenidConnectToken);

		for (int iAttempt = 0; iAttempt < iMaxRetry; iAttempt ++) {

			WasdiLog.debugLog("TerrascopeProviderAdapter.downloadHttpsPost: attemp #" + iAttempt);

			try {
				sResult = downloadViaHttpPost(sFileURL, asHeaders, sPayload, sSaveDirOnServer);
			} catch (Exception oEx) {
				WasdiLog.debugLog("TerrascopeProviderAdapter.downloadHttpsPost: exception in download via http call: " + oEx.toString());
			}

			if (!Utils.isNullOrEmpty(sResult)) {
				return sResult;
			}

			try {
				int iMsSleep = (int) ((Math.random() * 15_000) + 10_000);
				Thread.sleep(iMsSleep);
			} catch (InterruptedException oEx) {
				Thread.currentThread().interrupt();
				WasdiLog.errorLog("TerrascopeProviderAdapter.downloadHttpsPost: exception in sleep for retry: ", oEx);
			}
		}

		return sResult;
	}

	@Override
	public String getFileName(String sFileURL) throws Exception {
		if (Utils.isNullOrEmpty(sFileURL)) return "";

		String sFileName = "";

		if (sFileURL.startsWith(TERRASCOPE_URL_ZIPPER) && sFileURL.contains("compressed_payload")) {
			String [] asParts = sFileURL.split(",");

			if (asParts != null) {
				sFileName = asParts[1];
			}
		} else {
			String [] asParts = sFileURL.split("/");

			if (asParts != null) {
				sFileName = asParts[asParts.length - 1];
			}
		}

		return sFileName;
	}

	@Override
	protected void internalReadConfig() {

		try {
			m_sDefaultProtocol = m_oDataProviderConfig.defaultProtocol; 
		} catch (Exception e) {
			WasdiLog.errorLog("TerrascopeProviderAdapter: Config reader is null");
		}
	}

	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {

		if (sPlatformType.equals(Platforms.SENTINEL1) 
				|| sPlatformType.equals(Platforms.DEM)
				|| sPlatformType.equals(Platforms.WORLD_COVER)) {
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
