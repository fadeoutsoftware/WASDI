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
 * Thread that calls all the computing nodes to ask to delete the file of a style
 * @author PetruPetrescu on 11/03/2022
 *
 */
@Getter
@Setter
@AllArgsConstructor
public class StyleDeleteFileWorker extends Thread {

	List<Node> m_aoNodes;
	String m_sSessionId;
	String m_sStyleId;
	String m_sStyleName;

	@Override
	public void run() {
		Utils.debugLog("StyleDeleteFileWorker.run: start nodes delete for style " + m_sStyleName);

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

				sUrl += "styles/nodedelete?styleId=" + m_sStyleId + "&styleName=" + m_sStyleName;

				Map<String, String> asHeaders = new HashMap<>();
				asHeaders.put("x-session-token", m_sSessionId);

				Utils.debugLog("StyleDeleteFileWorker.run: calling url: " + sUrl);

				HttpUtils.httpDelete(sUrl, asHeaders);

				Utils.debugLog("StyleDeleteFileWorker.run: node deleted " + oNode.getNodeCode());
			} catch (Exception oEx) {
				Utils.debugLog("StyleDeleteFileWorker.run: Exception " + oEx.getMessage());
			}
		}

		Utils.debugLog("StyleDeleteFileWorker.run: distribuited delete done");
	}

}
