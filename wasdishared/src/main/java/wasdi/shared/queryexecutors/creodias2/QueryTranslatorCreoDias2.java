package wasdi.shared.queryexecutors.creodias2;

import wasdi.shared.queryexecutors.odata.QueryTranslatorOData;

public class QueryTranslatorCreoDias2 extends QueryTranslatorOData {
	
	public QueryTranslatorCreoDias2() {
		super();
		this.m_sApiBaseUrl = "https://datahub.creodias.eu/odata/v1/Products?";
	}
	
	
}
