package wasdi.dataproviders;

import java.util.Map;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.log.WasdiLog;

public class CDS2ProviderAdapter extends PythonBasedProviderAdapter {

	public CDS2ProviderAdapter() {
		super();
		m_sDataProviderCode = "CDS";
	}
	
	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		WasdiLog.debugLog("CDS2ProviderAdapter.getDownloadFileSize: download file size not available in CDS");
		return 0;
	}
	
	
	public static void main(String [] args) throws Exception {
		String sPayload = "https://cds.climate.copernicus.eu/api/v2/resources?payload={\"date\":\"20240730\",\"boundingBox\":\"54.523555732794485,+23.475552944622965,+51.40035647150239,+29.805568688028007\",\"variables\":\"RH\",\"presureLevels\":\"1000\",\"format\":\"netcdf\",\"dataset\":\"reanalysis-era5-pressure-levels\",\"monthlyAggregation\":\"false\",\"productType\":\"reanalysis\"}";
		System.out.println(sPayload.split("payload=")[1]);
		
		WasdiConfig.readConfig("C:/temp/wasdi/wasdiLocalTESTConfig.json");
		
		ProcessWorkspace oPW = new ProcessWorkspace();
		oPW.setProductName("test_CDS.py");
		
		CDS2ProviderAdapter oAdapter = new CDS2ProviderAdapter();
		oAdapter.readConfig();
		oAdapter.executeDownloadFile(sPayload, 
				"valentina.leone@wasdi.cloud", 
				"password", 
				"C:/Users/valentina.leone/Downloads", 
				oPW, 
				2);
	}
}
