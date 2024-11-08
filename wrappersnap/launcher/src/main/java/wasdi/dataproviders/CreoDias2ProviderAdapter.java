
package wasdi.dataproviders;

import java.io.File;
import java.io.FileOutputStream;
import java.net.IDN;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipOutputStream;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.base.Preconditions;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.config.DataProviderConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.creodias2.ResponseTranslatorCreoDias2;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.JsonUtils;
import wasdi.shared.utils.MissionUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.ZipFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;

public class CreoDias2ProviderAdapter extends ProviderAdapter {
	
	private static final String SODATA_NAME = "Name";
	private static final String SODATA_VALUE = "Value";
	
	private static final String SAUTHENTICATION_URL = "https://identity.cloudferro.com/auth/realms/wekeo-elasticity/protocol/openid-connect/token";
	private static final String SDOWNLOAD_URL_END = "?token=";
	
	private static final String SODATA_FILE_URL_START = "https://zipper.creodias.eu/odata/v1/Products(";
	private static final String SODATA_FILE_URL_END = ")/$value";
	
	private static final String SCREODIAS_BASE_URL = "https://datahub.creodias.eu/odata/v1/Products?";
	
	/**
	 * Base path of the folder mounted with EO Data
	 */
	private String m_sProviderBasePath = "";
	
	
	public CreoDias2ProviderAdapter() {
		super();
		m_sDataProviderCode = "CREODIAS2";
	}
	
	protected boolean isFileProtocolNew(String sUrl) {
		try {
		
			if (Utils.isNullOrEmpty(sUrl)) {
				WasdiLog.debugLog("CreoDias2.isFileProtocolNew - sUrl is null or empty");
				return false;
			}
			
			WasdiLog.debugLog("CreoDias2.isFileProtocolNew - sUrl : " + sUrl);
			
			
			String sWorkspaceId = m_oProcessWorkspace.getWorkspaceId();
			String sCloud = getWorkspaceCloud();
						
			WasdiLog.debugLog("CreoDias2.isFileProtocolNew - workspace id: " + sWorkspaceId + " on cloud " + sCloud);
			
			if (Utils.isNullOrEmpty(sWorkspaceId) || Utils.isNullOrEmpty(sCloud)) {
				WasdiLog.debugLog("CreoDias2.isFileProtocolNew - workspace id or cloud is are empty or null");
				return false;
			}
			
			if (!sCloud.equals("CREODIAS")) {
				WasdiLog.debugLog("CreoDias2.isFileProtocolNew - the workspace is not on CREODIAS. File access not possible.");
				return false;
			} 
			
			WasdiLog.debugLog("CreoDias2.isFileProtocolNew - the workspace is on CREODIAS.");			
			
			String sS3Path =extractProductIdentifierFromURL(sUrl);
			
			if (Utils.isNullOrEmpty(sS3Path)) {
				WasdiLog.debugLog("CreoDias2.isFileProtocolNew - S3 path is null or empty. File access is not possible");
				return false;
			}
			
			WasdiLog.debugLog("CreoDias2.isFileProtocolNew - identified file access path: " + sS3Path);
			
			if (!Files.exists(Paths.get(sS3Path))){
				WasdiLog.debugLog("CreoDias2.isFileProtocolNew - the path of the file is not on the server. ");
				return false;
			}
			
			return true;
			
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("CreoDias2.isFileProtocolNew - exception: ", oEx);
			return false;
		}
		
	}
	
	/* Ideally, this method can be deleted once the test has been done. */
	protected boolean isFileAccessEnabled() {
		
		try {
			
			String sUserId = m_oProcessWorkspace.getUserId();
			
			WasdiLog.debugLog("CreoDias2.isFileAccessEnabled - user id: " + sUserId);		
			
			DataProviderConfig oProviderConfig = WasdiConfig.Current.getDataProviderConfig(m_sDataProviderCode);
			
			if (oProviderConfig == null) {
				WasdiLog.debugLog("CreoDias2.isFileAccessEnabled - didn't find a configuration for the provider");
				return false;
			}
			
			String sAdapterConfigFile = oProviderConfig.adapterConfig;		// let's read the adapter config file, which should contain the info to perform the test
			
			if (Utils.isNullOrEmpty(sAdapterConfigFile)) {
				WasdiLog.debugLog("CreoDias2.isFileAccessEnabled - the path of the adapter config file is null. Test cannot start");
				return false;
			}
			
			if (!Files.isRegularFile(Paths.get(sAdapterConfigFile))) {
				WasdiLog.debugLog("CreoDias2.isFileAccessEnabled - the path of the adapter config does not existing on the server");
				return false;
			}
			
			// at this point, we know that the file with the configuration file for the adapter is present
			JSONObject oAppConf = JsonUtils.loadJsonFromFile(sAdapterConfigFile);
			
			if (oAppConf == null) {
				WasdiLog.debugLog("CreoDias2.isFileAccessEnabled - could not load JSON object from the adapter config file");
				return false;
			}
			
			boolean bIsFileAccessTestEnabled = oAppConf.optBoolean("fileAccessTestEnabled");
			
			if (!bIsFileAccessTestEnabled) {
				WasdiLog.debugLog("CreoDias2.isFileAccessEnabled - the flag for the test is not enabled");
				return false;
			}
			
			JSONArray asTestUsers = oAppConf.optJSONArray("testingUsers");
			
			if (asTestUsers == null || asTestUsers.length() == 0) {
				WasdiLog.debugLog("CreoDias2.isFileAccessEnabled - no user(s) specified for enabling the test");
				return false;
			}
			
			boolean bIsUserEnabledForTest = false;
			
			for(int i = 0; i < asTestUsers.length(); i++) {
				
				String sTestUser = asTestUsers.getString(i);
				if (Utils.isNullOrEmpty(sTestUser)) continue;
				
				if (sTestUser.equals(sUserId)|| sTestUser.equals("*")) {
					bIsUserEnabledForTest = true;
					break;
				}
			}
			
			if (!bIsUserEnabledForTest) {
				WasdiLog.debugLog("CreoDias2.isFileAccessEnabled - the user is not enabled for the test");
				return false;
			}

			WasdiLog.debugLog("CreoDias2.isFileAccessEnabled - file access test is ideally possible");
			
			return true;
			
		} catch (Exception oEx) {
			WasdiLog.errorLog("CreoDias2.isFileAccessEnabled - exception", oEx);
			return false;
		}	
	}


	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		
		WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize. Retrieving information for product: " + sFileURL);
		
		// create a creodias query like: https://datahub.creodias.eu/odata/v1/Products?$filter=Id eq '79d7807b-66e5-41d2-a586-1628eb82c3e0'&$expand=Attributes
		
		
		long lSizeInBytes = 0L;
		
		// TEST: FILE ACCESS
		if (isFileAccessEnabled()) { 
			WasdiLog.debugLog("CreoDias2ProviderAdapter.getDownloadFileSize - test for file access enabled");
			
			if (isFileProtocolNew(sFileURL)) {
				WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize - file access protocol detected with NEW method");
				
				try {
				
					String sFilesystemPath = extractProductIdentifierFromURL(sFileURL);
					
					WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize - file system path from URL: " + sFilesystemPath);
					
					if (!Utils.isNullOrEmpty(sFilesystemPath)) {
						File oSourceFile = new File(sFilesystemPath);
						
						if (oSourceFile.exists()) {
							lSizeInBytes = getSourceFileLength(oSourceFile);
							WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize - detected file size: " + lSizeInBytes);
							return lSizeInBytes;
						} else {
							WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize - the file at the provided system path does not exist");
						}
					} else {
						WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize - the file system path is null or empty");
					}
				} catch (Exception oEx) {
					WasdiLog.errorLog("CreoDias2ProviderAdaper.getDownloadFileSize - detected file size: ", oEx);
				}	
			}
			
		} else if (isFileProtocol(m_sDefaultProtocol)) {
			
			String sPath = null;
			if (isFileProtocol(sFileURL)) {
				sPath = removePrefixFile(sFileURL);
				WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize: file protocol detected. File path " + sFileURL);
			} else if(isHttpsProtocol(sFileURL)) {
				sPath = extractFilePathFromHttpsUrl(sFileURL);
				WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize: http protocol detected. File path " + sFileURL);
			} else {
				WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize: unknown protocol " + sFileURL);
			}

			if (sPath != null) {
				File oSourceFile = new File(sPath);

				if (oSourceFile != null && oSourceFile.exists()) {
					lSizeInBytes = getSourceFileLength(oSourceFile);
					WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize: detected file size: " + lSizeInBytes);
					return lSizeInBytes;
				}
			}
		}

		
		if (isHttpsProtocol(sFileURL)) {
			
			String sDownloadUrl = getODataDownloadUrl(sFileURL);
			String sProductId = getProductIdFromURL(sDownloadUrl);
			String sUrl = SCREODIAS_BASE_URL + "$filter=Id eq '" + sProductId + "'&$expand=Attributes";
			URL oURL = new URL(sUrl);
			URI oURI = new URI(oURL.getProtocol(), oURL.getUserInfo(), IDN.toASCII(oURL.getHost()), oURL.getPort(), oURL.getPath(), oURL.getQuery(), oURL.getRef());
			
			HttpCallResponse sResponse = null;
			
			try {
				sResponse = HttpUtils.httpGet(oURI.toASCIIString());
				if (sResponse == null || sResponse.getResponseCode() < 200 || sResponse.getResponseCode() > 299 || Utils.isNullOrEmpty(sResponse.getResponseBody())) {
					WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize. Error retrieving the information about the product from the provider. " + sUrl);
					return -1L;
				}
			} catch (Exception oEx) {
				WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize. An exception occurred while retrieving the product information from the provider. " + oEx.getMessage());
				return -1L;
			}
			
			WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize. Answer well received from the provider");
				
			
			JSONObject oJsonBody = new JSONObject(sResponse.getResponseBody());
			JSONArray aoValues = oJsonBody.optJSONArray("value");
			if (aoValues == null || aoValues.length() != 1) {
				WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize. Array of attributes not found on CreoDias");
				return 0;
			}
			
			JSONObject oValue = (JSONObject) aoValues.get(0);
			String sFileSize = oValue.optString(ResponseTranslatorCreoDias2.SODATA_SIZE, "0");
			
			
			if (Utils.isNullOrEmpty(sFileSize)) {
				WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize. Array of attributes not found for key " + ResponseTranslatorCreoDias2.SODATA_SIZE);
			}
			
			lSizeInBytes = Long.parseLong(sFileSize);
			WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize. File size: " + lSizeInBytes);

		}

		return lSizeInBytes;
		
	}
	
	
	
	/**
	 * Extract the file-system path of the file out of an HTTPS URL.
	 * @param sHttpsURL the HTTPS URL containing the product identifier (i.e. /eodata/Sentinel-1/SAR/GRD/2021/01/01/S1B_S1_GRDH_1SDH_20210101T152652_20210101T152706_024962_02F890_39B4.SAFE)
	 * @return the file-system path (/eodata/Sentinel-1/SAR/GRD/2021/01/01/S1B_S1_GRDH_1SDH_20210101T152652_20210101T152706_024962_02F890_39B4.SAFE)
	 */
	private String extractFilePathFromHttpsUrl(String sHttpsURL) {
		String sProductIdentifier = extractProductIdentifierFromURL(sHttpsURL);
		String sFileSystemPath = m_sProviderBasePath + sProductIdentifier;

		WasdiLog.debugLog("CREODIASProviderAdapter.extractFilePathFromHttpsUrl: HTTPS URL: " + sProductIdentifier);
		WasdiLog.debugLog("CREODIASProviderAdapter.extractFilePathFromHttpsUrl: file path: " + sFileSystemPath);

		return sFileSystemPath;
	}
	
	private String extractProductIdentifierFromURL(String sFileURL) {
		Preconditions.checkNotNull(sFileURL, "URL is null");
		try {
			String[] asParts = sFileURL.split(ResponseTranslatorCreoDias2.SLINK_SEPARATOR_CREODIAS2);
			String sProductIdentifier = asParts[ResponseTranslatorCreoDias2.IPOSITIONOF_PRODUCTIDENTIFIER];
			return sProductIdentifier;
		} catch (Exception oE) {
			WasdiLog.errorLog("CREODIASProviderAdapter.extractProductIdentifierFromURL: ", oE);
		}
		return null;
	}

	
	
	
	@Override
	protected void internalReadConfig() {
		try {
			m_sDefaultProtocol = m_oDataProviderConfig.defaultProtocol; 
		} catch (Exception e) {
			WasdiLog.errorLog("CreoDias2ProvierAdapter: Config reader is null");
		}
	}
	
	
	private String getAttribute(JSONArray aoAttributes, String sAttributeName) {
		for (Object oAtt : aoAttributes) {
			JSONObject oJsonAtt = (JSONObject) oAtt;
			if (oJsonAtt.get(SODATA_NAME).equals(sAttributeName))
				return oJsonAtt.optString(SODATA_VALUE);
		}
		return "";
	}
	
	
	private String getProductIdFromURL(String sFileUrl) {
		// url in input is something like: https://datahub.creodias.eu/odata/v1/Products(a6212de3-f2e4-58c2-840b-7f42c3c8c612)/$value
		return sFileUrl.replace(SODATA_FILE_URL_START, "").replace(SODATA_FILE_URL_END, "");
	}
	

	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {
		// main method. sFileUrl is the url received by the lined data provider. sSaveDirOnServer is the local folder. 
		// must return the valid file full path or "" if the download was not possible. 
		
		WasdiLog.debugLog("CreoDias2ProviderAdaper.executeDownloadFile. Execute download for file: " + sFileURL);
		
		String sResult = null;
		
		// Is this a file access?
		if (isFileAccessEnabled()) {
			
			WasdiLog.debugLog("Creodias2ProviderAdapter.executeDownloadFile. Test if file access is possible.");
			
			if (isFileProtocolNew(sFileURL)) {
				try {
					WasdiLog.debugLog("CreoDias2ProviderAdaper.executeDownloadFile: detected file protocol with NEW method");
		
					String sFilesystemPath = extractProductIdentifierFromURL(sFileURL);
					
					WasdiLog.debugLog("CreoDias2ProviderAdaper.executeDownloadFile:  file system path from URL: " + sFilesystemPath);
					
					if (!Utils.isNullOrEmpty(sFilesystemPath)) {
						
						File oSourceFile = new File(sFilesystemPath);
						
						if (!oSourceFile.exists()) {
							WasdiLog.debugLog("CreoDias2ProviderAdaper.executeDownloadFile: path not found in the file system. File access not possible with new method");
						} 
						else if (sFilesystemPath.endsWith(".zip") || sFilesystemPath.endsWith(".SAFE")) {
							WasdiLog.debugLog("CreoDias2ProviderAdaper.executeDownloadFile: try to copy the file from the file system to the workspace folder.");
							long lStartTime = System.currentTimeMillis();
							sResult = copyFile("file:" + sFilesystemPath, sDownloadUser, sDownloadPassword, sSaveDirOnServer, oProcessWorkspace, iMaxRetry);
							long lEstimatedEndTime = System.currentTimeMillis() - lStartTime;
							WasdiLog.debugLog("CreoDias2ProviderAdaper.executeDownloadFile: file has been copied in " + lEstimatedEndTime + " milliseconds. Path: " + sResult);
							return sResult;
						} 
						else {
							WasdiLog.debugLog("CreoDias2ProviderAdapter.executeDownloadFile: file does not exist or does not have a .zip/.SAFE extension");
						}
					} else {
						WasdiLog.debugLog("CreoDias2ProviderAdaper.executeDownloadFile: file system path is empty");
					}
				} catch (Exception oEx) {
					WasdiLog.errorLog("CreoDias2ProviderAdapter.executeDownloadFile: exception ", oEx);
				}

			}
		} 	

		if(isHttpsProtocol(sFileURL)) { 
			int iAttemptCount = 0;
			int iWaitDelta = 100;
			long lLowerWatingTime = 1L;
			long lUpperWatingTime = 10L;
			
			while (iAttemptCount < iMaxRetry) {
				String sAccessToken = getAuthenticationToken(sDownloadUser, sDownloadPassword);
				
				if (Utils.isNullOrEmpty(sAccessToken)) {
					WasdiLog.debugLog("CreoDias2ProviderAdaper.executeDownloadFile. Error retrieving the access token. Impossible to continue.");
					// TODO: better to return an empty string or null?
					return "";
				}
				
				WasdiLog.debugLog("CreoDias2ProviderAdaper.executeDownloadFile. Access token correctly received.");
				String sODataDownloadUrl = getODataDownloadUrl(sFileURL);
//				String sProductId = getProductIdFromURL(sODataDownloadUrl);
				// with the auth token, we can send the download request
				String sDownloadUrl = sODataDownloadUrl + SDOWNLOAD_URL_END + sAccessToken;
				
				WasdiLog.debugLog("CreoDias2ProviderAdaper.executeDownloadFile. Access token correctly received. Download url (withouth accesso token): " + sODataDownloadUrl + SDOWNLOAD_URL_END);
				
				// TODO: understand if I should also pass the name of the file - I think no. The method already parses the "File-disposition" attribute in the header. Should also work for Creodias2
				long lStartTime = System.currentTimeMillis();
				String sDownloadedFilePath = downloadViaHttp(sDownloadUrl, Collections.emptyMap(), sSaveDirOnServer);
				
				if(Utils.isNullOrEmpty(sDownloadedFilePath)) {
					// we will try again
					iAttemptCount++;
					long lRandomWaitSeconds = new SecureRandom().longs(lLowerWatingTime, lUpperWatingTime).findFirst().getAsLong();
					//prepare to wait longer next time
					lLowerWatingTime = lRandomWaitSeconds;
					lUpperWatingTime += iWaitDelta;
					WasdiLog.warnLog("CreoDias2ProviderAdapter.executeDownloadFile. Download failed. Trying again after waiting  " + lRandomWaitSeconds +" seconds.");
					TimeUnit.SECONDS.sleep(lRandomWaitSeconds);
				} else {
					// download completed
					long lEndTime = System.currentTimeMillis() - lStartTime;
					WasdiLog.debugLog("CreoDias2ProviderAdapter.executeDownloadFile. Download completed in " + lEndTime + " milliseconds at path " + sDownloadedFilePath);
					return sDownloadedFilePath;
				}
			}
		}
		// TODO: better to return an empty string or null? 
		return "";
	}
	
	private String getODataDownloadUrl(String sFileURL) {
		if (Utils.isNullOrEmpty(sFileURL)) {
			WasdiLog.errorLog("CREODIASProviderAdapter.getODataDownloadUrl: Empty file URL");
			return "";
		}
		try {
			return sFileURL.split(ResponseTranslatorCreoDias2.SLINK_SEPARATOR_CREODIAS2)[ResponseTranslatorCreoDias2.IPOSITIONOF_LINK];
		} catch (Exception oE) {
			WasdiLog.errorLog("CREODIASProviderAdapter.getODataDownloadUrl: " + oE);
		}
		return "";
	}
	
	private String copyFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {

		String sResult = "";
		// Domain check
		if (Utils.isNullOrEmpty(sFileURL)) {
			WasdiLog.debugLog("CREODIASProviderAdapter.ExecuteDownloadFile: sFileURL is null");
			return "";
		}
		if (Utils.isNullOrEmpty(sSaveDirOnServer)) {
			WasdiLog.debugLog("CREODIASProviderAdapter.ExecuteDownloadFile: sSaveDirOnServer is null");
			return "";
		}
		
		WasdiLog.debugLog("CREODIASProviderAdapter.ExecuteDownloadFile: start");
		
		setProcessWorkspace(oProcessWorkspace);

		if (isFileProtocol(sFileURL)) {
			if (isZipFile(sFileURL)) {
				sResult = localFileCopy(sFileURL, sSaveDirOnServer, iMaxRetry);
			} 
			else if (isSafeDirectory(sFileURL.toUpperCase())) {
				String sSourceFile = removePrefixFile(sFileURL);
				String sDestinationFile = getFileName(sFileURL);

				// set the destination folder
				if (sSaveDirOnServer.endsWith("/") == false) sSaveDirOnServer += "/";
				sDestinationFile = sSaveDirOnServer + sDestinationFile;

				sDestinationFile = addZipExtension(removeSafeTermination(sDestinationFile));

				downloadZipFile(sSourceFile, sDestinationFile);
				
				sResult = sDestinationFile;
			}
		} 

		return sResult;
	}
	
	
	private void downloadZipFile(String sSourceFile, String sDestinationFile) throws Exception {
        FileOutputStream oFos = new FileOutputStream(sDestinationFile);
        ZipOutputStream oZipOut = new ZipOutputStream(oFos);
        File oFileToZip = new File(sSourceFile);

        ZipFileUtils.zipFile(oFileToZip, oFileToZip.getName(), oZipOut, 32*1024);
        oZipOut.close();
        oFos.close();
	}
	
	
	
	
	private String getAuthenticationToken(String sUsername, String sPassword) {
		String sPayload = "client_id=CLOUDFERRO_PUBLIC&password=" + sPassword + "&username=" + sUsername + "&grant_type=password";
		HttpCallResponse oResponse = null;
		try {
			oResponse = HttpUtils.httpPost(SAUTHENTICATION_URL, sPayload);
			if (oResponse == null ||  oResponse.getResponseCode() < 200 || oResponse.getResponseCode() > 299) {
				WasdiLog.debugLog("CreoDias2ProviderAdaper.getAuthenticationToken. Error code while trying to retrieve the auth token.");
				return "";
			} 
		} catch (Exception oEx) {
			WasdiLog.debugLog("CreoDias2ProviderAdaper.getAuthenticationToken. Error while trying to retrieve the authentication token." + oEx.getMessage());
			return "";
		}
		
		JSONObject oResponseBody = new JSONObject(oResponse.getResponseBody());
		String sAccessToken = oResponseBody.optString("access_token", "");
		
		if (Utils.isNullOrEmpty(sAccessToken)) {
			WasdiLog.debugLog("CreoDias2ProviderAdaper.executeDownloadFile. No access token found.");
			return "";
		}
		
		return sAccessToken;
	}
	
	/**
	 * Obtain the name of the file from the url. 
	 * In this case is one of the info passed in the url, comma separated, from the web server
	 * and it is in position 1.
	 */
	@Override
	public String getFileName(String sFileURL) throws Exception {
		
		String sResult = "";
		
		WasdiLog.debugLog("CreoDias2ProviderAdaper.getFileName. Retrieving the product's file name from URL: " + sFileURL);
		
		if (Utils.isNullOrEmpty(sFileURL)) {
			WasdiLog.errorLog("CreoDias2ProviderAdaper.getFileName: sFileURL is null or Empty");
			return sResult;
		}
		
		
		if (isHttpsProtocol(sFileURL)) {
			WasdiLog.debugLog("CreoDias2ProviderAdaper.getFileName: http protocol - Retrieve file at local path: " + sFileURL);
			try {			
				String[] asTokens = sFileURL.split(ResponseTranslatorCreoDias2.SLINK_SEPARATOR_CREODIAS2);
				sResult = asTokens[ResponseTranslatorCreoDias2.IPOSITIONOF_FILENAME];
			} catch (Exception oEx) {
				WasdiLog.errorLog("CreoDias2ProviderAdaper.getFileName: exception while trying to retrieve the file name." + oEx.getMessage());
				sResult = "";
			}
		} 
		else if (isFileProtocol(sFileURL)) {
			
			WasdiLog.debugLog("CreoDias2ProviderAdaper.getFileName: file protocol - Retrieve file at local path: " + sFileURL);

			String[] asParts = sFileURL.split("/");

			if (asParts != null && asParts.length > 1) {
				sResult = asParts[asParts.length-1];
			}
			
			WasdiLog.debugLog("CreoDias2ProviderAdaper.getFileName. File name without extension: " + sResult);
		}

		return sResult;
	}

	@Override
	protected int internalGetScoreForFile(String sFileName, String sPlatformType) {
		
		// returns the score auto-evaluated by the Provider Adapter to download sFileName of sPlatformType.
		boolean bOnCloud = isWorkspaceOnSameCloud();
		
		if (sPlatformType.equals(Platforms.SENTINEL1)) {
			String sType = MissionUtils.getProductTypeSatelliteImageFileName(sFileName);
			
			if (sType.equals("SLC") || sType.equals("GRD") || sType.equals("GRD-COG") || sType.equals("RTC")) {
				if (bOnCloud) return DataProviderScores.FILE_ACCESS.getValue();
				else return DataProviderScores.DOWNLOAD.getValue();
			}
			else if (sType.equals("RAW")) {
				Date oImageDate = MissionUtils.getDateFromSatelliteImageFileName(sFileName);
				
				if (oImageDate!=null) {
					Date oNow = new Date();
					
					long lDistance = oNow.getTime() - oImageDate.getTime();
					
					if (lDistance> 365*24*60*60*1000) {
						return -1;
					}				
					else {
						if (bOnCloud) return DataProviderScores.FILE_ACCESS.getValue();
						else return DataProviderScores.DOWNLOAD.getValue();					
					}					
				}
				else {
					return DataProviderScores.LTA.getValue();
				}
			}
			
			return DataProviderScores.LTA.getValue();
		}
		else if (sPlatformType.equals(Platforms.SENTINEL2)) {
			//String sType = WasdiFileUtils.getProductTypeSatelliteImageFileName(sFileName);
			if (bOnCloud) return DataProviderScores.FILE_ACCESS.getValue();
			else return DataProviderScores.DOWNLOAD.getValue();				
		}
		else if (sPlatformType.equals(Platforms.ENVISAT)) {
			if (sFileName.startsWith("ASA_")) {
				return -1;
			}
			if (bOnCloud) return DataProviderScores.FILE_ACCESS.getValue();
			else return DataProviderScores.DOWNLOAD.getValue();			
		}
		else if (sPlatformType.equals(Platforms.SENTINEL3) 
				|| sPlatformType.equals(Platforms.SENTINEL5P)
				|| sPlatformType.equals(Platforms.SENTINEL6)
				|| sPlatformType.equals(Platforms.LANDSAT8)
				|| sPlatformType.equals(Platforms.LANDSAT5)
				|| sPlatformType.equals(Platforms.LANDSAT7)) {
			if (bOnCloud) return DataProviderScores.FILE_ACCESS.getValue();
			else return DataProviderScores.DOWNLOAD.getValue();
		}
		
		return 0;
	}
	
}
