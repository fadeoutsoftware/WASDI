/**
 * Created by Cristiano Nattero on 2020-01-21
 * 
 * Fadeout software
 *
 */
package wasdi.shared.queryexecutors.sobloo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.base.Preconditions;

import wasdi.shared.queryexecutors.ResponseTranslator;
import wasdi.shared.utils.TimeEpochUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;

/**
 * @author c.nattero
 *
 */
public class ResponseTranslatorSOBLOO extends ResponseTranslator {

	private static Map<String, String> s_asKeyMap;
	
	static{
		s_asKeyMap = new HashMap<>();
		
		s_asKeyMap.put("polarization", "polarisationmode");
		s_asKeyMap.put("direction", "orbitdirection");
		s_asKeyMap.put("uid", "uuid");
		s_asKeyMap.put("type", "producttype");
		s_asKeyMap.put("sensorId", "instrumentshortname");
		s_asKeyMap.put("sensorMode", "sensoroperationalmode");
		s_asKeyMap.put("missionName", "platformname");
		
	}

	@Override
	public List<QueryResultViewModel> translateBatch(String sJson, boolean bFullViewModel) {
		Preconditions.checkNotNull(sJson, "ResponseTranslatorSOBLOO.translateBatch: sJson is null" );

		List<QueryResultViewModel> aoResults = new ArrayList<>();

		//isolate single entry, pass it to translate and append the result
		try {
			JSONObject oJson = new JSONObject(sJson);
			JSONArray aoHits = oJson.optJSONArray("hits");
			for (Object oItem : aoHits) {
				if(null!=oItem) {
					JSONObject oJsonItem = (JSONObject)(oItem);
					QueryResultViewModel oViewModel = translate(oJsonItem, bFullViewModel);
					if(null != oViewModel) {
						aoResults.add(oViewModel);
					}
				}
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("ResponseTranslatorSOBLOO.translateBatch: " + oE);
		}
		return aoResults;
	}

	private QueryResultViewModel translate(JSONObject oJsonItem, boolean bFullViewModel) {
		Preconditions.checkNotNull(oJsonItem, "ResponseTranslatorSOBLOO.translate: JSONObject is null");

		QueryResultViewModel oResult = new QueryResultViewModel();
		oResult.setProvider("SOBLOO");
		parseMd(oJsonItem, oResult);
		parseData(oJsonItem, oResult);

		finalizeViewModel(oResult);
		return oResult;
	}


	private void parseMd(JSONObject oJsonItem, QueryResultViewModel oResult) {
		Preconditions.checkNotNull(oJsonItem, "ResponseTranslatorSOBLOO.parseMd: JSONObject is null");

		try {
			if(oJsonItem.has("md")) {
				oJsonItem = oJsonItem.optJSONObject("md");
				String sBuffer = null;
				long lBuffer = -1l;
				JSONObject oJsonBuffer = null;
				JSONArray aoArrayBuffer = null;

				sBuffer = oJsonItem.optString("id", null);
				if(!Utils.isNullOrEmpty(sBuffer)) {
					oResult.setId(sBuffer);
				}

				lBuffer = oJsonItem.optLong("timestamp", -1l);
				sBuffer = "" + lBuffer;
				if(!Utils.isNullOrEmpty(sBuffer)) {
					addToProperties(oResult, getStandardKey("timestamp"), sBuffer);
				}

				oJsonBuffer = oJsonItem.optJSONObject("geometry");
				if(oJsonBuffer != null) {
					sBuffer = oJsonBuffer.optString("type", null);
					if(null!=sBuffer && sBuffer.equals("Polygon")) {
						aoArrayBuffer = oJsonBuffer.optJSONArray("coordinates");
						if(aoArrayBuffer != null) {
							parseCoordinates(aoArrayBuffer, oResult);
						}
					}
				}

			}
		} catch (Exception oE) {
			WasdiLog.debugLog("ResponseTranslatorSOBLOO.parseMd( " + oJsonItem + ", QueryResultViewModel )" + oE);
		}

	}

	private void parseCoordinates(JSONArray aoArrayBuffer, QueryResultViewModel oResult) {
		try {
			aoArrayBuffer = (JSONArray) aoArrayBuffer.opt(0);
			String sCoordinates = "";
			for (Object oItem : aoArrayBuffer) {
				try{
					JSONArray aDCooordinatesPair = (JSONArray) oItem; 
					double dX = aDCooordinatesPair.optDouble(0);
					double dY = aDCooordinatesPair.optDouble(1);
					sCoordinates += dX + " " + dY + ", ";
				} catch (Exception oE) {
					WasdiLog.debugLog("ResponseTranslatorSOBLOO.parseCoordinates: " + oE);
				}

			}
			while(sCoordinates.endsWith(", ")) {
				sCoordinates = sCoordinates.substring(0, sCoordinates.length() - 2);
			}
			sCoordinates = "MULTIPOLYGON (((" + sCoordinates + ")))";
			oResult.setFootprint(sCoordinates);
		} catch (Exception oE) {
			WasdiLog.debugLog("ResponseTranslatorSOBLOO.parseCoordinates: " + oE);
		}
	}

	private void parseData(JSONObject oJsonItem, QueryResultViewModel oResult) {
		Preconditions.checkNotNull(oJsonItem, "ResponseTranslatorSOBLOO.parseData: JSONObject is null");

		try {
			if(oJsonItem.has("data")) {
				oJsonItem = oJsonItem.optJSONObject("data");
				String sBuffer = null;
				int iBuffer = 0;
				
				if(oJsonItem.has("uid")) {
					sBuffer = oJsonItem.optString("uid", null);
					if(!Utils.isNullOrEmpty(sBuffer)){
						addToProperties(oResult, getStandardKey("uuid"), sBuffer);
					}
				}

				if(oJsonItem.has("identification")) {
					JSONObject oJsonIdentification = oJsonItem.optJSONObject("identification");
					if(oJsonIdentification != null) {
						sBuffer = oJsonIdentification.optString("externalId", null);
						if(!Utils.isNullOrEmpty(sBuffer)) {
							oResult.setTitle(sBuffer);
						} else {
							throw new NullPointerException("ResponseTranslatorSOBLOO.parseData: could not find file name in the response, failing");
						}
						
						sBuffer = oJsonIdentification.optString("type", null);
						if(!Utils.isNullOrEmpty(sBuffer)) {
							addToProperties(oResult, getStandardKey("type"), sBuffer);
						}
					}
				}

				if(oJsonItem.has("archive")) {
					JSONObject oJsonArchive = oJsonItem.optJSONObject("archive");
					if(null!=oJsonArchive) {
						iBuffer = 0;
						iBuffer = oJsonArchive.optInt("size", 0);
						addToProperties(oResult, getStandardKey("size"), Utils.getNormalizedSize((double)iBuffer, "MB"));
						addToProperties(oResult, getStandardKey("content-length"), ""+((long)iBuffer)*1024*1024);
					}
				}

				if(oJsonItem.has("acquisition")){
					JSONObject oJsonAcquisition = oJsonItem.optJSONObject("acquisition");
					Iterator<String> oKeys = oJsonAcquisition.keys();
					while(oKeys.hasNext()) {
						String sKey = oKeys.next();
						if(!Utils.isNullOrEmpty(sKey)) {
							switch (sKey) {
							case "endViewingDate":
								addPositiveLongToProperties(oJsonAcquisition, sKey, oResult);
								break;
							case "beginViewingDate":
								addPositiveLongToProperties(oJsonAcquisition, sKey, oResult);
								break;
							default:
								addStringToProperties(oJsonAcquisition, sKey, oResult);
								break;
							}
						}
					}
				}
				
				if(oJsonItem.has("orbit")){
					try {
						JSONObject oOrbit = oJsonItem.optJSONObject("orbit");
						if(null!=oOrbit) {
							iBuffer = oOrbit.optInt("relativeNumber", -1);
							if(iBuffer >= 0) {
								addToProperties(oResult, getStandardKey("relativeorbitnumber"), ""+iBuffer);
							}
							sBuffer = oOrbit.optString("direction", null);
							if(!Utils.isNullOrEmpty(sBuffer)) {
								addToProperties(oResult, getStandardKey("direction"), sBuffer);
							}
						}
					}catch (Exception oE) {
						WasdiLog.debugLog("ResponseTranslatorSOBLOO.parseData: " + oE);
					}
				}
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("ResponseTranslatorSOBLOO.parseData: " + oE);
		}	
	}

	private void addStringToProperties(JSONObject oJson, String sKey, QueryResultViewModel oResult) {
		Preconditions.checkNotNull(oJson);
		Preconditions.checkNotNull(sKey);
		Preconditions.checkNotNull(oResult);

		String sBuffer = null;
		sBuffer = oJson.optString(sKey, null);
		if(!Utils.isNullOrEmpty(sBuffer)) {
			addToProperties(oResult, getStandardKey(sKey), sBuffer);
		}

	}

	private String getStandardKey(String sKey) {
		Preconditions.checkNotNull(sKey, "ResponseTranslatorSOBLOO.getProviderKey: null key passed");
		Preconditions.checkArgument(!sKey.isEmpty(), "ResponseTranslatorSOBLOO.getProviderKey: empty key passed");
		
		String sResult = sKey;
		try {
			if(s_asKeyMap.containsKey(sKey)) {
				sResult = s_asKeyMap.get(sKey);
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("ResponseTranslatorSOBLOO.getProviderKey: " + oE);
			sResult = sKey;
		}
		return sResult;
	}

	/**
	 * @param oJson
	 * @param sKey
	 * @param oResult
	 */
	protected void addPositiveLongToProperties(JSONObject oJson, String sKey, QueryResultViewModel oResult) {
		Preconditions.checkNotNull(oJson);
		Preconditions.checkNotNull(sKey);
		Preconditions.checkNotNull(oResult);

		try {
			long lBuffer = -1;
			lBuffer = oJson.optLong(sKey, -1);
			if(0<lBuffer) {
				addToProperties(oResult, getStandardKey(sKey), ""+lBuffer);
			}
		} catch (Exception oE) {
			WasdiLog.debugLog("ResponseTranslatorSOBLOO.addPositiveLongToProperties: " + oE);
		}
	}

	private void finalizeViewModel(QueryResultViewModel oResult) {
		Preconditions.checkNotNull(oResult, "ResponseTranslatorSOBLOO.finalizeViewModel: QueryResultViewModel is null");

		String sDate = "";
		try {
			long lBeginViewingDate = Long.parseLong(oResult.getProperties().get("beginViewingDate"));
			sDate = TimeEpochUtils.fromEpochToDateString(lBeginViewingDate);
			addToProperties(oResult, "ingestiondate", sDate);
			addToProperties(oResult, "beginposition", sDate);
		} catch (Exception oE) {
			WasdiLog.debugLog("ResponseTranslatorSOBLOO.finalizeViewModel: " + oE);
		}
		
		String sSummary = "Date: " + sDate + ", ";
		String sInstrument = oResult.getProperties().get("instrumentshortname");
//		addToProperties(oResult, getStandardKey("sensorId"), sInstrument);
		
		sSummary = sSummary + "Instrument: " + sInstrument + ", ";
		String sMode = oResult.getProperties().get("sensoroperationalmode");
//		addToProperties(oResult, "sensorMode", sMode);
		sSummary = sSummary + "Mode: " + sMode + ", ";

		String sSatellite = oResult.getProperties().get("platformname");
		sSummary = sSummary + "Satellite: " + sSatellite + ", ";
		
		String sSize = oResult.getProperties().get("size");
		addToProperties(oResult, "size", sSize);
		
		sSummary = sSummary + "Size: " + sSize;// + " " + sChosenUnit;
		oResult.setSummary(sSummary);

		String sLink = "https://sobloo.eu/api/v1/services/download/" + oResult.getId();
		addToProperties(oResult, "originalLink", sLink);

		sSize = oResult.getProperties().get("content-length");
		sLink += "|||fileName=" + oResult.getTitle() + "|||size=" + sSize;
		addToProperties(oResult, "hackedLink", sLink);
		oResult.setLink(sLink);
		
		addToProperties(oResult, "filename", oResult.getTitle());
		String sMission = oResult.getProperties().get("missionName");
		addToProperties(oResult, getStandardKey("missionName"), sMission);

	}

	@Override
	public int getCountResult(String sQueryResult) {
		int iResult = 0;
		try {
			JSONObject oJson = new JSONObject(sQueryResult);
			String sResult = "" + oJson.optInt("totalnb", -1);
			
			iResult = Integer.parseInt(sResult);
			
		} catch (NumberFormatException oNfe) {
			WasdiLog.debugLog("QueryExecutor.executeCount: the response ( " + sQueryResult + " ) was not an int: " + oNfe);
			return -1;
		}
		return iResult;
	}

}
