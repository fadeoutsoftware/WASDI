/**
 * Created by Cristiano Nattero on 2018-11-28
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch;

import com.google.common.base.Preconditions;

import wasdi.shared.utils.Utils;

/**
 * @author c.nattero
 *
 */
public abstract class DiasQueryTranslator {
	
	protected String m_sParserConfigPath;
	protected String m_sAppConfigPath;

	public String translateAndEncode(String sQueryFromClient) {
		Utils.debugLog("DiasQueryTranslator.translateAndEncode");
		return encode(translate(sQueryFromClient));
	}
	
	public void setParserConfigPath(String sParserConfigPath) {
		if(null==sParserConfigPath) {
			Utils.debugLog("DiasQueryTranslator.setParserConfigPath: warning: parser config path is null");
		} else if(sParserConfigPath.isEmpty()) {
			Utils.debugLog("DiasQueryTranslator.setParserConfigPath: warning: parser config path is empty");
		}
		this.m_sParserConfigPath = sParserConfigPath;
	}
	
	public void setAppconfigPath(String sAppConfigPath) {
		if(null==sAppConfigPath) {
			Utils.debugLog("DiasQueryTranslator.setParserConfigPath: warning: app config path is null");
		} else if(sAppConfigPath.isEmpty()) {
			Utils.debugLog("DiasQueryTranslator.setParserConfigPath: warning: app config path is empty");
		}
		this.m_sAppConfigPath = sAppConfigPath;
	}
	
	//translates from WASDI query (OpenSearch) to <derived class> format
	protected abstract String translate(String sQueryFromClient);

	protected String encode( String sDecoded ) {
		String sResult = sDecoded; 
		sResult = sResult.replace(" ", "%20");
		sResult = sResult.replaceAll("\"", "%22");
		//sResult = java.net.URLEncoder.encode(sDecoded, m_sEnconding);
		return sResult;
	}
	
	protected String prepareQuery(String sInput) {
		String sQuery = new String(sInput);
		//insert space before and after round brackets
		sQuery = sQuery.replaceAll("\\(", " \\( ");
		sQuery = sQuery.replaceAll("\\)", " \\) ");
		//remove space before and after square brackets 
		sQuery = sQuery.replaceAll(" \\[", "\\[");
		sQuery = sQuery.replaceAll("\\[ ", "\\[");
		sQuery = sQuery.replaceAll(" \\]", "\\]");
		sQuery = sQuery.replaceAll("\\] ", "\\]");
		sQuery = sQuery.replaceAll("POLYGON", "POLYGON ");
		sQuery = sQuery.replaceAll("\\: ", "\\:");
		sQuery = sQuery.replaceAll(" \\: ", "\\:");

		sQuery = sQuery.replaceAll("AND", " AND ");
		sQuery = sQuery.trim().replaceAll(" +", " ");
		sQuery = convertRanges(sQuery);
		return sQuery;
	}

	protected String convertRanges(String sQuery) {
		return sQuery;
	}
	
	
	protected abstract String parseTimeFrame(String sQuery);
	protected abstract String parseFootPrint(String sQuery);
}
