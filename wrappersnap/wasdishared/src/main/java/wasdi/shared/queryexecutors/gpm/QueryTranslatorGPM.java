package wasdi.shared.queryexecutors.gpm;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.QueryTranslator;
import wasdi.shared.utils.Utils;

/**
 * Query Translator GPM.
 * 
 * GPM is not a real catalogue. See QueryExecutorGPM for reason.
 * This class is just a placeholder
 * 
 * @author PetruPetrescu
 *
 */
public class QueryTranslatorGPM extends QueryTranslator {

	@Override
	public String getCountUrl(String sQuery) {
		Utils.debugLog("QueryTranslatorGPM.getCountUrl | sQuery: " + sQuery);

		return null;
	}

	@Override
	public String getSearchUrl(PaginatedQuery oQuery) {
		Utils.debugLog("QueryTranslatorGPM.getSearchUrl | sQuery: " + oQuery.getQuery());

		return null;
	}

}
