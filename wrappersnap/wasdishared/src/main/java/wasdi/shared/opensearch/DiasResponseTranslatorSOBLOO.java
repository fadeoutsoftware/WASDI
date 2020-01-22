/**
 * Created by Cristiano Nattero on 2020-01-21
 * 
 * Fadeout software
 *
 */
package wasdi.shared.opensearch;

import java.util.ArrayList;
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
		// TODO Auto-generated method stub
		Preconditions.checkNotNull(oJsonItem, "DiasResponseTranslatorSOBLOO.translate: JSONObject is null");
		
		QueryResultViewModel oResult = new QueryResultViewModel();
		parseMd(oJsonItem, oResult);
		parseData(oJsonItem, oResult);
		
		return null;
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
				JSONArray aDCooordinatesPair = (JSONArray) oItem; 
				double dX = aDCooordinatesPair.optDouble(0);
				double dY = aDCooordinatesPair.optDouble(1);
				
			}
			
		} catch (Exception oE) {
			Utils.debugLog("DiasResponseTranslatorSOBLOO.parseCoordinates: " + oE);
		}
	}

	private void parseData(JSONObject oJsonItem, QueryResultViewModel oResult) {
		Preconditions.checkNotNull(oJsonItem, "DiasResponseTranslatorSOBLOO.parseMd: JSONObject is null");
		//TODO implement
	}
	
}
