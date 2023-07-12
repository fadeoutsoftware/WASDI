package wasdi.shared.queryexecutors.creodias2;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

import wasdi.shared.config.WasdiConfig;
import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.queryexecutors.http.QueryExecutorHttpGet;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;

public class QueryExecutorCreoDias2 extends QueryExecutorHttpGet {
	
	
	public QueryExecutorCreoDias2() {
		this.m_sProvider = "CREODIAS2";
		this.m_oQueryTranslator = new QueryTranslatorCreoDias2();
		this.m_oResponseTranslator = new ResponseTranslatorCreoDias2();
		this.m_asSupportedPlatforms.addAll(
				Arrays.asList(Platforms.SENTINEL1, Platforms.SENTINEL2, Platforms.SENTINEL3, Platforms.SENTINEL5P, Platforms.ENVISAT, Platforms.LANDSAT8));
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
			
			String sResults = standardHttpGETQuery(sOutputQuery);
			
			iCount = m_oResponseTranslator.getCountResult(sResults);
			
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
			String sCreodiasResult = standardHttpGETQuery(sCreodiasQuery);
			
			
			
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
