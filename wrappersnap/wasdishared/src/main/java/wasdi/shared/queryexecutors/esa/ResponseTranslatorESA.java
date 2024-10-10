package wasdi.shared.queryexecutors.esa;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

import wasdi.shared.queryexecutors.ResponseTranslator;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;

public class ResponseTranslatorESA extends ResponseTranslator {

	public ResponseTranslatorESA() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public List<QueryResultViewModel> translateBatch(String sResponse, boolean bFullViewModel) {
		
		if (Utils.isNullOrEmpty(sResponse)) {
			WasdiLog.debugLog("ResponseTranslatorESA.translateBatch. Response is null or empty");
			return null;
		}
		
		JSONObject oJSONResponse = new JSONObject(sResponse);
		
		JSONArray aoFeatureArray = oJSONResponse.optJSONArray("features");
		
		if (aoFeatureArray == null) {
			WasdiLog.debugLog("ResponseTranslatorESA.translateBatch. No 'features' entry found in the response");
			return null;
		}
		
		List<QueryResultViewModel> aoResults = new LinkedList<>();
		
		try {
			for (Object oItem : aoFeatureArray) {
				if (oItem != null) {
					JSONObject oJsonItem = (JSONObject) (oItem);
					aoResults.add(processProduct(oJsonItem, bFullViewModel));
	
				}
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("ResponseTranslatorESA.translateBatch. Exception when translating the results in the Wasdi view model ", oEx);
		}
		
		return null;
	}
	
	private QueryResultViewModel processProduct(JSONObject oJsonItem, boolean bFullViewModel) {
		return null;
	}

	@Override
	public int getCountResult(String sQueryResult) {
		// TODO Auto-generated method stub
		return 0;
	}

}
