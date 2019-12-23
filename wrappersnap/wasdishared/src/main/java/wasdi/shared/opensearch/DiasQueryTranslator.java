/**
 * Created by Cristiano Nattero on 2018-11-28
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch;

import java.util.HashMap;

import wasdi.shared.utils.Utils;

/**
 * @author c.nattero
 *
 */
public abstract class DiasQueryTranslator {
	
	protected HashMap<String, String> keyMapping;
	protected HashMap<String, String> valueMapping;

	public String translateAndEncode(String sQuery) {
		Utils.debugLog("DiasQueryTranslator.translateAndEncode");
		return encode(translate(sQuery));
	}
	
	//translates from WASDI query (OpenSearch) to <derived class> format
	protected abstract String translate(String sQuery);

	protected String encode( String sDecoded ) {
		String sResult = sDecoded; 
		sResult = sResult.replace(" ", "%20");
		sResult = sResult.replaceAll("\"", "%22");
		//sResult = java.net.URLEncoder.encode(sDecoded, m_sEnconding);
		return sResult;
	}
	
}
