package wasdi.shared.queryexecutors.skywatch;

import java.util.List;

import wasdi.shared.queryexecutors.ResponseTranslator;
import wasdi.shared.viewmodels.search.QueryResultViewModel;

/**
 * Skywatch is not a real catalogue. See QueryExecutorSkywatch for reason.
 * This class is just a placeholder
 * 
 * @author PetruPetrescu
 *
 */
public class ResponseTranslatorSkywatch extends ResponseTranslator {

	@Override
	public List<QueryResultViewModel> translateBatch(String sResponse, boolean bFullViewModel) {
		return null;
	}

	@Override
	public int getCountResult(String sQueryResult) {
		return 0;
	}

}
