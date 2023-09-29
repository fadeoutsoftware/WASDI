package wasdi.dataproviders;

import java.io.File;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.lpdaac.ResponseTranslatorLpDaac;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.modis.MODISUtils;

public class LpDaacProviderAdapter extends ProviderAdapter {
	
	public LpDaacProviderAdapter() {
		m_sDataProviderCode = "LPDAAC";
	}

	@Override
	protected void internalReadConfig() {		
	}

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		long lResult = 0L;
		WasdiLog.debugLog("LpDaacProviderAdapter.getDownloadFileSize. File url: " + sFileURL);
		try {			
			String[] asTokens = sFileURL.split(ResponseTranslatorLpDaac.SLINK_SEPARATOR);
			lResult = Long.parseLong(asTokens[ResponseTranslatorLpDaac.S_IFILE_SIZE_INDEX]);
		} catch (Exception oEx) {
			WasdiLog.errorLog("LpDaacProviderAdapter.getDownloadFileSize: exception while trying to retrieve the file size." + oEx.getMessage());
		}
		return lResult;
	}

	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {
		
		WasdiLog.debugLog("LpDaacProviderAdapter.executeDownloadFile. File url: " + sFileURL);
				
		String sDownloadUrl = null;
		
		try {			
			String[] asTokens = sFileURL.split(ResponseTranslatorLpDaac.SLINK_SEPARATOR);
			sDownloadUrl = asTokens[ResponseTranslatorLpDaac.S_IURL_INDEX];
		} catch (Exception oEx) {
			WasdiLog.errorLog("LpDaacProviderAdapter.executeDownloadFile: exception while trying to retrieve the download url." + oEx.getMessage());
			return sDownloadUrl;
		}
		
		if (Utils.isNullOrEmpty(sDownloadUrl)) {
			WasdiLog.errorLog("LpDaacProviderAdapter.executeDownloadFile: download url is null or empty");
			return sDownloadUrl;
		}
		
		WasdiLog.debugLog("LpDaacProviderAdapter.executeDownloadFile. Accessing resource at URL: " + sDownloadUrl);

		InputStream oInputStream  = null;
	    try {
	    	CookieHandler.setDefault( new CookieManager(null, CookiePolicy.ACCEPT_ALL));
	 
            /* Retrieve a stream for the resource */
            oInputStream = MODISUtils.getResource(sDownloadUrl, sDownloadUser, sDownloadPassword);
            
            WasdiLog.debugLog("LpDaacProviderAdapter.executeDownloadFile. Input stream opened. Is it not null? " + (oInputStream != null));

	        File oSaveDir = new File(sSaveDirOnServer);
	        
			WasdiLog.debugLog("LpDaacProviderAdapter.executeDownloadFile. New file opened at: " + sSaveDirOnServer);

            String sSavedFilePath = oSaveDir + File.separator + sDownloadUrl.substring(sDownloadUrl.lastIndexOf('/') + 1).trim();
            
			WasdiLog.debugLog("LpDaacProviderAdapter.executeDownloadFile. Path of the output strea,: " + sSaveDirOnServer);

            Path outputPath = Paths.get(sSavedFilePath);
            Files.copy(oInputStream, outputPath, StandardCopyOption.REPLACE_EXISTING);
            
    		WasdiLog.debugLog("LpDaacProviderAdapter.executeDownloadFile. File path downloaded at: " + sSavedFilePath);
    		
    		sDownloadUrl = sSavedFilePath;
        }
        catch( Exception oEx) {
			WasdiLog.errorLog("LpDaacProviderAdapter.executeDownloadFile: exception when trying to download the file " + oEx.getMessage());
        } finally {
        	if (oInputStream != null)
        		oInputStream.close();
        }

		return sDownloadUrl;
	}

	@Override
	public String getFileName(String sFileURL) throws Exception {
		String sResult = "";
		WasdiLog.debugLog("LpDaacProviderAdapter.getFileName. File url: " + sFileURL);
		try {			
			String[] asTokens = sFileURL.split(ResponseTranslatorLpDaac.SLINK_SEPARATOR);
			sResult = asTokens[ResponseTranslatorLpDaac.S_IFILE_NAME_INDEX];
		} catch (Exception oEx) {
			WasdiLog.errorLog("LpDaacProviderAdapter.getFileName: exception while trying to retrieve the file name." + oEx.getMessage());
		}
		return sResult;
	}

	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
		if (sPlatformType.equals(Platforms.TERRA) ) {
			if (isWorkspaceOnSameCloud()) {
				return DataProviderScores.SAME_CLOUD_DOWNLOAD.getValue();
			}
			else {
				return DataProviderScores.DOWNLOAD.getValue();
			}
		}

		return 0;
	}

}
