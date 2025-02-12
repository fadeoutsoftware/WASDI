package wasdi.dataproviders;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class GPMProviderAdapter extends ProviderAdapter {

	/**
	 * Flag to know if we already authenticated to the GPM Data Center or no
	 */
	boolean m_bAuthenticated = false;

	public GPMProviderAdapter() {
	}

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		WasdiLog.debugLog("GPMProviderAdapter.getDownloadFileSize | sFileURL: " + sFileURL);

		if (Utils.isNullOrEmpty(sFileURL)) {
			WasdiLog.errorLog("GPMProviderAdapter.GetDownloadFileSize: sFileURL is null or Empty");
			return 0l;
		}

		long lSizeInBytes = 0L;

		String sResult = "";

		try {
			Map<String, String> asHeaders = getGPMHeaders(m_sProviderUser, m_sProviderPassword);

			lSizeInBytes = HttpUtils.getDownloadFileSizeViaHttp(sFileURL, asHeaders);

			WasdiLog.debugLog("GPMProviderAdapter.getDownloadFileSize: file size is: " + sResult);
		} catch (Exception oE) {
			WasdiLog.debugLog("GPMProviderAdapter.getDownloadFileSize: could not extract file size due to " + oE);
		}

		return lSizeInBytes;
	}

	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {
		WasdiLog.debugLog("GPMProviderAdapter.executeDownloadFile | sFileURL: " + sFileURL);

		String sResult = "";

		for (int iAttemp = 0; iAttemp < iMaxRetry; iAttemp ++) {

			WasdiLog.debugLog("GPMProviderAdapter.executeDownloadFile: attemp #" + iAttemp);

			try {
				sResult = downloadViaHttp(sFileURL, sDownloadUser, sDownloadPassword, sSaveDirOnServer);
			} catch (Exception oEx) {
				WasdiLog.debugLog("GPMProviderAdapter.executeDownloadFile: exception in download via http call: " + oEx.toString());
			}

			if (!Utils.isNullOrEmpty(sResult)) {
				return sResult;
			}

			try {
				int iMsSleep = (int) ( (Math.random() * 1500.0) + 1000.0 );
				Thread.sleep(iMsSleep);
			} catch (InterruptedException oEx) {
				Thread.currentThread().interrupt();
				WasdiLog.debugLog("GPMProviderAdapter.executeDownloadFile: exception in sleep for retry: " + oEx.toString());
			}
		}

		return sResult;
	}

	/**
	 * get the headers for calls to GPM.
	 * @return a map containing the authorization header
	 */
	protected HashMap<String, String> getGPMHeaders(String sDownloadUser, String sDownloadPassword) {
		HashMap<String, String> asHeaders = new HashMap<String, String>();

		String sAuth = sDownloadUser + ":" + sDownloadPassword;
		String sEncodedAuth = Base64.getEncoder().encodeToString(sAuth.getBytes(StandardCharsets.UTF_8));
		String sAuthHeaderValue = "Basic " + sEncodedAuth;

		try {
			asHeaders.put("Authorization", sAuthHeaderValue);
		} catch (Exception oE) {
			WasdiLog.debugLog("GPMProviderAdapter.getGPMHeaders: " + oE);
		}

		return asHeaders;
	}

	@Override
	public String getFileName(String sFileURL, String sDownloadPath) throws Exception {
		WasdiLog.debugLog("GPMProviderAdapter.getFileName | sFileURL: " + sFileURL);

		if (Utils.isNullOrEmpty(sFileURL)) return "";

		if (!sFileURL.contains("3B-")) return "";

		String sFileName = sFileURL.substring(sFileURL.indexOf("3B-"));

		return sFileName;
	}

	@Override
	protected void internalReadConfig() {
		try {
			m_sDefaultProtocol = m_oDataProviderConfig.defaultProtocol; 
			m_sProviderUser = m_oDataProviderConfig.user;
			m_sProviderPassword = m_oDataProviderConfig.password;
		} catch (Exception e) {
			WasdiLog.errorLog("GPMProviderAdapter: Config reader is null");
		}
	}

	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
		if (sPlatformType.equals(Platforms.IMERG)) {
			return DataProviderScores.DOWNLOAD.getValue();
		}

		return 0;
	}

}
