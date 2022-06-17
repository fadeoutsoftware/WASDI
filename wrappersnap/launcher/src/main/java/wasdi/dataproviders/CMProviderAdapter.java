package wasdi.dataproviders;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.cm.CMHttpUtils;
import wasdi.shared.utils.LoggerWrapper;
import wasdi.shared.utils.Utils;

public class CMProviderAdapter extends ProviderAdapter {

	public CMProviderAdapter() {
		m_sDataProviderCode = "COPERNICUSMARINE";
	}

	public CMProviderAdapter(LoggerWrapper logger) {
		super(logger);
		m_sDataProviderCode = "COPERNICUSMARINE";
	}

	@Override
	protected void internalReadConfig() {
		try {
			m_sProviderUser = m_oDataProviderConfig.user;
			m_sProviderPassword = m_oDataProviderConfig.password;
		} catch (Exception e) {
			m_oLogger.error("CMProviderAdapter: Config reader is null");
		}
	}

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		Utils.debugLog("CPMProviderAdapter.getDownloadFileSize | sFileURL: " + sFileURL);

		if (Utils.isNullOrEmpty(sFileURL)) {
			m_oLogger.error("CPMProviderAdapter.getDownloadFileSize: sFileURL is null or Empty");
			return 0l;
		}

		if (sFileURL.contains("&size=")) {
			String sSize = sFileURL.substring(sFileURL.indexOf("&size=") + 6);
			System.out.println("sSize :" + sSize);

			return Long.valueOf(sSize);
		}

		return 0L;
	}

	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {
		Utils.debugLog("CMProviderAdapter.executeDownloadFile | sFileURL: " + sFileURL);

		if (Utils.isNullOrEmpty(sFileURL)) {
			m_oLogger.error("CPMProviderAdapter.executeDownloadFile: sFileURL is null or Empty");
			return null;
		}

		if (!sFileURL.contains("&size=") || !sFileURL.contains("&size=") || !sFileURL.contains("&size=")) {
			return null;
		}

		String sService = sFileURL.substring(sFileURL.indexOf("&service=") + 9, sFileURL.indexOf("&product", sFileURL.indexOf("&service=")));
		String sProduct = sFileURL.substring(sFileURL.indexOf("&product=") + 9, sFileURL.indexOf("&query", sFileURL.indexOf("&product=")));
		String sQuery = sFileURL.substring(sFileURL.indexOf("&query=") + 7, sFileURL.indexOf("&size", sFileURL.indexOf("&query=")));

		String sResult = CMHttpUtils.downloadProduct(sService, sProduct, sQuery, m_sProviderUser, m_sProviderPassword, sSaveDirOnServer);

		return sResult;
	}

	@Override
	public String getFileName(String sFileURL) throws Exception {
		Utils.debugLog("CMProviderAdapter.getFileName | sFileURL: " + sFileURL);

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
