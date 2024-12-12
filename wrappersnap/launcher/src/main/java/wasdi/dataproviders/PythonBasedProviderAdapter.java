package wasdi.dataproviders;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.runtime.RunTimeUtils;
import wasdi.shared.utils.runtime.ShellExecReturn;

public class PythonBasedProviderAdapter extends ProviderAdapter {
	
	private static final Object s_oTempFolderLock = new Object();

	protected String m_sPythonScript = "";
	protected String m_sExchangeFolder = ""; 
	
	@Override
	protected void internalReadConfig() {
		String sAdapterConfigPath = "";
		try {
			sAdapterConfigPath = m_oDataProviderConfig.adapterConfig;
			JSONObject oAppConf = JsonUtils.loadJsonFromFile(sAdapterConfigPath);
			m_sPythonScript = oAppConf.getString("pythonScript");
			m_sExchangeFolder = WasdiConfig.Current.paths.wasdiTempFolder;
		} catch(Exception oEx) {
			WasdiLog.errorLog("PythonBasedProviderAdapter.internalReadConfig: exception reading parser config file " + sAdapterConfigPath);
		}
	}
	
	
	/**
	 * Prepare the command line arguments to pass to the Python data provider.
	 * @param sSubsetJsonParameters the JSON string to write in the input file of the Python data provider. It contains the parameters of the subset to download.
	 * @return the ordered list of arguments to pass to the shell execute
	 */
	protected List<String> getCommandLineArgsForDownload(String sSubsetJsonParameters) {
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
			WasdiLog.errorLog("PythonBasedProviderAdapter.executeDownloadFile: error ", oEx);
		}
		return asArgs;
	}
	
	protected void addOperativeParametersToWasdiPayload(Map<String, Object> oPayloadMap, String sSaveFolderPath, String sDownloadedFileName, int iMaxRetries) {
		oPayloadMap.put("downloadDirectory", sSaveFolderPath);
		oPayloadMap.put("downloadFileName", sDownloadedFileName);
		oPayloadMap.put("maxRetry", iMaxRetries); // not sure I need
	}
	
	
	
	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {
				
		String sResultDownloadedFilePath = null;
		
		String sInputFullPath = "";
		String sOutputFullPath = "";
		
		WasdiLog.infoLog("PythonBasedProviderAdapter.executeDownloadFile: product name " + oProcessWorkspace.getProductName());	
		
		try {	
			
			// sSaveDirOnServer contains the path until the workspace. However, if the workspace has just been created, the workspace
			// directory is not yet present and needs to be created
			File oTargetDir = new File(sSaveDirOnServer);
			boolean oDirCreated = oTargetDir.mkdirs();
			if (oDirCreated)
				WasdiLog.infoLog("PythonBasedProviderAdapter.executeDownloadFile. Workspace directory has been crated");
			
			
			// let's add the additional information to the json passed to the Python data provider
			Map<String, Object> oMap = fromWasdiPayloadToObjectMap(sFileURL);
			addOperativeParametersToWasdiPayload(oMap, sSaveDirOnServer, oProcessWorkspace.getProductName(), iMaxRetry);

			
			String sEnrichedJsonParameters = JsonUtils.stringify(oMap);
			
			// get the parameters to pass to the shell execute
			List<String> asArgs = getCommandLineArgsForDownload(sEnrichedJsonParameters);
			
			if (asArgs == null) {
				WasdiLog.errorLog("PythonBasedProviderAdapter.executeDownloadFile: not arguments for shellExec");
			}
			
			if (asArgs.size() < 5) {
				WasdiLog.errorLog("PythonBasedProviderAdapter.executeDownloadFile: not enough args");
			}
			
			sInputFullPath = asArgs.get(2);
			sOutputFullPath = asArgs.get(3);
			
			
			ShellExecReturn oShellExecReturn = RunTimeUtils.shellExec(asArgs, true, true, true, true);
			
			WasdiLog.infoLog("PythonBasedProviderAdapter.executeDownloadFile: python output = " + oShellExecReturn.getOperationLogs());;
			
			File oOutputFile = new File(sOutputFullPath);
			
			if (!oOutputFile.exists()) {
				WasdiLog.warnLog("PythonBasedProviderAdapter.executeDownloadFile: impossible to read the output file of the Python data provider");
			}
			
			WasdiLog.infoLog("PythonBasedProviderAdapter.executeDownloadFile: got output file form the Python data provider " + oOutputFile.getAbsolutePath());
			
			// the output will simply be the path of the downloaded file
			JSONObject oJsonOutput = JsonUtils.loadJsonFromFile(sOutputFullPath);
			String sDownloadedFilePath = oJsonOutput.optString("outputFile");
			
			if (!Utils.isNullOrEmpty(sDownloadedFilePath)) {
				WasdiLog.infoLog("PythonBasedProviderAdapter.executeDownloadFile: path to the downloaded file " + sDownloadedFilePath);
				sResultDownloadedFilePath = sDownloadedFilePath;
			} else {
				WasdiLog.errorLog("PythonBasedProviderAdapter.executeDownloadFile: path to the downloaded file is null or empty");
			}
			
		} catch(Exception oEx) {
			WasdiLog.errorLog("PythonBasedProviderAdapter.executeDonwloadFile: error ", oEx);
		} finally {
			if (!Utils.isNullOrEmpty(sInputFullPath))
				FileUtils.deleteQuietly(new File(sInputFullPath));
			if (!Utils.isNullOrEmpty(sOutputFullPath))
				FileUtils.deleteQuietly(new File(sOutputFullPath));
		}
		
		return sResultDownloadedFilePath;
	}
	
	
	
	protected Map<String, Object> fromWasdiPayloadToObjectMap(String sUrl) {
		String sDecodedUrl = decodeUrl(sUrl);

		String sPayload = null;
		if (!Utils.isNullOrEmpty(sDecodedUrl)) {
			String[] asTokens = sDecodedUrl.split("payload=");
			if (asTokens.length == 2) {
				sPayload = asTokens[1];
				WasdiLog.debugLog("PythonBasedProviderAdapter.fromtWasdiPayloadToObjectMap json string: " + sPayload);
				return JsonUtils.jsonToMapOfObjects(sPayload);
			}
			WasdiLog.warnLog("PythonBasedProviderAdapter.fromtWasdiPayloadToObjectMap. Payload not found in url " + sUrl);
		}
		
		WasdiLog.warnLog("PythonBasedProviderAdapter.fromtWasdiPayloadToObjectMap. Decoded url is null or empty " + sUrl);
		return null;
	}
	
	protected Map<String, String> fromWasdiPayloadToStringMap(String sUrl) {
		String sDecodedUrl = decodeUrl(sUrl);

		String sPayload = null;
		if (!Utils.isNullOrEmpty(sDecodedUrl)) {
			String[] asTokens = sDecodedUrl.split("payload=");
			if (asTokens.length == 2) {
				sPayload = asTokens[1];
				WasdiLog.debugLog("PythonBasedProviderAdapter.extractWasdiPayloadFromUrl json string: " + sPayload);
				return JsonUtils.jsonToMapOfStrings(sPayload);
			}
			WasdiLog.warnLog("PythonBasedProviderAdapter.extractWasdiPayloadFromUrl. Payload not found in url " + sUrl);
		}
		
		WasdiLog.warnLog("PythonBasedProviderAdapter.extractWasdiPayloadFromUrl. Decoded url is null or empty " + sUrl);
		return null;
	}
	
	protected static String decodeUrl(String sUrlEncoded) {
		try {
			return URLDecoder.decode(sUrlEncoded, java.nio.charset.StandardCharsets.UTF_8.toString());
		} catch (UnsupportedEncodingException oE) {
			WasdiLog.warnLog("PythonBasedProviderAdapter.decodeUrl: could not decode URL " + oE);
			return null;
		}
	}

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		return 0;
	}

	@Override
	public String getFileName(String sFileURL) throws Exception {
		return null;
	}

	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
		return 0;
	}

}
