package wasdi.shared.queryexecutors.copernicusDataspace;

import wasdi.shared.queryexecutors.odata.QueryExecutorOData;

public class QueryExecutorCopernicusDataspace extends QueryExecutorOData {

	public QueryExecutorCopernicusDataspace() {
		this.m_oQueryTranslator = new QueryTranslatorCopernicusDataspace();
		this.m_oResponseTranslator = new ResponseTranslatorCopernicusDataspace();
	}
	

	
}
