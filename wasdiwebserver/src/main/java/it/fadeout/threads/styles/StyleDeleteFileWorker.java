package it.fadeout.threads.styles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import wasdi.shared.business.Node;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.wasdiAPI.StyleAPIClient;

/**
 * Thread that calls all the computing nodes to ask to delete the file of a style
 * @author PetruPetrescu on 11/03/2022
 *
 */
public class StyleDeleteFileWorker extends Thread {
	
	public StyleDeleteFileWorker() {
		
	}

	public StyleDeleteFileWorker(List<Node> aoNodes, String sSessionId, String sStyleId, String sStyleName ) {
		this.m_aoNodes = aoNodes;
		this.m_sSessionId = sSessionId;
		this.m_sStyleId = sStyleId;
		this.m_sStyleName = sStyleName;
	}
	
	List<Node> m_aoNodes;
	String m_sSessionId;	
	String m_sStyleId;
	String m_sStyleName;

	@Override
	public void run() {
		WasdiLog.debugLog("StyleDeleteFileWorker.run: start nodes delete for style " + m_sStyleName);

		for (Node oNode : m_aoNodes) {

			if (oNode.getNodeCode().equals("wasdi")) {
				continue;
			}

			if (!oNode.getActive()) {
				continue;
			}

			try {
				StyleAPIClient.nodeDelete(oNode, m_sSessionId, m_sStyleId, m_sStyleName);				
				WasdiLog.debugLog("StyleDeleteFileWorker.run: node deleted " + oNode.getNodeCode());
			} catch (Exception oEx) {
				WasdiLog.debugLog("StyleDeleteFileWorker.run: Exception " + oEx.getMessage());
			}
		}

		WasdiLog.debugLog("StyleDeleteFileWorker.run: distribuited delete done");
	}

}
