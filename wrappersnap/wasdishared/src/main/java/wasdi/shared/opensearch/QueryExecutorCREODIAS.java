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
	public int executeCount(String sQuery) throws IOException {
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
				//todo parse json response: 
				// properties.totalResults
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
		return getUrl(sQuery);
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
