package wasdi.shared.queryexecutors.skywatch;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.QueryTranslator;

/**
 * Query Translator Skywatch.
 * 
 * Skywatch is not a real catalogue. See QueryExecutorSkywatch for reason.
 * This class is just a placeholder
 * 
 * @author PetruPetrescu
 *
 */
public class QueryTranslatorSkywatch extends QueryTranslator {

	@Override
	public String getCountUrl(String sQuery) {
		return "";
	}

	@Override
	public String getSearchUrl(PaginatedQuery oQuery) {
		return "";
	}

}
