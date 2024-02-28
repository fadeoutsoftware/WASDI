package it.fadeout.threads.styles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import wasdi.shared.business.Node;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.wasdiAPI.StyleAPIClient;

/**
 * Thread that calls all the computing nodes to ask to update the file of a style
 * @author PetruPetrescu on 11/03/2022
 *
 */
public class StyleUpdateFileWorker extends Thread {
	
	List<Node> m_aoNodes;
	String m_sSessionId;
	String m_sStyleId;
	String m_sFilePath;
	
	public StyleUpdateFileWorker(List<Node> aoNodes, String sSessionId, String sStyleId, String sFilePath ) {
		this.m_aoNodes = aoNodes;
		this.m_sSessionId = sSessionId;
		this.m_sStyleId = sStyleId;
		this.m_sFilePath = sFilePath;
	}

	@Override
	public void run() {
		WasdiLog.debugLog("StyleUpdateFileWorker.run: start nodes update for style " + m_sStyleId);

		for (Node oNode : m_aoNodes) {

			if (oNode.getNodeCode().equals("wasdi")) {
				continue;
			}

			if (!oNode.getActive()) {
				continue;
			}

			try {
				StyleAPIClient.updateFile(oNode, m_sSessionId, m_sStyleId, m_sFilePath);
				WasdiLog.debugLog("StyleUpdateFileWorker.run: node updated " + oNode.getNodeCode());
			} 
			catch (Exception oEx) {
				WasdiLog.debugLog("StyleUpdateFileWorker.run: Exception " + oEx.getMessage());
			}
		}

		WasdiLog.debugLog("StyleUpdateFileWorker.run: distribuited update done");
	}

}
