package wasdi.shared.queryexecutors.creodias2;

import wasdi.shared.queryexecutors.odata.ResponseTranslatorOData;

public class ResponseTranslatorCreoDias2 extends ResponseTranslatorOData {
	
		
	public ResponseTranslatorCreoDias2() {
		m_sODataBaseDownloadURL = "https://zipper.creodias.eu/odata/v1/Products(";
		m_sProvider = "CREODIAS2";
	}
	
}