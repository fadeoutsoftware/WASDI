package wasdi.shared.utils.wasdiAPI;

import wasdi.shared.business.Node;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;

public class WorkspaceAPIClient {
	
	public static HttpCallResponse deleteWorkspace(Node oNode, String sSessionId, String sWorkspaceId)  {
		try {
			
			String sUrl = oNode.getNodeBaseAddress();

			if (!sUrl.endsWith("/")) {
				sUrl += "/";
			}

			sUrl += "ws/delete?workspace=" + sWorkspaceId + "&deletelayer=true&deletefile=true";
			
			WasdiLog.debugLog("WorkspaceAPIClient.deleteWorkspace: calling url: " + sUrl);
			
			return HttpUtils.httpDelete(sUrl, HttpUtils.getStandardHeaders(sSessionId)); 
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("StyleAPIClient.nodeDelete: error ", oEx);
		}
		
		return new HttpCallResponse();
	}
}
