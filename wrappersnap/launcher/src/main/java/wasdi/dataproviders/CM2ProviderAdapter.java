package wasdi.dataproviders;

import java.util.*;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.runtime.RunTimeUtils;
import wasdi.shared.utils.runtime.ShellExecReturn;

public class CM2ProviderAdapter extends PythonBasedProviderAdapter {
	
	private static final String s_sDatasetId = "datasetId";
	
	
	protected String m_sPythonScript = "";
	protected String m_sExchangeFolder = ""; 
	
	public CM2ProviderAdapter() {
		m_sDataProviderCode = "COPERNICUSMARINE";
	}
	

	/*
	@Override
	protected void internalReadConfig() {
		String sAdapterConfigPath = "";
		try {
			sAdapterConfigPath = m_oDataProviderConfig.adapterConfig;
			JSONObject oAppConf = JsonUtils.loadJsonFromFile(sAdapterConfigPath);
			m_sPythonScript = oAppConf.getString("pythonScript");
			m_sExchangeFolder = WasdiConfig.Current.paths.wasdiTempFolder;
		} catch(Exception oEx) {
			WasdiLog.errorLog("CM2ProviderAdapter.internalReadConfig: exception reading parser config file " + sAdapterConfigPath);
		}
	}
	*/

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		WasdiLog.debugLog("CM2ProviderAdapter.getDownloadFileSize: download file size not available in Copernicus Marine");
		return 0;
	}
	
//	protected List<String> getCommandLineArgsForDownload(String sSubsetJsonParameters) {
		/**
		 * Prepare the command line arguments to pass to the Copernicus Marine Python data provider.
		 * @param sSubsetJsonParameters the JSON string to write in the input file of the Python data provider. It contains the parameters of the subset to download.
		 * @return the ordered list of argument to pass to the shell execut
		 */
	/*
		List<String> asArgs = new ArrayList<>();
		
		String sInputFullPath = "";
		String sOutputFullPath = "";
		
		try {
			String sInputFile = Utils.getRandomName();
			String sOutputFile = Utils.getRandomName();
			
			
			synchronized (s_oTempFolderLock) {
				if (!Utils.isNullOrEmpty(m_sExchangeFolder)) {
					
					boolean bIsInputFileUnique = false;
					
					while (!bIsInputFileUnique) {
						sInputFullPath = m_sExchangeFolder;
						if (!m_sExchangeFolder.endsWith("/")) {
							sInputFullPath += "/";
						}
						sInputFullPath += sInputFile;
						
						if (new File(sInputFullPath).exists()) {
							sInputFile = Utils.getRandomName();
						} else {
							bIsInputFileUnique = true;
						}
					}
					
					WasdiFileUtils.writeFile(sSubsetJsonParameters, sInputFullPath);
					
					boolean bOutputFileUnique = false;
					
					while (!bOutputFileUnique) {
						sOutputFullPath = m_sExchangeFolder;
						if (!m_sExchangeFolder.endsWith("/")) {
							sOutputFullPath += "/";
						}
						sOutputFullPath += sOutputFile;
						
						if (new File(sOutputFullPath).exists()) {
							sOutputFile = Utils.getRandomName();
						} else {
							bOutputFileUnique = true;
						}
					}	
					
					File oOutputFile = new File(sOutputFullPath);
					oOutputFile.createNewFile();
				}
			}
			
						
			asArgs.add(m_sPythonScript); 				// arg[1] - name of the python data provider
			asArgs.add("2"); 							// arg[2] - code for the download operation
			asArgs.add(sInputFullPath); 				// arg[3] - path of the input file
			asArgs.add(sOutputFullPath);				// arg[4] - path of the output file
			asArgs.add(WasdiConfig.Current.paths.wasdiConfigFilePath);		// arg[5] - wasdiConfig file path
			
		} catch(Exception oEx) {
			WasdiLog.errorLog("CM2ProviderAdapter.executeDownloadFile: error ", oEx);
		}
		return asArgs;
	}
	*/

	/*
	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {
				
		String sResultDownloadedFilePath = null;
		
		String sInputFullPath = "";
		String sOutputFullPath = "";
		
		WasdiLog.debugLog("CM2ProviderAdapter.executeDownloadFile: product name " + oProcessWorkspace.getProductName());	
		
		try {		
			// let's add the additional information to the json passed to the Python data provider
			Map<String, Object> oMap = extractWasdiPayloadFromUrl(sFileURL);
			oMap.put("downloadDirectory", sSaveDirOnServer);
			oMap.put("downloadFileName", oProcessWorkspace.getProductName());
			oMap.put("maxRetry", iMaxRetry); // not sure I need
			
			String sEnrichedJsonParameters = JsonUtils.stringify(oMap);
			
			// get the parameters to pass to the shell execute
			List<String> asArgs = getCommandLineArgsForDownload(sEnrichedJsonParameters);
			
			if (asArgs == null) {
				WasdiLog.errorLog("CM2ProviderAdapter.executeDownloadFile: not arguments for shellExec");
			}
			
			if (asArgs.size() < 5) {
				WasdiLog.errorLog("CM2ProviderAdapter.executeDownloadFile: not enough args");
			}
			
			sInputFullPath = asArgs.get(2);
			sOutputFullPath = asArgs.get(3);
			
			
			ShellExecReturn oShellExecReturn = RunTimeUtils.shellExec(asArgs, true, true, true, true);
			
			WasdiLog.debugLog("CM2ProviderAdapter.executeDownloadFile: python output = " + oShellExecReturn.getOperationLogs());;
			
			File oOutputFile = new File(sOutputFullPath);
			
			if (!oOutputFile.exists()) {
				WasdiLog.warnLog("CM2ProviderAdapter.executeDownloadFile: impossible to read the output file of the Python data provider");
			}
			
			WasdiLog.debugLog("CM2ProviderAdapter.executeDownloadFile: got output file form the Python data provider");
			
			// the output (for now) it will simply be the path of the downloaded file
			JSONObject oJsonOutput = JsonUtils.loadJsonFromFile(sOutputFullPath);
			String sDownloadedFilePath = oJsonOutput.optString("outputFile");
			
			if (!Utils.isNullOrEmpty(sDownloadedFilePath)) {
				WasdiLog.debugLog("CM2ProviderAdapter.executeDownloadFile: path to the downloaded file " + sDownloadedFilePath);
				sResultDownloadedFilePath = sDownloadedFilePath;
			} else {
				WasdiLog.errorLog("CM2ProviderAdapter.executeDownloadFile: path to the downloaded file is null or empty");
			}
			
		} catch(Exception oEx) {
			WasdiLog.errorLog("CM2ProviderAdapter.executeDonwloadFile: error ", oEx);
		} finally {
			if (!Utils.isNullOrEmpty(sInputFullPath))
				FileUtils.deleteQuietly(new File(sInputFullPath));
			if (!Utils.isNullOrEmpty(sOutputFullPath))
				FileUtils.deleteQuietly(new File(sOutputFullPath));
		}
		
		return sResultDownloadedFilePath;
	}
	*/

	@Override
	public String getFileName(String sFileURL) throws Exception {
		WasdiLog.debugLog("CM2ProviderAdapter.getFileName: received url " + sFileURL);
		
		String sFileName = "dataset.nc";
		
		try {
			// the link is a JSON string that we can parse into a map
			Map<String, Object> oJsonMap =  fromtWasdiPayloadToObjectMap(sFileURL);
			String sDatasetId = (String) oJsonMap.getOrDefault(s_sDatasetId, "");
			
			if (!Utils.isNullOrEmpty(sDatasetId)) {
				sFileName = sDatasetId + "_" + Utils.nowInMillis().longValue() + ".nc";
			}
		} catch (Exception oEx) {
			WasdiLog.errorLog("CM2ProviderAdapter.getFileName: exception retrieving thefile name ", oEx);
		}
		
		WasdiLog.debugLog("CM2ProviderAdapter.getFileName: file name for CM subset file " + sFileName);
		return sFileName;
	}
	
	/*
	private Map<String, Object> extractWasdiPayloadFromUrl(String sUrl) {
		String sDecodedUrl = decodeUrl(sUrl);

		String sPayload = null;
		if (sDecodedUrl != null && sDecodedUrl.startsWith("https://payload=")) {
			sPayload = sDecodedUrl.replace("https://payload=", "");
			WasdiLog.debugLog("CM2ProviderAdapter.extractWasdiPayloadFromUrl json string: " + sPayload);
			return JsonUtils.jsonToMapOfObjects(sPayload);
		}

		return null;
	}
	*/
	
	/*
	private static String decodeUrl(String sUrlEncoded) {
		try {
			return URLDecoder.decode(sUrlEncoded, java.nio.charset.StandardCharsets.UTF_8.toString());
		} catch (UnsupportedEncodingException oE) {
			WasdiLog.warnLog("CM2ProviderAdapter.decodeUrl: could not decode URL " + oE);
			return null;
		}
	}
	*/
	

	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
		if (sPlatformType.equals(Platforms.CM)) {
			return DataProviderScores.DOWNLOAD.getValue();
		}

		return 0;
	}


}
