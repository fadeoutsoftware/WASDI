/**
 * Created by Cristiano Nattero on 2019-12-20
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.abdera.i18n.templates.Template;
import org.json.JSONObject;

import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.QueryResultViewModel;

/**
 * @author c.nattero
 *
 */
public class QueryExecutorCREODIAS extends QueryExecutor {
	static {
		s_sClassName = "CREODIAS";
	}

	public QueryExecutorCREODIAS() {
		Utils.debugLog(s_sClassName);
		m_sProvider="s_sClassName";
		this.m_oQueryTranslator = new DiasQueryTranslatorCREODIAS();
		this.m_oResponseTranslator = new DiasResponseTranslatorCREODIAS();
	}


	@Override
	public int executeCount(String sQuery) {
		Utils.debugLog(s_sClassName + ".executeCount( " + sQuery + " )");
		if(null == sQuery) {
			throw new NullPointerException("QueryExecutorCREODIAS.getCountURL: sQuery is null");
		}
		int iResult = 0;
		try {
			String sUrl = getCountUrl(sQuery);
			String sResult = null;
			try {
				sResult = httpGetResults(sUrl, "count");
				if(!Utils.isNullOrEmpty(sResult)) {
					//parse response as json
					JSONObject oJson = new JSONObject(sResult);
					if(null!=oJson) {
						JSONObject oProperties = oJson.optJSONObject("properties");
						if(null!=oProperties) {
							//properties.totalResults
							iResult = oProperties.optInt("totalResults", -1);
						}
					}
				}
			} catch (Exception oE) {
				Utils.debugLog(s_sClassName + ".executeCount( " + sQuery + " ): " + oE.getMessage());
				iResult = -1;
			}
		} catch (Exception oEout) {
			Utils.debugLog("QueryExecutorCREODIAS.executeCount: " + oEout);
		}
		return iResult;
	}

	@Override
	public ArrayList<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery) throws IOException {
		Utils.debugLog("QueryExecutorCREODIAS.executeAndRetrieve( <oQuery> )");
		return executeAndRetrieve(oQuery,true);
	}

	@Override
	public ArrayList<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		Utils.debugLog("QueryExecutorCREODIAS.executeAndRetrieve( <oQuery>, " + bFullViewModel + " )");
		String sUrl = getSearchUrl(oQuery);
		
		String sResult = httpGetResults(sUrl, "count");

		return buildResultViewModel(sResult, bFullViewModel);
	}
	
	@Override
	protected ArrayList<QueryResultViewModel> buildResultViewModel(String sJson, boolean bFullViewModel){
		return null;
	}
	
	@Override
	protected ArrayList<QueryResultViewModel> buildResultLightViewModel(String sJson, boolean bFullViewModel){
		return null;
	}

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.QueryExecutor#getUrlPath()
	 */
	@Override
	protected String[] getUrlPath() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.QueryExecutor#getTemplate()
	 */
	@Override
	protected Template getTemplate() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.QueryExecutor#getCountUrl(java.lang.String)
	 */
	@Override
	protected String getCountUrl(String sQuery) {
		
		String sTempUrl = getUrl(sQuery);
		String sUrl = sTempUrl + "&maxRecords=1";

		
		return sUrl;
	}

	@Override
	protected String getSearchListUrl(PaginatedQuery oQuery) {
		return getSearchUrl(oQuery);
	}
	
	@Override
	protected String getSearchUrl(PaginatedQuery oQuery) {
		String sUrl = getUrl(oQuery.getQuery());
		sUrl += "&maxRecords=" + oQuery.getLimit();
		sUrl += "&sortParam=" + oQuery.getSortedBy(); //"startDate"
		sUrl += "&sortOrder=" + oQuery.getOrder(); //"descending"
		sUrl += "&status=all&dataset=ESA-DATASET";
		return sUrl;
	}
	
	private String getUrl(String sQuery) {
		//Utils.debugLog(s_sClassName + "getCountUrl");
		if(Utils.isNullOrEmpty(sQuery)) {
			Utils.debugLog(s_sClassName + ".getUrl: sQuery is null");
		}
		String sUrl = "https://finder.creodias.eu/resto/api/collections/";
		sUrl+=m_oQueryTranslator.translateAndEncode(sQuery);
		return sUrl;
	}

}
