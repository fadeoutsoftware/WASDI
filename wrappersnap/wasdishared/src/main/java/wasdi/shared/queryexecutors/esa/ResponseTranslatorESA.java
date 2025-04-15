package wasdi.shared.queryexecutors.esa;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

import wasdi.shared.queryexecutors.ResponseTranslator;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;

public class ResponseTranslatorESA extends ResponseTranslator {

	public ResponseTranslatorESA() {
		super();
	}

	@Override
	public List<QueryResultViewModel> translateBatch(String sResponse, boolean bFullViewModel) {
		
		if (Utils.isNullOrEmpty(sResponse)) {
			WasdiLog.debugLog("ResponseTranslatorESA.translateBatch. Response is null or empty");
			return null;
		}
		
		JSONObject oJSONResponse = new JSONObject(sResponse);
		
		JSONArray aoFeatureArray = oJSONResponse.optJSONArray("features");
		
		List<QueryResultViewModel> aoResults = null;

		if (aoFeatureArray == null) {
			WasdiLog.debugLog("ResponseTranslatorESA.translateBatch. No 'features' entry found in the response.");
			// might be a single result
			QueryResultViewModel oResultVM = processProduct(oJSONResponse, bFullViewModel); 
			if (oResultVM != null) {
				aoResults = Arrays.asList(oResultVM);
				return aoResults;
			}
			return null;
		}
			
		try {
			for (Object oItem : aoFeatureArray) {
				if (oItem != null) {
					JSONObject oJsonItem = (JSONObject) (oItem);
					QueryResultViewModel oResultVM = processProduct(oJsonItem, bFullViewModel); 
					if (oResultVM != null) {
						if (aoResults == null) aoResults = new ArrayList<>();
						aoResults.add(oResultVM);
					}
				}
			}
			
			return aoResults;
		} catch (Exception oEx) {
			WasdiLog.errorLog("ResponseTranslatorESA.translateBatch. Exception when translating the results in the Wasdi view model ", oEx);
		}
		
		return null;
	}
	
	private QueryResultViewModel processProduct(JSONObject oJsonItem, boolean bFullViewModel) {
		try {
			// BASIC INFO
			String sProductId = oJsonItem.optString("id");
			String sProductTitle = oJsonItem.optString("title");
			if (Utils.isNullOrEmpty(sProductTitle))
				sProductTitle = sProductId;
			String sLink = getDownloadLink(oJsonItem); 
			String sFootprint = getFootprint(oJsonItem);
			
			// INFO FOR THE SUMMARY: date, size, instrument, mode, platform, platform serial id
			JSONObject oJsonAcquisitionInfo = oJsonItem.optJSONObject("acquisitionInformation");
			String sBeginningDateTime = "";
			String sMode = "";
			String sInstrumentShortName = "";
			String sPlatformName = "";
			String sPlatformSerialIdentifier = "";
			
			if (oJsonAcquisitionInfo != null) {
				JSONObject oJsonAcquisitionParameters = oJsonAcquisitionInfo.optJSONObject("acquisitionParameters");
				
				if (oJsonAcquisitionInfo != null) {
					sBeginningDateTime = oJsonAcquisitionParameters.getString("beginningDateTime");
					sMode = oJsonAcquisitionParameters.getString("operationalMode");
					JSONObject oJsonInstrument = oJsonItem.optJSONObject("instrument");
					
					if (oJsonInstrument != null) {
						sInstrumentShortName = oJsonInstrument.getString("instumentShortName");
					}
					
					JSONObject oJsonPlatform = oJsonItem.optJSONObject("platform");
					sPlatformName = oJsonPlatform.optString("platfromShortName", "");
					sPlatformSerialIdentifier = oJsonItem.optString("platformSerialIdentifier", "");
				}		
			}
			
			String sSummary = getSummary(sBeginningDateTime, "0B", sInstrumentShortName, sMode, sPlatformName, sPlatformSerialIdentifier);
			
			QueryResultViewModel oResult = new QueryResultViewModel();
			setBasicInfo(oResult, sProductId, sProductTitle, sLink, sSummary, sFootprint);
			setBasicProperties(oResult, sBeginningDateTime, sPlatformName, sPlatformSerialIdentifier, sInstrumentShortName, sMode, "0B");
			if (bFullViewModel)
				setAllProviderProperties(oResult, oJsonItem);
			return oResult;
		} catch (Exception oEx) {
			WasdiLog.errorLog("ResponseTranslatorESA.processProduct. Error", oEx);
		}

		return null;
	}
	
	private String getDownloadLink(JSONObject oJsonItem) {
		JSONArray aoLinks = oJsonItem.optJSONArray("links");
		String sDownloadLink = "https://payload={}"; 
		String sTitle = "";
		
		for (int i = 0; i < aoLinks.length(); i++) {
			JSONObject oItem = aoLinks.getJSONObject(i);
			sTitle = oItem.optString("title");
			if (!Utils.isNullOrEmpty(sTitle) && sTitle.equalsIgnoreCase("Download")) {
				String sHref = oItem.getString("href");
				sDownloadLink = "https://payload={\"url\": \"" + sHref + "\"}";
				break;
			}
			if (!sDownloadLink.contains("url")) {
				String sHref = oItem.optString("href");
				if (!Utils.isNullOrEmpty(sHref)) {
					sDownloadLink = "https://payload={\"url\": \"" + sHref + "\"}";
					break;
				}
			}
		}
		return sDownloadLink;
	}
	
	private String getFootprint(JSONObject oJsonItem) {
		JSONObject oJsonGeometry = oJsonItem.getJSONObject("geometry");
		String sGeometryType = oJsonGeometry.optString("type");
		
		if (Utils.isNullOrEmpty(sGeometryType)) {
			WasdiLog.debugLog("ResponseTranslatorESA.getFootprint. Can not determine geometry type");
			return null;
		}
		
		if (sGeometryType.equalsIgnoreCase("Polygon")) {
			// "coordinates": [ [ [30.0, 10.0], [40.0, 40.0], [20.0, 40.0], [10.0, 20.0], [30.0, 10.0] ] ]
			JSONArray aoCoordinates = oJsonGeometry.getJSONArray("coordinates");
			String sPolygonBorders = parsePolygon(aoCoordinates);
			return "POLYGON(" + String.join(",", sPolygonBorders) + ")";
		}
		else if (sGeometryType.equalsIgnoreCase("MultiPolygon")) {
			// "coordinates": [ [ [ [30.0, 20.0], [45.0, 40.0], [10.0, 40.0], [30.0, 20.0] ] ], [ [ [15.0, 5.0], [40.0, 10.0], [10.0, 20.0], [5.0, 10.0], [15.0, 5.0] ] ] ]
			JSONArray aoCoordinates = oJsonGeometry.getJSONArray("coordinates");
			List<String> asPolygons = new ArrayList<>();
			for (int i = 0; i < aoCoordinates.length(); i++) {
				JSONArray aoPolygons = aoCoordinates.getJSONArray(i);
				
				for (int j = 0; j < aoPolygons.length(); i++) {
					JSONArray oPolygon = aoPolygons.getJSONArray(i);
					String sPolygon = parsePolygonBorders(oPolygon);
					asPolygons.add("(" + sPolygon + ")");
				}
			}
			return "MULTIPOLYGON(" + String.join(", ", asPolygons) + ")";
		}
		return null;
	}
	
	private String parsePolygon(JSONArray aoCoordinates ) {
		List<String> asPolygonBorders = new ArrayList<>();
		
		for (int i = 0; i < aoCoordinates.length(); i++) {
			// [ [30.0, 10.0], [40.0, 40.0], [20.0, 40.0], [10.0, 20.0], [30.0, 10.0] ]
			JSONArray oPolygonBoders = aoCoordinates.getJSONArray(i); 
			String sBorder = parsePolygonBorders(oPolygonBoders);
			asPolygonBorders.add(sBorder);
		}
		
		return String.join(", ", asPolygonBorders);
	}
	
	private String parsePolygonBorders(JSONArray aoJsonPolygon) {
		// [ [30.0, 10.0], [40.0, 40.0], [20.0, 40.0], [10.0, 20.0], [30.0, 10.0] ]
		List<String> asCoordinates = new ArrayList<>();
		
		for (int i = 0; i < aoJsonPolygon.length(); i++) {
			JSONArray adCoordinates = aoJsonPolygon.getJSONArray(i);
			asCoordinates.add(adCoordinates.getDouble(0) + " " + adCoordinates.getDouble(1));
		}
		
		return "(" + String.join(", ", asCoordinates) + ")";
		
		
	}
	private String getSummary(String sDate, String sSize, String sInstrument, String sMode, String sPlatform, String sPlatformSerialId) {
		List<String> asRes = new ArrayList<>();
		if (!Utils.isNullOrEmpty(sDate))
			asRes.add("Date: " + sDate);
		if (!Utils.isNullOrEmpty(sInstrument))
			asRes.add("Instrument: " + sInstrument);
		if (!Utils.isNullOrEmpty(sMode))
			asRes.add("Mode: " + sMode);
		if (!Utils.isNullOrEmpty(sPlatform))
			asRes.add("Satellite: " + sPlatform + (!Utils.isNullOrEmpty(sPlatformSerialId) ? sPlatformSerialId : ""));
		if (!Utils.isNullOrEmpty(sSize))
			asRes.add("Size: " + sSize);
		return String.join(", ", asRes);
		
	}
	
	
	private void setBasicInfo(QueryResultViewModel oViewModel, String sProductId, String sProductTitle, String sLink, String sSummary, String sFootprint) {
		oViewModel.setProvider("CREODIAS2");
		
		if (!Utils.isNullOrEmpty(sProductId))
		oViewModel.setId(sProductId);
		
		if (!Utils.isNullOrEmpty(sProductTitle))
			oViewModel.setTitle(sProductTitle);
		
		if (!Utils.isNullOrEmpty(sLink))
			oViewModel.setLink(sLink);
		
		if (!Utils.isNullOrEmpty(sSummary))
			oViewModel.setSummary(sSummary);
		
		if (!Utils.isNullOrEmpty(sFootprint))
			oViewModel.setFootprint(sFootprint);
	}
	
	private void setBasicProperties(QueryResultViewModel oViewModel, String sDate, String sPlatform, String sPlatformSerialId, String sInstrument, String sMode, String sSize) {
		Map<String, String> oMapProperties = oViewModel.getProperties();
		
		if (!Utils.isNullOrEmpty(sDate))
			oMapProperties.put("date", sDate);
		
		if (!Utils.isNullOrEmpty(sPlatform))
			oMapProperties.put("platformname", sPlatform + (!Utils.isNullOrEmpty(sPlatformSerialId) ? sPlatformSerialId : "") );
		
		if (!Utils.isNullOrEmpty(sInstrument))
			oMapProperties.put("instrumentshortname", sInstrument);
		
		if (!Utils.isNullOrEmpty(sMode))
			oMapProperties.put("sensoroperationalmode", sMode);
		
		if (!Utils.isNullOrEmpty(sSize))
			oMapProperties.put("size", sSize);		
	}
	
	private void setAllProviderProperties(QueryResultViewModel oViewModel, JSONObject oJsonItem) {
		Map<String, String> oMapProperties = oViewModel.getProperties();
		JSONObject oProperties = oJsonItem.getJSONObject("properties");
		
		if (!Utils.isNullOrEmpty(oProperties.optString("id")))
			oMapProperties.putIfAbsent("kind", oProperties.getString("id"));
		
		if (!Utils.isNullOrEmpty(oProperties.optString("collection")))
			oMapProperties.putIfAbsent("collection", oProperties.getString("collection"));
		
		if (!Utils.isNullOrEmpty(oProperties.optString("title")))
			oMapProperties.putIfAbsent("title", oProperties.getString("title"));
		
		if (!Utils.isNullOrEmpty(oProperties.optString("updated")))
			oMapProperties.putIfAbsent("updated", oProperties.getString("updated"));
		
		if (!Utils.isNullOrEmpty(oProperties.optString("status")))			
			oMapProperties.putIfAbsent("status", oProperties.getString("status"));
		
		JSONObject oProductInformation = oProperties.optJSONObject("productInformation");
		if (oProductInformation != null) {		
			Iterator<String> oProductInfoIterator = oProductInformation.keys();
			while(oProductInfoIterator.hasNext()) {
				String sKey = oProductInfoIterator.next();
				oMapProperties.putIfAbsent(sKey, oProductInformation.getString(sKey));
			}
		}
		
		JSONArray aoAcquisitionInformation = oProperties.optJSONArray("acquisitionInformation");
		if (aoAcquisitionInformation != null) {
			for (int i = 0; i < aoAcquisitionInformation.length(); i++) {
				JSONObject oItem = aoAcquisitionInformation.getJSONObject(i);
				Iterator<String> oAcquisitionInfoIteratorr = oItem.keys();
				while (oAcquisitionInfoIteratorr.hasNext()) {
					String sKey = oAcquisitionInfoIteratorr.next();
					JSONObject oSubItem = oItem.getJSONObject(sKey);
					if (oSubItem != null) {
						setPropertiesFromJSONObject(oMapProperties, oSubItem);
					}
				}
			}
		}
		
	}
	
	public void setPropertiesFromJSONObject(Map<String, String> oMapProperties, JSONObject oJsonObjectProperties) {
		Map<String, Object> oProductProperties = oJsonObjectProperties.toMap();
		for (String sKey : oProductProperties.keySet()) {
			oMapProperties.put(sKey, oProductProperties.get(sKey).toString());
		}
	}

	@Override
	public int getCountResult(String sQueryResult) {
		// TODO Auto-generated method stub
		return 0;
	}

}
