/**
 * Created by Cristiano Nattero on 2020-01-21
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch;

import java.io.IOException;
import java.util.List;

import org.apache.abdera.i18n.templates.Template;
import org.json.JSONObject;

import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.QueryResultViewModel;

import com.google.common.base.Preconditions;

/**
 * @author c.nattero
 *
 */
public class QueryExecutorSOBLOO extends QueryExecutor {
	
	protected String s_sBaseUrl = "https://sobloo.eu/api/v1/services/search?";

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.QueryExecutor#getUrlPath()
	 */
	@Override
	protected String[] getUrlPath() {
		// TODO Auto-generated method stub
		return null;
	}

//	/* (non-Javadoc)
//	 * @see wasdi.shared.opensearch.QueryExecutor#buildUrl(wasdi.shared.opensearch.PaginatedQuery)
//	 */
//	@Override
//	protected String buildUrl(PaginatedQuery oQuery) {
//		// TODO generate url from oQuery
//		return "https://sobloo.eu/api/v1/services/search?f=acquisition.missionName:eq:Sentinel-1A";
//	}

//	/* (non-Javadoc)
//	 * @see wasdi.shared.opensearch.QueryExecutor#executeCount(java.lang.String)
//	 */
//	@Override
//	public int executeCount(String sQuery) throws IOException {
//		// TODO Auto-generated method stub
//		return super.executeCount(sQuery);
//	}
	
	@Override
	protected String extractNumberOfResults(String sResponse) {
		Preconditions.checkNotNull(sResponse, "QueryExecutorSOBLOO.extractNumberOfResults: sResponse is null");
		String sResult = null;
		try {
			JSONObject oJson = new JSONObject(sResponse);
			sResult = "" + oJson.optInt("totalnb", -1);
		} catch (Exception oE) {
			Utils.debugLog("QueryExecutorSOBLOO.extractNumberOfResults( " + sResponse + " ): " + oE);
		}
		return sResult;
	}

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.QueryExecutor#executeAndRetrieve(wasdi.shared.opensearch.PaginatedQuery, boolean)
	 */
	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {Utils.debugLog(s_sClassName + ".executeAndRetrieve(" + oQuery + ", " + bFullViewModel + ")");
	String sResult = null;
	String sUrl = null;
	try {
		if(bFullViewModel) {
			sUrl = getSearchUrl(oQuery);
		} else {
			sUrl = getSearchListUrl(oQuery);
		}
		sResult = httpGetResults(sUrl, "search");		
		List<QueryResultViewModel> aoResult = null;
		if(!Utils.isNullOrEmpty(sResult)) {
			//todo build result view model
			//aoResult = buildResultViewModel(sResult, bFullViewModel);
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


	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.QueryExecutor#executeAndRetrieve(wasdi.shared.opensearch.PaginatedQuery)
	 */
	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery) throws IOException {
		// TODO Auto-generated method stub
		return super.executeAndRetrieve(oQuery);
	}

	@Override
	protected String getSearchUrl(PaginatedQuery oQuery){
		//todo implement
		return sMockUrlGen();
	}
	
	@Override
	protected String getSearchListUrl(PaginatedQuery oQuery) {
		//TODO implement
		return sMockUrlGen();
	}
	

	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.QueryExecutor#getCountUrl(java.lang.String)
	 */
	@Override
	protected String getCountUrl(String sQuery) {
		Preconditions.checkNotNull(sQuery, "QueryExecutorSOBLOO.getCountUrl: sQuery is null");
		
		return s_sBaseUrl + "f=acquisition.missionName:eq:Sentinel-1A";
	}

	
	private String sMockUrlGen() {
		return s_sBaseUrl + "f=acquisition.missionName:eq:Sentinel-1A";
	}

	
	
	@Override
	protected List<QueryResultViewModel> buildResultViewModel(String sJson, boolean bFullViewModel){
		Utils.debugLog("QueryExecutorSOBLOO.buildResultViewModel( sJson, " + bFullViewModel + " )");
		if(null==sJson ) {
			Utils.debugLog(s_sClassName + ".buildResultLightViewModel: passed a null string");
			throw new NullPointerException(s_sClassName + ".buildResultLightViewModel: passed a null string");
		}
		
		List<QueryResultViewModel> aoResult = m_oResponseTranslator.translateBatch(sJson, bFullViewModel, m_sDownloadProtocol);
		
		if(null == aoResult || aoResult.isEmpty()) {
			Utils.debugLog(s_sClassName + ".buildResultViewModel: no results");
		}
		return aoResult;
	}
		
	
	/* (non-Javadoc)
	 * @see wasdi.shared.opensearch.QueryExecutor#getTemplate()
	 */
	@Override
	protected Template getTemplate() {
		// TODO Auto-generated method stub
		return null;
	}

	
	public static void main(String[] args) {
		try {
			QueryExecutorSOBLOO oQE = new QueryExecutorSOBLOO();
			String sQuery = "( footprint:\"intersects(POLYGON((7.955097431033318 38.58252615935333,7.955097431033318 41.50857729743935,11.031307820285987 41.50857729743935,11.031307820285987 38.58252615935333,7.955097431033318 38.58252615935333)))\" ) AND ( beginPosition:[2020-01-14T00:00:00.000Z TO 2020-01-21T23:59:59.999Z] AND endPosition:[2020-01-14T00:00:00.000Z TO 2020-01-21T23:59:59.999Z] ) AND   (platformname:Sentinel-1 AND producttype:GRD) OR (platformname:Sentinel-2 AND producttype:S2MSI1C)";
			int iOffset = 0;
			int iLimit = 10;
			PaginatedQuery oQuery = new PaginatedQuery(sQuery, ""+iOffset, ""+iLimit, null, null);
			int iCount = oQE.executeCount(oQuery.getQuery());
			Utils.debugLog(iCount);
			
		} catch(Exception oE) {
			oE.printStackTrace();
		}
		
		
		/*
	http://217.182.93.57//wasdiwebserver/rest/search/query?sQuery=(%20footprint:%22intersects(POLYGON((7.955097431033318%2038.58252615935333,7.955097431033318%2041.50857729743935,11.031307820285987%2041.50857729743935,11.031307820285987%2038.58252615935333,7.955097431033318%2038.58252615935333)))%22%20)%20AND%20(%20beginPosition:%5B2020-01-14T00:00:00.000Z%20TO%202020-01-21T23:59:59.999Z%5D%20AND%20endPosition:%5B2020-01-14T00:00:00.000Z%20TO%202020-01-21T23:59:59.999Z%5D%20)%20AND%20%20%20(platformname:Sentinel-1%20AND%20producttype:GRD)%20OR%20(platformname:Sentinel-2%20AND%20producttype:S2MSI1C)&offset=0&limit=10&providers=SENTINEL		

		 */
	}
}