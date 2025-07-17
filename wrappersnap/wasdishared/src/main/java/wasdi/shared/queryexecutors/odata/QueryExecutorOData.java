package wasdi.shared.queryexecutors.odata;

import java.util.List;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.http.QueryExecutorHttpGet;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.MissionUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;

public class QueryExecutorOData extends QueryExecutorHttpGet {

	public QueryExecutorOData() {
		super();
	}
	
	@Override
	public int executeCount(String sQuery) {
		int iCount = -1;

		try {
			
			QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(sQuery);
			
			if (!m_asSupportedPlatforms.contains(oQueryViewModel.platformName)) {
				WasdiLog.debugLog("QueryExecutorOData.executeCount. Platform not supported by the data provider for query: " + sQuery);
				return -1;
			}
			
			String sOutputQuery = m_oQueryTranslator.getCountUrl(sQuery);
			
			HttpCallResponse oHttpResponse = HttpUtils.httpGet(sOutputQuery, null, null);  
			
			String sResult = oHttpResponse.getResponseBody();
						
			if (oHttpResponse.getResponseCode()>=200 && oHttpResponse.getResponseCode()<=299) {
				iCount = m_oResponseTranslator.getCountResult(sResult);
			}
			else {
				WasdiLog.debugLog("QueryExecutorOData.executeCount. Error when trying to retrieve the number of results. Response code: " + oHttpResponse.getResponseCode());
				return -1;
			}
			
			
		} catch (Exception oException) {
			WasdiLog.errorLog("QueryExecutorOData.executeCount. Error when trying to retrieve the number of results ", oException);
			return -1;
		}
		
		return iCount;
	}
	
	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		
		try {
	
			QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(oQuery.getQuery());
	
			if (!m_asSupportedPlatforms.contains(oQueryViewModel.platformName)) {
				WasdiLog.debugLog("QueryExecutorOData.executeAndRetrieve. Platform not supported by the data provider for query: " + oQuery.getQuery());
				return null;
			}
			
			String sCreodiasQuery = m_oQueryTranslator.getSearchUrl(oQuery);
			
			// We add some retry if the query goes wrong
			int iMaxAttempt = 5;
			int iRetry = 0;
			
			HttpCallResponse oHttpResponse = null;
			
			for (iRetry=0; iRetry<iMaxAttempt; iRetry++) {
				// Call standard http get API
				oHttpResponse = HttpUtils.httpGet(sCreodiasQuery, null, null);
							
				// Check if we have a valid HTTP Response: CREODIAS returns 200 also in case of zero results
				if (oHttpResponse.getResponseCode()< 200 || oHttpResponse.getResponseCode() > 299) {
					// So these codes are a problem
					WasdiLog.warnLog("QueryExecutorOData.executeAndRetrieve: Attempt: " + iRetry + " Error when trying to retrieve the results on CreoDias. Response code: " + oHttpResponse.getResponseCode() + " Message: " + oHttpResponse.getResponseBody());
					
					try {
						// Sleep a little bit
						Thread.sleep(2000);
					} 
					catch (InterruptedException oEx) {
						Thread.currentThread().interrupt();
					}
				}
				else {
					// Ok we got a valid response
					break;
				}
			}
			
			String sCreodiasResult = oHttpResponse.getResponseBody();
			
			// We need to re-check: we may be here for a good one or because we finished retries
			if (oHttpResponse.getResponseCode()< 200 || oHttpResponse.getResponseCode() > 299) {
				WasdiLog.errorLog("QueryExecutorOData.executeAndRetrieve. Final error when trying to retrieve the results on CreoDias. Response code: " + oHttpResponse.getResponseCode() + " Message: " + sCreodiasResult);
				return null;
			}

			if (Utils.isNullOrEmpty(sCreodiasResult)) {
				WasdiLog.debugLog("QueryExecutorOData.executeAndRetrieve. The data provider returned an empty string for query: " + sCreodiasQuery);
				return null;
			}
			
			// Convert the results
			List<QueryResultViewModel> aoRes = m_oResponseTranslator.translateBatch(sCreodiasResult, bFullViewModel);
			
			return aoRes;
		
		} catch (Exception oEx) {
			WasdiLog.errorLog("QueryExecutorOData.executeAndRetrieve. Error when trying to retrieve the Creodias results for query. ", oEx);
			return null;
		}
		
	}
	
	/**
	 * Overrided because it looks that CREODIAS can return the Sentinel1 files also named _COG.
	 * In this case we need to select the correct original one
	 */
	@Override
	public String getUriFromProductName(String sProduct, String sProtocol, String sOriginalUrl, String sPlatform) {
		try {
			String sClientQuery = "";
			
			if (Utils.isNullOrEmpty(sPlatform)) {
				sPlatform = MissionUtils.getPlatformFromSatelliteImageFileName(sProduct);
			}
			/*
			if (sPlatform.equals(Platforms.LANDSAT8)) {
				sPlatform = "LANDSAT-8-ESA";
			}
			*/
			
			sClientQuery = sProduct;
			
			if (!Utils.isNullOrEmpty(sPlatform)) {
				sClientQuery = sClientQuery + "AND (platformname:" + sPlatform + " )";
			}			
			
			PaginatedQuery oQuery = new PaginatedQuery(sClientQuery, "0", "10", null, null);
			List<QueryResultViewModel> aoResults = executeAndRetrieve(oQuery);
			
			if (aoResults != null) {
				if (aoResults.size()>0) {
					
					QueryResultViewModel oResult = aoResults.get(0);
					
					if (sPlatform.equals(Platforms.SENTINEL1)) {
						// Clean it
						oResult = null;
					
						for (int iResults = 0; iResults<aoResults.size(); iResults++) {
							QueryResultViewModel oTempResult = aoResults.get(iResults);
														
							if (oTempResult.getTitle().equals(sProduct)) {
								oResult = oTempResult;
								break;
							}
						}
					}
					
					if (oResult!=null) {
						return oResult.getLink();
					}
				}
			}
			
			return "";					
		}
		catch (Exception oEx) {
			WasdiLog.debugLog("QueryExecutor.getUriFromProductName: exception " + oEx.toString());
		}
		
		return "";
	}	

}
