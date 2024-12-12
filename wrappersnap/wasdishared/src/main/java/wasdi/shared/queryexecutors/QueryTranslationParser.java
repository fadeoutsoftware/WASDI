/**
 * Created by Cristiano Nattero on 2020-02-05
 * 
 * Fadeout software
 *
 */
package wasdi.shared.queryexecutors;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.base.Preconditions;

import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

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
		// assumption: the configurations only include the json object relative to the
		// mission being considered, e.g. Sentinel-1
		setParseconf(oParseConf);
		setAppConf(oAppConf);
	}

	public String parse(String sQuery) {
		// platformname:Sentinel-1 AND filename:S1A_* AND producttype:GRD AND
		// polarisationmode:HH AND sensoroperationalmode:IW AND relativeorbitnumber:23
		// AND swathidentifier:whatIsAGoodValueForASwath

		String sResult = "";
		try {
			String sLocalFilter = null;
			String[] asKeysAndValues = sQuery.split(" AND ");
			for (String sQueryItem : asKeysAndValues) {

				// todo translate mission/product first of all
				String sPlatformIndexname = m_oAppConf.optString("indexname", null);
				String sPlatformIndexvalue = m_oAppConf.optString("indexvalue", null);
				if (sQueryItem.equals(sPlatformIndexname + ':' + sPlatformIndexvalue)) {
					// todo translate key and value
					String sProviderKey = this.m_oKeysTranslation.optString(sPlatformIndexname);
					JSONObject oIndexJson = this.m_oValuesTranslation.optJSONObject("indexvalue");
					if (null == oIndexJson) {
						throw new NullPointerException(
								"QueryTranslationParser.parse( " + sQuery + " ): cannot map collection name, aborting");
					}
					String sProviderValue = oIndexJson.optString(sPlatformIndexvalue);
					sResult += sProviderKey + sProviderValue;
					continue;
				}

				// now proceed parsing the rest

				String[] asWasdiKeyValuePair = sQueryItem.split(":");

				// translate key
				String sWasdiQueryKey = asWasdiKeyValuePair[0];
				String sWasdiQueryValue = asWasdiKeyValuePair[1];
				String sProviderKey = m_oKeysTranslation.optString(sWasdiQueryKey, null);
				if (null == sProviderKey) {
					WasdiLog.warnLog("QueryTranslationParser.parse: cannot translate key: " + sWasdiQueryKey + ", skipping");
					// skip this filter if its key cannot be tranlsated
					continue;
				}

				String sProviderValue = null;

				// translate filter
				Iterator<Object> oIterator = m_aoFilters.iterator();
				while (oIterator.hasNext()) {
					JSONObject oFilter = (JSONObject) oIterator.next();
					String sIndexname = oFilter.optString("indexname", null);
					if (!sWasdiQueryKey.equals(sIndexname)) {
						continue;
					}

					String sIndexlabel = oFilter.optString("indexlabel", null);
					if (null == sIndexlabel) {
						WasdiLog.warnLog("QueryTranslationParser.parse: indexlabel for key " + sWasdiQueryKey + " is null, configuration does not look good");
					}

					String sIndexvalues = oFilter.optString("indexvalues", null);
					if (null == sIndexvalues) {
						WasdiLog.errorLog("QueryTranslationParser.parse: indexvalues for key " + sWasdiQueryKey + " is null, configuration does not look good");
						continue;
					}
					String sRegex = oFilter.optString("regex", null);
					if (null == sRegex) {
						WasdiLog.errorLog("QueryTranslationParser.parse( " + sQuery+ " ): regex is null, configuration does not look good");
						continue;
					}

					if (
							(sPlatformIndexvalue.equals("Sentinel-1") && (
									(sIndexname.equals("swathidentifier")) || (sIndexname.equals("relativeorbitnumber"))) ) ||
							(sPlatformIndexvalue.equals("Sentinel-2") && sIndexname.equals("cloudcoverpercentage")) ||
							(sPlatformIndexvalue.equals("Sentinel-3") && sIndexname.equals("relativeorbitstart")) ||
							(sPlatformIndexvalue.equals("Proba-V") && (
									sIndexname.equals("cloudcoverpercentage") || sIndexname.equals("snowcoverpercentage") || sIndexname.equals("productref") ||
									sIndexname.equals("cameraId") || sIndexname.equals("ProductID") || sIndexname.equals("Year")
									) ||
									(sPlatformIndexvalue.equals("Landsat-*") && sIndexname.equals("cloudCoverPercentage"))
									)
							) {
						sProviderValue = sWasdiQueryValue;

					} else if (".*".equals(sRegex) && !"".equals(sIndexvalues)) {
						// normal key:value
						JSONObject oValues = m_oValuesTranslation.optJSONObject(sWasdiQueryKey);
						if (null == oValues) {
							WasdiLog.errorLog("QueryTranslationParser.parse( " + sQuery + " ): could not translate value for key: " + sWasdiQueryKey);
							break;
						}

						sProviderValue = oValues.optString(sWasdiQueryValue, null);

					} 
					else {
						// then it can be:
						// a number
						// an interval -> hint contains "TO"
						// todo check index min and max

						// todo handle this case
					}

					// add filter to the query
					sLocalFilter = sProviderKey + sProviderValue;
					if (!Utils.isNullOrEmpty(sLocalFilter)) {
						sResult += sLocalFilter;
					}
				}

				if (null == sProviderValue) {
					// todo log an error
					continue;
				}

				//

				// todo finalize

			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("QueryTranslationParser( " + sQuery + " ): could not complete translation, aborting", oEx);
		}

		return sResult;

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
}
