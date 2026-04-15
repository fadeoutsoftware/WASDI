package wasdi.shared.queryexecutors;

/**
 * Concrete but "empty" implementation of a Query Translator.
 * The goal is to be able to use the parseWasdiQuery method without
 * a specific query translator
 * @author p.campanella
 *
 */
public class ConcreteQueryTranslator extends QueryTranslator {

	@Override
	public String getCountUrl(String sQuery) {
		return null;
	}

	@Override
	public String getSearchUrl(PaginatedQuery oQuery) {
		return null;
	}

}
