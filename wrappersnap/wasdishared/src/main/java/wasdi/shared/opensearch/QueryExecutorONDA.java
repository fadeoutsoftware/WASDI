/**
 * Created by Cristiano Nattero and Alessio Corrado on 2018-11-27
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.abdera.i18n.templates.Template;
import org.json.JSONArray;
import org.json.JSONObject;

import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.QueryResultViewModel;

/**
 * @author c.nattero
 *
 */
public class QueryExecutorONDA extends QueryExecutor {

	static {
		s_sClassName = "QueryExecutorONDA";
	}
	
	public QueryExecutorONDA() {
		Utils.debugLog(s_sClassName);
		m_sProvider="ONDA";
		this.m_oQueryTranslator = new DiasQueryTranslatorONDA();
		this.m_oResponseTranslator = new DiasResponseTranslatorONDA();
	}

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.QueryExecutor#getUrlPath()
	 */
	@Override
	protected String[] getUrlPath() {
		return null;
	}

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.QueryExecutor#getTemplate()
	 */
	@Override
	protected Template getTemplate() {
		return null;
	}

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.QueryExecutor#getCountUrl(java.lang.String)
	 */
	@Override
	protected String getCountUrl(String sQuery) {
		//Utils.debugLog(s_sClassName + "getCountUrl");
		if(Utils.isNullOrEmpty(sQuery)) {
			Utils.debugLog(s_sClassName + ".getCountUrl: sQuery is null");
		}
		String sUrl = "https://catalogue.onda-dias.eu/dias-catalogue/Products/$count?$search=%22";
		sUrl+=m_oQueryTranslator.translateAndEncode(sQuery);
		sUrl+="%22";
		return sUrl;
	}

	@Override
	protected String getSearchUrl(PaginatedQuery oQuery){
		//Utils.debugLog(s_sClassName + ".BuildUrl( " + oQuery + " )");
		if(null==oQuery) {
			Utils.debugLog(s_sClassName + ".buildUrl: oQuery is null");
		}
		String sUrl = buildUrlPrefix(oQuery);		

		String sMetadata = "$expand=Metadata";
		if(!Utils.isNullOrEmpty(sMetadata)) {
			sUrl += "&" + sMetadata;
		}
		sUrl = buildUrlSuffix(oQuery, sUrl);
		return sUrl;
	}

	@Override
	protected String getSearchListUrl(PaginatedQuery oQuery) {
		//Utils.debugLog(s_sClassName + ".buildUrlForList( " + oQuery + " )");
		if(null==oQuery) {
			Utils.debugLog(s_sClassName + ".buildUrlForList: oQuery is null");
		}
		String sUrl = buildUrlPrefix(oQuery);

		sUrl += "&$expand=Metadata&$select=id,name,creationDate,beginPosition,offline,size,pseudopath,footprint";

		sUrl = buildUrlSuffix(oQuery, sUrl);
		return sUrl;
	}

	private String buildUrlPrefix(PaginatedQuery oQuery) {
		//Utils.debugLog(s_sClassName + ".BuildBaseUrl( " + oQuery + " )");
		if(null==oQuery) {
			throw new NullPointerException("QueryExecutorONDA.buildBaseUrl: oQuery is null");
		}
		String sUrl = "https://catalogue.onda-dias.eu/dias-catalogue/Products?$search=%22";
		sUrl+=m_oQueryTranslator.translateAndEncode(oQuery.getQuery()) + "%22";
		return sUrl;
	}

	protected String buildUrlSuffix(PaginatedQuery oQuery, String sInUrl) {
		//Utils.debugLog(s_sClassName + ".BuildUrlSuffix( " + oQuery + ", " + sInUrl + " )");
		String sUrl = sInUrl;
		sUrl+="&$top=" + oQuery.getLimit() + "&$skip="+ oQuery.getOffset();

		String sFormat = "$format=json";
		if(!Utils.isNullOrEmpty(sFormat)) {
			sUrl += "&" + sFormat;
		}

		String sOrderBy = oQuery.getSortedBy();
		//XXX do not use hardcoded values
		sOrderBy = "$orderby=creationDate";
		if(!Utils.isNullOrEmpty(sOrderBy)) {
			sUrl += "&" + sOrderBy;
		}
		return sUrl;
	}


	@Override
	public int executeCount(String sQuery) throws IOException {
		Utils.debugLog(s_sClassName + ".executeCount( " + sQuery + " )");
		String sUrl = getCountUrl(sQuery);
		int iResult = 0;
		String sResult = "0";
		try {
			sResult = httpGetResults(sUrl, "count");
			if(null!=sResult) {
				iResult = Integer.parseInt(sResult);
			} else {
				iResult = -1;
			}
		} catch (Exception oE) {
			Utils.debugLog(s_sClassName + ".executeCount( " + sQuery + " ): " + oE.getMessage());
			iResult = -1;
		}
		return iResult;
	}


	@Override
	public ArrayList<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		Utils.debugLog(s_sClassName + ".executeAndRetrieve(" + oQuery + ", " + bFullViewModel + ")");
		String sResult = null;
		String sUrl = null;
		try {
			if(bFullViewModel) {
				sUrl = getSearchUrl(oQuery);
			} else {
				sUrl = getSearchListUrl(oQuery);
			}
			sResult = httpGetResults(sUrl, "search");		
			ArrayList<QueryResultViewModel> aoResult = null;
			if(!Utils.isNullOrEmpty(sResult)) {
				aoResult = buildResultViewModel(sResult, bFullViewModel);
				if(null==aoResult) {
					throw new NullPointerException(s_sClassName + ".executeAndRetrieve: aoResult is null"); 
				}
			} else {
				Utils.debugLog(s_sClassName + ".executeAndRetrieve(2 args): could not fetch results for url: " + sUrl);
			}
			return aoResult;
		} catch (Exception oE) {
			Utils.debugLog(s_sClassName + ".executeAndRetrieve(2 args, with sUrl=" + sUrl + "): " + oE);
		}
		return null;
	}


	@Override
	public ArrayList<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery_AssumeFullviewModel) throws IOException {
		Utils.debugLog(s_sClassName + ".executeAndRetrieve("+ oQuery_AssumeFullviewModel + ")");
		return executeAndRetrieve(oQuery_AssumeFullviewModel, true);

		////use this to test with just 3 results
		////sUrl = "https://catalogue.onda-dias.eu/dias-catalogue/Products?$search=%22(%20(%20name:S1*%20AND%20name:S1A_*%20AND%20name:*SLC*%20AND%20name:*%20AND%20sensorOperationalMode:SM%20)%20)%20AND%20(%20(%20beginPosition:[2018-12-02T00:00:00.000Z%20TO%202018-12-02T23:59:59.999Z]%20AND%20endPosition:[2018-12-02T00:00:00.000Z%20TO%202018-12-02T23:59:59.999Z]%20)%20)%22&$orderby=creationDate%20desc&$top=15&$skip=0&$format=json";
	}

	protected ArrayList<QueryResultViewModel> buildResultViewModel(String sJson, boolean bFullViewModel){
		Utils.debugLog(s_sClassName + ".buildResultViewModel( sJson, " + bFullViewModel + " )");
		if(null==sJson ) {
			Utils.debugLog("QueryExecutor.buildResultLightViewModel: passed a null string");
			throw new NullPointerException("QueryExecutor.buildResultLightViewModel: passed a null string");
		}
		ArrayList<QueryResultViewModel> aoResult = new ArrayList<>();
		try {
			JSONObject oJsonOndaResponse = new JSONObject(sJson);
			JSONArray aoJsonArray = oJsonOndaResponse.optJSONArray("value");
			if(null!=aoJsonArray) {
				if(aoJsonArray.length()<=0) {
					Utils.debugLog(s_sClassName + ".buildResultViewModel: JSON string contains an empty array");
				} else {
					for (Object oObject : aoJsonArray) {
						if(null!=oObject) {
							JSONObject oOndaFullEntry = new JSONObject("{}");
							JSONObject oOndaEntry = (JSONObject)(oObject);
							if(!bFullViewModel) {
								String sQuicklook = oOndaEntry.optString("quicklook");
								if(!Utils.isNullOrEmpty(sQuicklook)) {
									oOndaEntry.put("quicklook", (String)null);
								}
							}
							String sEntryKey = "entry";
							oOndaFullEntry.put(sEntryKey, oOndaEntry);
							QueryResultViewModel oRes = m_oResponseTranslator.translate(oOndaFullEntry, m_sDownloadProtocol);
							aoResult.add(oRes);
						}
					}
				}
			}
		} catch (Exception oE) {
			Utils.debugLog(s_sClassName + ".buildResultViewModel: " + oE);
		}
		if(aoResult.isEmpty()) {
			Utils.debugLog(s_sClassName + ".buildResultViewModel: no results");
		}
		return aoResult;
	}

}

