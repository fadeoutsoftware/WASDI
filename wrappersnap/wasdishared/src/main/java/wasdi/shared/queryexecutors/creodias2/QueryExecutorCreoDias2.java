package wasdi.shared.queryexecutors.creodias2;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.creodias.QueryExecutorCREODIAS;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;


public class QueryExecutorCreoDias2 extends QueryExecutorCREODIAS {
	
	
	public QueryExecutorCreoDias2() {
		this.m_sProvider = "CREODIAS2";
		this.m_oQueryTranslator = new QueryTranslatorCreoDias2();
		this.m_oResponseTranslator = new ResponseTranslatorCreoDias2();
		this.m_asSupportedPlatforms.addAll( Arrays.asList(
						Platforms.SENTINEL1, 
						Platforms.SENTINEL2, 
						Platforms.SENTINEL3, 
						Platforms.SENTINEL5P, 
						Platforms.SENTINEL6,
						Platforms.ENVISAT,
						Platforms.LANDSAT5,
						Platforms.LANDSAT7,
						Platforms.LANDSAT8));
	}

	@Override
	public int executeCount(String sQuery) {
		int iCount = -1;

		try {
			
			QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(sQuery);
			
			if (!m_asSupportedPlatforms.contains(oQueryViewModel.platformName)) {
				WasdiLog.debugLog("QueryExecutorCreoDias2.executeCount. Platform not supported by the data provider for query: " + sQuery);
				return -1;
			}
			
			String sOutputQuery = m_oQueryTranslator.getCountUrl(sQuery);
			
			HttpCallResponse oHttpResponse = HttpUtils.httpGet(sOutputQuery, null, null);  
			
			String sResult = oHttpResponse.getResponseBody();
						
			if (oHttpResponse.getResponseCode()>=200 && oHttpResponse.getResponseCode()<=299) {
				iCount = m_oResponseTranslator.getCountResult(sResult);
			}
			else {
				WasdiLog.debugLog("QueryExecutorCreoDias2.executeCount. Error when trying to retrieve the number of results. Response code: " + oHttpResponse.getResponseCode());
				return -1;
			}
			
			
		} catch (Exception oException) {
			WasdiLog.debugLog("QueryExecutorCreoDias2.executeCount. Error when trying to retrieve the number of results " + oException.getMessage());
			return -1;
		}
		
		return iCount;
	}

	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		
		try {
	
			QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(oQuery.getQuery());
	
			if (!m_asSupportedPlatforms.contains(oQueryViewModel.platformName)) {
				WasdiLog.debugLog("QueryExecutorCreoDias2.executeAndRetrieve. Platform not supported by the data provider for query: " + oQuery.getQuery());
				return Collections.emptyList();
			}
			
			String sCreodiasQuery = m_oQueryTranslator.getSearchUrl(oQuery);
			
			// Call standard http get API
			HttpCallResponse oHttpResponse = HttpUtils.httpGet(sCreodiasQuery, null, null);  
						
			if (oHttpResponse.getResponseCode()< 200 || oHttpResponse.getResponseCode() > 299) {
				WasdiLog.debugLog("QueryExecutorCreoDias2.executeAndRetrieve. Error when trying to retrieve the results on CreoDias. Response code: " + oHttpResponse.getResponseCode());
				return null;
			}
		
			String sCreodiasResult = oHttpResponse.getResponseBody();

			if (Utils.isNullOrEmpty(sCreodiasResult)) {
				WasdiLog.debugLog("QueryExecutorCreoDias2.executeAndRetrieve. The data provider returned an empty string for query: " + sCreodiasQuery);
				return Collections.emptyList();
			}
			
			// TODO: what's the bFullViewModel
			List<QueryResultViewModel> aoRes = m_oResponseTranslator.translateBatch(sCreodiasResult, bFullViewModel);
			
			return aoRes;
		
		} catch (Exception oEx) {
			WasdiLog.debugLog("QueryExecutorCreoDias2.executeAndRetrieve. Error when trying to retrieve the Creodias results for query. " + oEx.getMessage());
		}
		
		return Collections.emptyList();
	}

}
