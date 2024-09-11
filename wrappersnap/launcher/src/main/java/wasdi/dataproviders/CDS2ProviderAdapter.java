package wasdi.dataproviders;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import org.apache.commons.io.FileUtils;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.cds.CDSUtils;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.runtime.RunTimeUtils;
import wasdi.shared.utils.runtime.ShellExecReturn;

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
	
	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {
		
		String sResultDownloadedFilePath = null;
		
		String sInputFullPath = "";
		String sOutputFullPath = "";
		
		WasdiLog.debugLog("PythonBasedProviderAdapter.executeDownloadFile: product name " + oProcessWorkspace.getProductName());	
		
		try {		
			// let's add the additional information to the json passed to the Python data provider
			Map<String, Object> oInputMap = new HashMap<String, Object>();
			Map<String, String> oWasdiPayloadMap = fromWasdiPayloadToStringMap(sFileURL);		
			
			String sCDSDataset = oWasdiPayloadMap.get("dataset");
			
			if (Utils.isNullOrEmpty(sCDSDataset)) {				
				WasdiLog.warnLog("CDS2ProviderAdapter.executeDownloadFile: information about the CDS dataset not available in WASDI payload");
				return null;
			}
			
			oInputMap.put("dataset", sCDSDataset);
			
			Map<String, Object> oCDSPayloadMap = CDSProviderAdapter.prepareCdsPayload(oWasdiPayloadMap);
			
			oInputMap.put("cds_payload", oCDSPayloadMap);
			
			addOperativeParametersToWasdiPayload(oInputMap, sSaveDirOnServer, oProcessWorkspace.getProductName(), iMaxRetry);
			
			String sEnrichedJsonParameters = JsonUtils.stringify(oInputMap);
			
			// get the parameters to pass to the shell execute
			List<String> asArgs = getCommandLineArgsForDownload(sEnrichedJsonParameters);
			
			if (asArgs == null) {
				WasdiLog.errorLog("CDS2ProviderAdapter.executeDownloadFile: not arguments for shellExec");
			}
			
			if (asArgs.size() < 5) {
				WasdiLog.errorLog("CDS2ProviderAdapter.executeDownloadFile: not enough args");
			}
			
			sInputFullPath = asArgs.get(2);
			sOutputFullPath = asArgs.get(3);
			
			WasdiLog.debugLog("CDS2ProviderAdapter: input file path " + sInputFullPath);
			WasdiLog.debugLog("CDS2ProviderAdapter: output file path " + sOutputFullPath);
			
			ShellExecReturn oShellExecReturn = RunTimeUtils.shellExec(asArgs, true, true, true, true);
			
			WasdiLog.debugLog("CDS2ProviderAdapter.executeDownloadFile: python output = " + oShellExecReturn.getOperationLogs());;
			
			File oOutputFile = new File(sOutputFullPath);
			
			if (!oOutputFile.exists()) {
				WasdiLog.warnLog("CDS2ProviderAdapter.executeDownloadFile: impossible to read the output file of the Python data provider");
			}
			
			WasdiLog.debugLog("CDS2ProviderAdapter.executeDownloadFile: got output file form the Python data provider " + oOutputFile.getAbsolutePath());
			
			// the output will simply be the path of the downloaded file
			JSONObject oJsonOutput = JsonUtils.loadJsonFromFile(sOutputFullPath);
			String sDownloadedFilePath = oJsonOutput.optString("outputFile");
			
			if (!Utils.isNullOrEmpty(sDownloadedFilePath)) {
				WasdiLog.debugLog("CDS2ProviderAdapter.executeDownloadFile: path to the downloaded file " + sDownloadedFilePath);
				sResultDownloadedFilePath = sDownloadedFilePath;
			} else {
				WasdiLog.errorLog("CDS2ProviderAdapter.executeDownloadFile: path to the downloaded file is null or empty");
			}
			
		} catch(Exception oEx) {
			WasdiLog.errorLog("CDS2ProviderAdapter.executeDonwloadFile: error ", oEx);
		} finally {
			if (!Utils.isNullOrEmpty(sInputFullPath))
				FileUtils.deleteQuietly(new File(sInputFullPath));
			if (!Utils.isNullOrEmpty(sOutputFullPath))
				FileUtils.deleteQuietly(new File(sOutputFullPath));
		}
		
		return sResultDownloadedFilePath;
	}
	
	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
		if (sPlatformType.equals(Platforms.ERA5)) {
			return DataProviderScores.DOWNLOAD.getValue();
		}

		return 0;
	}
	
	@Override
	public String getFileName(String sFileURL) throws Exception {
		if (Utils.isNullOrEmpty(sFileURL)) return "";

		Map<String, String> aoWasdiPayload = fromWasdiPayloadToStringMap(sFileURL);
		if (aoWasdiPayload == null) {
			return null;
		}
		
		String sDataset = JsonUtils.getProperty(aoWasdiPayload, "dataset");
		String sVariables = JsonUtils.getProperty(aoWasdiPayload, "variables");
		String sDate =  JsonUtils.getProperty(aoWasdiPayload, "date");
		String sStartDate = JsonUtils.getProperty(aoWasdiPayload, "startDate");
		String sEndDate = JsonUtils.getProperty(aoWasdiPayload, "endDate");
		String sFormat = JsonUtils.getProperty(aoWasdiPayload, "format");
		String sFootprint = JsonUtils.getProperty(aoWasdiPayload, "boundingBox");
		String sFootprintForFileName = CDSProviderAdapter.getFootprintForFileName(sFootprint, sDataset);
		String sExtension = "." + sFormat;

		// filename: reanalysis-era5-pressure-levels_UV_20211201
		String sFileName = CDSUtils.getFileName(sDataset, sVariables, sDate, sStartDate, sEndDate, sExtension, sFootprintForFileName);
			
		return sFileName;
	}
	
	
	public static void main(String [] args) throws Exception {
		// String sPayload = "https%3A%2F%2Fcds.climate.copernicus.eu%2Fapi%2Fv2%2Fresources%3Fpayload%3D%7B%22date%22%3A%2220240802%22%2C%22boundingBox%22%3A%2252.58950154463953%2C+6.328125000000001%2C+47.98745256063311%2C+13.535156250000002%22%2C%22variables%22%3A%22RH+U+V%22%2C%22presureLevels%22%3A%221000%22%2C%22format%22%3A%22netcdf%22%2C%22dataset%22%3A%22reanalysis-era5-pressure-levels%22%2C%22monthlyAggregation%22%3A%22false%22%2C%22productType%22%3A%22reanalysis%22%7D";
			
		String sPayload = "https%3A%2F%2Fcds.climate.copernicus.eu%2Fapi%2Fv2%2Fresources%3Fpayload%3D%7B%22date%22%3A%2220240801%22%2C%22boundingBox%22%3A%2245.18978009667531%2C+7.525634765625001%2C+44.91035917458495%2C+7.866210937500001%22%2C%22variables%22%3A%22toa_incident_solar_radiation%22%2C%22format%22%3A%22netcdf%22%2C%22dataset%22%3A%22reanalysis-era5-single-levels%22%2C%22monthlyAggregation%22%3A%22false%22%2C%22productType%22%3A%22reanalysis%22%7D";
		
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
