package it.fadeout.threads;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import wasdi.shared.business.Node;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;

public class ForceLibraryUpdateWorker extends Thread  {
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
	 * Initializes the thread members' varialbes.
	 * @param aoNodes List of nodes
	 * @param sSessionId session id 
	 * @param sWorkspaceId workspace id
	 * @param sProcessorId processor id
	 * @param sProcessorName Processor Name
	 * @param sProcessorType Processor Type
	 */
	public void init(List<Node> aoNodes, String sSessionId, String sWorkspaceId, String sProcessorId) {
		m_aoNodes = aoNodes;
		m_sSessionId = sSessionId;
		m_sWorkspaceId = sWorkspaceId;
		m_sProcessorId = sProcessorId;
	}
	
	/**
	 * Starts the thread: will try to call the 
	 * processors/redeploy
	 * API to all the involved nodes 
	 */
	@Override
	public void run() {
		
		Utils.debugLog("ForceLibraryUpdateWorker.run: start force lib update for processor " + m_sProcessorId);
		
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
				sUrl += "processors/libupdate?processorId="+m_sProcessorId+"&workspace="+m_sWorkspaceId;
				
				// Add the auth header				
				Map<String, String> asHeaders = new HashMap<String, String>();
				asHeaders.put("x-session-token", m_sSessionId);
				
				Utils.debugLog("ForceLibraryUpdateWorker.run: calling url: " + sUrl);
				
				// It is a get call				
				HttpUtils.httpGet(sUrl, asHeaders);
				
				Utils.debugLog("ForceLibraryUpdateWorker.run: lib updated on node " + oNode.getNodeCode());
				
			}
			catch (Exception oEx) {
				Utils.debugLog("ForceLibraryUpdateWorker.run: Exception " + oEx.toString());
			}
		}

		// All nodes updated
		Utils.debugLog("ForceLibraryUpdateWorker.run: distribuited force lib update done");
	}
}
