package wasdi.dataproviders;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.cm.CMHttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class CMProviderAdapter extends ProviderAdapter {

	public CMProviderAdapter() {
		m_sDataProviderCode = "COPERNICUSMARINE";
	}

	@Override
	protected void internalReadConfig() {
		try {
			m_sProviderUser = m_oDataProviderConfig.user;
			m_sProviderPassword = m_oDataProviderConfig.password;
		} catch (Exception e) {
			WasdiLog.errorLog("CMProviderAdapter: Config reader is null");
		}
	}

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		WasdiLog.debugLog("CPMProviderAdapter.getDownloadFileSize | sFileURL: " + sFileURL);

		if (Utils.isNullOrEmpty(sFileURL)) {
			WasdiLog.errorLog("CPMProviderAdapter.getDownloadFileSize: sFileURL is null or Empty");
			return 0l;
		}

		if (sFileURL.contains("&size=")) {
			String sSize = sFileURL.substring(sFileURL.indexOf("&size=") + 6);

			return Long.valueOf(sSize);
		}

		return 0L;
	}

	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {
		WasdiLog.debugLog("CMProviderAdapter.executeDownloadFile | sFileURL: " + sFileURL);

		if (Utils.isNullOrEmpty(sFileURL)) {
			WasdiLog.errorLog("CPMProviderAdapter.executeDownloadFile: sFileURL is null or Empty");
			return null;
		}

		if (Utils.isNullOrEmpty(m_oDataProviderConfig.link)) {
			WasdiLog.errorLog("CPMProviderAdapter.executeDownloadFile: provider URL is null or Empty");
			return null;
		}

		if (!sFileURL.contains("&size=") || !sFileURL.contains("&size=") || !sFileURL.contains("&size=")) {
			return null;
		}

		String sService = sFileURL.substring(sFileURL.indexOf("&service=") + 9, sFileURL.indexOf("&product", sFileURL.indexOf("&service=")));
		String sProduct = sFileURL.substring(sFileURL.indexOf("&product=") + 9, sFileURL.indexOf("&query", sFileURL.indexOf("&product=")));
		String sQuery = sFileURL.substring(sFileURL.indexOf("&query=") + 7, sFileURL.indexOf("&size", sFileURL.indexOf("&query=")));

		String sLinks = m_oDataProviderConfig.link;
		String[] asLinks = sLinks.split(" ");

		for (int iAttemp = 0; iAttemp < iMaxRetry; iAttemp ++) {
			WasdiLog.debugLog("CMProviderAdapter.executeDownloadFile: attemp #" + iAttemp);

			for (String sDomainUrl : asLinks) {
				String sDownloadProductResult = CMHttpUtils.downloadProduct(sService, sProduct, sQuery, sDomainUrl, m_sProviderUser, m_sProviderPassword, sSaveDirOnServer);

				if (!Utils.isNullOrEmpty(sDownloadProductResult)) {
					return sDownloadProductResult;
				}
			}

			try {
				Thread.sleep(1_000);
			} catch (Exception oEx) {
				WasdiLog.debugLog("CMProviderAdapter.executeDownloadFile: exception in sleep for retry: " + oEx.toString());
			}
		}

		return null;
	}

	@Override
	public String getFileName(String sFileURL) throws Exception {
		WasdiLog.debugLog("CMProviderAdapter.getFileName | sFileURL: " + sFileURL);

		if (sFileURL.contains("&product=")) {
			String sProduct = sFileURL.substring(sFileURL.indexOf("&product=") + 9, sFileURL.indexOf("&", sFileURL.indexOf("&product=") + 9));

			return sProduct + "_" + Utils.nowInMillis().longValue() + ".nc";
		}

		return "dataset";
	}

	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
		if (sPlatformType.equals(Platforms.CM)) {
			return DataProviderScores.DOWNLOAD.getValue();
		}

		return 0;
	}

}
