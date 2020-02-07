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
		Preconditions.checkNotNull(m_sAppConfigPath, "DiasQueryTranslatorSOBLOO.translate: app config path is null");
		Preconditions.checkNotNull(m_sParserConfigPath, "DiasQueryTranslatorSOBLOO.translate: parser config is null");
		
		String sResult = null;
		try {
			JSONObject oAppConf = WasdiFileUtils.loadJsonFromFile(m_sAppConfigPath);
			JSONObject oParseConf = WasdiFileUtils.loadJsonFromFile(m_sParserConfigPath);
			String sQuery = this.prepareQuery(sQueryFromClient);
			
			sResult = "";
			//TODO parse footprint
			sResult += parseFootprint(sQuery);
			//TODO parse time frame
			
			
			for (Object oObject : oAppConf.optJSONArray("missions")) {
				JSONObject oJson = (JSONObject) oObject;
				String sName = oJson.optString("indexname", null);
				String sValue = oJson.optString("indexvalue", null);
				String sToken = sName + ':' + sValue; 
				if(sQuery.contains(sToken)) {
					//todo isolate relevant portion of query
					int iStart = Math.max(0, sQuery.indexOf(sToken));
					int iEnd = sQuery.indexOf(')', iStart);
					if(0>iEnd) {
						iEnd = sQuery.length();
					}
					String sQueryPart = sQuery.substring(iStart, iEnd).trim();
					QueryTranslationParser oParser = new QueryTranslationParser(oParseConf.optJSONObject(sValue), oJson);
					String sLocalPart = oParser.parse(sQueryPart); 
					sResult += sLocalPart;
				}
			}
			
		} catch (Exception oE) {
			Utils.debugLog("DiasQueryTranslatorSOBLOO.translate( " + sQueryFromClient + " ): " + oE);
		}

		return sResult;
	}

	private String parseFootprint(String sQuery) {
		String sResult = "";
		if(sQuery.contains("footprint")) {
			String sIntro = "( footprint:\"intersects ( POLYGON ( ( ";
			int iStart = sQuery.indexOf(sIntro);
			if(iStart >= 0) {
				iStart += sIntro.length();
			}
			int iEnd = sQuery.indexOf(')', iStart);
			if(0>iEnd) {
				iEnd = sQuery.length();
			}
			
			String sCoordinates = sQuery.substring(iStart, iEnd).replace(' ', ',');
			sResult = "f=gintersect=" + sCoordinates;
		}
		return sResult;
	}
	
//	private String parseTimeFrame(String sQuery) {
//		
//	}

}
