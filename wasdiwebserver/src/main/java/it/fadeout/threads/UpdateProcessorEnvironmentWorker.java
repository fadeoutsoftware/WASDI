package it.fadeout.threads;

import java.util.List;

import wasdi.shared.business.Node;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.wasdiAPI.PackageManagerAPIClient;

/**
 * Thread that calls all the computing nodes to ask to update the processor's environment
 * @author p.petrescu
 *
 */
public class UpdateProcessorEnvironmentWorker extends Thread {

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
	 * Update Command
	 */
	String m_sUpdateCommand;

	/**
	 * Initializes the thread members' variables.
	 * @param aoNodes List of nodes
	 * @param sSessionId session id 
	 * @param sWorkspaceId workspace id
	 * @param sProcessorId processor id
	 * @param sUpdateCommand update command
	 */
	public void init(List<Node> aoNodes, String sSessionId, String sWorkspaceId, String sProcessorId, String sUpdateCommand) {
		m_aoNodes = aoNodes;
		m_sSessionId = sSessionId;
		m_sWorkspaceId = sWorkspaceId;
		m_sProcessorId = sProcessorId;
		m_sUpdateCommand = sUpdateCommand;
	}

	/**
	 * Starts the thread: will try to call the 
	 * processors/node environment update
	 * API to all the involved nodes
	 */
	@Override
	public void run() {
		WasdiLog.debugLog("UpdateProcessorEnvironmentWorker.run: start nodes environment update for processor " + m_sProcessorId);

		// For each node
		for (Node oNode : m_aoNodes) {

			// Jump the main one that is the only one that has to start this thread
			if (oNode.getNodeCode().equals("wasdi")) continue;

			// The node must be active
			if (!oNode.getActive()) continue;

			try {

				PackageManagerAPIClient.environmentUpdate(oNode, m_sSessionId, m_sProcessorId, m_sWorkspaceId, m_sUpdateCommand);
				WasdiLog.debugLog("UpdateProcessorEnvironmentWorker.run: environment update on node " + oNode.getNodeCode());
			}
			catch (Exception oEx) {
				WasdiLog.debugLog("UpdateProcessorEnvironmentWorker.run: Exception " + oEx.toString());
			}
		}

		// All nodes updated
		WasdiLog.debugLog("UpdateProcessorEnvironmentWorker.run: distribuited environment update done");
	}
}
