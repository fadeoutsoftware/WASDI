package wasdi.dataproviders;

import java.io.File;
import java.io.FileOutputStream;
import java.net.IDN;
import java.net.URI;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipOutputStream;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.base.Preconditions;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.queryexecutors.Platforms;
import wasdi.shared.queryexecutors.creodias2.ResponseTranslatorCreoDias2;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.WasdiFileUtils;
import wasdi.shared.utils.ZipFileUtils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;

public class CreoDias2ProviderAdapter extends ProviderAdapter {
	
	private static final String SAUTHENTICATION_URL = "https://identity.cloudferro.com/auth/realms/wekeo-elasticity/protocol/openid-connect/token";
	private static final String SDOWNLOAD_URL_END = "?token=";
	
	private static final String SODATA_FILE_URL_START = "https://datahub.creodias.eu/odata/v1/Products(";
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
	

	@Override
	public long getDownloadFileSize(String sFileURL) throws Exception {
		// receives the file URI and must return the size of the file. Useful to give progress to the user
		
		WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize. Retrieving information for product: " + sFileURL);
		
		long lSizeInBytes = 0L;
		
		if (isFileProtocol(m_sDefaultProtocol)) {
			String sPath = null;
			if (isFileProtocol(sFileURL)) {
				sPath = removePrefixFile(sFileURL);
				WasdiLog.debugLog("CREODIASProviderAdapter.getDownloadFileSize: file protocol detected. File path " + sFileURL);
			} else if(isHttpsProtocol(sFileURL)) {
				sPath = extractS3FilePath(sFileURL);
				WasdiLog.debugLog("CREODIASProviderAdapter.getDownloadFileSize: http protocol detected. File path " + sFileURL);
			} else {
				WasdiLog.debugLog("CREODIASProviderAdapter.getDownloadFileSize: unknown protocol " + sFileURL);
			}

			if (sPath != null) {
				File oSourceFile = new File(sPath);
				if (oSourceFile != null && oSourceFile.exists()) {
					lSizeInBytes = getSourceFileLength(oSourceFile);
					WasdiLog.debugLog("CREODIASProviderAdapter.getDownloadFileSize: detected file size: " + lSizeInBytes);
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
					return lSizeInBytes;
				}
			} catch (Exception oEx) {
				WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize. An exception occurred while retrieving the product information from the provider. " + oEx.getMessage());
				return lSizeInBytes;
			}
			
			WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize. Answer well received from the provider");			
			
			JSONObject oJsonBody = new JSONObject(sResponse.getResponseBody());
			JSONArray aoValues = oJsonBody.optJSONArray("value");
			if (aoValues == null || aoValues.length() != 1) {
				WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize. Array of attributes not found on CreoDias");
				return lSizeInBytes;
			}
			
			JSONObject oValue = (JSONObject) aoValues.get(0);
			String sFileSize = oValue.optString(ResponseTranslatorCreoDias2.SODATA_SIZE, "0");
			
			
			if (!Utils.isNullOrEmpty(sFileSize)) {
				try {
					lSizeInBytes = Long.parseLong(sFileSize);
				} catch(Exception oEx) {
					WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize. Exception while converting the file size from string to double. " + oEx.getMessage());
				}
			} else {
				WasdiLog.debugLog("CreoDias2ProviderAdaper.getDownloadFileSize. Array of attributes not found for key " + ResponseTranslatorCreoDias2.SODATA_SIZE);
			}
		}

		return lSizeInBytes;
		
	}
	
	
	/**
	 * Extracts the file-system path of the file out of the URL sent by the query executor (in the format "download_url,file_name,file:size,S3_path")
	 * @param sUrl the url string containing the S3 path to the file
	 * @return the file-system path (i.e. C:/temp/wasdi/eodata/Sentinel-1/SAR/GRD/2021/01/01/S1B_S1_GRDH_1SDH_20210101T152652_20210101T152706_024962_02F890_39B4.SAFE)
	 */
	private String extractS3FilePath(String sUrl) {
		Preconditions.checkNotNull(sUrl, "URL is null");
		String sFileSystemPath = null;
		
		try {
			String[] asParts = sUrl.split(ResponseTranslatorCreoDias2.SLINK_SEPARATOR_CREODIAS2);
			String sProductIdentifier = asParts[ResponseTranslatorCreoDias2.IPOSITIONOF_PRODUCTIDENTIFIER];		
			sFileSystemPath = m_sProviderBasePath + sProductIdentifier;
			WasdiLog.debugLog("CREODIASProviderAdapter.extractS3FilePath. S3 path: " + sProductIdentifier);
			WasdiLog.debugLog("CREODIASProviderAdapter.extractS3FilePath. file path: " + sFileSystemPath);
		} catch (Exception oE) {
			WasdiLog.errorLog("CREODIASProviderAdapter.extractS3FilePath. S3Path idenfier not found. " + oE);
		}

		return sFileSystemPath;
	}
	
	
	/**
	 * Get The length, in bytes, of the source-file.
	 * @param oSourceFile the source-file
	 * @return the length, in bytes, of the source-file, or 0L if the file does not exist
	 */
	protected long getSourceFileLength(File oSourceFile) {
		if (oSourceFile == null) {
			WasdiLog.debugLog("ProviderAdapter.getSourceFileLength: FILE DOES NOT EXISTS");
			return 0L;
		}
		
		WasdiLog.debugLog("ProviderAdapter.getSourceFileLength. Get file lengh for file " + oSourceFile.getAbsolutePath());

		long lLenght;
		if (oSourceFile.isDirectory()) {
			lLenght = 0L;

			for (File f : oSourceFile.listFiles()) {
				lLenght += getSourceFileLength(f);
			}
		} else {
			lLenght = oSourceFile.length();
		}
		
		if (!oSourceFile.exists()) {
			WasdiLog.debugLog("ProviderAdapter.getSourceFileLength. FILE DOES NOT EXISTS");
		}
		WasdiLog.debugLog("ProviderAdapter.getSourceFileLength. Found length " + lLenght);

		return lLenght;
	}
	
	
	
	@Override
	protected void internalReadConfig() {
		try {
			m_sDefaultProtocol = m_oDataProviderConfig.defaultProtocol; 
		} catch (Exception e) {
			WasdiLog.errorLog("CreoDias2ProvierAdapter: Config reader is null");
		}
	}
	
	
	/**
	 * From the Creodias URL for the download of a product, extracts the id of the product
	 * @param sFileUrl the Creodias URL for downloading a product, following the format https://datahub.creodias.eu/odata/v1/Products(some_product_id)/$value
	 * @return the id of the product
	 */
	private String getProductIdFromURL(String sFileUrl) {
		// url in input is something like: 
		return sFileUrl.replace(SODATA_FILE_URL_START, "").replace(SODATA_FILE_URL_END, "");
	}
	

	@Override
	public String executeDownloadFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {
		// main method. sFileUrl is the url received by the lined data provider. sSaveDirOnServer is the local folder. 
		// must return the valid file full path or "" if the download was not possible. 
		
		WasdiLog.debugLog("CreoDias2ProviderAdaper.executeDownloadFile. Execute download for file: " + sFileURL);
		
		
		String sResult = null;

		if (isFileProtocol(m_sDefaultProtocol)) {
			WasdiLog.debugLog("CreoDias2ProviderAdaper.executeDownloadFile: detected file protocol ");

			String sFilesystemPath = null;
			if (isFileProtocol(sFileURL)) {
				sFilesystemPath = removePrefixFile(sFileURL);
				WasdiLog.debugLog("CreoDias2ProviderAdaper.executeDownloadFile:  file system path from file protocol " + sFilesystemPath);
			} else if (isHttpsProtocol(sFileURL)) {
				sFilesystemPath = extractS3FilePath(sFileURL);
				WasdiLog.debugLog("CreoDias2ProviderAdaper.executeDownloadFile:  file system path from http protocol " + sFilesystemPath);
			} else {
				WasdiLog.debugLog("CreoDias2ProviderAdaper.executeDownloadFile: unknown protocol " + sFileURL);
			}

			if (sFilesystemPath != null) {
				File oSourceFile = new File(sFilesystemPath);

				if (oSourceFile != null && oSourceFile.exists()) {
					sResult = copyFile("file:" + sFilesystemPath, sDownloadUser, sDownloadPassword, sSaveDirOnServer, oProcessWorkspace, iMaxRetry);
					WasdiLog.debugLog("CreoDias2ProviderAdaper.executeDownloadFile: destination file path " + sResult);

					return sResult;
				}
			}
			WasdiLog.debugLog("CreoDias2ProviderAdaper.executeDownloadFile: no operation was effective for file protocol");
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
					return sResult;
				}
				
				WasdiLog.debugLog("CreoDias2ProviderAdaper.executeDownloadFile. Access token correctly received.");
				String sODataDownloadUrl = getODataDownloadUrl(sFileURL);
				String sDownloadUrl = sODataDownloadUrl + SDOWNLOAD_URL_END + sAccessToken;
				
				WasdiLog.debugLog("CreoDias2ProviderAdaper.executeDownloadFile. Access token correctly received. Download url (withouth accesso token): " + sODataDownloadUrl + SDOWNLOAD_URL_END);
				
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
					WasdiLog.debugLog("CreoDias2ProviderAdapter.executeDownloadFile. Download completed: " + sDownloadedFilePath);
					sResult = sDownloadedFilePath;
					break;
				}
			}
		}
		return sResult;
	}
	
	
	/**
	 * From the link that was created in the Creodias' response translator, extracts the part related to the URL for downloading the product
	 * @param sFileURL the link following the pattern "download_url,file_name,file:size,S3_path"
	 * @return the URL for downloading the product if present. An empty string otherwise. 
	 */
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
	
	
	/**
	 * Copies the file specified at sFileURL to the destination specified by sSaveDirOnServer
	 * @param sFileURL file system path to the file to copy
	 * @param sDownloadUser user name 
	 * @param sDownloadPassword password
	 * @param sSaveDirOnServer destination folder
	 * @param oProcessWorkspace process workspace
	 * @param iMaxRetry maximum number of times the operation should be tried
	 * @return the path to the file in the destination folder. If the path of the file or the destination folder are not found in the file system then it returns an empty string.
	 * @throws Exception
	 */
	private String copyFile(String sFileURL, String sDownloadUser, String sDownloadPassword,
			String sSaveDirOnServer, ProcessWorkspace oProcessWorkspace, int iMaxRetry) throws Exception {

		String sResult = "";
		// Domain check
		if (Utils.isNullOrEmpty(sFileURL)) {
			WasdiLog.debugLog("CREODIASProviderAdapter.ExecuteDownloadFile: sFileURL is null");
			return sResult;
		}
		if (Utils.isNullOrEmpty(sSaveDirOnServer)) {
			WasdiLog.debugLog("CREODIASProviderAdapter.ExecuteDownloadFile: sSaveDirOnServer is null");
			return sResult;
		}
		
		WasdiLog.debugLog("CREODIASProviderAdapter.ExecuteDownloadFile: start");
		
		setProcessWorkspace(oProcessWorkspace);

		if (isFileProtocol(sFileURL)) {
			if (isZipFile(sFileURL)) {
				sResult = localFileCopy(sFileURL, sSaveDirOnServer, iMaxRetry);
			} else if (isSafeDirectory(sFileURL)) {
				String sourceFile = removePrefixFile(sFileURL);
				String destinationFile = getFileName(sFileURL);

				// set the destination folder
				if (sSaveDirOnServer.endsWith("/") == false) sSaveDirOnServer += "/";
				destinationFile = sSaveDirOnServer + destinationFile;

				destinationFile = addZipExtension(removeSafeTermination(destinationFile));

				downloadZipFile(sourceFile, destinationFile);
				
				sResult = destinationFile;
			}
		} 

		return sResult;
	}
	
	

	private void downloadZipFile(String sourceFile, String destinationFile) throws Exception {
        FileOutputStream fos = new FileOutputStream(destinationFile);
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        File fileToZip = new File(sourceFile);

        ZipFileUtils.zipFile(fileToZip, fileToZip.getName(), zipOut);
        zipOut.close();
        fos.close();
	}
	
	
	
	/**
	 * Retrieves the authentication token to be added to the request for downloading a product from Creodias. 
	 * @param sUsername user name
	 * @param sPassword password
	 * @return the token to be added to the download request, if the authentication was successful. An empty string, otherwise.
	 */
	private String getAuthenticationToken(String sUsername, String sPassword) {
		String sPayload = "client_id=CLOUDFERRO_PUBLIC&password=" + sPassword + "&username=" + sUsername + "&grant_type=password";
		HttpCallResponse oResponse = null;
		String sAccessToken = "";
		try {
			oResponse = HttpUtils.httpPost(SAUTHENTICATION_URL, sPayload);
			if (oResponse == null ||  oResponse.getResponseCode() < 200 || oResponse.getResponseCode() > 299) {
				WasdiLog.debugLog("CreoDias2ProviderAdaper.getAuthenticationToken. Error code while trying to retrieve the auth token.");
				return sAccessToken;
			} 
		} catch (Exception oEx) {
			WasdiLog.debugLog("CreoDias2ProviderAdaper.getAuthenticationToken. Error while trying to retrieve the authentication token." + oEx.getMessage());
			return sAccessToken;
		}
		
		JSONObject oResponseBody = new JSONObject(oResponse.getResponseBody());
		sAccessToken = oResponseBody.optString("access_token", "");
		
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
	 * @param sFileURL the url as passed from the web server, in the format "download_url,file_name,file:size,S3_path"
	 * @return 
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
			String sType = WasdiFileUtils.getProductTypeSatelliteImageFileName(sFileName);
			
			if (sType.equals("SLC") || sType.equals("GRD") || sType.equals("GRD-COG") || sType.equals("RTC")) {
				if (bOnCloud) return DataProviderScores.FILE_ACCESS.getValue();
				else return DataProviderScores.DOWNLOAD.getValue();
			}
			else if (sType.equals("RAW")) {
				Date oImageDate = WasdiFileUtils.getDateFromSatelliteImageFileName(sFileName);
				
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
			
			return DataProviderScores.LTA.getValue();
		}
		else if (sPlatformType.equals(Platforms.SENTINEL2)) {
			String sType = WasdiFileUtils.getProductTypeSatelliteImageFileName(sFileName);
			
			
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
				|| sPlatformType.equals(Platforms.LANDSAT5)
				|| sPlatformType.equals(Platforms.LANDSAT7)
				|| sPlatformType.equals(Platforms.LANDSAT8)
				|| sPlatformType.equals(Platforms.SMOS)) {
			if (bOnCloud) return DataProviderScores.FILE_ACCESS.getValue();
			else return DataProviderScores.DOWNLOAD.getValue();
		}
		
		return 0;
	}
	
}
