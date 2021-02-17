package it.fadeout.threads;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.fadeout.Wasdi;
import wasdi.shared.business.Node;
import wasdi.shared.utils.Utils;

public class DeleteProcessorWorker extends Thread  {
	List<Node> m_aoNodes;
	String m_sSessionId;
	String m_sWorkspaceId;
	String m_sProcessorId;
	String m_sProcessorName;
	String m_sProcessorType;
	
	public void init(List<Node> aoNodes, String sSessionId, String sWorkspaceId, String sProcessorId, String sProcessorName, String sProcessorType) {
		m_aoNodes = aoNodes;
		m_sSessionId = sSessionId;
		m_sWorkspaceId = sWorkspaceId;
		m_sProcessorId = sProcessorId;
		m_sProcessorName = sProcessorName;
		m_sProcessorType = sProcessorType;
	}
	
	@Override
	public void run() {
		
		Utils.debugLog("DeleteProcessorWorker.run: start nodes update for processor " + m_sProcessorId);
		
		for (Node oNode : m_aoNodes) {
			
			if (oNode.getNodeCode().equals("wasdi")) continue;
			
			if (oNode.getActive() == false) continue;
			
			try {
				String sUrl = oNode.getNodeBaseAddress();
				
				if (!sUrl.endsWith("/")) sUrl += "/";
				
				sUrl += "processors/nodedelete?processorId="+m_sProcessorId+"&workspace="+m_sWorkspaceId+"&processorName="+m_sProcessorName+"&processorType="+m_sProcessorType;
				
				Map<String, String> asHeaders = new HashMap<String, String>();
				asHeaders.put("x-session-token", m_sSessionId);
				
				Utils.debugLog("DeleteProcessorWorker.run: calling url: " + sUrl);
				
				Wasdi.httpGet(sUrl, asHeaders);
				
				Utils.debugLog("DeleteProcessorWorker.run: deleted on node " + oNode.getNodeCode());
				
			}
			catch (Exception oEx) {
				Utils.debugLog("DeleteProcessorWorker.run: Exception " + oEx.toString());
			}
		}
				
		Utils.debugLog("DeleteProcessorWorker.run: distribuited delete done");
	}
}
