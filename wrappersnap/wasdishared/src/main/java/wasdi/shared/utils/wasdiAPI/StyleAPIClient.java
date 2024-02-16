package wasdi.shared.utils.wasdiAPI;

import java.util.HashMap;
import java.util.Map;

import wasdi.shared.business.Node;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;

public class StyleAPIClient {

	public static HttpCallResponse nodeDelete(Node oNode, String sSessionId, String sStyleId, String sStyleName)  {
		try {
			
			String sUrl = oNode.getNodeBaseAddress();

			if (!sUrl.endsWith("/")) {
				sUrl += "/";
			}

			sUrl += "styles/nodedelete?styleId=" + sStyleId + "&styleName=" + sStyleName;
			
			WasdiLog.debugLog("StyleAPIClient.nodeDelete: calling url: " + sUrl);
			
			return HttpUtils.httpDelete(sUrl, HttpUtils.getStandardHeaders(sSessionId)); 
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("StyleAPIClient.nodeDelete: error ", oEx);
		}
		
		return new HttpCallResponse();
	}
	
	public static boolean updateFile(Node oNode, String sSessionId, String sStyleId, String sFilePath)  {
		try {
			
			String sUrl = oNode.getNodeBaseAddress();

			if (!sUrl.endsWith("/")) {
				sUrl += "/";
			}

			sUrl += "styles/updatefile?styleId=" + sStyleId+"&zipped=true";

			Map<String, String> asHeaders = new HashMap<>();
			asHeaders.put("x-session-token", sSessionId);

			WasdiLog.debugLog("StyleAPIClient.updateFile: calling url: " + sUrl);

			return HttpUtils.httpPostFile(sUrl, sFilePath, HttpUtils.getStandardHeaders(sSessionId));
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("StyleAPIClient.nodeDelete: error ", oEx);
		}
		
		return false;
	}
}
