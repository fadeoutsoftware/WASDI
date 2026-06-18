package wasdi.shared.utils.wasdiAPI;

import java.util.ArrayList;

import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.StringUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.search.QueryResultViewModel;

public class SearchAPIClient {
	
	public static QueryResultViewModel[] searchList(String sSessionId, String sProvider, String sQuery, String sWasdiUrl) {
		
		try {
			
			if (Utils.isNullOrEmpty(sWasdiUrl)) {
				sWasdiUrl = WasdiConfig.Current.baseUrl;
			}
			
			if (!sWasdiUrl.endsWith("/")) sWasdiUrl += "/";
						
			String sUrl = sWasdiUrl+"search/querylist?providers="+sProvider;
			
			ArrayList<String> asQueries = new ArrayList<>();
			asQueries.add(sQuery);
			
			HttpCallResponse oResponse = HttpUtils.httpPost(sUrl, JsonUtils.stringify(asQueries), HttpUtils.getStandardHeaders(sSessionId));
			
			if (oResponse == null) {
				WasdiLog.errorLog("SearchAPIClient.searchList: got null response");
				return null;
			}
			
			String sResponseBody = oResponse.getResponseBody();
			
			if (Utils.isNullOrEmpty(sResponseBody)) return null;
			
			
			QueryResultViewModel[] aoResults = JsonUtils.s_oMapper.readValue(sResponseBody, QueryResultViewModel[].class);
			
			return aoResults;
		}
		catch(Exception oEx) {
			WasdiLog.errorLog("SearchAPIClient.searchList: exception ", oEx);
		}
		
		return null;
	}
	
	public static int count(String sSessionId, String sProvider, String sQuery, String sWasdiUrl) {
		
		try {
			
			if (Utils.isNullOrEmpty(sWasdiUrl)) {
				sWasdiUrl = WasdiConfig.Current.baseUrl;
			}
			
			if (!sWasdiUrl.endsWith("/")) sWasdiUrl += "/";
			
			String sEncodedQuery = StringUtils.encodeUrl(sQuery);
						
			String sUrl = sWasdiUrl+"search/query/count?query="+sEncodedQuery+"providers="+sProvider;
			
			ArrayList<String> asQueries = new ArrayList<>();
			asQueries.add(sQuery);
			
			HttpCallResponse oResponse = HttpUtils.httpGet(sUrl, HttpUtils.getStandardHeaders(sSessionId));
			
			if (oResponse == null) {
				WasdiLog.errorLog("SearchAPIClient.count: got null response");
				return -1;
			}
			
			String sResponseBody = oResponse.getResponseBody();
			
			if (Utils.isNullOrEmpty(sResponseBody)) return -1;
			
			return Integer.parseInt(sResponseBody);
		}
		catch(Exception oEx) {
			WasdiLog.errorLog("SearchAPIClient.count: exception ", oEx);
		}
		
		return -1;
	}	
}
