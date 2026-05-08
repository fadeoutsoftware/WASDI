package wasdi.shared.queryexecutors.http;

import java.util.ArrayList;
import java.util.List;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;

/**
 * Derived class for a standard Query Executor that needs an http get
 * 
 * The class implements executeCount and executeAndRetrieve methods.
 * 
 * Both uses the QueryTranslator and ResponsaTranslator Data Members.
 * 
 * Each Query Exeuctor that uses standard http calls, must derive from this class and implement the abstracts
 * method of QueryTranslator to get Search and Count URL and of Response Translator to get the count from 
 * the return of the count query and to convert the return of the search query in WASDI View Models 
 * 
 * executeCount steps are:
 * 	.Check if the platform is supported
 * 	.call QueryTranslator.getCountUrl
 * 	.execute std http get call with that url
 * 	.call m_oResponseTranslator.getCountResult to get the number of results.
 * 
 * executeAndRetrive steps are:
 * 	.Check if the platform is supported
 * 	.call QueryTranslator.getSearchUrl
 * 	.execute std http get call with that url
 * 	.call m_oResponseTranslator.translateBatch to get the number of results.
 * 
 * 
 * @author p.campanella
 *
 */
public abstract class QueryExecutorHttpGet extends QueryExecutor {
	
	/**
	 * Standard execute Count that uses http GET API 
	 */
	@Override
	public int executeCount(String sQuery) {
		
		try {
			// Check if we have the Query Translator
			if (m_oQueryTranslator == null) {
				WasdiLog.debugLog("QueryExecutorHttpGet.executeCount: Query Translator is null. Return -1");
				return -1;
			}
			
			// And if we have the response translator
			if (m_oResponseTranslator == null) {
				WasdiLog.debugLog("QueryExecutorHttpGet.executeCount: Response Translator is null. Return -1");
				return -1;
			}
			
			// Parse the query to get the QueryViewModel
			QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(sQuery);
			
			// Check if the platform is supported
			if (m_asSupportedPlatforms.contains(oQueryViewModel.platformName) == false) {
				WasdiLog.debugLog("QueryExecutorHttpGet.executeCount: platform " + oQueryViewModel.platformName + " not supported by " + m_sDataProviderCode + ". Return -1.");
				return 0;
			}		
			
			// Get count url
			String sOutputQuery = m_oQueryTranslator.getCountUrl(sQuery);
			// execute standard http query
			String sResults = standardHttpGETQuery(sOutputQuery);
			
			if (sResults == null) return -1;
			
			// Let the transaltor give us the number of results
			int iResults = m_oResponseTranslator.getCountResult(sResults);
			
			
			WasdiLog.debugLog("QueryExecutorHttpGet.executeCount: count done, found " + iResults);
			
			// Return the number
			return iResults;			
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("QueryExecutorHttpGet.executeCount: exception " + oEx.toString());
		}
		
		WasdiLog.debugLog("QueryExecutorHttpGet.executeCount: Something went wrong, return -1");
		return -1;
	}

	
	/**
	 * Standard execute and retrive that uses http GET API 
	 */
	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		
		try {
			
			// Check if we have the Query Translator
			if (m_oQueryTranslator == null) {
				WasdiLog.debugLog("QueryExecutorHttpGet.executeAndRetrieve: Query Translator is null. Return empty list.");
				return new ArrayList<QueryResultViewModel>();
			}
			
			// Check if we have the Response Translator
			if (m_oResponseTranslator == null) {
				WasdiLog.debugLog("QueryExecutorHttpGet.executeAndRetrieve: Response Translator is null. Return empty list.");
				return new ArrayList<QueryResultViewModel>();
			}
			
			// Parse the query to get the QueryViewModel
			QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(oQuery.getQuery());
			
			// Check if the platform is supported
			if (m_asSupportedPlatforms.contains(oQueryViewModel.platformName) == false) {
				WasdiLog.debugLog("QueryExecutorHttpGet.executeAndRetrieve: platform " + oQueryViewModel.platformName + " not supported by " + m_sDataProviderCode + ". Return empty list.");
				return new ArrayList<QueryResultViewModel>();
			}		
			
			// Get Search Url
			String sOutputQuery = m_oQueryTranslator.getSearchUrl(oQuery);
			// Call standard http get API
			String sResults = standardHttpGETQuery(sOutputQuery);
			
			if (sResults==null) return null;
			
			// Transalte the result
			List<QueryResultViewModel> aoResults = m_oResponseTranslator.translateBatch(sResults, bFullViewModel);
			
			WasdiLog.debugLog("QueryExecutorHttpGet.executeAndRetrieve: query done, return " + aoResults.size() + " results.");
			
			// Return the list to the user
			return aoResults;			
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("QueryExecutorHttpGet.executeAndRetrieve: exception " + oEx.toString());
		}
		
		WasdiLog.debugLog("QueryExecutorHttpGet.executeAndRetrieve: Something went wrong, return null list");
		return null;
	}

}

