package it.fadeout.threads;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.fadeout.Wasdi;
import wasdi.shared.business.Node;
import wasdi.shared.utils.Utils;

/**
 * Thread that calls all the computing nodes to ask to update the files of a processor
 * @author p.campanella
 *
 */
public class UpdateProcessorFilesWorker extends Thread {
	
	/**
	 * List of nodes to update
	 */
	List<Node> m_aoNodes;
	/**
	 * Path of the file with the users' update
	 */
	String m_sFilePath;
	/**
	 * User session
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
	 * Initialized the thread member variables
	 * @param aoNodes List of nodes
	 * @param sFilePath Path of the file with the users' update
	 * @param sSessionId Session Id
	 * @param sWorkspaceId Workspace Id
	 * @param sProcessorId Processor Id
	 */
	public void init(List<Node> aoNodes, String sFilePath, String sSessionId, String sWorkspaceId, String sProcessorId) {
		m_aoNodes = aoNodes;
		m_sFilePath = sFilePath;
		m_sSessionId = sSessionId;
		m_sWorkspaceId = sWorkspaceId;
		m_sProcessorId = sProcessorId;
	}
	
	@Override
	public void run() {
		
		Utils.debugLog("UpdateProcessorFilesWorker.run: start nodes update for processor " + m_sProcessorId);
		
		for (Node oNode : m_aoNodes) {
			
			if (oNode.getNodeCode().equals("wasdi")) continue;
			
			if (oNode.getActive() == false) continue;
			
			try {
				
				File oFile = new File(m_sFilePath);
				
				String sUrl = oNode.getNodeBaseAddress();
				
				if (!sUrl.endsWith("/")) sUrl += "/";
				
				sUrl += "processors/updatefiles?processorId="+m_sProcessorId+"&workspace="+m_sWorkspaceId+"&file=" + oFile.getName();
				
				Map<String, String> asHeaders = new HashMap<String, String>();
				asHeaders.put("x-session-token", m_sSessionId);
				
				Utils.debugLog("UpdateProcessorFilesWorker.run: calling url: " + sUrl);
				
				Wasdi.httpPostFile(sUrl, m_sFilePath, asHeaders);
				
				Utils.debugLog("UpdateProcessorFilesWorker.run: node updated " + oNode.getNodeCode());
				
			}
			catch (Exception oEx) {
				Utils.debugLog("UpdateProcessorFilesWorker.run: Exception " + oEx.toString());
			}
		}
		
		File oZipFile = new File(m_sFilePath);
		
		if (oZipFile.exists()) {
			
			if (oZipFile.getName().toUpperCase().endsWith(".ZIP")) {
				try {
					Utils.debugLog("UpdateProcessorFilesWorker.run: deleting zip file ");
					if (!oZipFile.delete()) {
						Utils.debugLog("UpdateProcessorFilesWorker.run: error deleting zip file ");
					}
				}
				catch (Exception oEx) {
					Utils.debugLog("UpdateProcessorFilesWorker.run: Exception " + oEx.toString());
				}
			}			
		}
		
		Utils.debugLog("UpdateProcessorFilesWorker.run: distribuited update done");
	}

}
