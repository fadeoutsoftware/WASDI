package wasdi.shared.queryexecutors.esa;

import java.util.List;

import org.json.JSONObject;

import wasdi.shared.config.DataProviderConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.utils.HttpUtils;
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
			WasdiLog.debugLog("QueryExecutorESA.executeCount. Unsupported platform: " + oQueryViewModel.platformName);
			return iCount;
		}
		
		// spatial filter
		Double dWest = oQueryViewModel.west;
		Double dNorth = oQueryViewModel.north;
		Double dEast = oQueryViewModel.east;
		Double dSouth = oQueryViewModel.south;

		// temporal filter
		String sDateFrom = oQueryViewModel.startFromDate;
		String sDateTo = oQueryViewModel.endToDate;
		
		// product type filter
		String sProductType = oQueryViewModel.productType;
		
		// name filter
		String sFileName = oQueryViewModel.productName; // TODO
		
		
		String sCountURL = m_ESABaseUrl;
		
		if (!sCountURL.endsWith("/")) 
			sCountURL += "/";
			
		sCountURL += "collections/" + sProductType + "/items?";
		sCountURL += "bbox=" + dWest + "," + dSouth + "," + dEast + "," + dNorth; 
		
		sCountURL += "&datetime=" + sDateFrom + "/" + sDateTo;
		
		WasdiLog.debugLog("QueryExecutorESA.executeCount. Sending count query to data provider: " + sCountURL);
		
		try {
			HttpCallResponse oResponse = HttpUtils.httpGet(sCountURL);
			
			int iResponseCode = oResponse.getResponseCode();
			
			if (iResponseCode >= 200 && iResponseCode <= 299) {
				String sResponseBody = oResponse.getResponseBody();
				JSONObject oJsonBody = new JSONObject(sResponseBody);
				Integer iMatches = oJsonBody.optInt("numberMatched");
				
				if (iMatches != null) {
					iCount = iMatches;
				}
				
				WasdiLog.debugLog("QueryExecutorESA.executeCount. Number of matches found: " + iMatches);
			} else {
				WasdiLog.warnLog("QueryExeutorESA.executeCount. Error code from HTTP request: " + iResponseCode);
			}
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("QueryExecutorESA.executeCount. Error retrieving number of results: ", oEx);
		}
						
		return iCount;
	}

	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public static void main(String[]args) throws Exception {
		boolean bResult = WasdiConfig.readConfig("C:/temp/wasdi/wasdiLocalTESTConfig.json");
		QueryExecutorESA oExecutor = new QueryExecutorESA();
		
	}

}
