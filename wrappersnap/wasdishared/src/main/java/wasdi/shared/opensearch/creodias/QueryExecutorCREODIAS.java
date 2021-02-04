/**
 * Created by Cristiano Nattero on 2019-12-20
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch.creodias;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.abdera.i18n.templates.Template;
import org.json.JSONObject;

import wasdi.shared.opensearch.PaginatedQuery;
import wasdi.shared.opensearch.QueryExecutor;
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
		m_sProvider=s_sClassName;
		this.m_oQueryTranslator = new DiasQueryTranslatorCREODIAS();
		this.m_oResponseTranslator = new DiasResponseTranslatorCREODIAS();
	}
	
	@Override
	public void setParserConfigPath(String sParserConfigPath) {
		super.setParserConfigPath(sParserConfigPath);
		this.m_oQueryTranslator.setParserConfigPath(this.m_sParserConfigPath);
	}

	@Override
	public void setAppconfigPath(String sAppconfigPath) {
		super.setAppconfigPath(sAppconfigPath);
		this.m_oQueryTranslator.setAppconfigPath(this.m_sAppConfigPath);
	}

	@Override
	public int executeCount(String sQuery) {
		//Utils.debugLog(s_sClassName + ".executeCount( " + sQuery + " )");
		if(null == sQuery) {
			throw new NullPointerException("QueryExecutorCREODIAS.getCountURL: sQuery is null");
		}
		int iResult = -1;
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
				Utils.debugLog(s_sClassName + ".executeCount( " + sQuery + " ): " + oE);
				iResult = -1;
			}
		} catch (Exception oEout) {
			Utils.debugLog("QueryExecutorCREODIAS.executeCount: " + oEout);
		}
		return iResult;
	}

	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery) throws IOException {
		//Utils.debugLog("QueryExecutorCREODIAS.executeAndRetrieve( <oQuery> )");
		return executeAndRetrieve(oQuery,true);
	}

	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		//Utils.debugLog("QueryExecutorCREODIAS.executeAndRetrieve( <oQuery>, " + bFullViewModel + " )");
		
	
//		String sUrl = "https://finder.creodias.eu/resto/api/collections/Sentinel1/search.json?startDate=2019-12-01T00:00:00Z&completionDate=2019-12-03T23:59:59Z&geometry=POLYGON((7.397874989401342+45.00475144371268,10.373746303074263+44.94785607558927,10.389830621260842+43.612039503172866,7.703504034412235+43.809704932512176,7.397874989401342+45.00475144371268))";
		String sUrl = getSearchUrl(oQuery);
		
		String sResult = httpGetResults(sUrl, "count");

		return buildResultViewModel(sResult, bFullViewModel);
	}
	
	@Override
	protected List<QueryResultViewModel> buildResultViewModel(String sJson, boolean bFullViewModel){
		return m_oResponseTranslator.translateBatch(sJson, bFullViewModel, m_sDownloadProtocol);
	}
	

	@Override
	protected String getSearchListUrl(PaginatedQuery oQuery) {
		return getSearchUrl(oQuery);
	}
	
	@Override
	protected String getSearchUrl(PaginatedQuery oQuery) {
		String sUrl = getUrl(oQuery.getQuery());
		sUrl += "&maxRecords=" + oQuery.getOriginalLimit();
		
		try {
			
			int iItemsPerPage = Integer.parseInt(oQuery.getOriginalLimit());
			int iActualOffset = Integer.parseInt(oQuery.getOffset());
			int iPage = (int) Math.floor( (double)iActualOffset / (double)iItemsPerPage );
			iPage++;
			
			if (iPage>1) {
				sUrl += "&page=" + iPage;
			}
		}
		catch (Exception oEx) {
			Utils.debugLog(s_sClassName + ".getSearchUrl: exception generating the page parameter  " + oEx.toString());
		}
		
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
		
		//faster, but the number is only an estimate:
		String sUrl = getUrl(sQuery) +  "&maxRecords=1";

		//accurate, but slower
		sUrl += "&exactCount=1";
		
		return sUrl;
	}
	
}
