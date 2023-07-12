package wasdi.shared.queryexecutors.creodias2;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedList;

import wasdi.shared.queryexecutors.ResponseTranslator;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;

public class ResponseTranslatorCreoDias2 extends ResponseTranslator {
	
	// OData keywords
	private static final String SODATA_ATTRIBUTES = "Attributes";
	private static final String SODATA_DATE = "OriginDate";
	private static final String SODATA_FOOTPRINT = "GeoFootprint";
	private static final String SODATA_COORDINATES = "coordinates";
	private static final String SODATA_INSTRUMENT = "instrumentShortName";
	private static final String SODATA_MODE = "operationalMode";
	private static final String SODATA_NAME = "Name";
	private static final String SODATA_ORBIT_NUMBER = "orbitNumber";
	private static final String SODATA_ORBIT_DIRECTION  = "orbitDirection";
	private static final String SODATA_PLATFORM_SERIAL_ID = "platformSerialIdentifier";
	private static final String SODATA_PLATFORM_SHORT_NAME = "platformShortName";
	private static final String SODATA_POLARISATION  = "polarisationChannels";
	private static final String SODATA_POLYGON = "Polygon";
	private static final String SODATA_PRODUCT_ID = "Id";
	private static final String SODATA_PRODUCT_TYPE = "productType";
	private static final String SODATA_RELATIVE_ORBIT = "relativeOrbitNumber";
	private static final String SODATA_SIZE = "ContentLength";
	private static final String SODATA_TYPE = "type";
	private static final String SODATA_VALUE = "Value";
	
	private static final String SODATA_BASE_DOWNLOAD_URL = "https://datahub.creodias.eu/odata/v1/Products(";
	private static final String SODATA_END_DOWNLOAD_URL = ")/$value";
	
	// WASDI  keywords
	private static final String SMULTI_POLYGON = "MULTYPOLYGON";
	private static final String SPOLYGON = "POLYGON";
		
	
	@Override
	public List<QueryResultViewModel> translateBatch(String sResponse, boolean bFullViewModel) {
		List<QueryResultViewModel> aoResults = new LinkedList();
		
		if (Utils.isNullOrEmpty(sResponse)) {
			WasdiLog.debugLog("ResponseTranslatorCreoDias2.translateBatch: response string is null or empty.");
			return aoResults;
		}
		
		try {
			JSONObject oJson = new JSONObject(sResponse);
			JSONArray aoFeatures = oJson.optJSONArray("value");
			for (Object oItem : aoFeatures) {
				if(oItem != null) {
					JSONObject oJsonItem = (JSONObject)(oItem);
					aoResults.add(processProduct(oJsonItem, bFullViewModel));
				}
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("ResponseTranslatorCREODIAS.translateBatch: " + oE);
			return Collections.emptyList();
		}
		
		return aoResults;
	}
	
	private QueryResultViewModel processProduct(JSONObject oJsonItem, boolean bFullViewModel) {
		// BASIC INFO: title, summary, product id, link, footprint, provider id
		String sProductTitle =  oJsonItem.optString(SODATA_NAME);
		String sProductId = oJsonItem.optString(SODATA_PRODUCT_ID);
		String sLink = SODATA_BASE_DOWNLOAD_URL + sProductId + SODATA_END_DOWNLOAD_URL;
		String sFootprint = parseFootPrint(oJsonItem);
		String sDate = oJsonItem.optString(SODATA_DATE);
		String sSize = parseSize(oJsonItem);
		
		// some properties to add to the summary. TODO: decide which are the one we need
		JSONArray aoAttributes = oJsonItem.optJSONArray(SODATA_ATTRIBUTES);
		String sIntrument = getAttribute(aoAttributes, SODATA_INSTRUMENT);
		String sMode = getAttribute(aoAttributes, SODATA_MODE);
		String sPlatform = getAttribute(aoAttributes, SODATA_PLATFORM_SHORT_NAME);
		String sPlatformSerialId = getAttribute(aoAttributes, SODATA_PLATFORM_SERIAL_ID);
		String sRelativeOrbit = getAttribute(aoAttributes, SODATA_RELATIVE_ORBIT);
		
		String sPolarisation = getAttribute(aoAttributes, SODATA_POLARISATION);
		String sProductType = getAttribute(aoAttributes, SODATA_PRODUCT_TYPE);
		
		String sSummary = getSummary(sDate, sSize, sIntrument, sMode, sPlatform, sPlatformSerialId);
		
		QueryResultViewModel oResult = new QueryResultViewModel();
		setBasicInfo(oResult, sProductId, sProductTitle, sLink, sSummary, sFootprint);
		setProperties(oResult, aoAttributes);
		
		return oResult;
	}
	
	
	private void setProperties(QueryResultViewModel oViewModel, JSONArray aoAttributes) {
		Map<String, String> oMapProperties = oViewModel.getProperties();
		for (Object oAtt : aoAttributes) {
			JSONObject oJsonAtt = (JSONObject) oAtt;
			oMapProperties.put(oJsonAtt.optString(SODATA_NAME), oJsonAtt.optString(SODATA_VALUE));
		}
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
	
	
	
	private String parseSize(JSONObject oJsonObject) {
		double dByteSize = (double) oJsonObject.optInt(SODATA_SIZE, -1);
		return dByteSize > 0 
				? Utils.getNormalizedSize(dByteSize)
				: "";
	}
	
	
	private String getAttribute(JSONArray aoAttributes, String sAttributeName) {
		for (Object oAtt : aoAttributes) {
			JSONObject oJsonAtt = (JSONObject) oAtt;
			if (oJsonAtt.get(SODATA_NAME).equals(sAttributeName))
				return oJsonAtt.optString(SODATA_VALUE);
		}
		return "";
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
	
	
	private String parseFootPrint(JSONObject oJsonObject) {
		JSONObject oJsonFootprint = new JSONObject(oJsonObject.optString(SODATA_FOOTPRINT));
		
		String sType = oJsonFootprint.optString(SODATA_TYPE);
		
		if (sType.equals(SODATA_POLYGON)) {
			JSONArray aoCoordinates = (JSONArray) oJsonFootprint.optJSONArray(SODATA_COORDINATES);
			String sCoordinates = parseCoordinates(aoCoordinates);
			return SPOLYGON + " ((" + sCoordinates + "))";
		}
		// TODO: how to manage the multi-polygon? How I can simulate and example in Creodias?
		return "";	
	}
	
	private String parseCoordinates(JSONArray aoCoordinates) {
		List<String> asCoordinates = new ArrayList<>();
		for (Object oItem: aoCoordinates) {
			if(oItem instanceof JSONArray) {
				for (int i = 0; i < ((JSONArray) oItem).length(); i++) {
					JSONArray oValues = ((JSONArray) oItem).getJSONArray(i);
					Double dX = oValues.getDouble(0);
					Double dY = oValues.getDouble(1);
					if(!Double.isNaN(dX) && !Double.isNaN(dY) )
						asCoordinates.add(dX + " " + dY);
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
	
	public static void main(String[] args) throws Exception {
		System.out.println("here");
		String sJson = "{\"@odata.context\":\"$metadata#Products(Attributes())\",\"value\":[{\"@odata.mediaContentType\":\"application/octet-stream\",\"Id\":\"b01f1287-eba8-4faf-8600-f0733470b30a\",\"Name\":\"LC08_L1TP_195028_20230703_20230703_02_RT\",\"ContentType\":\"application/octet-stream\",\"ContentLength\":1369917440,\"OriginDate\":\"2023-07-03T10:16:32.529Z\",\"PublicationDate\":\"2023-07-05T10:52:09.616Z\",\"ModificationDate\":\"2023-07-05T10:52:09.616Z\",\"Online\":true,\"EvictionDate\":\"\",\"S3Path\":\"/eodata/Landsat-8/OLI_TIRS/L1TP/2023/07/03/LC08_L1TP_195028_20230703_20230703_02_RT\",\"Checksum\":[{}],\"ContentDate\":{\"Start\":\"2023-07-03T10:16:32.529Z\",\"End\":\"2023-07-03T10:16:32.529Z\"},\"Footprint\":\"geography'SRID=4326;POLYGON ((5.82802 47.07606, 5.94948 44.91108, 8.95943 44.95183, 8.95781 47.12, 5.82802 47.07606))'\",\"GeoFootprint\":{\"type\":\"Polygon\",\"coordinates\":[[[5.82802,47.07606],[5.94948,44.91108],[8.95943,44.95183],[8.95781,47.12],[5.82802,47.07606]]]},\"Attributes\":[{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"authority\",\"Value\":\"ESA\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"rowNumber\",\"Value\":28,\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.DoubleAttribute\",\"Name\":\"cloudCover\",\"Value\":29.0,\"ValueType\":\"Double\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"pathNumber\",\"Value\":195,\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"sensorType\",\"Value\":\"OPTICAL\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"composition\",\"Value\":\"LC\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"numberOfBands\",\"Value\":12,\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"acquisitionType\",\"Value\":\"NOMINAL\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"operationalMode\",\"Value\":\"DEFAULT\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"processingLevel\",\"Value\":\"LEVEL1TP\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"sunAzimuthAngle\",\"Value\":139.02850055,\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"platformShortName\",\"Value\":\"LANDSAT-8\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"spatialResolution\",\"Value\":100,\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"sunElevationAngle\",\"Value\":62.05285548,\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"collectionCategory\",\"Value\":\"RT\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"instrumentShortName\",\"Value\":\"OLI_TIRS\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"productType\",\"Value\":\"L1TP\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.DateTimeOffsetAttribute\",\"Name\":\"beginningDateTime\",\"Value\":\"2023-07-03T10:16:32.529Z\",\"ValueType\":\"DateTimeOffset\"},{\"@odata.type\":\"#OData.CSC.DateTimeOffsetAttribute\",\"Name\":\"endingDateTime\",\"Value\":\"2023-07-03T10:16:32.529Z\",\"ValueType\":\"DateTimeOffset\"}]},{\"@odata.mediaContentType\":\"application/octet-stream\",\"Id\":\"5d82719f-7c52-42bf-b6a3-28d7f27113b0\",\"Name\":\"LC08_L1TP_195029_20230703_20230703_02_RT\",\"ContentType\":\"application/octet-stream\",\"ContentLength\":1340897280,\"OriginDate\":\"2023-07-03T10:16:56.415Z\",\"PublicationDate\":\"2023-07-05T10:54:38.185Z\",\"ModificationDate\":\"2023-07-05T10:54:38.185Z\",\"Online\":true,\"EvictionDate\":\"\",\"S3Path\":\"/eodata/Landsat-8/OLI_TIRS/L1TP/2023/07/03/LC08_L1TP_195029_20230703_20230703_02_RT\",\"Checksum\":[{}],\"ContentDate\":{\"Start\":\"2023-07-03T10:16:56.415Z\",\"End\":\"2023-07-03T10:16:56.415Z\"},\"Footprint\":\"geography'SRID=4326;POLYGON ((5.34363 45.6494, 5.47694 43.48494, 8.41456 43.53775, 8.39235 45.70634, 5.34363 45.6494))'\",\"GeoFootprint\":{\"type\":\"Polygon\",\"coordinates\":[[[5.34363,45.6494],[5.47694,43.48494],[8.41456,43.53775],[8.39235,45.70634],[5.34363,45.6494]]]},\"Attributes\":[{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"authority\",\"Value\":\"ESA\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"rowNumber\",\"Value\":29,\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.DoubleAttribute\",\"Name\":\"cloudCover\",\"Value\":18.0,\"ValueType\":\"Double\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"pathNumber\",\"Value\":195,\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"sensorType\",\"Value\":\"OPTICAL\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"composition\",\"Value\":\"LC\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"numberOfBands\",\"Value\":12,\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"acquisitionType\",\"Value\":\"NOMINAL\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"operationalMode\",\"Value\":\"DEFAULT\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"processingLevel\",\"Value\":\"LEVEL1TP\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"sunAzimuthAngle\",\"Value\":136.47892546,\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"platformShortName\",\"Value\":\"LANDSAT-8\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"spatialResolution\",\"Value\":100,\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"sunElevationAngle\",\"Value\":62.90773812,\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"collectionCategory\",\"Value\":\"RT\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"instrumentShortName\",\"Value\":\"OLI_TIRS\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"productType\",\"Value\":\"L1TP\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.DateTimeOffsetAttribute\",\"Name\":\"beginningDateTime\",\"Value\":\"2023-07-03T10:16:56.415Z\",\"ValueType\":\"DateTimeOffset\"},{\"@odata.type\":\"#OData.CSC.DateTimeOffsetAttribute\",\"Name\":\"endingDateTime\",\"Value\":\"2023-07-03T10:16:56.415Z\",\"ValueType\":\"DateTimeOffset\"}]},{\"@odata.mediaContentType\":\"application/octet-stream\",\"Id\":\"140a0cf4-0f06-4885-83d8-e1f7c8d1f419\",\"Name\":\"LC08_L1TP_195030_20230703_20230703_02_RT\",\"ContentType\":\"application/octet-stream\",\"ContentLength\":1154795520,\"OriginDate\":\"2023-07-03T10:17:20.302Z\",\"PublicationDate\":\"2023-07-05T10:43:14.011Z\",\"ModificationDate\":\"2023-07-05T10:43:14.011Z\",\"Online\":true,\"EvictionDate\":\"\",\"S3Path\":\"/eodata/Landsat-8/OLI_TIRS/L1TP/2023/07/03/LC08_L1TP_195030_20230703_20230703_02_RT\",\"Checksum\":[{}],\"ContentDate\":{\"Start\":\"2023-07-03T10:17:20.302Z\",\"End\":\"2023-07-03T10:17:20.302Z\"},\"Footprint\":\"geography'SRID=4326;POLYGON ((4.8737 44.22096, 5.01711 42.05443, 7.8907 42.11824, 7.85063 44.28977, 4.8737 44.22096))'\",\"GeoFootprint\":{\"type\":\"Polygon\",\"coordinates\":[[[4.8737,44.22096],[5.01711,42.05443],[7.8907,42.11824],[7.85063,44.28977],[4.8737,44.22096]]]},\"Attributes\":[{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"authority\",\"Value\":\"ESA\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"rowNumber\",\"Value\":30,\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.DoubleAttribute\",\"Name\":\"cloudCover\",\"Value\":6.0,\"ValueType\":\"Double\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"pathNumber\",\"Value\":195,\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"sensorType\",\"Value\":\"OPTICAL\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"composition\",\"Value\":\"LC\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"numberOfBands\",\"Value\":12,\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"acquisitionType\",\"Value\":\"NOMINAL\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"operationalMode\",\"Value\":\"DEFAULT\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"processingLevel\",\"Value\":\"LEVEL1TP\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"sunAzimuthAngle\",\"Value\":133.80062295,\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"platformShortName\",\"Value\":\"LANDSAT-8\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"spatialResolution\",\"Value\":100,\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"sunElevationAngle\",\"Value\":63.71449917,\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"collectionCategory\",\"Value\":\"RT\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"instrumentShortName\",\"Value\":\"OLI_TIRS\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"productType\",\"Value\":\"L1TP\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.DateTimeOffsetAttribute\",\"Name\":\"beginningDateTime\",\"Value\":\"2023-07-03T10:17:20.302Z\",\"ValueType\":\"DateTimeOffset\"},{\"@odata.type\":\"#OData.CSC.DateTimeOffsetAttribute\",\"Name\":\"endingDateTime\",\"Value\":\"2023-07-03T10:17:20.302Z\",\"ValueType\":\"DateTimeOffset\"}]},{\"@odata.mediaContentType\":\"application/octet-stream\",\"Id\":\"11861c22-7ca7-490b-9ead-d5cf085ed106\",\"Name\":\"LC08_L1TP_193028_20230705_20230705_02_RT\",\"ContentType\":\"application/octet-stream\",\"ContentLength\":1347430400,\"OriginDate\":\"2023-07-05T10:04:12.130Z\",\"PublicationDate\":\"2023-07-11T00:14:20.059Z\",\"ModificationDate\":\"2023-07-11T00:14:20.059Z\",\"Online\":true,\"EvictionDate\":\"\",\"S3Path\":\"/eodata/Landsat-8/OLI_TIRS/L1TP/2023/07/05/LC08_L1TP_193028_20230705_20230705_02_RT\",\"Checksum\":[{}],\"ContentDate\":{\"Start\":\"2023-07-05T10:04:12.130Z\",\"End\":\"2023-07-05T10:04:12.130Z\"},\"Footprint\":\"geography'SRID=4326;POLYGON ((9.01318 47.09301, 9.01268 44.97614, 11.95182 44.93798, 12.06652 47.05194, 9.01318 47.09301))'\",\"GeoFootprint\":{\"type\":\"Polygon\",\"coordinates\":[[[9.01318,47.09301],[9.01268,44.97614],[11.95182,44.93798],[12.06652,47.05194],[9.01318,47.09301]]]},\"Attributes\":[{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"authority\",\"Value\":\"ESA\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"rowNumber\",\"Value\":28,\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.DoubleAttribute\",\"Name\":\"cloudCover\",\"Value\":25.0,\"ValueType\":\"Double\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"pathNumber\",\"Value\":193,\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"sensorType\",\"Value\":\"OPTICAL\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"composition\",\"Value\":\"LC\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"numberOfBands\",\"Value\":12,\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"acquisitionType\",\"Value\":\"NOMINAL\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"operationalMode\",\"Value\":\"DEFAULT\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"processingLevel\",\"Value\":\"LEVEL1TP\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"sunAzimuthAngle\",\"Value\":139.06558545,\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"platformShortName\",\"Value\":\"LANDSAT-8\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"spatialResolution\",\"Value\":100,\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"sunElevationAngle\",\"Value\":61.86329262,\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"collectionCategory\",\"Value\":\"RT\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"instrumentShortName\",\"Value\":\"OLI_TIRS\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"productType\",\"Value\":\"L1TP\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.DateTimeOffsetAttribute\",\"Name\":\"beginningDateTime\",\"Value\":\"2023-07-05T10:04:12.130Z\",\"ValueType\":\"DateTimeOffset\"},{\"@odata.type\":\"#OData.CSC.DateTimeOffsetAttribute\",\"Name\":\"endingDateTime\",\"Value\":\"2023-07-05T10:04:12.130Z\",\"ValueType\":\"DateTimeOffset\"}]},{\"@odata.mediaContentType\":\"application/octet-stream\",\"Id\":\"c5f7d19a-f469-4200-b7bd-82ffecb10909\",\"Name\":\"LC08_L1TP_193029_20230705_20230705_02_RT\",\"ContentType\":\"application/octet-stream\",\"ContentLength\":1300039680,\"OriginDate\":\"2023-07-05T10:04:36.017Z\",\"PublicationDate\":\"2023-07-11T00:35:47.820Z\",\"ModificationDate\":\"2023-07-11T00:35:47.820Z\",\"Online\":true,\"EvictionDate\":\"\",\"S3Path\":\"/eodata/Landsat-8/OLI_TIRS/L1TP/2023/07/05/LC08_L1TP_193029_20230705_20230705_02_RT\",\"Checksum\":[{}],\"ContentDate\":{\"Start\":\"2023-07-05T10:04:36.017Z\",\"End\":\"2023-07-05T10:04:36.017Z\"},\"Footprint\":\"geography'SRID=4326;POLYGON ((8.5237 45.66916, 8.54074 43.54643, 11.41411 43.52186, 11.50357 45.64271, 8.5237 45.66916))'\",\"GeoFootprint\":{\"type\":\"Polygon\",\"coordinates\":[[[8.5237,45.66916],[8.54074,43.54643],[11.41411,43.52186],[11.50357,45.64271],[8.5237,45.66916]]]},\"Attributes\":[{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"authority\",\"Value\":\"ESA\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"rowNumber\",\"Value\":29,\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.DoubleAttribute\",\"Name\":\"cloudCover\",\"Value\":22.0,\"ValueType\":\"Double\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"pathNumber\",\"Value\":193,\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"sensorType\",\"Value\":\"OPTICAL\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"composition\",\"Value\":\"LC\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"numberOfBands\",\"Value\":12,\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"acquisitionType\",\"Value\":\"NOMINAL\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"operationalMode\",\"Value\":\"DEFAULT\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"processingLevel\",\"Value\":\"LEVEL1TP\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"sunAzimuthAngle\",\"Value\":136.53544422,\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"platformShortName\",\"Value\":\"LANDSAT-8\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"spatialResolution\",\"Value\":100,\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"sunElevationAngle\",\"Value\":62.71918202,\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"collectionCategory\",\"Value\":\"RT\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"instrumentShortName\",\"Value\":\"OLI_TIRS\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"productType\",\"Value\":\"L1TP\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.DateTimeOffsetAttribute\",\"Name\":\"beginningDateTime\",\"Value\":\"2023-07-05T10:04:36.017Z\",\"ValueType\":\"DateTimeOffset\"},{\"@odata.type\":\"#OData.CSC.DateTimeOffsetAttribute\",\"Name\":\"endingDateTime\",\"Value\":\"2023-07-05T10:04:36.017Z\",\"ValueType\":\"DateTimeOffset\"}]},{\"@odata.mediaContentType\":\"application/octet-stream\",\"Id\":\"cef12a18-84d8-4934-a77e-27c50bcb50b2\",\"Name\":\"LC08_L1TP_193030_20230705_20230705_02_RT\",\"ContentType\":\"application/octet-stream\",\"ContentLength\":1088471040,\"OriginDate\":\"2023-07-05T10:04:59.904Z\",\"PublicationDate\":\"2023-07-11T00:14:05.737Z\",\"ModificationDate\":\"2023-07-11T00:14:05.737Z\",\"Online\":true,\"EvictionDate\":\"\",\"S3Path\":\"/eodata/Landsat-8/OLI_TIRS/L1TP/2023/07/05/LC08_L1TP_193030_20230705_20230705_02_RT\",\"Checksum\":[{}],\"ContentDate\":{\"Start\":\"2023-07-05T10:04:59.904Z\",\"End\":\"2023-07-05T10:04:59.904Z\"},\"Footprint\":\"geography'SRID=4326;POLYGON ((8.05072 44.24029, 8.08309 42.11454, 10.89512 42.10255, 10.96199 44.22738, 8.05072 44.24029))'\",\"GeoFootprint\":{\"type\":\"Polygon\",\"coordinates\":[[[8.05072,44.24029],[8.08309,42.11454],[10.89512,42.10255],[10.96199,44.22738],[8.05072,44.24029]]]},\"Attributes\":[{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"authority\",\"Value\":\"ESA\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"rowNumber\",\"Value\":30,\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.DoubleAttribute\",\"Name\":\"cloudCover\",\"Value\":7.0,\"ValueType\":\"Double\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"pathNumber\",\"Value\":193,\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"sensorType\",\"Value\":\"OPTICAL\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"composition\",\"Value\":\"LC\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"numberOfBands\",\"Value\":12,\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"acquisitionType\",\"Value\":\"NOMINAL\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"operationalMode\",\"Value\":\"DEFAULT\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"processingLevel\",\"Value\":\"LEVEL1TP\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"sunAzimuthAngle\",\"Value\":133.87859552,\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"platformShortName\",\"Value\":\"LANDSAT-8\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"spatialResolution\",\"Value\":100,\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"sunElevationAngle\",\"Value\":63.52734423,\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"collectionCategory\",\"Value\":\"RT\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"instrumentShortName\",\"Value\":\"OLI_TIRS\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.StringAttribute\",\"Name\":\"productType\",\"Value\":\"L1TP\",\"ValueType\":\"String\"},{\"@odata.type\":\"#OData.CSC.DateTimeOffsetAttribute\",\"Name\":\"beginningDateTime\",\"Value\":\"2023-07-05T10:04:59.904Z\",\"ValueType\":\"DateTimeOffset\"},{\"@odata.type\":\"#OData.CSC.DateTimeOffsetAttribute\",\"Name\":\"endingDateTime\",\"Value\":\"2023-07-05T10:04:59.904Z\",\"ValueType\":\"DateTimeOffset\"}]}]}";
		ResponseTranslatorCreoDias2 oRT = new ResponseTranslatorCreoDias2();
		List<QueryResultViewModel> aoRes = oRT.translateBatch(sJson, true);
		for (QueryResultViewModel oVM : aoRes) {
			System.out.println("Product id: " + oVM.getId());
			System.out.println("Product name: " + oVM.getTitle());
			System.out.println("Download Link: " + oVM.getLink());
			System.out.println("Footprint: " + oVM.getFootprint());
			System.out.println("Provider: "+ oVM.getProvider());
			System.out.println("Summary: " + oVM.getSummary());
			System.out.println();
		}
		
	}

}