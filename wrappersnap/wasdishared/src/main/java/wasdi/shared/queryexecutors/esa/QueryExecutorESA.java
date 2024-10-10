package wasdi.shared.queryexecutors.esa;

import java.util.List;
import java.util.ArrayList;

import org.json.JSONObject;

import wasdi.shared.config.DataProviderConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.Platforms;
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
		this.m_sProvider = "ESA";
		this.m_oQueryTranslator = new QueryTranslatorESA();
		this.m_oResponseTranslator = new ResponseTranslatorESA();
		this.m_asSupportedPlatforms.add(Platforms.ERS);
		
		DataProviderConfig oESADataProvider = WasdiConfig.Current.getDataProviderConfig("ESA");
		m_ESABaseUrl = oESADataProvider != null 
				? oESADataProvider.link 
				: null;
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
			
			Integer iMatches = oJsonBody.optInt("numberMatched");
				
			if (iMatches != null) {
				iCount = iMatches;
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
		
		List<QueryResultViewModel> oResults = new ArrayList<>(); 
		try {
			
			String sResult =  getResultsFromEOCAT(oQueryViewModel);
			
			if (Utils.isNullOrEmpty(sResult)) {
				WasdiLog.debugLog("QueryExecutorESA.executeAndRetrieve. Error getting the results from EOCAt");
				return null;
			}
			
			
			WasdiLog.debugLog("QueryExecutorESA.executeAndRetrieve. Returning results: " + oResults.size());
			
			return oResults;
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("QueryExecutorESA.executeAndRetrieve. Error retrieving number of results: ", oEx);
		}
		
		return null;
		
	}
	
	
	public String getResultsFromEOCAT(QueryViewModel oQueryViewModel) {
		
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
	
	public static void main(String[]args) throws Exception {
		boolean bResult = WasdiConfig.readConfig("C:/temp/wasdi/wasdiLocalTESTConfig.json");
		QueryExecutorESA oExecutor = new QueryExecutorESA();
		
	}

}
