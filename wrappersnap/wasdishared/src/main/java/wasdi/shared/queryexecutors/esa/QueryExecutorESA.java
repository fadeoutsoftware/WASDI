package wasdi.shared.queryexecutors.esa;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;

public class QueryExecutorESA extends QueryExecutor {
	
	private String m_ESABaseUrl;

	public QueryExecutorESA() {
		this.m_oQueryTranslator = new QueryTranslatorESA();
		this.m_oResponseTranslator = new ResponseTranslatorESA();		
	}
	
	@Override
	public void init() {
		super.init();
		m_ESABaseUrl = m_oDataProviderConfig.link;
		
	}
	
	@Override
	public String getUriFromProductName(String sProduct, String sProtocol, String sOriginalUrl, String sPlatform) {
		if (sProduct.toUpperCase().startsWith("SAR_IMP_1P")
				|| sProduct.toUpperCase().startsWith("SAR_IMS_1P")
				|| sProduct.toUpperCase().startsWith("SAR_IMM_1P" )) {
			return sOriginalUrl;
		}
		return null;
	}

	@Override
	public int executeCount(String sQuery) {
		WasdiLog.debugLog("QueryExecutorESA.executeCount. Query: " + sQuery);
		
		int iCount = -1;
		
		QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(sQuery);
		
		if (!m_asSupportedPlatforms.contains(oQueryViewModel.platformName)) {
			WasdiLog.warnLog("QueryExecutorESA.executeCount. Unsupported platform: " + oQueryViewModel.platformName);
			return iCount;
		}
		
		try {
			String sResult =  getResultsFromEOCAT(oQueryViewModel);
			
			if (Utils.isNullOrEmpty(sResult)) {
				WasdiLog.debugLog("QueryExecutorESA.executeCount. Error getting the results from EOCAt");
				return iCount;
			}
			
			JSONObject oJsonBody = new JSONObject(sResult);
			Integer iMatches = null; 
					
			if (oJsonBody.keySet().contains("numberMatched")) {
				iMatches = oJsonBody.optInt("numberMatched");				
			}
			
			if (iMatches != null) {
				iCount = iMatches;
			}
			else if (!Utils.isNullOrEmpty(oQueryViewModel.productName) && !Utils.isNullOrEmpty(oJsonBody.optString("id"))){
				iCount = 1;
			}
				
			WasdiLog.debugLog("QueryExecutorESA.executeCount. Number of matches found: " + iMatches);
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("QueryExecutorESA.executeCount. Error retrieving number of results: ", oEx);
		}
						
		return iCount;
	}

	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		
		
		QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(oQuery.getQuery());
		
		if (!m_asSupportedPlatforms.contains(oQueryViewModel.platformName)) {
			WasdiLog.warnLog("QueryExecutorESA.executeAndRetrieve. Unsupported platform: " + oQueryViewModel.platformName);
			return null;
		}
		
		List<QueryResultViewModel> aoResults = new ArrayList<>(); 
		try {
			
			int iLimit = 10;
			int iOffset = 0;
			
			try {
				if (!Utils.isNullOrEmpty(oQuery.getLimit())) {
					iLimit = Integer.parseInt(oQuery.getLimit());
				}
			} catch (Exception oEx) {
				WasdiLog.errorLog("QueryExecutorESA.executeAndRetrieve. Impossible to parse limit", oEx);
			}
			
			try {
				if (!Utils.isNullOrEmpty(oQuery.getOffset())) {
					iOffset = Integer.parseInt(oQuery.getOffset());					
				}
			} catch (Exception oEx) {
				WasdiLog.errorLog("QueryExecutorESA.executeAndRetrieve. Impossible to parse offset", oEx);
			}
			
			String sResult =  getResultsFromEOCAT(oQueryViewModel, iLimit, iOffset);
			
			if (Utils.isNullOrEmpty(sResult)) {
				WasdiLog.debugLog("QueryExecutorESA.executeAndRetrieve. Error getting the results from EOCAt");
				return null;
			}
			
			aoResults = m_oResponseTranslator.translateBatch(sResult, bFullViewModel);
						
			return aoResults;
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("QueryExecutorESA.executeAndRetrieve. Error retrieving number of results: ", oEx);
		}
		
		return null;
		
	}
	
	
	public String getResultsFromEOCAT(QueryViewModel oQueryViewModel) {
		return getResultsFromEOCAT(oQueryViewModel, -1, 1);
	}
	
	
	public String getResultsFromEOCAT(QueryViewModel oQueryViewModel, int iLimit, int iOffset) {
		
		// product type filter
		String sProductType = oQueryViewModel.productType;
		
		if (Utils.isNullOrEmpty(sProductType)) {
			WasdiLog.warnLog("QueryExecutorESA.getResultsFromEOCAT. Product type not specified");
			return null;
		}
		
		// name filter
		String sFileName = oQueryViewModel.productName;
		
		String sURL = m_ESABaseUrl;
		
		if (!sURL.endsWith("/")) 
			sURL += "/";
			
		sURL += "collections/" + sProductType + "/items";
		
		if (!Utils.isNullOrEmpty(sFileName)) {
			// example: https://eocat.esa.int/eo-catalogue/collections/SAR_IMP_1P/items/SAR_IMP_1PNESA20110212_100804_00000015A165_00165_82684_0000
			// search by name
			if (!sURL.endsWith("/"))
				sURL += "/";
			sURL += sFileName;
		}
		else {
			// spatial filter
			Double dWest = oQueryViewModel.west;
			Double dNorth = oQueryViewModel.north;
			Double dEast = oQueryViewModel.east;
			Double dSouth = oQueryViewModel.south;

			// temporal filter
			String sDateFrom = oQueryViewModel.startFromDate;
			String sDateTo = oQueryViewModel.endToDate;
			
			if (dWest == null || dNorth == null || dEast == null || dSouth == null) {
				WasdiLog.warnLog("QueryExecutorESA.getResultsFromEOCAT. Some coordinates were not specified");
				return null;
			}
			
			if (Utils.isNullOrEmpty(sDateFrom) || Utils.isNullOrEmpty(sDateFrom)) {
				WasdiLog.warnLog("QueryExecutorESA.getResultsFromEOCAT. Missing start or end date");
				return null;
			}
			
			sURL += "?bbox=" + dWest + "," + dSouth + "," + dEast + "," + dNorth; 
			
			sURL += "&datetime=" + sDateFrom + "/" + sDateTo;	
			
			if (iLimit > -1 && iOffset > -1) {
				// offset in WASDI start from 0, but in ESA APIs starts from 1
				iOffset += 1;
				sURL += "&startRecord=" + iOffset + "&limit=" + iLimit;
			}
		}
		
		WasdiLog.debugLog("QueryExecutorESA.getResultsFromEOCAT. Sending  query to data provider: " + sURL);
		
		try {
			HttpCallResponse oResponse = HttpUtils.httpGet(sURL);
			
			int iResponseCode = oResponse.getResponseCode();
			
			if (iResponseCode >= 200 && iResponseCode <= 299) {
				String sResponseBody = oResponse.getResponseBody();
				return sResponseBody;		
			} else {
				WasdiLog.warnLog("QueryExeutorESA.getResultsFromEOCAT. Error code from HTTP request: " + iResponseCode);
			}
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("QueryExecutorESA.getResultsFromEOCAT. Error retrieving number of results: ", oEx);
		}
		
		return null;
	}
	


}
