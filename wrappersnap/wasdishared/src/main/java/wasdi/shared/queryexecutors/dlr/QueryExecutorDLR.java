package wasdi.shared.queryexecutors.dlr;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

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
				WasdiLog.debugLog("QueryExecutorDLR.executeCount. Retrieving matching tiles for WSF_2019");
				List<JSONObject> aoOverlappingTiles = listOverlappingTiles(dQueryNorth, dQueryEast, dQuerySouth, dQueryWest);
				
				if (aoOverlappingTiles == null) {
					WasdiLog.warnLog("QueryExecutorDLR.executeCount. List of overlapping tile is null");
					return -1;
				}
				
				WasdiLog.debugLog("QueryExecutorDLR.executeCount. Number of retrieved tiles: " + aoOverlappingTiles.size());
				return aoOverlappingTiles.size();
			} else {
				WasdiLog.warnLog("QueryExecutorDLR.executeCount. Product type not recognized " + sProductType);
			}
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("QueryExecutorDLR.executeCount. Error when trying to retrieve the number of products ", oEx);
			return -1;
		}
		
		return -1;

	}

	@Override
	public List<QueryResultViewModel> executeAndRetrieve(PaginatedQuery oQuery, boolean bFullViewModel) {		
		
		try {
			String sQuery = oQuery.getQuery();
			// int iLimit = Integer.getInteger(oQuery.getLimit());
			// int iOffset = Integer.getInteger(oQuery.getOffset());
			
			if (Utils.isNullOrEmpty(sQuery)) {
				WasdiLog.warnLog("QueryExecutorDLR.executeAndRetrieve. Query from client is null or empty");
				return null;
			}
		
			QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(sQuery);
			
			if (!m_asSupportedPlatforms.contains(oQueryViewModel.platformName)) {
				WasdiLog.warnLog("QueryExecutorDLR.executeAndRetrieve. Platform not supported by the data provider");
				return null;
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
				WasdiLog.warnLog("QueryExecutorDLR.executeAndRetrieve. Some null values in the bounding box");
				return null;
			}
			
			if (Utils.isNullOrEmpty(sProductType)) {
				WasdiLog.warnLog("QueryExecutorDLR.executeAndRetrieve. Product type is empty");
				return null;
			}
			
			if (sProductType.equals("WSF_2019")) {
				WasdiLog.debugLog("QueryExecutorDLR.executeAndRetrieve. Retrieving matching tiles for WSF_2019");
				List<JSONObject> aoOverlappingTiles = listOverlappingTiles(dQueryNorth, dQueryEast, dQuerySouth, dQueryWest);
				
				if (aoOverlappingTiles == null) {
					WasdiLog.warnLog("QueryExecutorDLR.executeAndRetrieve.List of overlapping tiles is null");
					return null;
				}
				
				WasdiLog.debugLog("QueryExecutorDLR.executeAndRetrieve. Number of overlapping tiles: " + aoOverlappingTiles.size());
				List<QueryResultViewModel> aoResults = new ArrayList<>();
				
				for (JSONObject oTile : aoOverlappingTiles) {
					aoResults.add(getResultViewModel(oTile));
				}
				
				WasdiLog.debugLog("QueryExecutorDLR.executeAndRetrieve. Number of returned result " + aoResults.size());
				return aoResults;
				
			} else {
				WasdiLog.warnLog("QueryExecutorDLR.executeAndRetrieve. Product type not recognized " + sProductType);
			}
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("QueryExecutorDLR.executeAndRetrieve. Error when trying to retrieve the number of products ", oEx);
			return null;
		}
		
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
	
	public boolean boundingBoxOverlap(double fN1, double fE1, double fS1, double fW1, double fN2, double fE2, double fS2, double fW2) {
		// ensuring the correct ordering of coordinates for the first bounding box
		double fTemp = -1;
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
	
	
	public List<JSONObject> listOverlappingTiles(double dQueryNorth, double dQueryEast, double dQuerySouth, double dQueryWest) {
		
		List<JSONObject> oOverlappingTiles = new ArrayList<>();
		
		JSONArray oTilesList = getWSFTiles();
		
		if (oTilesList == null) {
			WasdiLog.warnLog("QueryExecutorDLR.listOverlappingTiles. No tiles retireved from the HTTP provider");
			return null;
		}
		
		int iTile = 0;
		
		
		while (iTile < oTilesList.length()) {
			JSONObject oTileJson = oTilesList.optJSONObject(iTile);
			
			if (oTileJson == null) {
				WasdiLog.debugLog("QueryExecutorDLR.listOverlappingTiles. ");
				return null;
			}
			
			
			JSONObject oGeometry = oTileJson.optJSONObject("geometry");
			
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
					WasdiLog.warnLog("QueryExecutorDLR.listOverlappingTiles. Not a rectangle");
					continue;
				}
				
				// I can get just two corners: the (West, North) and the (East, South) corner
				try {
					JSONArray aoTopLeftCorner = aoOnePolygon.optJSONArray(0);
					
					JSONArray aoBottomRightCorner = aoOnePolygon.optJSONArray(2);
					
					if (aoTopLeftCorner == null || aoBottomRightCorner == null) {
						WasdiLog.warnLog("QueryExecutorDLR.listOverlappingTiles. Missing coordinates");
						return null;
					}
					
					double dTileWest = aoTopLeftCorner.optDouble(0);
					double dTileNorth = aoTopLeftCorner.optDouble(1);
					
					double dTileEast = aoBottomRightCorner.optDouble(0);
					double dTileSouth = aoBottomRightCorner.optDouble(1);
					
					// boundingBoxOverlap(float fN1, float fE1, float fS1, float fW1, float fN2, float fE2, float fS2, float fW2)
					
					boolean bIsOverlap = boundingBoxOverlap(dQueryNorth, dQueryEast, dQuerySouth, dQueryWest, dTileNorth, dTileEast, dTileSouth, dTileWest);
					
					if (bIsOverlap) 
						oOverlappingTiles.add(oTileJson);
					
				} catch (Exception oEx) {
					WasdiLog.errorLog("QueryExecutorDLR.listOverlappingTiles. Error reading coordinates from data provider", oEx);
				}
				 
			}
			iTile ++;
		}
		
		return oOverlappingTiles;
	}
	
	public QueryResultViewModel getResultViewModel(JSONObject oJsonResult) {
		
		JSONObject oProperties = oJsonResult.getJSONObject("properties");
		String sFileName = oProperties.getString("filename");
		String sId = oProperties.getString("id");
		String sSize = oProperties.getString("filesize");
		String sLink = oProperties.getString("Download");
		
		JSONObject oGeometry = oJsonResult.getJSONObject("geometry");
		JSONArray aoCoordinates = oGeometry.getJSONArray("coordinates");
		JSONArray aoOnePolygon = aoCoordinates.getJSONArray(0);
		
		JSONArray aoTopLeftCorner = aoOnePolygon.optJSONArray(0);
		
		JSONArray aoBottomRightCorner = aoOnePolygon.optJSONArray(2);
		
		if (aoTopLeftCorner == null || aoBottomRightCorner == null) {
			WasdiLog.warnLog("QueryExecutorDLR.listOverlappingTiles. Missing coordinates");
			return null;
		}
		
		double dTileWest = aoTopLeftCorner.optDouble(0);
		double dTileNorth = aoTopLeftCorner.optDouble(1);
		
		double dTileEast = aoBottomRightCorner.optDouble(0);
		double dTileSouth = aoBottomRightCorner.optDouble(1);
		
		List<String> asCoordinates = new ArrayList<>();
		asCoordinates.add(dTileWest + " " + dTileSouth);
		asCoordinates.add(dTileWest + " " + dTileNorth);
		asCoordinates.add(dTileEast + " " + dTileNorth);
		asCoordinates.add(dTileEast + " " + dTileSouth);
		asCoordinates.add(dTileWest + " " + dTileSouth);
		String sCoordinates = String.join(",", asCoordinates);
		
		String sFootprint = "POLYGON" + " ((" + sCoordinates + "))";
		
		String sSummary = "Date: 2019-01-01T00:00:00.000Z, Instrument: BOH, Mode: Boh, Satellite: Boh, Size: 100B";
		
		
		QueryResultViewModel oVM = new QueryResultViewModel();
		oVM.setFootprint(sFootprint);
		oVM.setId(sId);
		oVM.setLink(sLink);
		oVM.setProvider("WSF");
		oVM.setTitle(sFileName);
		oVM.setSummary(sSummary);
		
		return oVM;
		
		
	}


}
