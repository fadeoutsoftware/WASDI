package wasdi.dataproviders;

import java.io.File;
import java.util.Map;

import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.utils.log.WasdiLog;

public class ESAProviderAdapter extends PythonBasedProviderAdapter {

	public ESAProviderAdapter() {
		super();
	}
	
	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		WasdiLog.debugLog("ESAProviderAdapter.getDownloadFileSize: download file size not available");
		return 0;
	}
	
	@Override
	public String getFileName(String sFileURL) throws Exception {
		String sFileName = "";
		try {
			Map<String, Object> oMap = fromWasdiPayloadToObjectMap(sFileURL);
			String sUrl = String.valueOf(oMap.get("url"));
			String[] sUrlParts = sUrl.split(File.separator);
			sFileName = sUrlParts[sUrlParts.length - 1];
		} catch (Exception oEx) {
			WasdiLog.errorLog("ESAProviderAdapter.getFileName. Error retrieving file name from URL", oEx);
		}
		return sFileName;
	}
	
	
	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
		if (sPlatformType.equals(Platforms.ERS)) {
			return DataProviderScores.DOWNLOAD.getValue();
		}

		return 0;
	}
}
