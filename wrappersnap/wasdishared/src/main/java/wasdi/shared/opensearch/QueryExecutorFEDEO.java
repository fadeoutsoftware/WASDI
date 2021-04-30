package wasdi.shared.opensearch;

import org.apache.abdera.i18n.templates.Template;

import wasdi.shared.utils.Utils;

public class QueryExecutorFEDEO extends QueryExecutor {

	QueryExecutorFEDEO(){
		Utils.debugLog("QueryExecutorFEDEO");
		m_sProvider = "FEDEO";
		m_oQueryTranslator = new DiasQueryTranslator();
	}
	
	@Override
	protected Template getTemplate() {
		return new Template("{scheme}://{-append|.|host}fedeo.esa.int{-opt|/|path}{-listjoin|/|path}{-prefix|/|page}{-opt|?|q}{-join|&|q,start,rows,orderby}");
	}

	@Override	
	protected String[] getUrlPath() {
		return new String[] {"opensearch","request"};
	}

	@Override
	protected String getCountUrl(String sQuery) {
		return null;
	}

	
}
