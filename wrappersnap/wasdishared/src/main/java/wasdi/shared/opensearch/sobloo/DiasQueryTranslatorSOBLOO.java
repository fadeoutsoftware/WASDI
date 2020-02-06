/**
 * Created by Cristiano Nattero on 2020-02-03
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch.sobloo;

import org.json.JSONObject;

import com.google.common.base.Preconditions;

import wasdi.shared.opensearch.DiasQueryTranslator;
import wasdi.shared.opensearch.QueryTranslationParser;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;

/**
 * @author c.nattero
 *
 */
public class DiasQueryTranslatorSOBLOO extends DiasQueryTranslator {
	

	//TODO one class per mission, w/ 2 maps: WASDI to SOBLOO keys, and WASDI to SOBLOO values 
	
	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.DiasQueryTranslator#translate(java.lang.String)
	 */
	@Override
	protected String translate(String sQueryFromClient) {
		Preconditions.checkNotNull(sQueryFromClient, "DiasQueryTranslatorSOBLOO.translate: query is null");
		
		String sResult = null;
		try {
			JSONObject oAppConf = WasdiFileUtils.loadJsonFromFile(m_sAppConfigPath);
			JSONObject oParseConf = WasdiFileUtils.loadJsonFromFile(m_sParserConfigPath);
			String sQuery = this.prepareQuery(sQueryFromClient);
			//TODO parse footprint
			//TODO parse time frame

			for (Object oObject : oAppConf.optJSONArray("missions")) {
				JSONObject oJson = (JSONObject) oObject;
				String sValue = oJson.optString("indexvalue", null);
				if(sQuery.contains(sValue)) {
					//todo isolate relevant portion of query
					int iStart = sQuery.indexOf(sValue);
					int iEnd = sQuery.indexOf(')', iStart);
					String sQueryPart = sQuery.substring(iStart, iEnd);
					QueryTranslationParser oParser = new QueryTranslationParser(oParseConf.optJSONObject(sValue), oJson);
					sResult += oParser.parse(sQueryPart);
				}
			}
			
		} catch (Exception oE) {
			Utils.debugLog("DiasQueryTranslatorSOBLOO.translate( " + sQueryFromClient + " ): " + oE);
		}

		return sResult;
	}

}
