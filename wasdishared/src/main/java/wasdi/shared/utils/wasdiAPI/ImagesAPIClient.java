package wasdi.shared.utils.wasdiAPI;

import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class ImagesAPIClient {
	public static String getImage(String sOutputPath, String sSessionId, String sCollection, String sFolder, String sImageName, String sWasdiUrl) {
		
		try {
			
			if (Utils.isNullOrEmpty(sWasdiUrl)) {
				sWasdiUrl = WasdiConfig.Current.baseUrl;
			}
			
			if (!sWasdiUrl.endsWith("/")) sWasdiUrl += "/";
						
			String sUrl = sWasdiUrl+"search/get?collection="+sCollection + "&folder="+sFolder+"&name="+sImageName;
			String sFinalPath = HttpUtils.downloadFile(sUrl, HttpUtils.getStandardHeaders(sSessionId), sOutputPath);
			
			if (Utils.isNullOrEmpty(sFinalPath)) {
				WasdiLog.errorLog("ImagesAPIClient.getImage: got null response");
				return "";
			}
			
			return sFinalPath;
		}
		catch(Exception oEx) {
			WasdiLog.errorLog("ImagesAPIClient.login: getImage ", oEx);
		}
		
		return "";
	}
}
