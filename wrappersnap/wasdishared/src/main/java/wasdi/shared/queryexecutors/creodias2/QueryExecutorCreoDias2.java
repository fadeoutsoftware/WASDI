package wasdi.shared.queryexecutors.creodias2;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.http.QueryExecutorHttpGet;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.MissionUtils;
import wasdi.shared.utils.TimeEpochUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;


public class QueryExecutorCreoDias2 extends QueryExecutorHttpGet {
	
	
	public QueryExecutorCreoDias2() {
		this.m_oQueryTranslator = new QueryTranslatorCreoDias2();
		this.m_oResponseTranslator = new ResponseTranslatorCreoDias2();
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
			
			if (sPlatform.equals(Platforms.LANDSAT8)) {
				sPlatform = "LANDSAT-8-ESA";
			}
			
			Date oProductDate = MissionUtils.getDateFromSatelliteImageFileName(sProduct, sPlatform);
			
			String sBeginPositionStart = "1893-09-07T00:00:00.000Z";
			String sBeginPositionEnd = "2893-09-07T00:00:00.000Z";
			
			String sEndPositionStart = "1893-09-07T23:59:59.999Z";
			String sEndPositionEnd = "2893-09-07T23:59:59.999Z";
			
			if (oProductDate != null) {
				Date oEndDate = new Date();
				Date oStartDate = TimeEpochUtils.getPreviousDate(oProductDate, 30);
				
				String sStart = new SimpleDateFormat("yyyy-MM-dd").format(oStartDate);
				String sEnd = new SimpleDateFormat("yyyy-MM-dd").format(oEndDate);
				
				sBeginPositionStart = sStart + "T00:00:00.000Z";
				sBeginPositionEnd =  sEnd + "T00:00:00.000Z";
				
				sEndPositionStart = sStart + "T23:59:59.999Z";
				sEndPositionEnd = sEnd + "T23:59:59.999Z";
			}
			
			sClientQuery = sProduct + " AND ( beginPosition:[" + sBeginPositionStart + " TO " + sBeginPositionEnd + "] AND endPosition:[" + sEndPositionStart + " TO " + sEndPositionEnd + "] ) ";
			
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
