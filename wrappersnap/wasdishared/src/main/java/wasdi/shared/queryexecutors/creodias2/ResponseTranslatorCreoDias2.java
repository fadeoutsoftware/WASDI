package wasdi.shared.queryexecutors.creodias2;

import java.util.List;
import java.util.Map;

import wasdi.shared.queryexecutors.ResponseTranslator;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.viewmodels.search.QueryResultViewModel;

public class ResponseTranslatorCreoDias2 extends ResponseTranslator {

	@Override
	public List<QueryResultViewModel> translateBatch(String sResponse, boolean bFullViewModel) {
		// TODO Auto-generated method stub
		return null;
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