package wasdi.dataproviders;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.Utils;
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
	protected Map<String, Object> fromWasdiPayloadToObjectMap(String sUrl) {
		String sDecodedUrl = decodeUrl(sUrl);

		String sPayload = null;
		if (!Utils.isNullOrEmpty(sDecodedUrl)) {
			String[] asTokens = sDecodedUrl.split("payload=");
			if (asTokens.length == 2) {
				sPayload = asTokens[1];
				WasdiLog.debugLog("ESAProviderAdapter.fromtWasdiPayloadToObjectMap json string: " + sPayload);
				return JsonUtils.jsonToMapOfObjects(sPayload);
			}
			WasdiLog.debugLog("ESAProviderAdapter.fromtWasdiPayloadToObjectMap. Payload not found in url " + sUrl);
		}
		
		HashMap<String, Object> aoReturnMap = new HashMap<>();
		aoReturnMap.put("url", sUrl);
		
		WasdiLog.warnLog("ESAProviderAdapter.fromtWasdiPayloadToObjectMap. Decoded url is null or empty " + sUrl);
		return aoReturnMap;
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
