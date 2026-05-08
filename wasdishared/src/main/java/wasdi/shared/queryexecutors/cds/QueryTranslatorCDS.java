package wasdi.shared.queryexecutors.cds;

import wasdi.shared.queryexecutors.PaginatedQuery;
import wasdi.shared.queryexecutors.QueryTranslator;

/**
 * Query Translator CDS.
 * 
 * CDS is not a real catalogue. See QueryExecutorCDS for reason.
 * This class is just a placeholder
 * 
 * @author PetruPetrescu
 *
 */
public class QueryTranslatorCDS extends QueryTranslator {

	@Override
	public String getCountUrl(String sQuery) {
		return "";
	}

	@Override
	public String getSearchUrl(PaginatedQuery oQuery) {
		return "";
	}

}
