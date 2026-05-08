package wasdi.shared.utils.wasdiAPI;

import java.io.File;

import wasdi.shared.business.Node;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;

public class ProcessorAPIClient {
	
	public static HttpCallResponse nodeDelete(Node oNode, String sSessionId, String sProcessorId, String sWorkspaceId, String sProcessorName, String sProcessorType, String sVersion)  {
		try {
			
			String sUrl = oNode.getNodeBaseAddress();
			
			// Safe programming
			if (!sUrl.endsWith("/")) sUrl += "/";
			
			// Compose the API string
			sUrl += "processors/nodedelete?processorId="+sProcessorId+"&workspace="+sWorkspaceId+"&processorName="+sProcessorName+"&processorType="+sProcessorType+"&version="+sVersion;
						
			WasdiLog.debugLog("ProcessorAPIClient.nodeDelete: calling url: " + sUrl);
			
			return HttpUtils.httpGet(sUrl, HttpUtils.getStandardHeaders(sSessionId)); 
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorAPIClient.nodeDelete: error ", oEx);
		}
		
		return new HttpCallResponse();
	}
	
	/**
	 * Call the main node delete Processor API 
	 * @param sSessionId
	 * @param sProcessorId
	 * @return
	 */
	public static HttpCallResponse delete(String sSessionId, String sProcessorId)  {
		try {
			
			String sUrl = WasdiConfig.Current.baseUrl;
			
			// Safe programming
			if (!sUrl.endsWith("/")) sUrl += "/";
			
			// Compose the API string
			sUrl += "processors/delete?processorId="+sProcessorId;
						
			WasdiLog.debugLog("ProcessorAPIClient.nodeDelete: calling url: " + sUrl);
			
			return HttpUtils.httpGet(sUrl, HttpUtils.getStandardHeaders(sSessionId)); 
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorAPIClient.nodeDelete: error ", oEx);
		}
		
		return new HttpCallResponse();
	}
	
	public static HttpCallResponse forceLibraryUpdate(Node oNode, String sSessionId, String sProcessorId, String sWorkspaceId)  {
		try {
			
			// Get the url
			String sUrl = oNode.getNodeBaseAddress();
			
			// Safe programming				
			if (!sUrl.endsWith("/")) sUrl += "/";
			
			// Compose the API string				
			sUrl += "processors/libupdate?processorId="+sProcessorId+"&workspace="+sWorkspaceId;
			
			WasdiLog.debugLog("ProcessorAPIClient.forceLibraryUpdate: calling url: " + sUrl);
			
			return HttpUtils.httpGet(sUrl, HttpUtils.getStandardHeaders(sSessionId)); 
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorAPIClient.forceLibraryUpdate: error ", oEx);
		}
		
		return new HttpCallResponse();
	}

	public static HttpCallResponse redeploy(Node oNode, String sSessionId, String sProcessorId, String sWorkspaceId)  {
		try {
			
			// Get the url
			String sUrl = oNode.getNodeBaseAddress();
			
			// Safe programming				
			if (!sUrl.endsWith("/")) sUrl += "/";
			
			// Compose the API string				
			sUrl += "processors/redeploy?processorId="+sProcessorId+"&workspace="+sWorkspaceId;
			
			WasdiLog.debugLog("ProcessorAPIClient.redeploy: calling url: " + sUrl);
			
			return HttpUtils.httpGet(sUrl, HttpUtils.getStandardHeaders(sSessionId)); 
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorAPIClient.redeploy: error ", oEx);
		}
		
		return new HttpCallResponse();
	}
	
	public static boolean updateProcessorFiles(Node oNode, String sSessionId, String sProcessorId, String sWorkspaceId, File oFile, String sFilePath)  {
		try {
			
			String sUrl = oNode.getNodeBaseAddress();
			
			if (!sUrl.endsWith("/")) sUrl += "/";
			
			sUrl += "processors/updatefiles?processorId="+sProcessorId+"&workspace="+sWorkspaceId+"&file=" + oFile.getName();
						
			WasdiLog.debugLog("ProcessorAPIClient.updateProcessorFiles: calling url: " + sUrl);
			
			return HttpUtils.httpPostFile(sUrl, sFilePath, HttpUtils.getStandardHeaders(sSessionId)); 
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessorAPIClient.redeploy: error ", oEx);
		}
		
		return false;
	}
	
}
