/**
 * Created by Cristiano Nattero on 2020-01-21
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch.sobloo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.base.Preconditions;

import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.QueryResultViewModel;

/**
 * @author c.nattero
 *
 */
public class DiasResponseTranslatorSOBLOO implements DiasResponseTranslator {


	@Override
	public List<QueryResultViewModel> translateBatch(String sJson, boolean bFullViewModel, String sDownloadProtocol) {
		Preconditions.checkNotNull(sJson, "DiasResponseTranslatorSOBLOO.translateBatch: sJson is null" );

		List<QueryResultViewModel> aoResults = new ArrayList<>();

		//isolate single entry, pass it to translate and append the result
		try {
			JSONObject oJson = new JSONObject(sJson);
			JSONArray aoHits = oJson.optJSONArray("hits");
			for (Object oItem : aoHits) {
				if(null!=oItem) {
					JSONObject oJsonItem = (JSONObject)(oItem);
					QueryResultViewModel oViewModel = translate(oJsonItem, sDownloadProtocol, bFullViewModel);
					if(null != oViewModel) {
						aoResults.add(oViewModel);
					}
				}
			}
		} catch (Exception oE) {
			Utils.debugLog("DiasResponseTranslatorSOBLOO.translateBatch: " + oE);
		}
		return aoResults;
	}

	private QueryResultViewModel translate(JSONObject oJsonItem, String sDownloadProtocol, boolean bFullViewModel) {
		Preconditions.checkNotNull(oJsonItem, "DiasResponseTranslatorSOBLOO.translate: JSONObject is null");

		QueryResultViewModel oResult = new QueryResultViewModel();
		oResult.setProvider("SOBLOO");
		parseMd(oJsonItem, oResult);
		parseData(oJsonItem, oResult);

		finalizeViewModel(oResult);
		return oResult;
	}


	private void parseMd(JSONObject oJsonItem, QueryResultViewModel oResult) {
		Preconditions.checkNotNull(oJsonItem, "DiasResponseTranslatorSOBLOO.parseMd: JSONObject is null");

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
					oResult.getProperties().put("timestamp", sBuffer);
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
			Utils.debugLog("DiasResponseTranslatorSOBLOO.parseMd( " + oJsonItem + ", QueryResultViewModel )" + oE);
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
					Utils.debugLog("DiasResponseTranslatorSOBLOO.parseCoordinates: " + oE);
				}

			}
			while(sCoordinates.endsWith(", ")) {
				sCoordinates = sCoordinates.substring(0, sCoordinates.length() - 2);
			}
			sCoordinates = "MULTIPOLYGON (((" + sCoordinates + ")))";
			oResult.setFootprint(sCoordinates);
		} catch (Exception oE) {
			Utils.debugLog("DiasResponseTranslatorSOBLOO.parseCoordinates: " + oE);
		}
	}

	private void parseData(JSONObject oJsonItem, QueryResultViewModel oResult) {
		Preconditions.checkNotNull(oJsonItem, "DiasResponseTranslatorSOBLOO.parseData: JSONObject is null");

		try {
			if(oJsonItem.has("data")) {
				oJsonItem = oJsonItem.optJSONObject("data");
				String sBuffer = null;
				int iBuffer = 0;
				long lBuffer = 0;

				if(oJsonItem.has("identification")) {
					JSONObject oJsonIdentification = oJsonItem.optJSONObject("identification");
					if(oJsonIdentification != null) {
						sBuffer = oJsonIdentification.optString("externalId", null);
						if(!Utils.isNullOrEmpty(sBuffer)) {
							oResult.setTitle(sBuffer);
						} else {
							throw new NullPointerException("DiasResponseTranslatorSOBLOO.parseData: could not find file name in the response, failing");
						}
					}
				}

				if(oJsonItem.has("archive")) {
					JSONObject oJsonArchive = oJsonItem.optJSONObject("archive");
					if(null!=oJsonArchive) {
						iBuffer = 0;
						iBuffer = oJsonArchive.optInt("size", 0);
						oResult.getProperties().put("size", Utils.getNormalizedSize((double)iBuffer, "MB"));
						oResult.getProperties().put("content-length", ""+((long)iBuffer)*1024*1024);
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
			}
		} catch (Exception oE) {
			Utils.debugLog("DiasResponseTranslatorSobloo.parseData: " + oE);
		}	
	}

	private void addStringToProperties(JSONObject oJson, String sKey, QueryResultViewModel oResult) {
		Preconditions.checkNotNull(oJson);
		Preconditions.checkNotNull(sKey);
		Preconditions.checkNotNull(oResult);

		String sBuffer = null;
		sBuffer = oJson.optString(sKey, null);
		if(!Utils.isNullOrEmpty(sBuffer)) {
			oResult.getProperties().put(sKey, sBuffer);
		}

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


		long lBuffer = -1;
		lBuffer = oJson.optLong(sKey, -1);
		if(0<lBuffer) {
			oResult.getProperties().put(sKey, ""+lBuffer);
		}
	}

	private void finalizeViewModel(QueryResultViewModel oResult) {
		Preconditions.checkNotNull(oResult, "DiasResponseTranslatorSOBLOO: QueryResultViewModel is null");

		String sDate = oResult.getProperties().get("timestamp");
		sDate = "" + Utils.fromTimestampToDate(Long.parseLong(sDate));
		
		String sSummary = "Date: " + sDate + ", ";
		String sInstrument = oResult.getProperties().get("sensorId");
		sSummary = sSummary + "Instrument: " + sInstrument + ", ";
		String sMode = oResult.getProperties().get("sensorMode");
		sSummary = sSummary + "Mode: " + sMode + ", ";

		String sSatellite = oResult.getProperties().get("missionName");
		sSummary = sSummary + "Satellite: " + sSatellite + ", ";
		String sSize = oResult.getProperties().get("size");
		sSummary = sSummary + "Size: " + sSize;// + " " + sChosenUnit;
		oResult.setSummary(sSummary);

		String sLink = "https://sobloo.eu/api/v1/services/download/" + oResult.getId();
		oResult.setLink(sLink);

		sSize = oResult.getProperties().get("content-length");
		sLink += "|||fileName=" + oResult.getTitle() + "|||size=" + sSize;
		oResult.getProperties().put("hackedLink", sLink);

	}

}
