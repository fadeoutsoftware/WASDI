package wasdi.shared.queryexecutors.ads;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.QueryTranslator;

/**
 * Query Translator ADS.
 * 
 * ADS is not a real catalogue. See QueryExecutorADS for reason.
 * This class is just a placeholder
 * 
 * @author PetruPetrescu
 *
 */
public class QueryTranslatorADS extends QueryTranslator {

	@Override
	public String getCountUrl(String sQuery) {
		return "";
	}

	@Override
	public String getSearchUrl(PaginatedQuery oQuery) {
		return "";
	}

}
