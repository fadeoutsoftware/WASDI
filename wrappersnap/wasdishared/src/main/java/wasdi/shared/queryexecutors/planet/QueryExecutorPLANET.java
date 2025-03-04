package wasdi.shared.queryexecutors.planet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;

import wasdi.shared.config.DataProviderConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.search.QueryResultViewModel;
import wasdi.shared.viewmodels.search.QueryViewModel;

public class QueryExecutorPLANET extends QueryExecutor {
	
	String m_sBaseUrl = "https://api.planet.com/data/v1/";
	
	public QueryExecutorPLANET() {
		this.m_oQueryTranslator = new QueryTranslatorPLANET();
		this.m_oResponseTranslator = new ResponseTranslatorPLANET();
		
	}
	
	@Override
	public void init() {
		super.init();
		
		DataProviderConfig oDataProviderConfig = WasdiConfig.Current.getDataProviderConfig(m_sDataProviderCode);
		
		if (oDataProviderConfig != null) {
			m_sUser = oDataProviderConfig.user;
			m_sPassword = oDataProviderConfig.password;
		}
	}

	@Override
	public int executeCount(String sQuery) {
		// We do not have a count with Planet!?!
		
		return 0;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		
		// Parse the query.
		QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(oQuery.getQuery());
		
		if (oQueryViewModel == null) {
			WasdiLog.debugLog("QueryExecutorPLANET.executeAndRetrieve: Error decoding input query");
			return null;
		}
		
		// Prepare Authentication: for PLANET we use Basic Http Auth
		// user must be the API key, pw empty.
		String sAuth = m_sUser+":";
		
		// Prepare headers
		Map<String, String> aoHeaders = new HashMap<>();
		aoHeaders.put("Content-Type", "application/json");
		aoHeaders.put("Accept", "*/*");		
		
		// If we do not have a specific product Name
		if (Utils.isNullOrEmpty(oQueryViewModel.productName)) {
			// Obtain the query payload
			String sPayload = m_oQueryTranslator.getSearchUrl(oQuery);
			
			
			// Initialize the base url
			String sUrl = m_sBaseUrl + "quick-search?";
			sUrl +="_sort=acquired%20asc&_page_size=" + oQuery.getLimit();
			
			// It looks we cannot page with numbers: we search all the elements within the limit, then we will filter
			int iOffset = 0;
			
			try {
				iOffset = Integer.parseInt(oQuery.getOffset());
			}
			catch (Exception e) {
			}
			
			// Call the http 
			HttpCallResponse oHttpCallResponse = HttpUtils.httpPost(sUrl, sPayload, aoHeaders, sAuth); 
			String sResponse = oHttpCallResponse.getResponseBody();
			
			// Translate the result
			List<QueryResultViewModel> aoFoundItems = m_oResponseTranslator.translateBatch(sResponse, bFullViewModel);
			
			// Filter only the actual page
			ArrayList<QueryResultViewModel> aoFilteredList = null;
			
			if (aoFoundItems != null) {
				
				aoFilteredList = new ArrayList<QueryResultViewModel>();
				
				// Jump the first iOffset results
				for (int iIndex=0; iIndex<aoFoundItems.size(); iIndex++) {
					if (iIndex<iOffset) continue;
					aoFilteredList.add(aoFoundItems.get(iIndex));
				}			
			}
			
			// Return my results list
			return aoFilteredList;			
		}
		else {
			
			try {
				
				// Recover the Planet Id from the Name
				String [] asNameParts = oQueryViewModel.productName.split("_");
				
				// File Name is produced as: PLANET_[ItemType]_[DATE]_[ID]
				String sItemType = asNameParts[1];
				String sId = "";
				
				// Id can also have "_" so jump the first ones and then add all to the Id
				for (int iParts = 0; iParts<asNameParts.length; iParts++) {
					if (iParts<=2) continue;
					if (iParts>3) sId += "_";
					sId += asNameParts[iParts];
				}
				
				//sItemType = "REOrthoTile";
				//sId = "20160707_195147_1057916_RapidEye-1";
				
				//https://api.planet.com/data/v1/item-types/{item_type_id}/items/{item_id}
				String sUrl = m_sBaseUrl + "item-types/" + sItemType + "/items/" + sId + "/assets";
				String sResult = standardHttpGETQuery(sUrl);
				
				if (sResult == null) return null;
				
				// Convert the response in the relative JSON Map representation
				TypeReference<HashMap<String,Object>> oMapType = new TypeReference<HashMap<String,Object>>() {};
				HashMap<String,Object> oPlanetResponse = MongoRepository.s_oMapper.readValue(sResult, oMapType);

				// In the Location, we have the direct file access.
				// Here we put both links: the one for the asset and the one for the file
				// The asset link is needed to check if the file is ready when downloaded
				String sFinalLink=sUrl;
				
				if (oPlanetResponse.containsKey("analytic")) {
					HashMap<String,Object> oAnalytic = (HashMap<String,Object>) oPlanetResponse.get("analytic");
					if (oAnalytic.containsKey("location")) {
						sFinalLink +=";" + oAnalytic.get("location").toString();
					}
					else {
						sFinalLink+=";ND";
					}
				}
				else {
					sFinalLink+=";ND";
				}
				
				sFinalLink+=";" + oQueryViewModel.productName;
				
				// Prepare the return list
				ArrayList<QueryResultViewModel> aoResult = new ArrayList<QueryResultViewModel>();
				
				// Create the QueryResultViewModel
				QueryResultViewModel oQueryResultViewModel = new QueryResultViewModel();
				oQueryResultViewModel.setTitle(oQueryViewModel.productName);
				oQueryResultViewModel.setLink(sFinalLink);
				aoResult.add(oQueryResultViewModel);
				
				// Return our single result
				return aoResult;
			}
			catch (Exception oEx) {
				WasdiLog.debugLog("QueryExecutorPLANET.executeAndRetrieve: exception searching for name");
			}
		}
		
		return null;

	}

}
