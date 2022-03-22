package it.fadeout.threads.styles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import wasdi.shared.business.Node;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;

/**
 * Thread that calls all the computing nodes to ask to add the file of a style
 * @author PetruPetrescu on 16/03/2022
 *
 */
@Getter
@Setter
@AllArgsConstructor
public class StyleAddFileWorker extends Thread {

	List<Node> m_aoNodes;
	String m_sSessionId;
	String m_sStyleName;
	String m_sStyleDescription;
	boolean m_bPublic;
	String m_sFilePath;

	@Override
	public void run() {
		Utils.debugLog("StyleAddFileWorker.run: start nodes add for style " + m_sStyleName);

		for (Node oNode : m_aoNodes) {

			if (oNode.getNodeCode().equals("wasdi")) {
				continue;
			}

			if (!oNode.getActive()) {
				continue;
			}

			try {
				String sUrl = oNode.getNodeBaseAddress();

				if (!sUrl.endsWith("/")) {
					sUrl += "/";
				}

				sUrl += "styles/uploadfile?name=" + m_sStyleName + "&description=" + m_sStyleDescription + "&public=" + m_bPublic;

				Map<String, String> asHeaders = new HashMap<>();
				asHeaders.put("x-session-token", m_sSessionId);
				asHeaders.put("Content-Type", "application/vnd.ogc.sld+xml");

				Utils.debugLog("StyleAddFileWorker.run: calling url: " + sUrl);

				HttpUtils.httpPostFile(sUrl, m_sFilePath, asHeaders);

				Utils.debugLog("StyleAddFileWorker.run: node updated " + oNode.getNodeCode());
			} catch (Exception oEx) {
				Utils.debugLog("StyleAddFileWorker.run: Exception " + oEx.getMessage());
			}
		}

		Utils.debugLog("StyleAddFileWorker.run: distribuited adding done");
	}

}
