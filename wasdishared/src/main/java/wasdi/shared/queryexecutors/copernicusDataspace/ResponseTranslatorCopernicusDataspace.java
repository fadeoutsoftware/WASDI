package wasdi.shared.queryexecutors.copernicusDataspace;

import wasdi.shared.queryexecutors.odata.ResponseTranslatorOData;

public class ResponseTranslatorCopernicusDataspace extends ResponseTranslatorOData {


	public ResponseTranslatorCopernicusDataspace() {
		m_sODataBaseDownloadURL = "https://download.dataspace.copernicus.eu/odata/v1/Products(";
		m_sProvider = "COPERNICUS_DATASPACE";
	}



}
