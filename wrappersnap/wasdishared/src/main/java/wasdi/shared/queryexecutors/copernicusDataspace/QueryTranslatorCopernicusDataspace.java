package wasdi.shared.queryexecutors.copernicusDataspace;

import wasdi.shared.queryexecutors.odata.QueryTranslatorOData;

public class QueryTranslatorCopernicusDataspace extends QueryTranslatorOData {

	public QueryTranslatorCopernicusDataspace() {
		super();
		this.m_sApiBaseUrl = "https://catalogue.dataspace.copernicus.eu/odata/v1/Products?";
	}

}
