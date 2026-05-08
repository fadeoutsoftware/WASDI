package wasdi.shared.queryexecutors.viirs;

import wasdi.shared.queryexecutors.QueryTranslator;
import wasdi.shared.queryexecutors.PaginatedQuery;

/**
 * Query Translator VIIRS.
 * 
 * VIIRS is not a real catalogue. See QueryExecutorVIIRS for reason.
 * This class is just a placeholder
 * 
 * @author p.campanella
 *
 */
public class QueryTranslatorVIIRS extends QueryTranslator {
	
	@Override
	public String getCountUrl(String oQuery) {
		return "";
	}

	@Override
	public String getSearchUrl(PaginatedQuery oQuery) {
		return"";
	}

}
