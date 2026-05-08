package wasdi.dataproviders;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.runtime.RunTimeUtils;
import wasdi.shared.utils.runtime.ShellExecReturn;

public class ExtPythonProviderAdapter extends PythonBasedProviderAdapter {
		
	public ExtPythonProviderAdapter() {
	}

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		WasdiLog.debugLog("ExtPythonProviderAdapter.getDownloadFileSize: download file size not available");
		return 0;
	}
	
	/**
	 * Prepare the command line arguments to pass to the Python data provider.
	 * @param sSubsetJsonParameters the JSON string to write in the input file of the Python data provider. It contains the parameters of the subset to download.
	 * @return the ordered list of arguments to pass to the shell execute
	 */
	protected List<String> getCommandLineArgsForGetFileName(String sSubsetJsonParameters) {
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
			asArgs.add("3"); 							// arg[2] - code for the getFileName operation
			asArgs.add(sInputFullPath); 				// arg[3] - path of the input file
			asArgs.add(sOutputFullPath);				// arg[4] - path of the output file
			asArgs.add(WasdiConfig.Current.paths.wasdiConfigFilePath);		// arg[5] - wasdiConfig file path
			
		} catch(Exception oEx) {
			WasdiLog.errorLog("PythonBasedProviderAdapter.executeDownloadFile: error ", oEx);
		}
		return asArgs;
	}

	@Override
	public String getFileName(String sFileURL, String sDownloadPath) throws Exception {
		String sResultFileName = "";
		
		String sInputFullPath = "";
		String sOutputFullPath = "";
		
		WasdiLog.debugLog("ExtPythonProviderAdapter.getFileName");	
		
		try {	
			
			// sSaveDirOnServer contains the path until the workspace. However, if the workspace has just been created, the workspace
			// directory is not yet present and needs to be created
			File oTargetDir = new File(sDownloadPath);
			boolean oDirCreated = oTargetDir.mkdirs();
			if (oDirCreated)
				WasdiLog.debugLog("ExtPythonProviderAdapter.getFileName. Workspace directory has been crated");
			
			
			// let's add the additional information to the json passed to the Python data provider
			Map<String, Object> oMap = new HashMap<>();
			oMap.put("url", new String(sFileURL));
			
			String sEnrichedJsonParameters = JsonUtils.stringify(oMap);
			
			// get the parameters to pass to the shell execute
			List<String> asArgs = getCommandLineArgsForGetFileName(sEnrichedJsonParameters);
			
			if (asArgs == null) {
				WasdiLog.errorLog("ExtPythonProviderAdapter.getFileName: not arguments for shellExec");
				return null;
			}
			
			if (asArgs.size() < 5) {
				WasdiLog.errorLog("ExtPythonProviderAdapter.getFileName: not enough args");
				return null;
			}
			
			sInputFullPath = asArgs.get(2);
			sOutputFullPath = asArgs.get(3);
			
			
			ShellExecReturn oShellExecReturn = RunTimeUtils.shellExec(asArgs, true, true, true, true);
			
			WasdiLog.debugLog("ExtPythonProviderAdapter.getFileName: python output = " + oShellExecReturn.getOperationLogs());;
			
			File oOutputFile = new File(sOutputFullPath);
			
			if (!oOutputFile.exists()) {
				WasdiLog.warnLog("PythonBasedProviderAdapter.getFileName: impossible to read the output file of the Python data provider");
				return "";
			}
			
			WasdiLog.debugLog("PythonBasedProviderAdapter.getFileName: got output file form the Python data provider " + oOutputFile.getAbsolutePath());
			
			// the output will simply be the path of the downloaded file
			JSONObject oJsonOutput = JsonUtils.loadJsonFromFile(sOutputFullPath);
			String sFileName = oJsonOutput.optString("fileName");
			
			if (!Utils.isNullOrEmpty(sFileName)) {
				WasdiLog.debugLog("PythonBasedProviderAdapter.getFileName: got fileName " + sFileName);
				sResultFileName = sFileName;
			} else {
				WasdiLog.errorLog("PythonBasedProviderAdapter.getFileName: path to the downloaded file is null or empty");
			}
			
		} 
		catch(Exception oEx) {
			WasdiLog.errorLog("PythonBasedProviderAdapter.getFileName: error ", oEx);
		} 
		finally {
			
			if (WasdiConfig.Current.dockers.removeParameterFilesForPythonsShellExec) {
				if (!Utils.isNullOrEmpty(sInputFullPath))
					FileUtils.deleteQuietly(new File(sInputFullPath));
				if (!Utils.isNullOrEmpty(sOutputFullPath))
					FileUtils.deleteQuietly(new File(sOutputFullPath));
			}
			else {
				WasdiLog.infoLog("PythonBasedProviderAdapter.getFileName: removeParameterFilesForPythonsShellExec is  FALSE, we keep the parameter files");
			}						
		}
		
		return sResultFileName;
	}

}
