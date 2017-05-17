package it.fadeout.opensearch;

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

}
