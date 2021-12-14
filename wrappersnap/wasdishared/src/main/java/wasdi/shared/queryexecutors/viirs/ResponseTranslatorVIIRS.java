package wasdi.shared.queryexecutors.viirs;

import java.util.List;

import wasdi.shared.queryexecutors.ResponseTranslator;
import wasdi.shared.viewmodels.search.QueryResultViewModel;

/**
 * VIIRS is not a real catalogue. See QueryExecutorVIIRS for reason.
 * This class is just a placeholder
 * 
 * @author p.campanella
 *
 */
public class ResponseTranslatorVIIRS extends ResponseTranslator {

	@Override
	public List<QueryResultViewModel> translateBatch(String sJson, boolean bFullViewModel) {
		return null;
	}

	@Override
	public int getCountResult(String sQueryResult) {
		return 0;
	}

}
