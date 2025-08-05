package wasdi.shared.queryexecutors.creodias2;

import wasdi.shared.queryexecutors.odata.QueryExecutorOData;


public class QueryExecutorCreoDias2 extends QueryExecutorOData {
	
	
	public QueryExecutorCreoDias2() {
		this.m_oQueryTranslator = new QueryTranslatorCreoDias2();
		this.m_oResponseTranslator = new ResponseTranslatorCreoDias2();
	}

	
}
