package wasdi.shared.queryexecutors.dlr;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;


/**
 * Data provider for the German Space Agency - Deutsches Zentrum für Luft- und Raumfahrt (DLR)
 */

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.QueryExecutor;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
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
	public String getUriFromProductName(String sProduct, String sProtocol, String sOriginalUrl) {
		return sOriginalUrl;
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
			
			if (!Utils.isNullOrEmpty(oQueryViewModel.productName)) {
				WasdiLog.debugLog("QueryExecutorDLR.executeCount. Searching product name: " + oQueryViewModel.productName);
				JSONObject oMatchingTile = getTileMatchingName(oQueryViewModel.productName);
				if (oMatchingTile == null) {
					WasdiLog.warnLog("QueryExecutorDLR.executeCount. Matching tile is null");
					return -1;
				}
				else if (Utils.isNullOrEmpty(oMatchingTile.optString("filename"))) {
					WasdiLog.debugLog("QueryExecutorDLR.executeCount. No tile matching the name " + oQueryViewModel.productName);;
					return 0;
				}
					
				WasdiLog.debugLog("QueryExecutorDLR.executeCount. Found tile matching the name " + oQueryViewModel.productName);;
				return 1;
			}
			
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
			
			int iLimit = 0;
			int iOffset = 0;
			
			try {
				iLimit = Integer.parseInt(oQuery.getLimit());
				iOffset = Integer.parseInt(oQuery.getOffset());
			} catch (Exception oEx) {
				WasdiLog.errorLog("QueryExecutorDLR.executeAndRetrieve. Error in parsing limit or offset");
				return null;
			}
			
			if (Utils.isNullOrEmpty(sQuery)) {
				WasdiLog.warnLog("QueryExecutorDLR.executeAndRetrieve. Query from client is null or empty");
				return null;
			}
		
			QueryViewModel oQueryViewModel = m_oQueryTranslator.parseWasdiClientQuery(sQuery);
			
			if (!m_asSupportedPlatforms.contains(oQueryViewModel.platformName)) {
				WasdiLog.warnLog("QueryExecutorDLR.executeAndRetrieve. Platform not supported by the data provider");
				return null;
			}
			
			if (!Utils.isNullOrEmpty(oQueryViewModel.productName)) {
				WasdiLog.debugLog("QueryExecutorDLR.executeCount. Searching product name: " + oQueryViewModel.productName);
				JSONObject oMatchingTile = getTileMatchingName(oQueryViewModel.productName);
				if (oMatchingTile == null) {
					WasdiLog.warnLog("QueryExecutorDLR.executeAndRetrieve. Matching tile is null");
					return null;
				}
				else if (Utils.isNullOrEmpty(oMatchingTile.optString("filename"))) {
					WasdiLog.debugLog("QueryExecutorDLR.executeAndRetrieve. No tile matching the name " + oQueryViewModel.productName);;
					return null;
				}
					
				WasdiLog.debugLog("QueryExecutorDLR.executeAndRetrieve. Found tile matching the name " + oQueryViewModel.productName);;
				return Arrays.asList(getResultViewModel(oMatchingTile));
			}
			
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
					QueryResultViewModel oVM = getResultViewModel(oTile);
					if (oVM != null)
					aoResults.add(oVM);
				}
				
				WasdiLog.debugLog("QueryExecutorDLR.executeAndRetrieve. Number of returned result " + aoResults.size());
				
				int iEnd = iOffset + iLimit > aoResults.size() ? aoResults.size() : iOffset + iLimit;
				return aoResults.subList(iOffset, iEnd);
				
			} else {
				WasdiLog.warnLog("QueryExecutorDLR.executeAndRetrieve. Product type not recognized " + sProductType);
				return null;
			}
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("QueryExecutorDLR.executeAndRetrieve. Error when trying to retrieve the number of products ", oEx);
			return null;
		}
		
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
				WasdiLog.debugLog("QueryExecutorDLR.listOverlappingTiles. Got null tile");
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
	
	public JSONObject getTileMatchingName(String sProductName) {
				
		JSONArray oTilesList = getWSFTiles();
		
		if (oTilesList == null) {
			WasdiLog.warnLog("QueryExecutorDLR.getTileMatchingName. No tiles retireved from the HTTP provider");
			return null;
		}
		
		int iTile = 0;
		
		
		while (iTile < oTilesList.length()) {
			JSONObject oTileJson = oTilesList.optJSONObject(iTile);
			
			if (oTileJson == null) {
				WasdiLog.debugLog("QueryExecutorDLR.getTileMatchingName. Got null tile");
				return null;
			}
					
			JSONObject oProperties = oTileJson.optJSONObject("properties");
			
			if (oProperties == null)
				continue;
			
			String sFileName = oProperties.optString("filename");
			
			if (sFileName.endsWith(".tif"))
				sFileName = sFileName.replace(".tif", "");
			
			if (sProductName.endsWith(".tif"))
				sProductName = sProductName.replace(".tif", "");
			
			if (sFileName.equals(sProductName))
				return oTileJson;
				 
			iTile ++;
		}
		
		return new JSONObject();
	}
	
	public QueryResultViewModel getResultViewModel(JSONObject oJsonResult) {
		
		JSONObject oProperties = oJsonResult.optJSONObject("properties");
		
		if (oProperties == null) {
			WasdiLog.warnLog("QueryExecutorDLR.getResultViewMolel. 'properties' field missing from json object");
			return null;
		}
		
		String sFileName = oProperties.optString("filename");
		String sId = oProperties.optString("id");
		String sSizeInBytes = oProperties.optString("filesize");
		double dSize = Double.parseDouble(sSizeInBytes);
		String sSize = Utils.isNullOrEmpty(sSizeInBytes) ? "0" : Utils.getNormalizedSize(dSize);
		
		String sLink = oProperties.optString("Download");
		
		JSONObject oGeometry = oJsonResult.optJSONObject("geometry");
		
		if (oGeometry == null) {
			WasdiLog.warnLog("QueryExecutorDLR.getResultViewModel. 'geometry' field missiong from json object");
			return null;
		}
		
		JSONArray aoCoordinates = oGeometry.optJSONArray("coordinates");
		
		if (aoCoordinates == null) {
			WasdiLog.warnLog("QueryExecutorDLR.getResultViewModel. 'coordinates' field missiong from json object");
			return null;
		}
		
		JSONArray aoOnePolygon = aoCoordinates.optJSONArray(0);
		
		if (aoOnePolygon == null) {
			WasdiLog.warnLog("QueryExecutorDLR.getResultViewModel. No poligons to describe the bounding box");
			return null;
		}
		
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
		
		String sSummary = "Date: 2019-01-01T00:00:00.000Z, Instrument: WSF, Mode: WSF, Satellite: WSF, Size: " + sSize;
		
		String sFileNameNoExtension = WasdiFileUtils.getFileNameWithoutLastExtension(sFileName);
		
		Map<String, String> oWasdiProperties = new HashMap<>();
		oWasdiProperties.put("fileName",sFileNameNoExtension);
		oWasdiProperties.put("id", sId);
		oWasdiProperties.put("filesize", sSizeInBytes);
		oWasdiProperties.put("download", sFileNameNoExtension);
		
		
		QueryResultViewModel oVM = new QueryResultViewModel();
		oVM.setFootprint(sFootprint);
		oVM.setId(sId);
		oVM.setLink(sLink + ";" + sSizeInBytes);
		oVM.setProvider("WSF");
		oVM.setTitle(WasdiFileUtils.getFileNameWithoutLastExtension(sFileNameNoExtension));
		oVM.setSummary(sSummary);
		oVM.setProperties(oWasdiProperties);
		
		return oVM;
		
		
	}


}
