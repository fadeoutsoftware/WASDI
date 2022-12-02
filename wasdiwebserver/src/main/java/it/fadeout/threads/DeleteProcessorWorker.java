package it.fadeout.threads;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import wasdi.shared.business.Node;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Thread that calls all the computing nodes to ask to delete a processor
 * @author p.campanella
 *
 */
public class DeleteProcessorWorker extends Thread  {
	
	/**
	 * List of WASDI nodes to update
	 */
	List<Node> m_aoNodes;
	/**
	 * Actual user session id
	 */
	String m_sSessionId;
	/**
	 * Workspace Id
	 */
	String m_sWorkspaceId;
	/**
	 * Processor id 
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
	 * Version
	 */
	String m_sVersion;
	
	/**
	 * Initializes the thread members' varialbes.
	 * @param aoNodes List of nodes
	 * @param sSessionId session id 
	 * @param sWorkspaceId workspace id
	 * @param sProcessorId processor id
	 * @param sProcessorName Processor Name
	 * @param sProcessorType Processor Type
	 */
	public void init(List<Node> aoNodes, String sSessionId, String sWorkspaceId, String sProcessorId, String sProcessorName, String sProcessorType, String sVersion) {
		m_aoNodes = aoNodes;
		m_sSessionId = sSessionId;
		m_sWorkspaceId = sWorkspaceId;
		m_sProcessorId = sProcessorId;
		m_sProcessorName = sProcessorName;
		m_sProcessorType = sProcessorType;
		m_sVersion = sVersion;
	}
	
	/**
	 * Starts the thread: will try to call the 
	 * processors/nodedelete
	 * API to all the involved nodes
	 */
	@Override
	public void run() {
		
		WasdiLog.debugLog("DeleteProcessorWorker.run: start nodes delete for processor " + m_sProcessorId);
		
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
				sUrl += "processors/nodedelete?processorId="+m_sProcessorId+"&workspace="+m_sWorkspaceId+"&processorName="+m_sProcessorName+"&processorType="+m_sProcessorType+"&version="+m_sVersion;
				
				// Add the auth header
				Map<String, String> asHeaders = new HashMap<String, String>();
				asHeaders.put("x-session-token", m_sSessionId);
				
				WasdiLog.debugLog("DeleteProcessorWorker.run: calling url: " + sUrl);
				
				// It is a get call
				HttpUtils.httpGet(sUrl, asHeaders);
				
				WasdiLog.debugLog("DeleteProcessorWorker.run: deleted on node " + oNode.getNodeCode());
				
			}
			catch (Exception oEx) {
				WasdiLog.debugLog("DeleteProcessorWorker.run: Exception " + oEx.toString());
			}
		}
		
		// All nodes updated
		WasdiLog.debugLog("DeleteProcessorWorker.run: distribuited delete done");
	}
}
