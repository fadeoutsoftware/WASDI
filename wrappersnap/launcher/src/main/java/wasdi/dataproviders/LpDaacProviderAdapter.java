package wasdi.dataproviders;

import java.io.File;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.lpdaac.ResponseTranslatorLpDaac;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.utils.modis.MODISUtils;
import wasdi.shared.viewmodels.HttpCallResponse;

public class LpDaacProviderAdapter extends ProviderAdapter {
	
	private static final String s_sUrlListTokens = "https://urs.earthdata.nasa.gov/api/users/tokens";
	private static final String s_sUrlCreateToken = "https://urs.earthdata.nasa.gov/api/users/token";
	
	public LpDaacProviderAdapter() {
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
		
		
		String sFileName = this.getFileName(sFileURL);
		
		if (Utils.isNullOrEmpty(sFileName)) {
			WasdiLog.warnLog("LpDaacProviderAdapter.executeDownloadFile: file name is empty. Cannot determine how to download the file");
			return null;
		}
		
        
        File oSaveDir = new File(sSaveDirOnServer);
        
        WasdiLog.debugLog("LpDaacProviderAdapter.executeDownloadFile. New file created at: " + sSaveDirOnServer);

        String sSavedFilePath = oSaveDir + File.separator + sDownloadUrl.substring(sDownloadUrl.lastIndexOf('/') + 1).trim();
        
		WasdiLog.debugLog("LpDaacProviderAdapter.executeDownloadFile. Path of the output stream: " + sSavedFilePath);
		
		// sSaveDirOnServer contains the path until the workspace. However, if the workspace has just been created, the workspace
		// directory is not yet present and needs to be created
		File oTargetFile = new File(sSavedFilePath);
		File oTargetDir = oTargetFile.getParentFile();
		boolean oDirCreated = oTargetDir.mkdirs();
		if (oDirCreated)
			WasdiLog.debugLog("LpDaacProviderAdapter.executeDownloadFile. Workspace directory has been crated");
		
		
		if (sFileName.toUpperCase().startsWith("VNP21A1D") 
				|| sFileName.toUpperCase().startsWith("VNP21A1N")
				|| sFileName.toUpperCase().startsWith("VNP15A2H")
				|| sFileName.toUpperCase().startsWith("MCD43A4")
				|| sFileName.toUpperCase().startsWith("MCD43A3")) {
			WasdiLog.debugLog("LpDaacProviderAdapter.executeDownloadFile: VIIRS product asked for download");
			return executeDownloadFromEarthData(sDownloadUrl, sDownloadUser, sDownloadPassword, sSavedFilePath, iMaxRetry);
		}
		
		WasdiLog.debugLog("LpDaacProviderAdapter.executeDownloadFile. Accessing resource at URL: " + sDownloadUrl);

		InputStream oInputStream  = null;
	    try {
	    	CookieHandler.setDefault( new CookieManager(null, CookiePolicy.ACCEPT_ALL));
	 
            /* Retrieve a stream for the resource */
            oInputStream = MODISUtils.getResource(sDownloadUrl, sDownloadUser, sDownloadPassword);
            
            if (oInputStream == null) {
            	WasdiLog.errorLog("LpDaacProviderAdapter.executeDownloadFile. Input stream is null. Nothing to download.");
            	return null;
            }
            	
            WasdiLog.debugLog("LpDaacProviderAdapter.executeDownloadFile. Input stream is not null. Proceed to save the stream on file."); 

            Path outputPath = Paths.get(sSavedFilePath);
            Files.copy(oInputStream, outputPath, StandardCopyOption.REPLACE_EXISTING);
            
    		WasdiLog.debugLog("LpDaacProviderAdapter.executeDownloadFile. File path downloaded at: " + sSavedFilePath);
    		
    		sDownloadUrl = sSavedFilePath;
        }
        catch( Exception oEx) {
			WasdiLog.errorLog("LpDaacProviderAdapter.executeDownloadFile: exception when trying to download the file " + oEx.getMessage());
			return null;
			
        } finally {
        	if (oInputStream != null)
        		oInputStream.close();
        }

		return sDownloadUrl;
	}
	
	
	public String executeDownloadFromEarthData(String sEarthDataURL, String sDownloadUser, String sDownloadPassword, String sSaveDirOnServer, int iMaxRetry) {
		
		// first we need to retrieve the authentication token
		String sAuthorizationToken = getEarthDataAuthorizationToken(sDownloadUser, sDownloadPassword);
		
		if (Utils.isNullOrEmpty(sAuthorizationToken)) {
			WasdiLog.warnLog("LpDaacProviderAdapter.executeDownloadFromEarthData. No auth token, impossible to proceed");
			return null;
		}
		
		// if we have an authentication token, then we can proceed with the download
		
		Map<String, String> aoHeaders = new HashMap<>();
    	aoHeaders.put("Authorization", "Bearer " + sAuthorizationToken);
    	aoHeaders.put("Content-Type", "application/json");
		
    	WasdiLog.debugLog("LpDaacProviderAdapter.executeDownloadFromEarthData. Download product from url: " + sEarthDataURL);
    	
    	String sDowloadedProductPath = null;
    	for (int i = 0; i < iMaxRetry; i++) {
    		WasdiLog.debugLog("LpDaacProviderAdapter.getEarthDataAuthorizationToken. Download attempt #" + i);
    		
    		sDowloadedProductPath = HttpUtils.downloadFile(sEarthDataURL, aoHeaders, sSaveDirOnServer);
    		
    		try {
	    		if (Utils.isNullOrEmpty(sDowloadedProductPath)) Thread.sleep(i * 1_000L);
	    		else break;
	    	}
    		catch (InterruptedException oEx) {
    			WasdiLog.errorLog("LpDaacProviderAdapter.getEarthDataAuthorizationToken. Thread was interrupted", oEx);
    		}
    	}
		
		if (Utils.isNullOrEmpty(sDowloadedProductPath)) {
			WasdiLog.warnLog("LpDaacProviderAdapter.getEarthDataAuthorizationToken. ");
			return null;
		}
		
		return sDowloadedProductPath;
	}
	
	
	public String getEarthDataAuthorizationToken(String sDownloadUser, String sDownloadPassword) {		
		
		String sCredentials = Base64.getEncoder().encodeToString((sDownloadUser + ":" + sDownloadPassword).getBytes(StandardCharsets.UTF_8));
		
		Map<String, String> aoHeaders = new HashMap<>();
    	aoHeaders.put("Authorization", "Basic " + sCredentials);
    	aoHeaders.put("Content-Type", "application/json");
		
    	HttpCallResponse oResponse = HttpUtils.httpGet(s_sUrlListTokens, aoHeaders);
    	
    	int iResponseCode = oResponse.getResponseCode();
    	
    	if (iResponseCode < 200 ||  iResponseCode > 299) {
    		WasdiLog.warnLog("LpDaacProviderAdapter.getEarthDataAuthorizationToken. Error code when retrieving the auth token: " + iResponseCode);
    		return null;
    	}
    	
		JSONArray oJsonResponse = new JSONArray(oResponse.getResponseBody());
    	
    	for (int i = 0; i < oJsonResponse.length(); i++) {
    		JSONObject oTokenItem = oJsonResponse.getJSONObject(i);
    		String sToken = oTokenItem.optString("access_token");
    		if (!Utils.isNullOrEmpty(sToken)) {
    			WasdiLog.debugLog("LpDaacProviderAdapter.getEarthDataAuthorizationToken. Found an auth token");
    			return sToken;
    		}
    	}
		
		WasdiLog.debugLog("LpDaacProviderAdapter.getEarthDataAuthorizationToken. No auth token found. Trying to generate a new one");
		
		oResponse = HttpUtils.httpPost(s_sUrlCreateToken, "", aoHeaders);
		
		iResponseCode = oResponse.getResponseCode();
		
		if (iResponseCode < 200 ||  iResponseCode > 299) {
    		WasdiLog.warnLog("LpDaacProviderAdapter.getEarthDataAuthorizationToken. Error code when creating the auth token: " + iResponseCode);
    		return null;
    	}
		
		JSONObject oJsonCreateResponse = new JSONObject(oResponse.getResponseBody());
		String sAuthToken = oJsonCreateResponse.optString("access_token");
		
		if (Utils.isNullOrEmpty(sAuthToken)) {
			WasdiLog.warnLog("LpDaacProviderAdapter.getEarthDataAuthorizationToken. Token not created");
			return null;
		}
		
		return sAuthToken;
	}

	@Override
	public String getFileName(String sFileURL, String sDownloadPath) throws Exception {
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
		if (sPlatformType.equals(Platforms.VIIRS) 
				&& (sFileName.startsWith("VNP21A1D") || sFileName.startsWith("VNP21A1N") || sFileName.startsWith("VNP15A2H"))) {
			return DataProviderScores.DOWNLOAD.getValue();
		}

		return 0;
	}

}
