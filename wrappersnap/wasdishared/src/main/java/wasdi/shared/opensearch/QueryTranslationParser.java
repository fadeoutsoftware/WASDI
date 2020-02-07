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
				
				//todo translate mission/product first of all
				String sName = m_oAppConf.optString("name");
				String sIndexname = m_oAppConf.optString("indexname", null);
				String sIndexvalue = m_oAppConf.optString("indexvalue", null);
				if(sQueryItem.equals(sIndexname+':'+sIndexvalue)) {
					//todo translate key and value
					String sProviderKey = this.m_oKeysTranslation.optString(sIndexname);
					JSONObject oIndexJson = this.m_oValuesTranslation.optJSONObject("indexvalue");
					if(null == oIndexJson) {
						throw new NullPointerException("QueryTranslationParser.parse( " + sQuery + " ): cannot map collection name, aborting");
					} 
					String sProviderValue = oIndexJson.optString(sIndexvalue);
					sResult += "&f=" + sProviderKey + ":eq:" + sProviderValue;
					continue;
				}
				
				//now proceed parsing the rest
				
				
				String[] asWasdiKeyValuePair = sQueryItem.split(":");
				
				//translate key
				String sWasdiQueryKey = asWasdiKeyValuePair[0];
				String sWasdiQueryValue = asWasdiKeyValuePair[1];
				String sProviderKey = m_oKeysTranslation.optString(sWasdiQueryKey, null);
				if(null == sProviderKey) {
					//todo log an error
					continue;
				}
				
				String sProviderValue = null;
				
				
				
				
				//translate filter
				Iterator<Object> oIterator = m_aoFilters.iterator();
				while( oIterator.hasNext()) {
					JSONObject oFilter = (JSONObject) oIterator.next();
					sIndexname = oFilter.optString("indexname", null);
					if(!sWasdiQueryKey.equals(sIndexname) ) {
						continue;
					}
					
					String sIndexlabel = oFilter.optString("indexlabel", null);
					if(null == sIndexlabel) {
						Utils.log("ERROR", "QueryTranslationParser.parse( "+ sQuery + " ): indexlabel is null, configuration does not look good");
						continue;
					}
					//this one might absent
					String sIndexhint = oFilter.optString("indexhint", null);
					String sIndexvalues = oFilter.optString("indexvalues", null);
					if(null == sIndexvalues) {
						Utils.log("ERROR", "QueryTranslationParser.parse( "+ sQuery + " ): indexvalues is null, configuration does not look good");
						continue;
					}
					String sRegex = oFilter.optString("regex", null);
					if(null == sRegex) {
						Utils.log("ERROR", "QueryTranslationParser.parse( "+ sQuery + " ): regex is null, configuration does not look good");
						continue;
					}
					//these might be absent
					int iIndexmin = oFilter.optInt("indexmin", -1);
					int iIndexmax = oFilter.optInt("indexmax", -1);

					
					if(".*".equals(sRegex) && !"".equals(sIndexvalues)) {
						JSONObject oValues = m_oValuesTranslation.optJSONObject(sWasdiQueryKey);
						if(null==oValues) {
							Utils.log("ERROR", "QueryTranslationParser.parse( " + sQuery + " ): could not translate value for key: " + sWasdiQueryKey);
							break;
						}
						
						sProviderValue = oValues.optString(sWasdiQueryValue, null);
						sResult += "&f=" + sProviderKey + ":eq:" + sProviderValue;
						
					} else if( "".equals(sIndexvalues) ){
						//then it can be:
						//a number
						//an interval -> hint contains "TO"
						//todo check index min and max 
						
						//todo handle this case
					}
				}
				
				if(null == sProviderValue) {
					//todo log an error
					continue;
				}

				
				//
				

				//todo finalize

			}
		} catch (Exception e) {
			Utils.log("ERROR", "QueryTranslationParser( " + sQuery + " ): could not complete translation, aborting");
		}
		
		return sResult;
		
		
	}
}
