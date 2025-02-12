package wasdi.dataproviders;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.data.MongoRepository;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.extweb.config.ExtWebConfig;
import wasdi.shared.queryexecutors.extweb.config.ExtWebDataProviderConfig;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

public class ExtWebProviderAdapter extends ProviderAdapter {
	
	private ExtWebConfig m_oExtWebConfig;
	
	public ExtWebProviderAdapter() {
	}

	@Override
	protected void internalReadConfig() {
		try {
			Stream<String> oLinesStream = null;
			
	        try {
	        	
	        	oLinesStream = Files.lines(Paths.get(m_oDataProviderConfig.adapterConfig), StandardCharsets.UTF_8);
				String sJson = oLinesStream.collect(Collectors.joining(System.lineSeparator()));
				m_oExtWebConfig = MongoRepository.s_oMapper.readValue(sJson,ExtWebConfig.class);
				
				for (ExtWebDataProviderConfig oDataProviderConfig : m_oExtWebConfig.providers) {
					if (m_asSupportedPlatforms.contains(oDataProviderConfig.mission)) {
						m_asSupportedPlatforms.add(oDataProviderConfig.mission);
						WasdiLog.debugLog("ExtWebProviderAdapter.internalReadConfig: adding support to " + oDataProviderConfig.mission);						
					}
				}
				
			} 
	        catch (Exception oEx) {
				WasdiLog.errorLog("ExtWebProviderAdapter.internalReadConfig: exception ", oEx);
			} 
	        finally {
				if (oLinesStream != null)  {
					oLinesStream.close();
				}
			}			
			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ExtWebProviderAdapter.internalReadConfig: exception reading config files ", oEx);
		}	
		
	}

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		return 0;
	}

	protected ExtWebDataProviderConfig getConfigByMissionName(String sMissionName) {
		for (ExtWebDataProviderConfig oDataProviderConfig : m_oExtWebConfig.providers) {
			if (oDataProviderConfig.mission.equals(sMissionName)) return oDataProviderConfig;
		}
		return null;
	}

	
	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {
		
		try {
			String [] asNamePlatform = sFileURL.split(";");
			
			ExtWebDataProviderConfig oConfig =  getConfigByMissionName(asNamePlatform[1]);
			
			String sUrl = oConfig.baseUrl;
			if (!sUrl.endsWith("/")) sUrl += "/";
			sUrl += "download?fileName=";
			
			sUrl +=  URLEncoder.encode(asNamePlatform[0], java.nio.charset.StandardCharsets.UTF_8.toString()); 
			
			Map<String, String> asHeaders = new HashMap<>();
			asHeaders.put("Content-Type", "application/json");			
			asHeaders.put("x-api-key", oConfig.apiKey);
			
			if (!sSaveDirOnServer.endsWith("/")) sSaveDirOnServer += "/";
			
			String sOutputFilePath = sSaveDirOnServer + asNamePlatform[0].replace("file://", "");
			
			if (asNamePlatform.length >= 2 && asNamePlatform[1].equals(Platforms.METEOCEAN)) {
				sOutputFilePath = sSaveDirOnServer + asNamePlatform[0].split(",")[7];
			}
			
			
			String sFilePath = HttpUtils.downloadFile(sUrl, asHeaders, sOutputFilePath);
			
			if (Utils.isNullOrEmpty(sFilePath)) {
				WasdiLog.errorLog("ExtWebProviderAdapter.executeDownloadFile: impossible to get a valid file Path");
				return "";
			}
			
			return sFilePath;
			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ExtWebProviderAdapter.executeDownloadFile: exception ", oEx);
		}
		
		return "";
	}

	@Override
	public String getFileName(String sFileURL, String sDownloadPath) throws Exception {
		
		
		
		try {
			String [] asNamePlatform = sFileURL.split(";");
			
			if (asNamePlatform.length >= 2 && asNamePlatform[1].equals(Platforms.METEOCEAN)) {
				return asNamePlatform[0].split(",")[7];
			}
						
			return asNamePlatform[0].replace("file://", "");
			
		}
		catch (Exception oEx) {
			WasdiLog.errorLog("ExtWebProviderAdapter.getFileName: exception ", oEx);
		}
		
		return "";
	}

	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
		return DataProviderScores.DOWNLOAD.getValue();
	}

}
