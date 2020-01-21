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
		return null;
	}

}
