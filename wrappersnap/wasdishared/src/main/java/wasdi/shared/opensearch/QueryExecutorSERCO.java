package wasdi.shared.opensearch;

import org.apache.abdera.i18n.templates.Template;

public class QueryExecutorSERCO extends QueryExecutor {

	public QueryExecutorSERCO() {
		m_sProvider = "SERCO";
	}
	
	@Override
	protected Template getTemplate() {
		return new Template("{scheme}://{-append|.|host}early.onda-dias.eu{-opt|/|path}{-listjoin|/|path}{-prefix|/|page}{-opt|?|q}{-join|&|q,start,rows,orderby}");
	}

	@Override	
	protected String[] getUrlPath() {
		return new String[] {"catalogue","search"};
	}

	@Override
	protected String getUrlSchema() {
		return "http";
	}

	@Override
	protected String getCountUrl(String sQuery) {
		return "http://early.onda-dias.eu/catalogue/count?filter=" + sQuery;
	}
	
}
