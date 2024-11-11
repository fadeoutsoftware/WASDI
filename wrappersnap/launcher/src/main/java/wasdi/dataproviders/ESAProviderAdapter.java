package wasdi.dataproviders;

import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.utils.log.WasdiLog;

public class ESAProviderAdapter extends PythonBasedProviderAdapter {

	public ESAProviderAdapter() {
		super();
		m_sDataProviderCode = "ESA";
	}
	
	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		WasdiLog.debugLog("ESAProviderAdapter.getDownloadFileSize: download file size not available in Copernicus Marine");
		return 0;
	}
	
	
	
	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
		if (sPlatformType.equals(Platforms.ERS)) {
			return DataProviderScores.DOWNLOAD.getValue();
		}

		return 0;
	}
}
