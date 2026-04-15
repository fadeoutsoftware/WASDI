package wasdi.shared.queryexecutors.gpm;

import java.util.List;

import wasdi.shared.queryexecutors.ResponseTranslator;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.search.QueryResultViewModel;

/**
 * GPM is not a real catalogue. See QueryExecutorGPM for reason.
 * This class is just a placeholder
 * 
 * @author PetruPetrescu
 *
 */
public class ResponseTranslatorGPM extends ResponseTranslator {

	@Override
	public List<QueryResultViewModel> translateBatch(String sResponse, boolean bFullViewModel) {
		WasdiLog.debugLog("ResponseTranslatorGPM.translateBatch | sResponse: " + sResponse);

		return null;
	}

	@Override
	public int getCountResult(String sQueryResult) {
		WasdiLog.debugLog("ResponseTranslatorGPM.getCountResult | sQueryResult: " + sQueryResult);

		return 0;
	}

}
