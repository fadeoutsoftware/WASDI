package it.fadeout.threads;

import java.util.List;

import wasdi.shared.business.Node;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.wasdiAPI.ProcessorAPIClient;

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
				ProcessorAPIClient.nodeDelete(oNode, m_sSessionId, m_sProcessorId, m_sWorkspaceId, m_sProcessorName, m_sProcessorType, m_sVersion);				
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
