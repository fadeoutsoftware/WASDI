package wasdi.shared.utils.wasdiAPI;

import wasdi.shared.business.Node;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.StringUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;

public class ProcessingAPIClient {
	
	public static HttpCallResponse runProcess(Node oNode, String sSessionId, String sOperationType, String sProductName, String sParentId, String sOperationSubId, String sPayload)  {
		try {
			
			String sUrl = oNode.getNodeBaseAddress();
			
			if (sUrl.endsWith("/") == false) sUrl += "/";
			sUrl += "processing/run?operation=" + sOperationType + "&name=" + StringUtils.encodeUrl(sProductName);
			
			// Is there a parent?
			if (!Utils.isNullOrEmpty(sParentId)) {
				WasdiLog.debugLog("ProcessingAPIClient.runProcess: adding parent " + sParentId);
				sUrl += "&parent=" + StringUtils.encodeUrl(sParentId);
			}
			
			// Is there a subType?
			if (!Utils.isNullOrEmpty(sOperationSubId)) {
				WasdiLog.debugLog("ProcessingAPIClient.runProcess: adding sub type " + sOperationSubId);
				sUrl += "&subtype=" + sOperationSubId;
			}

			WasdiLog.debugLog("ProcessingAPIClient.runProcess: calling url: " + sUrl);
			
			// call the API on the destination node 
			return HttpUtils.httpPost(sUrl, sPayload, HttpUtils.getStandardHeaders(sSessionId)); 
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessingAPIClient.runProcess: error ", oEx);
		}
		
		return new HttpCallResponse();
	}

}
