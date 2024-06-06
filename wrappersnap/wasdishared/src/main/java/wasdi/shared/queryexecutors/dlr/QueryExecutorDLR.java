package wasdi.shared.queryexecutors.dlr;

import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.node.POJONode;

import wasdi.shared.config.WasdiConfig;

/**
 * Data provider for the German Space Agency - Deutsches Zentrum für Luft- und Raumfahrt (DLR)
 */

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.search.QueryViewModel;
import wasdi.shared.viewmodels.search.QueryResultViewModel;

public class QueryExecutorDLR extends QueryExecutor {
	
	private final String s_sSearchURL = "https://download.geoservice.dlr.de/WSF2019/grid.geojson";

	public QueryExecutorDLR() {
		this.m_sProvider = "DLR";
		this.m_oQueryTranslator = new QueryTranslatorDLR();
		this.m_oResponseTranslator = new ResponseTranslatorDLR();
		this.m_asSupportedPlatforms.add(Platforms.WSF);
	}

	@Override
	public int executeCount(String sQuery) {
				
		if (Utils.isNullOrEmpty(sQuery)) {
			WasdiLog.warnLog("QueryExecutorDLR.executeCount. Query from client is null or empty");
			return -1;
		}
		
		try {
			QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(sQuery);
			
			if (!m_asSupportedPlatforms.contains(oQueryViewModel.platformName)) {
				WasdiLog.warnLog("QueryExecutorDLR.executeCount. Platform not supported by the data provider");
				return -1;
			}
			
			// TODO: implement search by name
			Double dQueryNorth = oQueryViewModel.north;
			Double dQuerySouth = oQueryViewModel.south;
			Double dQueryEast = oQueryViewModel.east;
			Double dQueryWest = oQueryViewModel.west;
			
			String sProductType = oQueryViewModel.productType;
			
			if (dQueryNorth == null
				|| dQuerySouth == null
				|| dQueryWest == null
				|| dQueryEast == null) {
				WasdiLog.warnLog("QueryExecutorDLR.executeCount. Some null values in the bounding box");
				return -1;
			}
			
			if (Utils.isNullOrEmpty(sProductType)) {
				WasdiLog.warnLog("QueryExecutorDLR.executeCount. Product type is empty");
				return -1;
			}
			
			if (sProductType.equals("WSF_2019")) {
				JSONArray oJsonArray = getWSFTiles();
				
				if (oJsonArray == null) {
					WasdiLog.warnLog("QueryExecutorDLR.executeCount. No tiles retireved from the HTTP provider");
					return -1;
				}
				
				int iTile = 0;
				
				while (iTile < oJsonArray.length()) {
					JSONObject oGeometry = oJsonArray.optJSONObject(iTile);
					
					if (oGeometry == null)
						continue;
					
					JSONArray aoCoordinates = oGeometry.optJSONArray("coordinates");
					
					if (aoCoordinates == null)
						continue;
					
					for (int j = 0; j < aoCoordinates.length(); j++) {
						
						JSONArray aoOnePolygon= aoCoordinates.optJSONArray(j);
						
						if (aoOnePolygon == null)
							continue;
						
						if (aoOnePolygon.length() != 5) {
							WasdiLog.warnLog("QueryExecutorDLR.executeCount. Not a rectangle");
							continue;
						}
						
						// I can get just two corners: the (West, North) and the (East, South) corner 
					}
					iTile ++;
				}
			} else {
				WasdiLog.warnLog("QueryExecutorDLR.executeCount. Product type not recognized " + sProductType);
			}
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("QueryExecutorDLR.executeCount. Error when trying to retrieve the number of products ", oEx);
			return -1;
		}
		
		return -1;
		/*
		
		HttpCallResponse oResponse = HttpUtils.httpGet("https://download.geoservice.dlr.de/WSF2019/grid.geojson");
	
		
		JSONObject oJSONResponse = new JSONObject(oResponse.getResponseBody());
		JSONArray oArr = oJSONResponse.optJSONArray("features");
		if (oArr == null) {
			WasdiLog.debugLog("array features does not exist");
		} else {
			WasdiLog.debugLog("" + oArr.length());
		}
		return 0;
		*/
	}

	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	/**
	 * Retrieve from the DLR website the list of tiles for the World Settlement Footprint (WSF) 2019 
	 * @return a JSONArray with the list of tiles, it the HTTP call to the service is successfull. Null otherwise
	 */
	public JSONArray getWSFTiles() {
		try {
			HttpCallResponse oResponse = HttpUtils.httpGet(s_sSearchURL);
			
			if (oResponse == null) {
				WasdiLog.warnLog("QueryExecutorDLR.getWSFTiles. Http Response is null");
				return null;
			} 
			
			if (oResponse.getResponseCode() < 200 || oResponse.getResponseCode() > 299) {
				WasdiLog.warnLog("QueryExecutorDLR.getWSFTiles. Got response code: " + oResponse.getResponseCode());
				return null;
			}
			
			JSONObject oJSONResponse = new JSONObject(oResponse.getResponseBody());
			JSONArray oFeatureArr = oJSONResponse.optJSONArray("features");
			
			if (oFeatureArr == null) {
				WasdiLog.debugLog("QueryExecutorDLR.getWSFTiles. Features array does not exist");
				return null;
			} 
			
			WasdiLog.debugLog("QueryExecutorDLR.getWSFTiles. Received " + oFeatureArr.length() + " features");
			return oFeatureArr;
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("QueryExecutorDLR.getWSFTiles. Error retrieving the json with the list of tiles", oEx);
		}
		
		return null;
	}
	
	public boolean boundingBoxOverlap(float fN1, float fE1, float fS1, float fW1, float fN2, float fE2, float fS2, float fW2) {
		// ensuring the correct ordering of coordinates for the first bounding box
		float fTemp = -1;
		if (fW1 > fE1) {
			fTemp = fW1;
			fW1 = fE1;
			fE1 = fTemp;
		}
		
		if (fS1 > fN1) {
			fTemp = fS1;
			fS1 = fN1;
			fN1 = fTemp;
		}
		
		// ensuring the correct ordering of coordinates for the first bounding box
		if (fW2 > fE2) {
			fTemp = fW2;
			fW2 = fE2;
			fE2 = fTemp;
		}
		
		if (fS2 > fN2) {
			fTemp = fS2;
			fS2 = fN2;
			fN2 = fTemp;
		}
		
		// Check if one bounding box is to the left (west) of the other
		if (fE1 < fW2 || fE2 < fW1) {
			return false;
		}
		
		if (fN1 < fS2 || fN2 < fS1) {
			return false;
		}
		
		// bounding box intersect
		return true;
	}
	
	public static void main (String[] args) throws Exception {
		WasdiConfig.readConfig("C:/temp/wasdi/wasdiLocalTESTConfig.json");
		
		

	}

}
