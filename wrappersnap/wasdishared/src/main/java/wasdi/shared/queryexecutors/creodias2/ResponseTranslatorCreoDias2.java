package wasdi.shared.queryexecutors.creodias2;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.LinkedList;

import wasdi.shared.queryexecutors.ResponseTranslator;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;

public class ResponseTranslatorCreoDias2 extends ResponseTranslator {
	
	private static final String SODATA_PRODUCT_ID = "Id";
	private static final String SODATA_NAME = "Name";
	private static final String SODATA_DATE = "OriginDate";
	private static final String SODATA_ATTRIBUTES = "Attributes";
	private static final String SODATA_INSTRUMENT = "instrumentShortName";
	private static final String SODATA_MODE = "operationalMode";
	private static final String SODATA_SIZE = "ContentLength";
	private static final String SODATA_PLATFORM_SERIAL_ID = "platformSerialIdentifier";
	private static final String SODDATA_FOOTPRINT = "GeoFootprint";
	private static final String SODATA_COORDINATES = "coordinates";
	private static final String SODATA_TYPE = "type";
		
	
	@Override
	public List<QueryResultViewModel> translateBatch(String sResponse, boolean bFullViewModel) {
		List<QueryResultViewModel> aoResults = new LinkedList();
		
		if (Utils.isNullOrEmpty(sResponse)) 
			return aoResults;
		
		try {
			JSONObject oJson = new JSONObject(sResponse);
			JSONArray aoFeatures = oJson.optJSONArray("value");
			for (Object oItem : aoFeatures) {
				if(null != oItem) {
					JSONObject oJsonItem = (JSONObject)(oItem);
					processProduct(oJsonItem, bFullViewModel);

				}
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("ResponseTranslatorCREODIAS.translateBatch: " + oE);
		}
		
		return null;
	}
	
	private QueryResultViewModel processProduct(JSONObject oJsonItem, boolean bFullViewModel) {
//		Title -> Name of the file
//
//		Summary -> Description. Supports a sort of std like: “Date: 2021-12-25T18:25:03.242Z, Instrument: SAR, Mode: IW, Satellite: S1A, Size: 0.95 GB” but is not mandatory
//		Id -> Provider unique id
//		TODO: Link -> Link to download the file
//		TODO: Footprint -> Bounding box in WKT
//		Provider -> Provider used to get this info.
		
		// Name
	
		
		String sProductName =  oJsonItem.optString(SODATA_NAME);
		String sProductId = oJsonItem.optString(SODATA_PRODUCT_ID);
		String sDate = oJsonItem.optString(SODATA_DATE);
		int sSize = oJsonItem.optInt(SODATA_SIZE);
		
		System.out.println(sProductName);
		System.out.println(sProductId);
		System.out.println(sDate);
		
		JSONArray aoAttributes = oJsonItem.optJSONArray(SODATA_ATTRIBUTES);
		parseAttributes(aoAttributes);
		
		return null;
	}
	
	private void parseAttributes(JSONArray aoAttributes) {
		String sInstrument = "";
		String sMode = "";
		String sPlatformSerialId = "";
		for (Object oAtt : aoAttributes) {
			JSONObject oJsonAtt = (JSONObject) oAtt;
			sInstrument = getAttribute(oJsonAtt, SODATA_INSTRUMENT);
			sMode = getAttribute(oJsonAtt, SODATA_MODE);
			sPlatformSerialId = getAttribute(oJsonAtt, SODATA_PLATFORM_SERIAL_ID);
		}
	}
	
	private String getAttribute(JSONObject oJsonObject, String attributeName) {
		if (oJsonObject.get(SODATA_NAME).equals(attributeName))
			return oJsonObject.optString("Value");
		return "";
	}
	
	private void parseFootPrint(JSONObject oJsonObject) {
		JSONObject oFootprint = new JSONObject(oJsonObject.optString(SODDATA_FOOTPRINT));
		// TODO: manage the polYgon
		
		JSONArray aoCoordinates = (JSONArray) oFootprint.optJSONArray(SODATA_COORDINATES);
		String sCoordinates = parseCoordinates(aoCoordinates);
		
	}
	
	private String parseCoordinates(JSONArray aoCoordinates) {
		List<String> asCoordinates = new ArrayList<>();
		for (Object oItem: aoCoordinates) {
			if(oItem instanceof JSONArray) {
				JSONArray aoPoint = (JSONArray) oItem;
				Double dx = aoPoint.optDouble(0);
				Double dy = aoPoint.optDouble(1);
				if(!Double.isNaN(dx) && !Double.isNaN(dy) ){
					asCoordinates.add(dx + " " + dy);
				}
			}
		}
		return String.join(", ", asCoordinates);
	}
	
	
	


	@Override
	public int getCountResult(String sQueryResult) {
		int iCount = -1; 
		
		if (Utils.isNullOrEmpty(sQueryResult)) 
			return iCount;
		
		Map<String, Object> aoProperties = JsonUtils.jsonToMapOfObjects(sQueryResult);
		
		iCount = (Integer) aoProperties.get("@odata.count");
		
		return iCount;
	}

}