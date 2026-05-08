package wasdi.dataproviders;

import java.util.*;

import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class CM2ProviderAdapter extends PythonBasedProviderAdapter {
	
	private static final String s_sDatasetId = "datasetId";
	
	protected String m_sPythonScript = "";
	protected String m_sExchangeFolder = ""; 
	
	public CM2ProviderAdapter() {
	}

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		WasdiLog.debugLog("CM2ProviderAdapter.getDownloadFileSize: download file size not available in Copernicus Marine");
		return 0;
	}
	
	@Override
	protected Map<String, Object> fromWasdiPayloadToObjectMap(String sUrl) {
		String sDecodedUrl = decodeUrl(sUrl);

		String sPayload = null;
		if (!Utils.isNullOrEmpty(sDecodedUrl)) {
			String[] asTokens = sDecodedUrl.split("payload=");
			if (asTokens.length == 2) {
				sPayload = asTokens[1];
				WasdiLog.debugLog("CM2ProviderAdapter.fromtWasdiPayloadToObjectMap json string: " + sPayload);
				return JsonUtils.jsonToMapOfObjects(sPayload);
			}
			WasdiLog.debugLog("CM2ProviderAdapter.fromtWasdiPayloadToObjectMap. Payload not found in url " + sUrl);
		}
		
		HashMap<String, Object> aoReturnMap = new HashMap<>();
		aoReturnMap.put("url", sUrl);
		
		WasdiLog.warnLog("CM2ProviderAdapter.fromtWasdiPayloadToObjectMap. Decoded url is null or empty " + sUrl);
		return aoReturnMap;
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
