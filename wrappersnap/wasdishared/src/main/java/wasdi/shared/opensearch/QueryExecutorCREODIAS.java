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
		//String sUrl = getUrl(sQuery)
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
		//todo replace value in string: maxRecords=n -> maxRecords=1
		String sTempUrl = getUrl(sQuery);
		String sMaxRecords = "maxRecords";
		String sUrl = sTempUrl;
		int iIndexOfEquals = sTempUrl.indexOf(sMaxRecords) + sMaxRecords.length() + 1;
		if( iIndexOfEquals > 0 ) {
			sUrl = sTempUrl.substring(0, iIndexOfEquals + 1) + "1";
			int iEnd = sTempUrl.indexOf('&', iIndexOfEquals);
			if(iEnd >= 0) {
				sUrl += sTempUrl.substring(iEnd);
			}
		}
		return sUrl;
	}

	private String getUrl(String sQuery) {
		//Utils.debugLog(s_sClassName + "getCountUrl");
		if(Utils.isNullOrEmpty(sQuery)) {
			Utils.debugLog(s_sClassName + ".getUrl: sQuery is null");
		}
		String sUrl = "https://finder.creodias.eu/resto/api/collections/";
		sUrl+=m_oQueryTranslator.translateAndEncode(sQuery);
		sUrl+="%22";
		return sUrl;
	}

}
