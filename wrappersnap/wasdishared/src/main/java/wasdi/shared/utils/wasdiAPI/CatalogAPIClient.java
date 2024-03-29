package wasdi.shared.utils.wasdiAPI;

import wasdi.shared.business.Node;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;

/**
 * Client of the WASDI Catalog Resource API 
 */
public class CatalogAPIClient {
	
	/**
	 * Download a file from WASDI Node
	 * @param oNode Target Node
	 * @param sSessionId Session Id
	 * @param sProductName Name of the product to download
	 * @param sWorkspaceId Workspace Id
	 * @param sDestinationPath Local Destination Path to save the file
	 * @return
	 */
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
	
	/**
	 * Check the physical availability of a file in a Node
	 * @param oNode Target Node
	 * @param sSessionId Active Session
	 * @param sFileName File to search
	 * @param sWorkspaceId Workspace where to search
	 * @return Primitive Result embedded in the HttpCallResponse
	 */
	public static HttpCallResponse checkFileByNode(Node oNode, String sSessionId, String sFileName,String  sWorkspaceId) {
		try {
			
			// Get the url
			String sUrl = oNode.getNodeBaseAddress();
			
			// Safe programming				
			if (!sUrl.endsWith("/")) sUrl += "/";
			
			// Compose the API string				
			sUrl += "catalog/fileOnNode?filename="+sFileName+"&workspace="+sWorkspaceId+"&token="+sSessionId;
			
			WasdiLog.debugLog("CatalogAPIClient.checkFileByNode: calling url: " + sUrl);
			
			return HttpUtils.httpGet(sUrl, HttpUtils.getStandardHeaders(sSessionId)); 
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("CatalogAPIClient.checkFileByNode: error ", oEx);
		}
		
		return new HttpCallResponse();		
	}
	
	/**
	 * Check the availability of a file in a node/workspace
	 * @param oNode Target Node
	 * @param sSessionId Active Session
	 * @param sFileName File to search
	 * @param sWorkspaceId Workspace where to search
	 * @param sProcessObjId Parent Process Id
	 * @param sVolumePath Path in the volume if and as returned by the search API
	 * @return Primitive Result embedded in the HttpCallResponse
	 */
	public static HttpCallResponse checkDownloadAvaialibityNyName(Node oNode, String sSessionId, String sFileName, String sWorkspaceId, String sProcessObjId, String sVolumePath) {
		try {
			
			// Get the url
			String sUrl = oNode.getNodeBaseAddress();
			
			// Safe programming				
			if (!sUrl.endsWith("/")) sUrl += "/";
			
			// Compose the API string				
			sUrl += "catalog/checkdownloadavaialibitybyname?filename="+sFileName+"&workspace="+sWorkspaceId+"&token="+sSessionId;
			
			if (!Utils.isNullOrEmpty(sProcessObjId)) {
				sUrl+="&procws="+sProcessObjId;
			}
			
			if (!Utils.isNullOrEmpty(sVolumePath)) {
				sUrl+="&volumepath="+sVolumePath;
			}
			
			WasdiLog.debugLog("CatalogAPIClient.checkDownloadAvaialibityNyName: calling url: " + sUrl);
			
			return HttpUtils.httpGet(sUrl, HttpUtils.getStandardHeaders(sSessionId)); 
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("CatalogAPIClient.checkFileByNode: error ", oEx);
		}
		
		return new HttpCallResponse();
	}
}
