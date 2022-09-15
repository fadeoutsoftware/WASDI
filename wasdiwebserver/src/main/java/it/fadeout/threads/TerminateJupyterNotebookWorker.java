package it.fadeout.threads;

import java.util.HashMap;
import java.util.Map;

import wasdi.shared.business.Node;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;

public class TerminateJupyterNotebookWorker extends Thread {

	/**
	 * The WASDI node to update
	 */
	Node m_oNode;

	/**
	 * Actual user session id
	 */
	String m_sSessionId;

	/**
	 * Workspace Id
	 */
	String m_sWorkspaceId;

	/**
	 * Initializes the thread members' variables.
	 * @param aNode the node
	 * @param sSessionId session id 
	 * @param sWorkspaceId workspace id
	 */
	public void init(Node oNode, String sSessionId, String sWorkspaceId) {
		m_oNode = oNode;
		m_sSessionId = sSessionId;
		m_sWorkspaceId = sWorkspaceId;
	}

	/**
	 * Starts the thread: will try to call the 
	 * processors/node environment update
	 * API to the involved node
	 */
	@Override
	public void run() {
		Utils.debugLog("TerminateJupyterNotebookWorker.run: terminate JupyterNotebook for workspaceId " + m_sWorkspaceId);

		Node oNode = m_oNode;

		// Skip the main one that is the only one that has to start this thread
		// The node must be active
		if (!oNode.getNodeCode().equals("wasdi") && oNode.getActive()) {
			try {
				// Get the url
				String sUrl = oNode.getNodeBaseAddress();

				// Safe programming
				if (!sUrl.endsWith("/")) sUrl += "/";

				// Compose the API string
				sUrl += "/console/terminate?" + "workspaceId=" + m_sWorkspaceId;

				// Add the auth header
				Map<String, String> asHeaders = new HashMap<>();
				asHeaders.put("x-session-token", m_sSessionId);

				Utils.debugLog("TerminateJupyterNotebookWorker.run: calling url: " + sUrl);

				// It is a get call
				HttpUtils.httpPost(sUrl, null, asHeaders);

			} catch (Exception oEx) {
				Utils.debugLog("TerminateJupyterNotebookWorker.run: Exception " + oEx.toString());
			}
		}

		// The node was called
		Utils.debugLog("TerminateJupyterNotebookWorker.run: distribuited Jupyter Notebook terminated");
	}

}
