package it.fadeout.threads;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import wasdi.shared.business.Node;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Thread that calls all the computing nodes to ask to redeploy a processor
 * @author p.campanella
 *
 */
public class RedeployProcessorWorker extends Thread  {
	/**
	 * List of nodes to update
	 */
	List<Node> m_aoNodes;
	/**
	 * User Session ID
	 */
	String m_sSessionId;
	/**
	 * Workspace id
	 */
	String m_sWorkspaceId;
	/**
	 * Processor Id
	 */
	String m_sProcessorId;
	/**
	 * Processor Name
	 */
	String m_sProcessorName;
	/**
	 * Processor Type
	 */
	String m_sProcessorType;
	
	/**
	 * Initializes the thread members' varialbes.
	 * @param aoNodes List of nodes
	 * @param sSessionId session id 
	 * @param sWorkspaceId workspace id
	 * @param sProcessorId processor id
	 * @param sProcessorName Processor Name
	 * @param sProcessorType Processor Type
	 */
	public void init(List<Node> aoNodes, String sSessionId, String sWorkspaceId, String sProcessorId, String sProcessorName, String sProcessorType) {
		m_aoNodes = aoNodes;
		m_sSessionId = sSessionId;
		m_sWorkspaceId = sWorkspaceId;
		m_sProcessorId = sProcessorId;
		m_sProcessorName = sProcessorName;
		m_sProcessorType = sProcessorType;
	}
	
	/**
	 * Starts the thread: will try to call the 
	 * processors/redeploy
	 * API to all the involved nodes 
	 */
	@Override
	public void run() {
		
		WasdiLog.debugLog("RedeployProcessorWorker.run: start nodes redeploy for processor " + m_sProcessorId);
		
		// For each node		
		for (Node oNode : m_aoNodes) {
			
			// Jump the main one that is the only one that has to start this thread			
			if (oNode.getNodeCode().equals("wasdi")) continue;
			
			// The node must be active			
			if (oNode.getActive() == false) continue;
			
			try {
				// Get the url
				String sUrl = oNode.getNodeBaseAddress();
				
				// Safe programming				
				if (!sUrl.endsWith("/")) sUrl += "/";
				
				// Compose the API string				
				sUrl += "processors/redeploy?processorId="+m_sProcessorId+"&workspace="+m_sWorkspaceId;
				
				// Add the auth header				
				Map<String, String> asHeaders = new HashMap<String, String>();
				asHeaders.put("x-session-token", m_sSessionId);
				
				WasdiLog.debugLog("RedeployProcessorWorker.run: calling url: " + sUrl);
				
				// It is a get call				
				HttpUtils.httpGet(sUrl, asHeaders);
				
				WasdiLog.debugLog("RedeployProcessorWorker.run: redeployed on node " + oNode.getNodeCode());
				
			}
			catch (Exception oEx) {
				WasdiLog.debugLog("RedeployProcessorWorker.run: Exception " + oEx.toString());
			}
		}

		// All nodes updated
		WasdiLog.debugLog("RedeployProcessorWorker.run: distribuited redeploy done");
	}
}
