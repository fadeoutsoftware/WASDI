package wasdi.shared.utils.wasdiAPI;

import java.util.HashMap;
import java.util.Map;

import wasdi.shared.business.Node;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;

/**
 * Class with static methods to call WASDI API
 */
public class ProcessWorkspaceAPIClient {
	
	/**
	 * Call process/runningTime/UI
	 * @param oNode
	 * @param sSessionId
	 * @param sTargetUserId
	 * @param sDateFrom
	 * @param sDateTo
	 * @return
	 */
	public static HttpCallResponse getRunningTimeUI(Node oNode, String sSessionId, String sTargetUserId,String sDateFrom,String sDateTo) {
		try {
			String sUrl = oNode.getNodeBaseAddress();
			if (!sUrl.endsWith("/")) sUrl += "/";
			sUrl += "process/runningTime/UI?userId=" + sTargetUserId +"&dateFrom=" + sDateFrom + "&dateTo=" + sDateTo;

			Map<String, String> asHeaders = new HashMap<String, String>();
			asHeaders.put("x-session-token", sSessionId);

			WasdiLog.debugLog("ProcessWorkspaceAPIClient.getRunningTimeUI: calling url: " + sUrl);

			return HttpUtils.httpGet(sUrl, asHeaders);			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessWorkspaceAPIClient.getRunningTimeUI: error ", oEx);
		}
		
		return new HttpCallResponse();
	}

	
	/**
	 * Call process/appstats
	 * @param oNode
	 * @param sSessionId
	 * @param sProcessorName
	 * @return
	 */
	public static HttpCallResponse getAppStats(Node oNode, String sSessionId, String sProcessorName) {
		try {
			
			String sUrl = oNode.getNodeBaseAddress();
			
			if (!sUrl.endsWith("/")) sUrl += "/";
			
			sUrl += "process/appstats?processorName="+sProcessorName;
			
			Map<String, String> asHeaders = new HashMap<String, String>();
			asHeaders.put("x-session-token", sSessionId);
			
			WasdiLog.debugLog("ProcessWorkspaceAPIClient.getAppStats: calling url: " + sUrl);
			
			
			return HttpUtils.httpGet(sUrl, asHeaders);
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessWorkspaceAPIClient.getAppStats: error ", oEx);
		}
		
		return new HttpCallResponse();
	}
	
	/**
	 * Call process/queuesStatus
	 * @param oNode
	 * @param sSessionId
	 * @param sStatuses
	 * @return
	 */
	public static HttpCallResponse getQueueStatus(Node oNode, String sSessionId, String sStatuses) {
		try {
			
			String sUrl = oNode.getNodeBaseAddress();
			if (sUrl.endsWith("/") == false) sUrl += "/";
			sUrl += "process/queuesStatus?nodeCode=" + oNode.getNodeCode() + "&statuses=" + sStatuses;
			
			WasdiLog.debugLog("ProcessWorkspaceAPIClient.getQueueStatus: calling url: " + sUrl);
			
			return HttpUtils.httpGet(sUrl, HttpUtils.getStandardHeaders(sSessionId)); 

		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessWorkspaceAPIClient.getQueueStatus: error ", oEx);
		}
		
		return new HttpCallResponse();
	}
	
	/**
	 * Call process/runningtimeproject
	 * @param oNode
	 * @param sSessionId
	 * @return
	 */
	public static HttpCallResponse getRunningTimePerProject(Node oNode, String sSessionId) {
		try {
			
			String sUrl = oNode.getNodeBaseAddress();
			
			if (!sUrl.endsWith("/")) sUrl += "/";
			sUrl += "process/runningtimeproject";

			WasdiLog.debugLog("ProcessWorkspaceAPIClient.getRunningTimePerProject: calling url: " + sUrl);
			return HttpUtils.httpGet(sUrl, HttpUtils.getStandardHeaders(sSessionId)); 
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessWorkspaceAPIClient.getRunningTimePerProject: error ", oEx);
		}
		
		return new HttpCallResponse();
	}
	
	/**
	 * Call process/runningtimeproject/byuser
	 * @param oNode
	 * @param sSessionId
	 * @return
	 */
	public static HttpCallResponse getRunningTimePerProjectPerUser(Node oNode, String sSessionId) {
		try {
			
			String sUrl = oNode.getNodeBaseAddress();
			
			if (!sUrl.endsWith("/")) sUrl += "/";
			sUrl += "process/runningtimeproject/byuser";

			WasdiLog.debugLog("ProcessWorkspaceAPIClient.getRunningTimePerProjectPerUser: calling url: " + sUrl);
			return HttpUtils.httpGet(sUrl, HttpUtils.getStandardHeaders(sSessionId)); 
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessWorkspaceAPIClient.getRunningTimePerProjectPerUser: error ", oEx);
		}
		
		return new HttpCallResponse();
	}	
	
	/**
	 * Call process/byapp?processorName
	 * @param oNode
	 * @param sSessionId
	 * @param sProcessorName
	 * @return
	 */
	public static HttpCallResponse getByApplication(Node oNode, String sSessionId, String sProcessorName) {
		try {
			
			String sUrl = oNode.getNodeBaseAddress();
			
			if (!sUrl.endsWith("/")) sUrl += "/";
			sUrl += "process/byapp?processorName="+sProcessorName;

			WasdiLog.debugLog("ProcessWorkspaceAPIClient.getByApplication: calling url: " + sUrl);
			return HttpUtils.httpGet(sUrl, HttpUtils.getStandardHeaders(sSessionId)); 
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessWorkspaceAPIClient.getByApplication: error ", oEx);
		}
		
		return new HttpCallResponse();
	}
	
	/**
	 * Call process/byusr
	 * @param oNode
	 * @param sSessionId
	 * @param sProcessorName
	 * @return
	 */
	public static HttpCallResponse getByUser(Node oNode, String sSessionId, boolean bOgcOnly) {
		try {
			
			String sUrl = oNode.getNodeBaseAddress();
			
			if (!sUrl.endsWith("/")) sUrl += "/";
			sUrl += "process/byusr?ogc=" + bOgcOnly;

			WasdiLog.debugLog("ProcessWorkspaceAPIClient.getByUser: calling url: " + sUrl);
			return HttpUtils.httpGet(sUrl, HttpUtils.getStandardHeaders(sSessionId)); 
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessWorkspaceAPIClient.getByUser: error ", oEx);
		}
		
		return new HttpCallResponse();
	}
	
	
	/**
	 * Call process/runningTime/SP
	 * @param oNode
	 * @param sSessionId
	 * @return
	 */
	public static HttpCallResponse getRunningTimeBySubscriptionAndProject(Node oNode, String sSessionId)  {
		try {
			
			String sUrl = oNode.getNodeBaseAddress();
			
			if (!sUrl.endsWith("/")) sUrl += "/";
			sUrl += "process/runningTime/SP";

			WasdiLog.debugLog("ProcessWorkspaceAPIClient.getRunningTimeBySubscriptionAndProject: calling url: " + sUrl);
			return HttpUtils.httpGet(sUrl, HttpUtils.getStandardHeaders(sSessionId)); 
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessWorkspaceAPIClient.getRunningTimeBySubscriptionAndProject: error ", oEx);
		}
		
		return new HttpCallResponse();
	}

	public static HttpCallResponse getById(Node oNode, String sSessionId, String sProcWorkspaceId)  {
		try {
			
			String sUrl = oNode.getNodeBaseAddress();
			
			if (!sUrl.endsWith("/")) sUrl += "/";
			sUrl += "process/byid?procws=" + sProcWorkspaceId;

			WasdiLog.debugLog("ProcessWorkspaceAPIClient.getById: calling url: " + sUrl);
			return HttpUtils.httpGet(sUrl, HttpUtils.getStandardHeaders(sSessionId)); 
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ProcessWorkspaceAPIClient.getById: error ", oEx);
		}
		
		return new HttpCallResponse();
	}
}
