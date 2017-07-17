package wasdi.shared.opensearch;

import org.apache.abdera.i18n.templates.Template;

public class QueryExecutorMATERA extends QueryExecutor {

	@Override
	protected String[] getUrlPath() {
		return new String[] {"search"};
	}

	@Override
	protected Template getTemplate() {
		return new Template("{scheme}://{-append|.|host}collaborative.mt.asi.it{-opt|/|path}{-listjoin|/|path}{-prefix|/|page}{-opt|?|q}{-join|&|q,start,rows,orderby}");
	}

	@Override
	protected String getCountUrl(String sQuery) {
		return "http://collaborative.mt.asi.it/api/stub/products/count?filter=" + sQuery;
	}

}
