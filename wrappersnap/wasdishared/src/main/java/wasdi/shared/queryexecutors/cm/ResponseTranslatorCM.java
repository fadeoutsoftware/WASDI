package wasdi.shared.queryexecutors.cm;

import java.util.List;

import wasdi.shared.queryexecutors.ResponseTranslator;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;

/**
 * CopernicusMarine is not a real catalogue. See QueryExecutorCM for reason.
 * This class is just a placeholder
 * 
 * @author PetruPetrescu
 *
 */
public class ResponseTranslatorCM extends ResponseTranslator {

	@Override
	public List<QueryResultViewModel> translateBatch(String sResponse, boolean bFullViewModel) {
		WasdiLog.debugLog("ResponseTranslatorCM.translateBatch | sResponse: " + sResponse);

		return null;
	}

	@Override
	public int getCountResult(String sQueryResult) {
		WasdiLog.debugLog("ResponseTranslatorCM.getCountResult | sQueryResult: " + sQueryResult);

		return 1;
	}

}
