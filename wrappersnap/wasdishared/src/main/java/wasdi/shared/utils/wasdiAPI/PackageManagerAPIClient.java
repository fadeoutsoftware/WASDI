package wasdi.shared.utils.wasdiAPI;

import wasdi.shared.business.Node;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;

public class PackageManagerAPIClient {
	
	public static HttpCallResponse environmentUpdate(Node oNode, String sSessionId, String sProcessorId, String sWorkspaceId,String sUpdateCommand)  {
		try {
			
			// Get the url
			String sUrl = oNode.getNodeBaseAddress();

			// Safe programming
			if (!sUrl.endsWith("/")) sUrl += "/";

			// Compose the API string
			sUrl += "packageManager/environmentupdate?processorId=" + sProcessorId + "&workspace=" + sWorkspaceId;

			if (sUpdateCommand != null) {
				sUrl += "&updateCommand=" + sUpdateCommand;
			}

			WasdiLog.debugLog("PackageManagerAPIClient.environmentUpdate: calling url: " + sUrl);
			
			return HttpUtils.httpGet(sUrl, HttpUtils.getStandardHeaders(sSessionId)); 
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("PackageManagerAPIClient.environmentUpdate: error ", oEx);
		}
		
		return new HttpCallResponse();
	}
}
