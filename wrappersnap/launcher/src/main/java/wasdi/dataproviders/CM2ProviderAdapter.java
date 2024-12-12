package wasdi.dataproviders;

import java.util.*;

import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class CM2ProviderAdapter extends PythonBasedProviderAdapter {
	
	private static final String s_sDatasetId = "datasetId";
	
	protected String m_sPythonScript = "";
	protected String m_sExchangeFolder = ""; 
	
	public CM2ProviderAdapter() {
		m_sDataProviderCode = "COPERNICUSMARINE";
	}

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		WasdiLog.debugLog("CM2ProviderAdapter.getDownloadFileSize: download file size not available in Copernicus Marine");
		return 0;
	}

	@Override
	public String getFileName(String sFileURL) throws Exception {
		WasdiLog.debugLog("CM2ProviderAdapter.getFileName: received url " + sFileURL);
		
		String sFileName = "dataset.nc";
		
		try {
			// the link is a JSON string that we can parse into a map
			Map<String, Object> oJsonMap =  fromWasdiPayloadToObjectMap(sFileURL);
			String sDatasetId = (String) oJsonMap.getOrDefault(s_sDatasetId, "");
			
			if (!Utils.isNullOrEmpty(sDatasetId)) {
				sFileName = sDatasetId + "_" + Utils.nowInMillis().longValue() + ".nc";
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("CM2ProviderAdapter.getFileName: exception retrieving thefile name ", oEx);
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
