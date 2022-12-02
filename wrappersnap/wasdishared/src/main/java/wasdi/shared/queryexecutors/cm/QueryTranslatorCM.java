package wasdi.shared.queryexecutors.cm;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.QueryTranslator;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Query Translator CopernicusMarine.
 * 
 * CM is not a real catalogue. See QueryExecutorCM for reason.
 * This class is just a placeholder
 * 
 * @author PetruPetrescu
 *
 */
public class QueryTranslatorCM extends QueryTranslator {

	@Override
	public String getCountUrl(String sQuery) {
		WasdiLog.debugLog("QueryTranslatorCM.getCountUrl | sQuery: " + sQuery);

		return null;
	}

	@Override
	public String getSearchUrl(PaginatedQuery oQuery) {
		WasdiLog.debugLog("QueryTranslatorCM.getSearchUrl | sQuery: " + oQuery.getQuery());

		return null;
	}


}
