/**
 * Created by Cristiano Nattero on 2020-02-05
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.base.Preconditions;

import wasdi.shared.utils.Utils;

/**
 * @author c.nattero
 *
 */
public class QueryTranslationParser {
	private JSONObject m_oKeysTranslation = null;
	private JSONObject m_oValuesTranslation = null;
	
	private JSONObject m_oAppConf = null;
	private JSONArray m_aoFilters = null;
	
	public QueryTranslationParser(JSONObject oParseConf, JSONObject oAppConf) {
		//assumption: the configurations only include the json object relative to the mission being considered, e.g. Sentinel-1
		setParseconf(oParseConf);
		setAppConf(oAppConf);
	}
	
	private void setParseconf(JSONObject oParseConf) {
		Preconditions.checkNotNull(oParseConf, "QueryTranslationParser.setParseconf: parse configuration is null");
		Preconditions.checkNotNull(oParseConf.optJSONObject("keys"));
		Preconditions.checkNotNull(oParseConf.optJSONObject("values"));
		
		m_oKeysTranslation = oParseConf.optJSONObject("keys");
		m_oValuesTranslation = oParseConf.optJSONObject("values");
	}
	
	private void setAppConf(JSONObject oAppConf) {
		Preconditions.checkNotNull(oAppConf, "QueryTranslationParser.setAppConf: app configuration is null");
		Preconditions.checkNotNull(oAppConf.optString("name", null));
		Preconditions.checkNotNull(oAppConf.optString("indexname", null));
		Preconditions.checkNotNull(oAppConf.optString("indexvalue", null));
		Preconditions.checkNotNull(oAppConf.optJSONArray("filters"));
		
		this.m_oAppConf = oAppConf;
		this.m_aoFilters = oAppConf.optJSONArray("filters");
	}
	
	public String parse(String sQuery) {
		//platformname:Sentinel-1 AND filename:S1A_* AND producttype:GRD AND polarisationmode:HH AND sensoroperationalmode:IW AND relativeorbitnumber:23 AND swathidentifier:whatIsAGoodValueForASwath
		
		String sResult = "";
		try {
			String[] asKeysAndValues = sQuery.split(" AND ");
			for (String sQueryItem : asKeysAndValues) {
				
				String[] asWasdiKeyValuePair = sQueryItem.split(":");
				
				//translate key
				String sWasdiQueryKey = asWasdiKeyValuePair[0];
				String sProviderKey = m_oKeysTranslation.optString(sWasdiQueryKey, null);
				if(null == sProviderKey) {
					//todo log an error
					continue;
				}
				
				String sProviderValue = null; 
						
				//verify value format before translating
				Iterator<Object> oIterator = m_aoFilters.iterator();
				while( oIterator.hasNext()) {
					JSONObject oFilter = (JSONObject) oIterator.next();
					if(sWasdiQueryKey.equals(oFilter.optString("indexname", null)) ) {
						String sRegex = oFilter.optString("regex", null);
						if(".*".equals(sRegex)) {
							String sIndexValues = oFilter.optString("indexvalues", null);
							if(!"".equals(sIndexValues)) {
								JSONObject oValues = m_oValuesTranslation.optJSONObject(sWasdiQueryKey);
								if(null==oValues) {
									Utils.log("ERROR", "QueryTranslationParser.parse( " + sQuery + " ): could not translate value for key: " + sWasdiQueryKey);
									break;
								}
								 //sProviderValue = 
							} else {
								//then it can be:
								//a number
								//an interval -> hint contains "TO"
								//todo check index min and max 
							}
						}
					}
				}
				if(null == sProviderValue) {
					//todo log an error
					continue;
				}

				
				//sWasdiQueryValue = asWasdiKeyValuePair[1];
				

//				sProviderKey = m_oParseConf.optString(key)
//				sResult += "&f=" + 
//				for( )
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		sResult = "f=acquisition.missionName:eq:Sentinel-1A";
		return sResult;
		
		
	}
}
