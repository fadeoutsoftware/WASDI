package wasdi.dataproviders;

import java.util.*;
import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class CM2ProviderAdapter extends ProviderAdapter {
	
	private static final String s_sDatasetId = "datasetId";

	@Override
	protected void internalReadConfig() {
		// nothing to do?
		
	}

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		WasdiLog.debugLog("CM2ProviderAdapter.getDownloadFileSize: download file size not available in Copernicus Marine");
		return 0;
	}

	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {
		
		return null;
	}

	@Override
	public String getFileName(String sFileURL) throws Exception {
		String sFileName = "dataset.nc";
		
		// the link is a JSON string that we can parse into a map
		Map<String, Object> oJsonMap = JsonUtils.jsonToMapOfObjects(sFileURL);
		String sDatasetId = (String) oJsonMap.getOrDefault(s_sDatasetId, "");
		
		if (!Utils.isNullOrEmpty(sDatasetId)) {
			sFileName = sDatasetId + "_" + Utils.nowInMillis().longValue() + ".nc";
		}
		
		WasdiLog.debugLog("CM2ProviderAdapter.getFileName: file name for CM subset file " + sFileName);
		return sFileName;
	}

	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
		if (sPlatformType.equals(Platforms.CM)) {
			return DataProviderScores.DOWNLOAD.getValue();
		}

		return 0;
	}
	

}
