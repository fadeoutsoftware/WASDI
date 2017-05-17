package it.fadeout.opensearch;

import org.apache.abdera.i18n.templates.Template;

public class QueryExecutorFEDEO extends QueryExecutor {

	@Override
	protected Template getTemplate() {
		return new Template("{scheme}://{-append|.|host}fedeo.esa.int{-opt|/|path}{-listjoin|/|path}{-prefix|/|page}{-opt|?|q}{-join|&|q,start,rows,orderby}");
	}

	@Override	
	protected String[] getUrlPath() {
		return new String[] {"opensearch","request"};
	}

	
}
