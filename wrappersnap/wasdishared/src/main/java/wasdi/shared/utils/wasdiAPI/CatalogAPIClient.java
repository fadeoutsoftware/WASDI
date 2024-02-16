package wasdi.shared.utils.wasdiAPI;

import wasdi.shared.business.Node;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;

public class CatalogAPIClient {
	
	
	public static String downloadFileByName(Node oNode, String sSessionId, String sProductName, String sWorkspaceId, String sDestinationPath)  {
		try {
			
			String sDownloadUrl = oNode.getNodeBaseAddress();
			
			if (!sDownloadUrl.endsWith("/")) sDownloadUrl += "/";
			
			sDownloadUrl += "catalog/downloadbyname?filename=" +  sProductName;
			sDownloadUrl += "&workspace=" + sWorkspaceId;
			sDownloadUrl += "&token="+sSessionId;
			
			WasdiLog.debugLog("CatalogAPIClient.downloadFileByName: calling url: " + sDownloadUrl);
			
			String sOutputPath = HttpUtils.downloadFile(sDownloadUrl, HttpUtils.getStandardHeaders(sSessionId), sDestinationPath);
			return sOutputPath;
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("CatalogAPIClient.downloadFileByName: error ", oEx);
		}
		
		return "";
	}
	
	public static HttpCallResponse checkFileByNode(Node oNode, String sSessionId, String sFileName,String  sWorkspaceId) {
		try {
			
			// Get the url
			String sUrl = oNode.getNodeBaseAddress();
			
			// Safe programming				
			if (!sUrl.endsWith("/")) sUrl += "/";
			
			// Compose the API string				
			sUrl += "catalog/fileOnNode?filename="+sFileName+"&workspace="+sWorkspaceId;
			
			WasdiLog.debugLog("CatalogAPIClient.checkFileByNode: calling url: " + sUrl);
			
			return HttpUtils.httpGet(sUrl, HttpUtils.getStandardHeaders(sSessionId)); 
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("CatalogAPIClient.checkFileByNode: error ", oEx);
		}
		
		return new HttpCallResponse();		
	}
}
